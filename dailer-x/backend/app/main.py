from datetime import datetime, timedelta, timezone
from typing import Literal
import hashlib
import hmac
from contextlib import asynccontextmanager
import os
import re
import sqlite3
from pathlib import Path
from fastapi import FastAPI, Header, HTTPException, Query, Request
from pydantic import BaseModel, Field

APP_DIR = Path(__file__).resolve().parent
DB_PATH = Path(os.getenv("DAILERX_DB", str(APP_DIR / "dailerx.db")))
ADMIN_KEY = os.getenv("DAILERX_ADMIN_KEY", "").strip()
REPORT_SALT = os.getenv("DAILERX_REPORT_SALT", "local-development-only-change-me")
REPORTS_PER_INSTALL_PER_DAY = int(os.getenv("DAILERX_REPORTS_PER_INSTALL_PER_DAY", "10"))
REPORTS_PER_NETWORK_PER_DAY = int(os.getenv("DAILERX_REPORTS_PER_NETWORK_PER_DAY", "50"))
SPAM_BLOCK_THRESHOLD = int(os.getenv("DAILERX_SPAM_BLOCK_THRESHOLD", "5"))


@asynccontextmanager
async def lifespan(app: FastAPI):
    init_db()
    yield


app = FastAPI(
    title="Dailer X API",
    version="0.3.0",
    description="Open-source caller intelligence MVP API",
    lifespan=lifespan,
)

DEMO_BUSINESSES = [
    {"number": "+14155550100", "name": "Northstar Sample Cafe", "category": "Restaurant", "source": "demo_seed", "verified": False},
    {"number": "+14155550101", "name": "Example Parcel Desk", "category": "Delivery", "source": "demo_seed", "verified": False},
    {"number": "+442079460000", "name": "Example Office Listing", "category": "Business", "source": "demo_seed", "verified": False},
]


def connect():
    conn = sqlite3.connect(DB_PATH)
    conn.row_factory = sqlite3.Row
    return conn


def normalize(number: str) -> str:
    raw = number.strip()
    if not raw:
        raise HTTPException(400, "Phone number is required")
    if not re.fullmatch(r"[+0-9().\-\s]{3,32}", raw):
        raise HTTPException(400, "Invalid characters in phone number")
    digits = re.sub(r"\D", "", raw)
    if len(digits) < 3 or len(digits) > 15:
        raise HTTPException(400, "Phone number must contain 3 to 15 digits")
    return ("+" if raw.lstrip().startswith("+") else "") + digits


def init_db():
    DB_PATH.parent.mkdir(parents=True, exist_ok=True)
    with connect() as db:
        db.execute("""CREATE TABLE IF NOT EXISTS business_listings (
            id INTEGER PRIMARY KEY, number TEXT NOT NULL, name TEXT NOT NULL,
            category TEXT NOT NULL DEFAULT 'Business', source TEXT NOT NULL,
            verified INTEGER NOT NULL DEFAULT 0, updated_at TEXT NOT NULL,
            UNIQUE(number, name, source))""")
        db.execute("""CREATE TABLE IF NOT EXISTS spam_reports (
            id INTEGER PRIMARY KEY, number TEXT NOT NULL, category TEXT NOT NULL,
            details TEXT NOT NULL DEFAULT '', created_at TEXT NOT NULL,
            reporter_hash TEXT NOT NULL DEFAULT '', network_hash TEXT NOT NULL DEFAULT '')""")
        # Lightweight migration for databases created by earlier MVP revisions.
        columns = {row[1] for row in db.execute("PRAGMA table_info(spam_reports)").fetchall()}
        if "reporter_hash" not in columns:
            db.execute("ALTER TABLE spam_reports ADD COLUMN reporter_hash TEXT NOT NULL DEFAULT ''")
        if "network_hash" not in columns:
            db.execute("ALTER TABLE spam_reports ADD COLUMN network_hash TEXT NOT NULL DEFAULT ''")
        db.execute("CREATE INDEX IF NOT EXISTS idx_business_number ON business_listings(number)")
        db.execute("CREATE INDEX IF NOT EXISTS idx_business_name ON business_listings(name)")
        db.execute("CREATE INDEX IF NOT EXISTS idx_reports_number_time ON spam_reports(number, created_at)")
        db.execute("CREATE INDEX IF NOT EXISTS idx_reports_reporter_time ON spam_reports(reporter_hash, created_at)")
        db.execute("CREATE INDEX IF NOT EXISTS idx_reports_network_time ON spam_reports(network_hash, created_at)")
        for item in DEMO_BUSINESSES:
            db.execute("INSERT OR IGNORE INTO business_listings(number,name,category,source,verified,updated_at) VALUES(?,?,?,?,?,?)",
                       (item["number"], item["name"], item["category"], item["source"], int(item["verified"]), datetime.now(timezone.utc).isoformat()))


class SpamReport(BaseModel):
    number: str = Field(min_length=3, max_length=32)
    category: Literal["telemarketing", "suspected_fraud", "nuisance", "other"]
    details: str = Field(default="", max_length=500)


class BusinessImport(BaseModel):
    number: str = Field(min_length=3, max_length=32)
    name: str = Field(min_length=1, max_length=160)
    category: str = Field(default="Business", max_length=80)
    source: str = Field(min_length=2, max_length=160)
    source_license: str = Field(min_length=2, max_length=300)


@app.get("/health")
def health():
    try:
        with connect() as db:
            db.execute("SELECT 1").fetchone()
        return {"status": "ok", "database": "ok", "app": "Dailer X API", "mode": "MVP; seed records are synthetic demo data"}
    except sqlite3.Error:
        raise HTTPException(503, "Database unavailable")


@app.get("/v1/lookup")
def lookup(number: str = Query(min_length=3, max_length=32)):
    normalized = normalize(number)
    with connect() as db:
        businesses = [dict(row) for row in db.execute(
            "SELECT name, number, category, source, verified, updated_at FROM business_listings WHERE number = ? ORDER BY verified DESC, name ASC", (normalized,)).fetchall()]
        reports = db.execute(
            "SELECT category, COUNT(DISTINCT reporter_hash) AS count FROM spam_reports WHERE number = ? AND created_at >= ? GROUP BY category",
            (normalized, (datetime.now(timezone.utc) - timedelta(days=30)).isoformat()),
        ).fetchall()
        total_reports = db.execute(
            "SELECT COUNT(DISTINCT reporter_hash) FROM spam_reports WHERE number = ? AND created_at >= ?",
            (normalized, (datetime.now(timezone.utc) - timedelta(days=30)).isoformat()),
        ).fetchone()[0]
    report_counts = {row["category"]: row["count"] for row in reports}
    confidence = "high" if total_reports >= 10 else "medium" if total_reports >= 5 else "low"
    return {
        "number": normalized,
        "matches": businesses,
        "spam": {
            "reports": total_reports,
            "categories": report_counts,
            "confidence": confidence,
            "recommended_action": "block" if total_reports >= SPAM_BLOCK_THRESHOLD else "review",
            "window_days": 30,
        },
        "notice": "Reports are unverified signals. A listing or report is not proof of caller identity or wrongdoing.",
    }


@app.post("/v1/reports/spam", status_code=201)
def create_spam_report(report: SpamReport, request: Request, x_install_token: str | None = Header(default=None)):
    normalized = normalize(report.number)
    # Prefer a random per-install token generated by the app; fall back to network identity for basic abuse throttling.
    # Only a salted one-way digest is stored. This is a coarse anti-abuse signal, not identity verification.
    token = (x_install_token or "").strip()
    if token and not re.fullmatch(r"[A-Za-z0-9._:-]{16,128}", token):
        raise HTTPException(400, "Invalid installation token")
    source_ip = request.client.host if request.client else "unknown"
    reporter_identity = f"install:{token}" if token else f"network:{source_ip}"
    reporter_hash = hashlib.sha256(f"{REPORT_SALT}:{reporter_identity}".encode("utf-8")).hexdigest()
    network_hash = hashlib.sha256(f"{REPORT_SALT}:network:{source_ip}".encode("utf-8")).hexdigest()
    now = datetime.now(timezone.utc)
    cutoff = (now - timedelta(hours=24)).isoformat()
    day_cutoff = (now - timedelta(days=1)).isoformat()
    with connect() as db:
        # Serialize check-and-insert so concurrent retries cannot create duplicate votes.
        db.execute("BEGIN IMMEDIATE")
        duplicate = db.execute(
            "SELECT id FROM spam_reports WHERE number = ? AND reporter_hash = ? AND created_at >= ? LIMIT 1",
            (normalized, reporter_hash, cutoff),
        ).fetchone()
        if duplicate:
            count = db.execute(
                "SELECT COUNT(DISTINCT reporter_hash) FROM spam_reports WHERE number = ? AND created_at >= ?",
                (normalized, (now - timedelta(days=30)).isoformat()),
            ).fetchone()[0]
            db.commit()
            return {"accepted": False, "number": normalized, "report_count": count,
                    "message": "A report for this number from this installation/network was already received in the last 24 hours."}
        daily_count = db.execute(
            "SELECT COUNT(*) FROM spam_reports WHERE reporter_hash = ? AND created_at >= ?",
            (reporter_hash, day_cutoff),
        ).fetchone()[0]
        if daily_count >= REPORTS_PER_INSTALL_PER_DAY:
            db.rollback()
            raise HTTPException(429, "Daily spam-report limit reached for this installation")
        network_daily_count = db.execute(
            "SELECT COUNT(*) FROM spam_reports WHERE network_hash = ? AND created_at >= ?",
            (network_hash, day_cutoff),
        ).fetchone()[0]
        if network_daily_count >= REPORTS_PER_NETWORK_PER_DAY:
            db.rollback()
            raise HTTPException(429, "Daily spam-report limit reached for this network")
        # Keep user-supplied details private; never expose them through lookup/search endpoints.
        db.execute(
            "INSERT INTO spam_reports(number,category,details,created_at,reporter_hash,network_hash) VALUES(?,?,?,?,?,?)",
            (normalized, report.category, report.details.strip(), now.isoformat(), reporter_hash, network_hash),
        )
        count = db.execute(
            "SELECT COUNT(DISTINCT reporter_hash) FROM spam_reports WHERE number = ? AND created_at >= ?",
            (normalized, (now - timedelta(days=30)).isoformat()),
        ).fetchone()[0]
        db.commit()
    return {"accepted": True, "number": normalized, "report_count": count,
            "recommended_action": "block" if count >= SPAM_BLOCK_THRESHOLD else "review",
            "message": "Report received. Reports are unverified and may be moderated."}


@app.post("/v1/businesses", status_code=201)
def import_business(item: BusinessImport, x_admin_key: str | None = Header(default=None)):
    # A blank admin key disables public imports by default. Configure it only for trusted ingestion.
    if not ADMIN_KEY:
        raise HTTPException(503, "Business imports are disabled until DAILERX_ADMIN_KEY is configured")
    if not x_admin_key or not hmac.compare_digest(x_admin_key, ADMIN_KEY):
        raise HTTPException(401, "Valid X-Admin-Key header required")
    normalized = normalize(item.number)
    if not item.name.strip() or not item.category.strip() or not item.source.strip() or not item.source_license.strip():
        raise HTTPException(400, "Name, category, source, and source license must not be blank")
    if item.source.strip() == "demo_seed":
        raise HTTPException(400, "Reserved source label")
    with connect() as db:
        cursor = db.execute("INSERT OR IGNORE INTO business_listings(number,name,category,source,verified,updated_at) VALUES(?,?,?,?,0,?)",
                   (normalized, item.name.strip(), item.category.strip(), f"{item.source.strip()} | license: {item.source_license.strip()}", datetime.now(timezone.utc).isoformat()))
    return {"accepted": True, "inserted": cursor.rowcount == 1, "number": normalized, "notice": "Listing is unverified. Only import data you are licensed to reuse."}


@app.get("/v1/search")
def search(q: str = Query(min_length=2, max_length=100), limit: int = Query(default=20, ge=1, le=50)):
    term = q.strip()
    with connect() as db:
        rows = db.execute("SELECT name, number, category, source, verified FROM business_listings WHERE name LIKE ? OR number LIKE ? ORDER BY verified DESC, name LIMIT ?",
                          (f"%{term}%", f"%{term}%", limit)).fetchall()
    return {"query": term, "results": [dict(r) for r in rows], "notice": "Only imported public business listings are searchable."}

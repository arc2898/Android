import os
import sys
import tempfile
from pathlib import Path

sys.path.insert(0, str(Path(__file__).resolve().parents[1]))
_tmp = tempfile.NamedTemporaryFile(delete=False)
os.environ["DAILERX_DB"] = _tmp.name
_tmp.close()
os.environ.setdefault("DAILERX_REPORT_SALT", "pytest-only-salt")

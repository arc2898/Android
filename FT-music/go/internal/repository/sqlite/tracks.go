// FT-music is an open-source music application.
// Copyright (C) 2026 FT-music contributors
// SPDX-License-Identifier: GPL-2.0-or-later

package sqlite

import (
	"database/sql"
	"github.com/ft-music/ft-music/internal/domain"
	"github.com/ft-music/ft-music/internal/repository"
	_ "modernc.org/sqlite"
	"strings"
	"time"
)

const schema = `CREATE TABLE IF NOT EXISTS tracks (id TEXT PRIMARY KEY, title TEXT NOT NULL, artist TEXT NOT NULL, album TEXT NOT NULL DEFAULT '', duration_seconds INTEGER NOT NULL DEFAULT 0, artwork_url TEXT NOT NULL DEFAULT '', created_at TEXT NOT NULL); CREATE INDEX IF NOT EXISTS idx_tracks_search ON tracks(title, artist, album);`

type Tracks struct{ db *sql.DB }

func Open(path string) (*Tracks, error) {
	db, err := sql.Open("sqlite", path)
	if err != nil {
		return nil, err
	}
	if _, err = db.Exec(schema); err != nil {
		_ = db.Close()
		return nil, err
	}
	return &Tracks{db: db}, nil
}
func (r *Tracks) Close() error { return r.db.Close() }
func (r *Tracks) Add(t domain.Track) (domain.Track, error) {
	if t.CreatedAt.IsZero() {
		t.CreatedAt = time.Now().UTC()
	}
	_, err := r.db.Exec(`INSERT INTO tracks (id,title,artist,album,duration_seconds,artwork_url,created_at) VALUES (?,?,?,?,?,?,?) ON CONFLICT(id) DO UPDATE SET title=excluded.title,artist=excluded.artist,album=excluded.album,duration_seconds=excluded.duration_seconds,artwork_url=excluded.artwork_url`, t.ID, t.Title, t.Artist, t.Album, t.Duration, t.ArtworkURL, t.CreatedAt.Format(time.RFC3339Nano))
	return t, err
}
func (r *Tracks) List() ([]domain.Track, error) {
	return r.query(`SELECT id,title,artist,album,duration_seconds,artwork_url,created_at FROM tracks ORDER BY created_at ASC`)
}
func (r *Tracks) Search(q string) ([]domain.Track, error) {
	q = strings.TrimSpace(q)
	if q == "" {
		return r.List()
	}
	pattern := "%" + q + "%"
	return r.query(`SELECT id,title,artist,album,duration_seconds,artwork_url,created_at FROM tracks WHERE title LIKE ? OR artist LIKE ? OR album LIKE ? ORDER BY created_at ASC`, pattern, pattern, pattern)
}
func (r *Tracks) Get(id string) (domain.Track, error) {
	var t domain.Track
	var created string
	err := r.db.QueryRow(`SELECT id,title,artist,album,duration_seconds,artwork_url,created_at FROM tracks WHERE id=?`, id).Scan(&t.ID, &t.Title, &t.Artist, &t.Album, &t.Duration, &t.ArtworkURL, &created)
	if err == sql.ErrNoRows {
		return domain.Track{}, repository.ErrNotFound
	}
	if err != nil {
		return domain.Track{}, err
	}
	t.CreatedAt, err = time.Parse(time.RFC3339Nano, created)
	return t, err
}
func (r *Tracks) query(q string, args ...any) ([]domain.Track, error) {
	rows, err := r.db.Query(q, args...)
	if err != nil {
		return nil, err
	}
	defer rows.Close()
	out := make([]domain.Track, 0)
	for rows.Next() {
		var t domain.Track
		var created string
		if err := rows.Scan(&t.ID, &t.Title, &t.Artist, &t.Album, &t.Duration, &t.ArtworkURL, &created); err != nil {
			return nil, err
		}
		t.CreatedAt, err = time.Parse(time.RFC3339Nano, created)
		if err != nil {
			return nil, err
		}
		out = append(out, t)
	}
	return out, rows.Err()
}

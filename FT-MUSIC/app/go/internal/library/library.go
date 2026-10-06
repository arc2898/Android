// FT-music is a reimplementation inspired by the BloomeeTunes feature set.
// Copyright (C) 2026 FT-music contributors
// SPDX-License-Identifier: GPL-2.0-or-later

package library

import (
	"errors"
	"sort"
	"strings"
	"sync"
	"time"
)

var ErrNotFound = errors.New("track not found")

type Track struct {
	ID         string    `json:"id"`
	Title      string    `json:"title"`
	Artist     string    `json:"artist"`
	Album      string    `json:"album,omitempty"`
	Duration   int       `json:"duration_seconds,omitempty"`
	ArtworkURL string    `json:"artwork_url,omitempty"`
	CreatedAt  time.Time `json:"created_at"`
}

type Library struct {
	mu     sync.RWMutex
	tracks map[string]Track
}

func New() *Library { return &Library{tracks: make(map[string]Track)} }

func (l *Library) Add(t Track) Track {
	if t.CreatedAt.IsZero() {
		t.CreatedAt = time.Now().UTC()
	}
	l.mu.Lock()
	defer l.mu.Unlock()
	l.tracks[t.ID] = t
	return t
}

func (l *Library) List() []Track {
	l.mu.RLock()
	defer l.mu.RUnlock()
	out := make([]Track, 0, len(l.tracks))
	for _, t := range l.tracks {
		out = append(out, t)
	}
	sort.Slice(out, func(i, j int) bool { return out[i].CreatedAt.Before(out[j].CreatedAt) })
	return out
}

func (l *Library) Search(query string) []Track {
	q := strings.ToLower(strings.TrimSpace(query))
	if q == "" {
		return l.List()
	}
	var out []Track
	for _, t := range l.List() {
		if strings.Contains(strings.ToLower(t.Title), q) || strings.Contains(strings.ToLower(t.Artist), q) || strings.Contains(strings.ToLower(t.Album), q) {
			out = append(out, t)
		}
	}
	return out
}

func (l *Library) Get(id string) (Track, error) {
	l.mu.RLock()
	defer l.mu.RUnlock()
	t, ok := l.tracks[id]
	if !ok {
		return Track{}, ErrNotFound
	}
	return t, nil
}

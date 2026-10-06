// FT-music is an open-source music application.
// Copyright (C) 2026 FT-music contributors
// SPDX-License-Identifier: GPL-2.0-or-later

package memory

import (
	"github.com/ft-music/ft-music/internal/domain"
	"github.com/ft-music/ft-music/internal/repository"
	"sort"
	"strings"
	"sync"
	"time"
)

type Tracks struct {
	mu     sync.RWMutex
	tracks map[string]domain.Track
}

func NewTracks() *Tracks { return &Tracks{tracks: make(map[string]domain.Track)} }
func (r *Tracks) Add(t domain.Track) (domain.Track, error) {
	if t.CreatedAt.IsZero() {
		t.CreatedAt = time.Now().UTC()
	}
	r.mu.Lock()
	defer r.mu.Unlock()
	r.tracks[t.ID] = t
	return t, nil
}
func (r *Tracks) List() ([]domain.Track, error) {
	r.mu.RLock()
	defer r.mu.RUnlock()
	out := make([]domain.Track, 0, len(r.tracks))
	for _, t := range r.tracks {
		out = append(out, t)
	}
	sort.Slice(out, func(i, j int) bool { return out[i].CreatedAt.Before(out[j].CreatedAt) })
	return out, nil
}
func (r *Tracks) Search(query string) ([]domain.Track, error) {
	q := strings.ToLower(strings.TrimSpace(query))
	all, _ := r.List()
	if q == "" {
		return all, nil
	}
	out := make([]domain.Track, 0)
	for _, t := range all {
		if strings.Contains(strings.ToLower(t.Title), q) || strings.Contains(strings.ToLower(t.Artist), q) || strings.Contains(strings.ToLower(t.Album), q) {
			out = append(out, t)
		}
	}
	return out, nil
}
func (r *Tracks) Get(id string) (domain.Track, error) {
	r.mu.RLock()
	defer r.mu.RUnlock()
	t, ok := r.tracks[id]
	if !ok {
		return domain.Track{}, repository.ErrNotFound
	}
	return t, nil
}

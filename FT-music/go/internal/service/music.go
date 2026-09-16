// FT-music is an open-source music application.
// Copyright (C) 2026 FT-music contributors
// SPDX-License-Identifier: GPL-2.0-or-later

package service

import (
	"context"
	"github.com/ft-music/ft-music/internal/domain"
	"github.com/ft-music/ft-music/internal/repository"
	"github.com/ft-music/ft-music/internal/streaming"
	"strings"
)

type Music struct {
	tracks    repository.TrackRepository
	streaming *streaming.Catalog
}

func NewMusic(tracks repository.TrackRepository, catalog *streaming.Catalog) *Music {
	return &Music{tracks: tracks, streaming: catalog}
}
func (m *Music) AddTrack(t domain.Track) (domain.Track, error) { return m.tracks.Add(t) }
func (m *Music) Library(query string) ([]domain.Track, error)  { return m.tracks.Search(query) }
func (m *Music) Track(id string) (domain.Track, error)         { return m.tracks.Get(id) }
func (m *Music) SearchRemote(ctx context.Context, query string) ([]domain.Track, error) {
	if strings.TrimSpace(query) == "" {
		return nil, streaming.ErrProviderUnavailable
	}
	for _, provider := range m.streaming.Providers() {
		tracks, err := provider.Search(ctx, query)
		if err == nil {
			return tracks, nil
		}
	}
	return nil, streaming.ErrProviderUnavailable
}

// FT-music is an open-source music application.
// Copyright (C) 2026 FT-music contributors
// SPDX-License-Identifier: GPL-2.0-or-later

package repository

import (
	"errors"
	"github.com/ft-music/ft-music/internal/domain"
)

var ErrNotFound = errors.New("track not found")

type TrackRepository interface {
	Add(domain.Track) (domain.Track, error)
	List() ([]domain.Track, error)
	Search(query string) ([]domain.Track, error)
	Get(id string) (domain.Track, error)
}

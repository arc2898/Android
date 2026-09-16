// FT-music is an open-source music application.
// Copyright (C) 2026 FT-music contributors
// SPDX-License-Identifier: GPL-2.0-or-later

package domain

import "time"

type Track struct {
	ID         string    `json:"id"`
	Title      string    `json:"title"`
	Artist     string    `json:"artist"`
	Album      string    `json:"album,omitempty"`
	Duration   int       `json:"duration_seconds,omitempty"`
	ArtworkURL string    `json:"artwork_url,omitempty"`
	CreatedAt  time.Time `json:"created_at"`
}

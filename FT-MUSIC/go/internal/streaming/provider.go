// FT-music is an open-source music application.
// Copyright (C) 2026 FT-music contributors
// SPDX-License-Identifier: GPL-2.0-or-later

package streaming

import (
	"context"
	"errors"

	"github.com/ft-music/ft-music/internal/domain"
)

var ErrProviderUnavailable = errors.New("streaming provider unavailable")

// Provider is the only boundary the application uses for remote music.
// Implementations must use documented, authorized provider APIs and respect
// their terms, licenses, rate limits, and content restrictions.
type Provider interface {
	Name() string
	Search(context.Context, string) ([]domain.Track, error)
	StreamURL(context.Context, string) (string, error)
}

type Catalog struct{ providers []Provider }

func NewCatalog(providers ...Provider) *Catalog { return &Catalog{providers: providers} }
func (c *Catalog) Providers() []Provider        { return append([]Provider(nil), c.providers...) }

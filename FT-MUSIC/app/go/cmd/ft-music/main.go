// FT-music is a reimplementation inspired by the BloomeeTunes feature set.
// Copyright (C) 2026 FT-music contributors
// SPDX-License-Identifier: GPL-2.0-or-later

package main

import (
	"log"
	"net/http"
	"os"

	"github.com/ft-music/ft-music/internal/library"
	"github.com/ft-music/ft-music/internal/server"
)

func main() {
	addr := os.Getenv("FT_MUSIC_ADDR")
	if addr == "" {
		addr = ":8080"
	}
	lib := library.New()
	h := server.New(lib).Routes()
	log.Printf("FT-music API listening on %s", addr)
	if err := http.ListenAndServe(addr, h); err != nil {
		log.Fatal(err)
	}
}

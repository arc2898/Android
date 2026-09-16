// FT-music is an open-source music application.
// Copyright (C) 2026 FT-music contributors
// SPDX-License-Identifier: GPL-2.0-or-later

package main

import (
	"github.com/ft-music/ft-music/internal/repository/sqlite"
	"github.com/ft-music/ft-music/internal/server"
	"github.com/ft-music/ft-music/internal/service"
	"github.com/ft-music/ft-music/internal/streaming"
	"log"
	"net/http"
	"os"
)

func main() {
	addr := os.Getenv("FT_MUSIC_ADDR")
	if addr == "" {
		addr = ":8080"
	}
	dbPath := os.Getenv("FT_MUSIC_DB")
	if dbPath == "" {
		dbPath = "ft-music.db"
	}
	tracks, err := sqlite.Open(dbPath)
	if err != nil {
		log.Fatal(err)
	}
	defer tracks.Close()
	music := service.NewMusic(tracks, streaming.NewCatalog())
	log.Printf("FT-music API listening on %s", addr)
	if err := http.ListenAndServe(addr, server.New(music).Routes()); err != nil {
		log.Fatal(err)
	}
}

// FT-music is a reimplementation inspired by the BloomeeTunes feature set.
// Copyright (C) 2026 FT-music contributors
// SPDX-License-Identifier: GPL-2.0-or-later

package server

import (
	"encoding/json"
	"errors"
	"net/http"
	"strings"

	"github.com/ft-music/ft-music/internal/library"
)

type Server struct{ library *library.Library }

func New(lib *library.Library) *Server { return &Server{library: lib} }

func (s *Server) Routes() http.Handler {
	mux := http.NewServeMux()
	mux.HandleFunc("GET /api/v1/health", s.health)
	mux.HandleFunc("GET /api/v1/library", s.listLibrary)
	mux.HandleFunc("GET /api/v1/library/{id}", s.getTrack)
	mux.HandleFunc("POST /api/v1/library", s.addTrack)
	return logging(mux)
}

func (s *Server) health(w http.ResponseWriter, _ *http.Request) {
	writeJSON(w, http.StatusOK, map[string]string{"service": "ft-music", "status": "ok"})
}

func (s *Server) listLibrary(w http.ResponseWriter, r *http.Request) {
	writeJSON(w, http.StatusOK, s.library.Search(r.URL.Query().Get("q")))
}

func (s *Server) getTrack(w http.ResponseWriter, r *http.Request) {
	t, err := s.library.Get(r.PathValue("id"))
	if errors.Is(err, library.ErrNotFound) {
		writeError(w, http.StatusNotFound, "track not found")
		return
	}
	writeJSON(w, http.StatusOK, t)
}

func (s *Server) addTrack(w http.ResponseWriter, r *http.Request) {
	var t library.Track
	if err := json.NewDecoder(r.Body).Decode(&t); err != nil || strings.TrimSpace(t.ID) == "" || strings.TrimSpace(t.Title) == "" {
		writeError(w, http.StatusBadRequest, "id and title are required")
		return
	}
	writeJSON(w, http.StatusCreated, s.library.Add(t))
}

func logging(next http.Handler) http.Handler {
	return http.HandlerFunc(func(w http.ResponseWriter, r *http.Request) { next.ServeHTTP(w, r) })
}
func writeJSON(w http.ResponseWriter, status int, value any) {
	w.Header().Set("Content-Type", "application/json")
	w.WriteHeader(status)
	_ = json.NewEncoder(w).Encode(value)
}
func writeError(w http.ResponseWriter, status int, message string) {
	writeJSON(w, status, map[string]string{"error": message})
}

// FT-music is an open-source music application.
// Copyright (C) 2026 FT-music contributors
// SPDX-License-Identifier: GPL-2.0-or-later

package server

import (
	"encoding/json"
	"errors"
	"github.com/ft-music/ft-music/internal/domain"
	"github.com/ft-music/ft-music/internal/repository"
	"github.com/ft-music/ft-music/internal/service"
	"net/http"
	"strings"
)

type Server struct{ music *service.Music }

func New(music *service.Music) *Server { return &Server{music: music} }
func (s *Server) Routes() http.Handler {
	mux := http.NewServeMux()
	mux.HandleFunc("GET /api/v1/health", s.health)
	mux.HandleFunc("GET /api/v1/library", s.listLibrary)
	mux.HandleFunc("GET /api/v1/library/{id}", s.getTrack)
	mux.HandleFunc("POST /api/v1/library", s.addTrack)
	return mux
}
func (s *Server) health(w http.ResponseWriter, _ *http.Request) {
	writeJSON(w, http.StatusOK, map[string]string{"service": "ft-music", "status": "ok"})
}
func (s *Server) listLibrary(w http.ResponseWriter, r *http.Request) {
	tracks, err := s.music.Library(r.URL.Query().Get("q"))
	if err != nil {
		writeError(w, http.StatusInternalServerError, "library unavailable")
		return
	}
	writeJSON(w, http.StatusOK, tracks)
}
func (s *Server) getTrack(w http.ResponseWriter, r *http.Request) {
	t, err := s.music.Track(r.PathValue("id"))
	if errors.Is(err, repository.ErrNotFound) {
		writeError(w, http.StatusNotFound, "track not found")
		return
	}
	if err != nil {
		writeError(w, http.StatusInternalServerError, "track unavailable")
		return
	}
	writeJSON(w, http.StatusOK, t)
}
func (s *Server) addTrack(w http.ResponseWriter, r *http.Request) {
	var t domain.Track
	if err := json.NewDecoder(r.Body).Decode(&t); err != nil || strings.TrimSpace(t.ID) == "" || strings.TrimSpace(t.Title) == "" {
		writeError(w, http.StatusBadRequest, "id and title are required")
		return
	}
	added, err := s.music.AddTrack(t)
	if err != nil {
		writeError(w, http.StatusInternalServerError, "track could not be saved")
		return
	}
	writeJSON(w, http.StatusCreated, added)
}
func writeJSON(w http.ResponseWriter, status int, value any) {
	w.Header().Set("Content-Type", "application/json")
	w.WriteHeader(status)
	_ = json.NewEncoder(w).Encode(value)
}
func writeError(w http.ResponseWriter, status int, message string) {
	writeJSON(w, status, map[string]string{"error": message})
}

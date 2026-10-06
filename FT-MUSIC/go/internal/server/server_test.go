package server

import (
	"bytes"
	"github.com/ft-music/ft-music/internal/repository/memory"
	"github.com/ft-music/ft-music/internal/service"
	"github.com/ft-music/ft-music/internal/streaming"
	"net/http"
	"net/http/httptest"
	"testing"
)

func testHandler() http.Handler {
	return New(service.NewMusic(memory.NewTracks(), streaming.NewCatalog())).Routes()
}
func TestHealthEndpoint(t *testing.T) {
	w := httptest.NewRecorder()
	testHandler().ServeHTTP(w, httptest.NewRequest(http.MethodGet, "/api/v1/health", nil))
	if w.Code != http.StatusOK {
		t.Fatalf("status = %d, want %d", w.Code, http.StatusOK)
	}
}
func TestAddAndSearchTrack(t *testing.T) {
	h := testHandler()
	add := httptest.NewRequest(http.MethodPost, "/api/v1/library", bytes.NewBufferString(`{"id":"t1","title":"Signal","artist":"FT"}`))
	add.Header.Set("Content-Type", "application/json")
	w := httptest.NewRecorder()
	h.ServeHTTP(w, add)
	if w.Code != http.StatusCreated {
		t.Fatalf("add status = %d, want %d", w.Code, http.StatusCreated)
	}
	w = httptest.NewRecorder()
	h.ServeHTTP(w, httptest.NewRequest(http.MethodGet, "/api/v1/library?q=signal", nil))
	if w.Code != http.StatusOK || !bytes.Contains(w.Body.Bytes(), []byte(`"id":"t1"`)) {
		t.Fatalf("search response = %s", w.Body.String())
	}
}

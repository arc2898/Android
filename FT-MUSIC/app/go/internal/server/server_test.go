package server

import (
	"bytes"
	"net/http"
	"net/http/httptest"
	"testing"

	"github.com/ft-music/ft-music/internal/library"
)

func TestHealthEndpoint(t *testing.T) {
	r := httptest.NewRequest(http.MethodGet, "/api/v1/health", nil)
	w := httptest.NewRecorder()
	New(library.New()).Routes().ServeHTTP(w, r)
	if w.Code != http.StatusOK {
		t.Fatalf("status = %d, want %d", w.Code, http.StatusOK)
	}
}

func TestAddAndSearchTrack(t *testing.T) {
	h := New(library.New()).Routes()
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

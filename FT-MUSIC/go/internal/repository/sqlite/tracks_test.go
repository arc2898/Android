package sqlite

import (
	"github.com/ft-music/ft-music/internal/domain"
	"github.com/ft-music/ft-music/internal/repository"
	"testing"
)

func TestTracksPersistAndSearch(t *testing.T) {
	r, err := Open("file::memory:?cache=shared")
	if err != nil {
		t.Fatal(err)
	}
	defer r.Close()
	if _, err = r.Add(domain.Track{ID: "1", Title: "Signal", Artist: "FT"}); err != nil {
		t.Fatal(err)
	}
	got, err := r.Search("signal")
	if err != nil || len(got) != 1 || got[0].ID != "1" {
		t.Fatalf("Search() = %#v, err=%v", got, err)
	}
	if _, err = r.Get("missing"); err != repository.ErrNotFound {
		t.Fatalf("Get() err = %v", err)
	}
}

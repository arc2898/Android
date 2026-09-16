package service

import (
	"github.com/ft-music/ft-music/internal/domain"
	"github.com/ft-music/ft-music/internal/repository/memory"
	"github.com/ft-music/ft-music/internal/streaming"
	"testing"
)

func TestLibraryDelegatesToRepository(t *testing.T) {
	music := NewMusic(memory.NewTracks(), streaming.NewCatalog())
	if _, err := music.AddTrack(domain.Track{ID: "1", Title: "Midnight Drive", Artist: "FT"}); err != nil {
		t.Fatal(err)
	}
	got, err := music.Library("midnight")
	if err != nil || len(got) != 1 || got[0].ID != "1" {
		t.Fatalf("Library() = %#v, err=%v", got, err)
	}
}

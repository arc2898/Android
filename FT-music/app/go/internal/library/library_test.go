package library

import "testing"

func TestSearchMatchesTrackMetadata(t *testing.T) {
	lib := New()
	lib.Add(Track{ID: "1", Title: "Midnight Drive", Artist: "FT"})
	lib.Add(Track{ID: "2", Title: "Daybreak", Artist: "Nova"})
	got := lib.Search("ft")
	if len(got) != 1 || got[0].ID != "1" {
		t.Fatalf("Search() = %#v, want track 1", got)
	}
}

func TestGetMissingTrack(t *testing.T) {
	if _, err := New().Get("missing"); err != ErrNotFound {
		t.Fatalf("Get() error = %v, want ErrNotFound", err)
	}
}

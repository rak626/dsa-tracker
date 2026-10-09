package daily

import (
	"testing"
	"time"
)

var loc = time.UTC

func day(y int, m time.Month, d int) time.Time {
	return time.Date(y, m, d, 0, 0, 0, 0, loc)
}

func buildItems(n int) []Item {
	items := make([]Item, n)
	for i := range items {
		items[i] = Item{
			ID:     int64(i + 1),
			Topics: []string{topicFor(i)},
		}
	}
	return items
}

func topicFor(i int) string {
	topics := []string{"Graph", "DP", "Arrays", "Trie", "Greedy"}
	return topics[i%len(topics)]
}

func TestGenerateIsDeterministicPerDate(t *testing.T) {
	items := buildItems(50)
	opts := Options{Size: 5, LookbackDays: 7, Day: day(2026, 10, 9)}

	first := Generate(items, opts)
	second := Generate(items, opts)

	if len(first) != 5 {
		t.Fatalf("expected 5 ids, got %d", len(first))
	}
	for i := range first {
		if first[i] != second[i] {
			t.Fatalf("set changed within the same day: %v vs %v", first, second)
		}
	}
}

func TestGenerateHasNoDuplicates(t *testing.T) {
	items := buildItems(20)
	got := Generate(items, Options{Size: 20, LookbackDays: 7, Day: day(2026, 1, 1)})

	seen := map[int64]bool{}
	for _, id := range got {
		if seen[id] {
			t.Fatalf("duplicate id %d in %v", id, got)
		}
		seen[id] = true
	}
	if len(got) != 20 {
		t.Fatalf("expected every question once, got %d", len(got))
	}
}

func TestGenerateRespectsSizeCap(t *testing.T) {
	items := buildItems(3)
	got := Generate(items, Options{Size: 5, LookbackDays: 7, Day: day(2026, 2, 2)})
	if len(got) != 3 {
		t.Fatalf("expected set capped at pool size, got %d", len(got))
	}
}

func TestGenerateExcludesRecentlySolved(t *testing.T) {
	items := buildItems(10)
	items[0].LastSolved = day(2026, 3, 1)  // solved the same day
	items[1].LastSolved = day(2026, 2, 27) // solved 2 days ago

	got := Generate(items, Options{Size: 5, LookbackDays: 7, Day: day(2026, 3, 1)})
	for _, id := range got {
		if id == 1 || id == 2 {
			t.Fatalf("recently solved question %d was picked: %v", id, got)
		}
	}
}

func TestGenerateFallsBackWhenEverythingIsFresh(t *testing.T) {
	items := buildItems(5)
	for i := range items {
		items[i].LastSolved = day(2026, 4, 10) // all solved today
	}
	got := Generate(items, Options{Size: 3, LookbackDays: 7, Day: day(2026, 4, 10)})
	if len(got) != 3 {
		t.Fatalf("expected fallback pool to still fill the set, got %d", len(got))
	}
}

func TestGenerateSpreadsAcrossTopics(t *testing.T) {
	items := buildItems(25) // 5 topics, 5 items each
	for offset := 0; offset < 20; offset++ {
		got := Generate(items, Options{Size: 5, LookbackDays: 7, Day: day(2026, 5, offset+1)})
		seen := map[string]bool{}
		for _, id := range got {
			seen[primaryTopic(items[id-1])] = true
		}
		if len(seen) != 5 {
			t.Fatalf("day %d: expected 5 distinct topics, got %d (%v)", offset+1, len(seen), got)
		}
	}
}

func TestGeneratePrefersImportantAndStale(t *testing.T) {
	high, low := 5, 0
	items := []Item{
		{ID: 1, Topics: []string{"Graph"}, Importance: &low},
		{ID: 2, Topics: []string{"DP"}, Importance: &high},
	}

	highWins := 0
	for i := 0; i < 400; i++ {
		got := Generate(items, Options{
			Size: 1, LookbackDays: 7, Day: day(2026, 6, 1).AddDate(0, 0, i),
		})
		if got[0] == 2 {
			highWins++
		}
	}
	if highWins < 240 {
		t.Fatalf("importance weight too weak: high importance won %d/400", highWins)
	}
}

func TestGenerateBoostsNeverPracticed(t *testing.T) {
	now := day(2026, 7, 1)
	items := []Item{
		{ID: 1, Topics: []string{"Graph"}, LastAttempt: now, SolveCount: 3},
		{ID: 2, Topics: []string{"DP"}},
	}

	fresh := 0
	for i := 0; i < 300; i++ {
		got := Generate(items, Options{
			Size: 1, LookbackDays: 7, Day: now.AddDate(0, 0, i),
		})
		if got[0] == 2 {
			fresh++
		}
	}
	if fresh < 230 {
		t.Fatalf("fresh question not prioritised: won %d/300", fresh)
	}
}

func TestGenerateDiffersAcrossDays(t *testing.T) {
	items := buildItems(60)
	seen := map[string]bool{}
	for i := 0; i < 30; i++ {
		got := Generate(items, Options{
			Size: 5, LookbackDays: 7, Day: day(2026, 8, i+1),
		})
		seen[formatIDs(got)] = true
	}
	if len(seen) < 10 {
		t.Fatalf("sets look suspiciously similar across days: %d unique out of 30", len(seen))
	}
}

func TestGenerateEmptyInput(t *testing.T) {
	if got := Generate(nil, Options{Size: 5}); got != nil {
		t.Fatalf("expected nil for empty input, got %v", got)
	}
}

func TestDayTruncatesToMidnight(t *testing.T) {
	in := time.Date(2026, 10, 9, 23, 59, 0, 0, time.UTC)
	if got := Day(in, time.UTC); got.Day() != 9 || got.Hour() != 0 {
		t.Fatalf("unexpected day truncation: %v", got)
	}
}

func formatIDs(ids []int64) string {
	out := ""
	for _, id := range ids {
		out += string(rune('0' + id%10))
	}
	return out
}

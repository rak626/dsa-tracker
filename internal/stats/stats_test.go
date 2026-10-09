package stats

import (
	"testing"
	"time"
)

var loc = time.UTC

func day(y int, m time.Month, d int) time.Time {
	return time.Date(y, m, d, 0, 0, 0, 0, loc)
}

func activity(days ...time.Time) Activity {
	out := Activity{}
	for _, d := range days {
		st := out[d]
		st.Entries++
		out[d] = st
	}
	return out
}

func TestStreaksEmpty(t *testing.T) {
	current, longest := Streaks(Activity{}, day(2026, 10, 9))
	if current != 0 || longest != 0 {
		t.Fatalf("expected 0,0 got %d,%d", current, longest)
	}
}

func TestStreaksSingleDay(t *testing.T) {
	a := activity(day(2026, 10, 9))
	current, longest := Streaks(a, day(2026, 10, 9))
	if current != 1 || longest != 1 {
		t.Fatalf("expected 1,1 got %d,%d", current, longest)
	}
}

func TestStreaksConsecutiveIncludingToday(t *testing.T) {
	a := activity(day(2026, 10, 7), day(2026, 10, 8), day(2026, 10, 9))
	current, longest := Streaks(a, day(2026, 10, 9))
	if current != 3 || longest != 3 {
		t.Fatalf("expected 3,3 got %d,%d", current, longest)
	}
}

func TestStreaksSurvivesUnfinishedToday(t *testing.T) {
	a := activity(day(2026, 10, 7), day(2026, 10, 8))
	current, longest := Streaks(a, day(2026, 10, 9))
	if current != 2 || longest != 2 {
		t.Fatalf("expected 2,2 got %d,%d", current, longest)
	}
}

func TestStreaksBrokenYesterday(t *testing.T) {
	a := activity(day(2026, 10, 5), day(2026, 10, 7), day(2026, 10, 9))
	current, longest := Streaks(a, day(2026, 10, 9))
	if current != 1 || longest != 1 {
		t.Fatalf("expected 1,1 got %d,%d", current, longest)
	}
}

func TestStreaksLongestAcrossGap(t *testing.T) {
	a := activity(
		day(2026, 9, 20), day(2026, 9, 21), day(2026, 9, 22), day(2026, 9, 23),
		day(2026, 10, 8), day(2026, 10, 9),
	)
	current, longest := Streaks(a, day(2026, 10, 9))
	if current != 2 || longest != 4 {
		t.Fatalf("expected 2,4 got %d,%d", current, longest)
	}
}

func TestMonthOctober2026(t *testing.T) {
	// October 2026 starts on a Thursday, so 3 leading padding cells.
	a := activity(day(2026, 10, 1), day(2026, 10, 9), day(2026, 10, 9))
	weeks := Month(2026, time.October, a, day(2026, 10, 9))

	for i, w := range weeks {
		if len(w) != 7 {
			t.Fatalf("week %d has %d cells, want 7", i, len(w))
		}
	}

	first := weeks[0]
	for i := 0; i < 3; i++ {
		if !first[i].Padding {
			t.Fatalf("expected padding at week 0 cell %d", i)
		}
	}
	if !first[3].Day.Equal(day(2026, 10, 1)) || first[3].Stat.Entries != 1 {
		t.Fatalf("expected Oct 1 with 1 entry, got %+v", first[3])
	}

	for _, w := range weeks {
		for _, c := range w {
			if c.Future != (!c.Padding && c.Day.After(day(2026, 10, 9))) {
				t.Fatalf("wrong future flag for %+v", c)
			}
		}
	}

	// Oct 31 is a Saturday, so the last week ends with 1 trailing pad.
	last := weeks[len(weeks)-1]
	if !last[6].Padding {
		t.Fatalf("expected trailing padding, got %+v", last[6])
	}

	active, entries, solved := MonthTotals(
		Activity{
			day(2026, 10, 1): {Entries: 1, Solved: 1},
			day(2026, 10, 9): {Entries: 2, Solved: 1},
			day(2026, 9, 30): {Entries: 5, Solved: 5},
		},
		day(2026, 10, 15))
	if active != 2 || entries != 3 || solved != 2 {
		t.Fatalf("expected 2 active days, 3 entries, 2 solves; got %d, %d, %d",
			active, entries, solved)
	}

	bestDay, bestStat, found := BestDay(
		Activity{
			day(2026, 10, 1): {Entries: 2, Solved: 1},
			day(2026, 10, 9): {Entries: 2, Solved: 2},
			day(2026, 9, 30): {Entries: 9, Solved: 9},
		},
		day(2026, 10, 15))
	if !found || !bestDay.Equal(day(2026, 10, 1)) || bestStat.Entries != 2 {
		t.Fatalf("expected tie broken to Oct 1, got %v %+v", bestDay, bestStat)
	}

	if _, _, found := BestDay(Activity{}, day(2026, 10, 15)); found {
		t.Fatal("expected no best day for empty activity")
	}
}

func TestMonthStartsMonday(t *testing.T) {
	// June 2026 starts on a Monday: no leading padding.
	weeks := Month(2026, time.June, Activity{}, day(2026, 6, 15))
	if weeks[0][0].Padding || !weeks[0][0].Day.Equal(day(2026, 6, 1)) {
		t.Fatalf("expected June 1 in week 0 cell 0, got %+v", weeks[0][0])
	}
}

func TestHeatLevel(t *testing.T) {
	cases := map[int]int{0: 0, 1: 1, 2: 2, 3: 2, 4: 3, 10: 3}
	for in, want := range cases {
		if got := HeatLevel(in); got != want {
			t.Fatalf("HeatLevel(%d) = %d, want %d", in, got, want)
		}
	}
}

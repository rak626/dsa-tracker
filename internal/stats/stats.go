package stats

import (
	"sort"
	"time"
)

// DayStat summarizes a single day's practice.
type DayStat struct {
	Entries int // every solve, revision and skip
	Solved  int // solves only
}

// Activity maps a day (UTC midnight) to its stat.
type Activity map[time.Time]DayStat

// Active reports whether the day had any practice.
func (a Activity) Active(day time.Time) bool {
	return a[day].Entries > 0
}

// Streaks returns the current and longest streaks of consecutive active
// days. Today counts when active; otherwise the streak is measured back
// from yesterday, so an unfinished today does not break it.
func Streaks(a Activity, today time.Time) (current, longest int) {
	today = midnight(today)

	days := make([]time.Time, 0, len(a))
	for d, st := range a {
		if st.Entries > 0 {
			days = append(days, d)
		}
	}
	sort.Slice(days, func(i, j int) bool { return days[i].Before(days[j]) })

	run := 0
	for i, d := range days {
		if i > 0 && d.Equal(days[i-1].AddDate(0, 0, 1)) {
			run++
		} else {
			run = 1
		}
		if run > longest {
			longest = run
		}
	}

	anchor := today
	if !a.Active(anchor) {
		anchor = anchor.AddDate(0, 0, -1)
	}
	for a.Active(anchor) {
		current++
		anchor = anchor.AddDate(0, 0, -1)
	}
	return current, longest
}

// Cell is one square of the month grid.
type Cell struct {
	Day     time.Time // zero for padding cells
	Stat    DayStat
	Future  bool
	Padding bool
}

// Month builds a Monday-first calendar matrix for the given month. Rows
// always have exactly 7 cells; leading and trailing padding fills the
// weeks that bleed into adjacent months.
func Month(year int, month time.Month, a Activity, today time.Time) [][]Cell {
	today = midnight(today)
	first := time.Date(year, month, 1, 0, 0, 0, 0, time.UTC)
	lead := int((first.Weekday() + 6) % 7)
	daysInMonth := first.AddDate(0, 1, -1).Day()

	cells := make([]Cell, 0, daysInMonth+lead+7)
	for i := 0; i < lead; i++ {
		cells = append(cells, Cell{Padding: true})
	}
	for d := 1; d <= daysInMonth; d++ {
		day := time.Date(year, month, d, 0, 0, 0, 0, time.UTC)
		cells = append(cells, Cell{Day: day, Stat: a[day], Future: day.After(today)})
	}
	for len(cells)%7 != 0 {
		cells = append(cells, Cell{Padding: true})
	}

	weeks := make([][]Cell, 0, len(cells)/7)
	for i := 0; i < len(cells); i += 7 {
		weeks = append(weeks, cells[i:i+7])
	}
	return weeks
}

// MonthTotals counts active days, entries and solves for the month that
// holds day.
func MonthTotals(a Activity, day time.Time) (activeDays, entries, solved int) {
	day = midnight(day)
	for d, st := range a {
		if st.Entries > 0 && d.Year() == day.Year() && d.Month() == day.Month() {
			activeDays++
			entries += st.Entries
			solved += st.Solved
		}
	}
	return activeDays, entries, solved
}

// BestDay returns the day with the most entries in the month holding day.
// Ties go to the earliest day.
func BestDay(a Activity, day time.Time) (time.Time, DayStat, bool) {
	day = midnight(day)
	var (
		best     time.Time
		bestStat DayStat
		found    bool
	)
	for d, st := range a {
		if d.Year() != day.Year() || d.Month() != day.Month() {
			continue
		}
		if st.Entries > bestStat.Entries ||
			(found && st.Entries == bestStat.Entries && d.Before(best)) {
			best, bestStat, found = d, st, true
		}
	}
	return best, bestStat, found
}

// Totals counts active days and entries across all history.
func Totals(a Activity) (activeDays, entries int) {
	for _, st := range a {
		if st.Entries > 0 {
			activeDays++
			entries += st.Entries
		}
	}
	return activeDays, entries
}

// HeatLevel buckets a day's entries for colouring: 0 none, 1 light,
// 2-3 medium, 4+ strong.
func HeatLevel(entries int) int {
	switch {
	case entries >= 4:
		return 3
	case entries >= 2:
		return 2
	case entries == 1:
		return 1
	default:
		return 0
	}
}

func midnight(t time.Time) time.Time {
	return time.Date(t.Year(), t.Month(), t.Day(), 0, 0, 0, 0, time.UTC)
}

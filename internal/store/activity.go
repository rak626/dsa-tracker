package store

import (
	"context"
	"time"
)

// DayStat mirrors stats.DayStat so the store stays independent of it.
type DayStat struct {
	Entries int
	Solved  int
}

// ActivityByDay returns per-day practice totals. Days are normalized to
// UTC midnight so they hash consistently in maps.
func (s *Store) ActivityByDay(ctx context.Context) (map[time.Time]DayStat, error) {
	rows, err := s.Pool.Query(ctx, `
		SELECT practiced_on,
		       count(*),
		       count(*) FILTER (WHERE action = 'solve')
		FROM practice_log
		GROUP BY practiced_on
		ORDER BY practiced_on`)
	if err != nil {
		return nil, err
	}
	defer rows.Close()

	out := map[time.Time]DayStat{}
	for rows.Next() {
		var (
			day time.Time
			st  DayStat
		)
		if err := rows.Scan(&day, &st.Entries, &st.Solved); err != nil {
			return nil, err
		}
		out[time.Date(day.Year(), day.Month(), day.Day(), 0, 0, 0, 0, time.UTC)] = st
	}
	return out, rows.Err()
}

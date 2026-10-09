package store

import (
	"context"
	"errors"
	"time"

	"github.com/jackc/pgx/v5"
)

type DailySet struct {
	Day         time.Time
	Size        int
	QuestionIDs []int64
}

func (s *Store) GetDailySet(ctx context.Context, day time.Time) (*DailySet, error) {
	var (
		set DailySet
		ids []int64
	)
	err := s.Pool.QueryRow(ctx, `
		SELECT day, size, question_ids FROM daily_sets WHERE day = $1::date`, day).
		Scan(&set.Day, &set.Size, &ids)
	if errors.Is(err, pgx.ErrNoRows) {
		return nil, nil
	}
	if err != nil {
		return nil, err
	}
	set.QuestionIDs = ids
	return &set, nil
}

// SaveDailySet inserts the set for a day. It never overwrites an existing row,
// which keeps today's set stable across reloads and redeploys.
func (s *Store) SaveDailySet(ctx context.Context, day time.Time, size int, ids []int64) error {
	_, err := s.Pool.Exec(ctx, `
		INSERT INTO daily_sets (day, size, question_ids)
		VALUES ($1::date, $2, $3)
		ON CONFLICT (day) DO NOTHING`, day, size, ids)
	return err
}

// ReplaceDailySet overwrites the set for a day (used when the user changes
// the daily set size in settings).
func (s *Store) ReplaceDailySet(ctx context.Context, day time.Time, size int, ids []int64) error {
	_, err := s.Pool.Exec(ctx, `
		INSERT INTO daily_sets (day, size, question_ids)
		VALUES ($1::date, $2, $3)
		ON CONFLICT (day) DO UPDATE SET size = EXCLUDED.size,
		                               question_ids = EXCLUDED.question_ids`,
		day, size, ids)
	return err
}

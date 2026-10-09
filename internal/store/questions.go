package store

import (
	"context"
	"errors"
	"strconv"
	"time"

	"github.com/jackc/pgx/v5"
)

type Question struct {
	ID         int64
	ExcelID    string
	Title      string
	Platform   string
	URL        string
	Topics     []string
	Subtopic   string
	Importance *int
	VideoURL   string
	VideoTitle string
}

type Progress struct {
	SolveCount      int
	ReviseCount     int
	LastSolvedOn    time.Time // zero value when never solved
	LastPracticedOn time.Time // zero value when never practiced
}

const questionColumns = `id, excel_id, title, platform, COALESCE(url, ''), topics,
	COALESCE(subtopic, ''), importance, COALESCE(video_url, ''), COALESCE(video_title, '')`

func scanQuestion(row pgx.Row) (Question, error) {
	var q Question
	err := row.Scan(&q.ID, &q.ExcelID, &q.Title, &q.Platform, &q.URL, &q.Topics,
		&q.Subtopic, &q.Importance, &q.VideoURL, &q.VideoTitle)
	return q, err
}

func (s *Store) AllQuestions(ctx context.Context) ([]Question, error) {
	rows, err := s.Pool.Query(ctx,
		`SELECT `+questionColumns+` FROM questions ORDER BY id`)
	if err != nil {
		return nil, err
	}
	defer rows.Close()

	var out []Question
	for rows.Next() {
		q, err := scanQuestion(rows)
		if err != nil {
			return nil, err
		}
		out = append(out, q)
	}
	return out, rows.Err()
}

func (s *Store) QuestionByID(ctx context.Context, id int64) (Question, error) {
	return scanQuestion(s.Pool.QueryRow(ctx,
		`SELECT `+questionColumns+` FROM questions WHERE id = $1`, id))
}

func (s *Store) Topics(ctx context.Context) ([]string, error) {
	rows, err := s.Pool.Query(ctx, `
		SELECT DISTINCT t FROM questions, UNNEST(topics) AS t ORDER BY t`)
	if err != nil {
		return nil, err
	}
	defer rows.Close()

	var out []string
	for rows.Next() {
		var t string
		if err := rows.Scan(&t); err != nil {
			return nil, err
		}
		out = append(out, t)
	}
	return out, rows.Err()
}

func (s *Store) ProgressMap(ctx context.Context) (map[int64]Progress, error) {
	rows, err := s.Pool.Query(ctx, `
		SELECT question_id,
		       count(*) FILTER (WHERE action = 'solve'),
		       count(*) FILTER (WHERE action = 'revise'),
		       max(practiced_on) FILTER (WHERE action = 'solve'),
		       max(practiced_on)
		FROM practice_log
		GROUP BY question_id`)
	if err != nil {
		return nil, err
	}
	defer rows.Close()

	out := map[int64]Progress{}
	for rows.Next() {
		var (
			id     int64
			p      Progress
			solved *time.Time
			last   *time.Time
		)
		if err := rows.Scan(&id, &p.SolveCount, &p.ReviseCount, &solved, &last); err != nil {
			return nil, err
		}
		if solved != nil {
			p.LastSolvedOn = *solved
		}
		if last != nil {
			p.LastPracticedOn = *last
		}
		out[id] = p
	}
	return out, rows.Err()
}

// PracticedOn reports the action recorded for each question on the given day.
func (s *Store) PracticedOn(ctx context.Context, day time.Time, ids []int64) (map[int64]string, error) {
	out := map[int64]string{}
	if len(ids) == 0 {
		return out, nil
	}
	rows, err := s.Pool.Query(ctx, `
		SELECT question_id, action
		FROM practice_log
		WHERE practiced_on = $1::date AND question_id = ANY($2)`, day, ids)
	if err != nil {
		return nil, err
	}
	defer rows.Close()

	priority := map[string]int{"skip": 0, "revise": 1, "solve": 2}
	for rows.Next() {
		var (
			id     int64
			action string
		)
		if err := rows.Scan(&id, &action); err != nil {
			return nil, err
		}
		if prev, ok := out[id]; !ok || priority[action] > priority[prev] {
			out[id] = action
		}
	}
	return out, rows.Err()
}

func (s *Store) Record(ctx context.Context, questionID int64, action string, day time.Time) error {
	_, err := s.Pool.Exec(ctx, `
		INSERT INTO practice_log (question_id, action, practiced_on)
		VALUES ($1, $2, $3::date)`, questionID, action, day)
	if err != nil && errors.Is(err, context.Canceled) {
		return err
	}
	return err
}

func (s *Store) Stats(ctx context.Context) (Stats, error) {
	var st Stats
	err := s.Pool.QueryRow(ctx, `
		SELECT
			(SELECT count(*) FROM questions),
			(SELECT count(DISTINCT question_id) FROM practice_log WHERE action = 'solve'),
			(SELECT count(*) FROM practice_log WHERE action = 'solve'),
			(SELECT count(*) FROM practice_log WHERE action = 'revise')`).
		Scan(&st.QuestionCount, &st.SolvedDistinct, &st.SolveEntries, &st.ReviseEntries)
	return st, err
}

type Stats struct {
	QuestionCount  int
	SolvedDistinct int
	SolveEntries   int
	ReviseEntries  int
}

// ImportanceLabel renders the priority for templates ("" when unknown).
func (q Question) ImportanceLabel() string {
	if q.Importance == nil {
		return ""
	}
	return strconv.Itoa(*q.Importance)
}

// HasPriority reports whether the question carries a non-zero priority, so
// templates can hide the noisy "0/5" badge.
func (q Question) HasPriority() bool {
	return q.Importance != nil && *q.Importance > 0
}

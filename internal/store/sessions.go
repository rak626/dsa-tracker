package store

import (
	"context"
	"errors"
	"time"

	"github.com/jackc/pgx/v5"
)

type Session struct {
	TokenHash []byte
	CSRF      []byte
	ExpiresAt time.Time
}

func (s *Store) CreateSession(ctx context.Context, tokenHash, csrf []byte, expiresAt time.Time) error {
	_, err := s.Pool.Exec(ctx, `
		INSERT INTO sessions (token_hash, csrf_secret, expires_at)
		VALUES ($1, $2, $3)`, tokenHash, csrf, expiresAt)
	return err
}

func (s *Store) GetSession(ctx context.Context, tokenHash []byte) (*Session, error) {
	var sess Session
	err := s.Pool.QueryRow(ctx, `
		SELECT token_hash, csrf_secret, expires_at
		FROM sessions
		WHERE token_hash = $1 AND expires_at > now()`, tokenHash).
		Scan(&sess.TokenHash, &sess.CSRF, &sess.ExpiresAt)
	if errors.Is(err, pgx.ErrNoRows) {
		return nil, nil
	}
	if err != nil {
		return nil, err
	}
	return &sess, nil
}

func (s *Store) DeleteSession(ctx context.Context, tokenHash []byte) error {
	_, err := s.Pool.Exec(ctx, `DELETE FROM sessions WHERE token_hash = $1`, tokenHash)
	return err
}

func (s *Store) PurgeExpiredSessions(ctx context.Context) error {
	_, err := s.Pool.Exec(ctx, `DELETE FROM sessions WHERE expires_at <= now()`)
	return err
}

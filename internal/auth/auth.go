package auth

import (
	"crypto/rand"
	"crypto/sha256"
	"crypto/subtle"
	"encoding/base64"
	"sync"
	"time"
)

// NewToken returns a URL-safe random token of n bytes entropy.
func NewToken(n int) string {
	buf := make([]byte, n)
	if _, err := rand.Read(buf); err != nil {
		panic(err)
	}
	return base64.RawURLEncoding.EncodeToString(buf)
}

// HashToken returns the SHA-256 digest stored in the sessions table, so a
// leaked database row cannot be replayed as a cookie.
func HashToken(token string) []byte {
	sum := sha256.Sum256([]byte(token))
	return sum[:]
}

// EqualSecret reports whether two secrets match in constant time.
func EqualSecret(a, b []byte) bool {
	return subtle.ConstantTimeCompare(a, b) == 1
}

// RateLimiter is a fixed-window limiter for login attempts.
type RateLimiter struct {
	mu     sync.Mutex
	hits   map[string]*bucket
	limit  int
	window time.Duration
}

type bucket struct {
	count    int
	resetsAt time.Time
}

func NewRateLimiter(limit int, window time.Duration) *RateLimiter {
	return &RateLimiter{
		hits:   map[string]*bucket{},
		limit:  limit,
		window: window,
	}
}

// Allow reports whether the key may attempt another try, counting the attempt.
func (r *RateLimiter) Allow(key string) bool {
	r.mu.Lock()
	defer r.mu.Unlock()

	now := time.Now()
	if len(r.hits) > 10_000 {
		for k, w := range r.hits {
			if !now.Before(w.resetsAt) {
				delete(r.hits, k)
			}
		}
	}

	w := r.hits[key]
	if w == nil || !now.Before(w.resetsAt) {
		r.hits[key] = &bucket{count: 1, resetsAt: now.Add(r.window)}
		return true
	}
	if w.count >= r.limit {
		return false
	}
	w.count++
	return true
}

// Reset clears the counter after a successful attempt.
func (r *RateLimiter) Reset(key string) {
	r.mu.Lock()
	defer r.mu.Unlock()
	delete(r.hits, key)
}

package auth

import (
	"strings"
	"testing"
	"time"
)

func TestHashAndVerifyPassword(t *testing.T) {
	hash, err := HashPassword("correct horse battery staple")
	if err != nil {
		t.Fatalf("hash: %v", err)
	}
	if !strings.HasPrefix(hash, "$argon2id$v=19$") {
		t.Fatalf("unexpected hash format: %s", hash)
	}
	if !VerifyPassword("correct horse battery staple", hash) {
		t.Fatal("correct password rejected")
	}
	if VerifyPassword("wrong password", hash) {
		t.Fatal("wrong password accepted")
	}
}

func TestVerifyRejectsMalformedHash(t *testing.T) {
	for _, bad := range []string{"", "plaintext", "$argon2i$v=19$m=1,t=1,p=1$aa$bb", "$argon2id$v=19$m=x,t=1,p=1$aa$bb"} {
		if VerifyPassword("anything", bad) {
			t.Fatalf("malformed hash accepted: %q", bad)
		}
	}
}

func TestValidatePassword(t *testing.T) {
	if err := ValidatePassword("short"); err == nil {
		t.Fatal("expected error for short password")
	}
	if err := ValidatePassword("long enough password"); err != nil {
		t.Fatalf("expected valid password, got %v", err)
	}
}

func TestHashesAreSalted(t *testing.T) {
	a, _ := HashPassword("same password")
	b, _ := HashPassword("same password")
	if a == b {
		t.Fatal("two hashes of the same password are identical")
	}
}

func TestRateLimiter(t *testing.T) {
	limiter := NewRateLimiter(3, time.Minute)
	for i := 0; i < 3; i++ {
		if !limiter.Allow("ip") {
			t.Fatalf("attempt %d unexpectedly blocked", i+1)
		}
	}
	if limiter.Allow("ip") {
		t.Fatal("4th attempt not blocked")
	}
	if !limiter.Allow("other-ip") {
		t.Fatal("different key was blocked")
	}
	limiter.Reset("ip")
	if !limiter.Allow("ip") {
		t.Fatal("reset did not restore the key")
	}
}

func TestHashTokenIsStable(t *testing.T) {
	token := NewToken(32)
	if len(token) < 40 {
		t.Fatalf("token too short: %s", token)
	}
	if !EqualSecret(HashToken(token), HashToken(token)) {
		t.Fatal("hash of the same token differs")
	}
	if EqualSecret(HashToken(token), HashToken(NewToken(32))) {
		t.Fatal("different tokens hashed equal")
	}
}

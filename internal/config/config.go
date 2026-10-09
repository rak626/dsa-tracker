package config

import (
	"errors"
	"os"
	"strconv"
	"time"

	// Embed the IANA timezone database so APP_TZ works in scratch/alpine images.
	_ "time/tzdata"
)

type Config struct {
	DatabaseURL  string
	Port         string
	PasswordHash string
	SessionTTL   time.Duration
	Location     *time.Location
	DailySize    int
	LookbackDays int
}

func Load() (*Config, error) {
	return load(true)
}

// LoadDatabase loads configuration for commands that only need the database
// (for example the Excel seeder).
func LoadDatabase() (*Config, error) {
	return load(false)
}

func load(requireAuth bool) (*Config, error) {
	dbURL := os.Getenv("DATABASE_URL")
	if dbURL == "" {
		return nil, errors.New("DATABASE_URL is not set")
	}

	hash := os.Getenv("AUTH_PASSWORD_HASH")
	if requireAuth && hash == "" && os.Getenv("AUTH_PASSWORD") == "" {
		return nil, errors.New("set AUTH_PASSWORD_HASH (or AUTH_PASSWORD for local development)")
	}

	locName := env("APP_TZ", "Asia/Kolkata")
	loc, err := time.LoadLocation(locName)
	if err != nil {
		return nil, errors.New("invalid APP_TZ: " + locName)
	}

	return &Config{
		DatabaseURL:  dbURL,
		Port:         env("PORT", "8080"),
		PasswordHash: hash,
		SessionTTL:   time.Duration(envInt("SESSION_TTL_DAYS", 30)) * 24 * time.Hour,
		Location:     loc,
		DailySize:    envInt("DEFAULT_DAILY_SIZE", 5),
		LookbackDays: envInt("LOOKBACK_DAYS", 7),
	}, nil
}

func env(key, fallback string) string {
	if v := os.Getenv(key); v != "" {
		return v
	}
	return fallback
}

func envInt(key string, fallback int) int {
	v := os.Getenv(key)
	if v == "" {
		return fallback
	}
	n, err := strconv.Atoi(v)
	if err != nil || n <= 0 {
		return fallback
	}
	return n
}

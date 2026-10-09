package main

import (
	"context"
	"errors"
	"log/slog"
	"net/http"
	"os"
	"os/signal"
	"syscall"
	"time"

	"github.com/rakesh/dsa-tracker/internal/auth"
	"github.com/rakesh/dsa-tracker/internal/config"
	"github.com/rakesh/dsa-tracker/internal/server"
	"github.com/rakesh/dsa-tracker/internal/store"
)

func main() {
	logger := slog.New(slog.NewTextHandler(os.Stdout, &slog.HandlerOptions{
		Level: slog.LevelInfo,
	}))

	if err := run(logger); err != nil {
		logger.Error("fatal", "error", err)
		os.Exit(1)
	}
}

func run(logger *slog.Logger) error {
	cfg, err := config.Load()
	if err != nil {
		return err
	}

	if cfg.PasswordHash == "" {
		// Development convenience: derive the hash from AUTH_PASSWORD.
		hash, err := auth.HashPassword(os.Getenv("AUTH_PASSWORD"))
		if err != nil {
			return err
		}
		cfg.PasswordHash = hash
		logger.Info("derived password hash from AUTH_PASSWORD (development mode)")
	}

	ctx := context.Background()
	st, err := store.Connect(ctx, cfg.DatabaseURL)
	if err != nil {
		return err
	}
	defer st.Close()

	if err := st.Migrate(ctx); err != nil {
		return err
	}

	srv, err := server.New(cfg, st, logger)
	if err != nil {
		return err
	}

	httpServer := &http.Server{
		Addr:              ":" + cfg.Port,
		Handler:           srv.Handler(),
		ReadHeaderTimeout: 10 * time.Second,
		IdleTimeout:       60 * time.Second,
	}

	errCh := make(chan error, 1)
	go func() {
		logger.Info("listening", "port", cfg.Port, "tz", cfg.Location)
		errCh <- httpServer.ListenAndServe()
	}()

	stop := make(chan os.Signal, 1)
	signal.Notify(stop, syscall.SIGINT, syscall.SIGTERM)

	select {
	case err := <-errCh:
		return err
	case sig := <-stop:
		logger.Info("shutting down", "signal", sig.String())
		shutdownCtx, cancel := context.WithTimeout(context.Background(), 20*time.Second)
		defer cancel()
		if err := httpServer.Shutdown(shutdownCtx); err != nil && !errors.Is(err, context.Canceled) {
			return err
		}
	}
	return nil
}

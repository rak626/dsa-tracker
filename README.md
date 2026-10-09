# DSA Tracker

A private, single-user DSA practice tracker. Every day it hands you a small,
deterministic set of questions chosen from your Excel question bank — weighted
by importance, recency and how often you have already solved each problem.

- **Stack:** Go · PostgreSQL · Tailwind CSS · server-rendered HTML (no SPA)
- **Deploy:** one container on Vercel (Hobby plan) + Neon Postgres (free tier)
- **Image size:** ~34 MB

## What it does

| Page | Purpose |
|---|---|
| `/` | Today's set: progress ring, the current question card (problem + lecture links), `Solved` / `Needs revision` / `Skip` actions |
| `/questions` | Read-only question bank with topic filters and client-side search |
| `/settings` | Daily set size (1–50) |

Questions are **seeded from Excel only** — there is no add/edit/delete UI.

### Daily set algorithm

The set for a date is derived from `sha256(YYYY-MM-DD)`, so it is identical on
every reload and survives redeploys. Within that deterministic stream:

```
weight = (1 + importance/5)          # Excel priority column, 0–5
       × (1 + daysSinceLastAttempt/14) # stale questions float up
       × 1/(1 + solveCount)            # weak questions float up
       × topicBoost                    # under-practised topics float up
```

Questions solved within `LOOKBACK_DAYS` (default 7) are excluded, selection is
without replacement, and topics are spread so one sheet cannot dominate a day.
The chosen set is persisted in `daily_sets`, so it never shifts mid-day.

## Quick start (local)

```bash
# 1. Postgres
docker compose up -d

# 2. Import the question bank (docs/DSA Pactice List.xlsx)
export DATABASE_URL='postgres://dsa:dsa_local@localhost:5432/dsa_tracker?sslmode=disable'
go run ./cmd/seed

# 3. Run
export AUTH_PASSWORD='choose-a-strong-password'   # dev only; see production
go run ./cmd/server                                # http://localhost:8080
```

Override the port with `PORT=7008`.

### Build the CSS

```bash
npm install
npm run build:css      # web/static/src/app.css -> web/static/app.css
npm run watch:css      # during development
```

`web/static/app.css` is committed so `go run` works without Node.

## Configuration

| Variable | Default | Notes |
|---|---|---|
| `DATABASE_URL` | — | required, e.g. `postgres://user:pass@host/db?sslmode=require` |
| `AUTH_PASSWORD_HASH` | — | **production**: Argon2id PHC hash (see below) |
| `AUTH_PASSWORD` | — | **dev only**: hashed at boot when the hash is absent |
| `PORT` | `8080` | Vercel injects its own port |
| `APP_TZ` | `Asia/Kolkata` | drives the "day" boundary for daily sets |
| `DEFAULT_DAILY_SIZE` | `5` | used until `daily_size` is set in the UI |
| `LOOKBACK_DAYS` | `7` | solved-within window excluded from the pool |
| `SESSION_TTL_DAYS` | `30` | session cookie / DB session lifetime |

Generate the production hash:

```bash
go run ./cmd/hashpw -          # reads the password from the terminal, hidden
# prints: $argon2id$v=19$m=65536,t=2,p=1$...$...
```

## Security model

Public URL, single owner:

- Argon2id password verification, constant-time comparison
- Server-side sessions in Postgres (only the SHA-256 of the token is stored),
  httpOnly + `Secure` + `SameSite=Strict` cookie
- Login rate limit: 5 attempts / 15 minutes per IP
- Per-session CSRF token required on every state-changing POST
- CSP, `X-Frame-Options: DENY`, nosniff, referrer policy on every response
- Every route except `/login`, `/static/*` and `/healthz` requires a session

## Deploy to Vercel

1. Push the repository to GitHub and import it in Vercel (preset: **Container**),
   or run `vercel deploy` with the CLI. Vercel builds `Dockerfile.vercel`.
2. Create a free [Neon](https://neon.com) project in **AWS Asia Pacific (Singapore)**
   and copy the pooled connection string.
3. Set the project environment variables:

   ```
   DATABASE_URL=<neon pooled connection string>
   AUTH_PASSWORD_HASH=<from go run ./cmd/hashpw ->
   APP_TZ=Asia/Kolkata
   ```

4. Seed the database once from your machine:

   ```bash
   DATABASE_URL='<neon url>' go run ./cmd/seed
   ```

5. Optional: set the function region to `sin1` (closest to the Neon region).

Container disk on Vercel is ephemeral, which is why all state lives in Neon.

## Commands

```bash
go build ./...        # compile everything
go vet ./...          # static analysis
go test ./...         # randomizer + auth + seed unit tests
gofmt -l .            # formatting check
docker compose up -d  # local Postgres
docker build -t dsa-tracker .   # local image
```

## Project structure

```
cmd/server/        HTTP entrypoint, graceful shutdown
cmd/seed/          Excel -> Postgres import (excelize)
cmd/hashpw/        Argon2id hash generator
internal/config/   env configuration
internal/store/    pgx access + embedded SQL migrations
internal/daily/    daily-set randomizer (pure, unit tested)
internal/stats/    streaks + month heatmap (pure, unit tested)
internal/auth/     password hashing, tokens, rate limiter
internal/server/   router, middleware, handlers, views
web/templates/     html/template pages
web/static/        Tailwind source + compiled CSS + small JS
docs/              DSA Pactice List.xlsx (source of truth for questions)
```

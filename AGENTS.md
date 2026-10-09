# dsa-tracker

Single-user DSA practice tracker: Go + PostgreSQL + Tailwind CSS, server-rendered
HTML. No SPA, no Java/Maven.

## Tech Stack
- Go 1.26, pgx/v5 (raw SQL, no ORM), embedded `html/template`
- PostgreSQL (local `docker compose up -d`; prod: Neon free tier)
- Tailwind CSS v4 via `@tailwindcss/cli` (source: `web/static/src/app.css`)
- `excelize/v2` for the Excel question bank
- Deployed as a single container on Vercel (Hobby), region `sin1`

## Development Commands
- `go run ./cmd/server` — serve on `:8080` (override with `PORT`)
- `go run ./cmd/seed` — import `docs/DSA Pactice List.xlsx` into Postgres (idempotent)
- `go run ./cmd/hashpw -` — generate an Argon2id PHC hash for `AUTH_PASSWORD_HASH`
- `go build ./... && go vet ./... && go test ./...` — compile, lint, test
- `gofmt -l .` — formatting check (must print nothing)
- `npm run build:css` — rebuild `web/static/app.css` (committed, so optional)
- `docker compose up -d` — local Postgres 16 (`dsa` / `dsa_local` / `dsa_tracker`)

## Configuration (env vars)
- `DATABASE_URL` (required), `PORT`, `APP_TZ=Asia/Kolkata`
- `AUTH_PASSWORD_HASH` (prod) or `AUTH_PASSWORD` (dev, hashed at boot)
- `DEFAULT_DAILY_SIZE=5`, `LOOKBACK_DAYS=7`, `SESSION_TTL_DAYS=30`

## Testing
- Unit tests only: `internal/daily/randomizer_test.go` (determinism, exclusion,
  spread), `internal/auth/*_test.go` (argon2, ratelimit), `cmd/seed/main_test.go`
  (Excel cleaning/dedupe fixtures)
- No HTTP tests yet; verify manually with curl (see below)

## Database
- Migrations: `internal/store/migrations/0001_init.sql`, applied at boot by
  `schema_migrations` runner
- Tables: `questions`, `practice_log`, `daily_sets`, `sessions`, `settings`
- Add a migration as a new `000N_*.sql`; never edit an applied one
- Local: `docker compose exec -T postgres psql -U dsa -d dsa_tracker`

## Security
- Every route except `/login`, `/static/*`, `/healthz` requires a session
- Argon2id + server-side sessions in Postgres + per-session CSRF on all POSTs
- Rate limit 5 logins / 15 min per IP; security headers on every response
- Never commit real credentials; `.env*` is gitignored

## Project Structure
```
cmd/server/        HTTP entrypoint, graceful shutdown
cmd/seed/          Excel -> Postgres import
cmd/hashpw/        Argon2id hash generator
internal/config/   env config (load() with requireAuth flag)
internal/store/    pgx pool, questions/daily/sessions, migrations
internal/daily/    deterministic weighted daily-set randomizer
internal/auth/     password, tokens, rate limiter
internal/server/   router, middleware, handlers, view data
web/templates/     base + login/today/questions/settings
web/static/        src/app.css (Tailwind), compiled app.css, app.js
docs/              DSA Pactice List.xlsx (question bank source of truth)
```

## Conventions
- Handlers are thin; SQL lives in `internal/store`; randomness/selection logic is
  pure in `internal/daily` so it stays unit-testable
- Templates get one `viewData` struct per page; navigation/flash live in `base.html`
- Mutations go through `POST` + `_csrf`; success/failure surfaces as
  `?notice=` / `?error=` query params
- `.HasPriority()` / `.ImportanceLabel()` on `store.Question` — importance `0`
  means "no priority", not a badge
- Comments only where intent is non-obvious; no decorative comments

## Smoke Test (curl)
```bash
curl -s -o /dev/null -w '%{http_code} %{redirect_url}\n' localhost:8080/   # 303 -> /login
curl -s -c /tmp/j -d 'password=...' localhost:8080/login                  # 303
curl -s -b /tmp/j -o /tmp/t.html -w '%{http_code}\n' localhost:8080/      # 200
curl -s -b /tmp/j localhost:8080/api/today                                # JSON set
curl -s -o /dev/null -w '%{http_code}\n' localhost:8080/healthz           # 200
```

## Deploy (Vercel + Neon)
- Build: `Dockerfile.vercel` (Node stage for Tailwind -> Go build -> Alpine)
- Neon region: `aws-ap-southeast-1`; set `DATABASE_URL` + `AUTH_PASSWORD_HASH`
- Seed once: `DATABASE_URL='<neon>' go run ./cmd/seed`
- Container disk is ephemeral — all state must stay in Neon

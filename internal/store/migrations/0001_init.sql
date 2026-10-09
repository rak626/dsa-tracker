CREATE TABLE questions (
    id          BIGSERIAL PRIMARY KEY,
    excel_id    TEXT NOT NULL UNIQUE,
    title       TEXT NOT NULL,
    platform    TEXT NOT NULL DEFAULT '',
    url         TEXT,
    topics      TEXT[] NOT NULL DEFAULT '{}',
    subtopic    TEXT,
    importance  SMALLINT,
    video_url   TEXT,
    video_title TEXT,
    created_at  TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX questions_topics_idx ON questions USING GIN (topics);

CREATE TABLE practice_log (
    id           BIGSERIAL PRIMARY KEY,
    question_id  BIGINT NOT NULL REFERENCES questions(id) ON DELETE CASCADE,
    action       TEXT NOT NULL CHECK (action IN ('solve', 'revise', 'skip')),
    practiced_on DATE NOT NULL,
    created_at   TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX practice_log_question_idx ON practice_log (question_id, practiced_on DESC);
CREATE INDEX practice_log_day_idx ON practice_log (practiced_on DESC);

CREATE TABLE daily_sets (
    day          DATE PRIMARY KEY,
    size         INTEGER NOT NULL,
    question_ids BIGINT[] NOT NULL
);

CREATE TABLE sessions (
    id          BIGSERIAL PRIMARY KEY,
    token_hash  BYTEA NOT NULL UNIQUE,
    csrf_secret BYTEA NOT NULL,
    expires_at  TIMESTAMPTZ NOT NULL,
    created_at  TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX sessions_expires_idx ON sessions (expires_at);

CREATE TABLE settings (
    key   TEXT PRIMARY KEY,
    value TEXT NOT NULL
);

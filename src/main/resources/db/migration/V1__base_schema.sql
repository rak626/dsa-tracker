/* =========================================================
   PATTERNS
   ========================================================= */

CREATE TABLE patterns
(
    id   BIGSERIAL PRIMARY KEY,
    name VARCHAR(255) NOT NULL
);

CREATE UNIQUE INDEX uk_patterns_name
    ON patterns (name);


/* =========================================================
   TOPICS
   ========================================================= */

CREATE TABLE topics
(
    id   BIGSERIAL PRIMARY KEY,
    name VARCHAR(255) NOT NULL
);

CREATE UNIQUE INDEX uk_topics_name
    ON topics (name);


/* =========================================================
   QUESTIONS
   ========================================================= */

CREATE TABLE questions
(
    id           BIGSERIAL PRIMARY KEY,

    video_id     VARCHAR(255),

    problem_name VARCHAR(255) NOT NULL,

    problem_link TEXT         NOT NULL,

    platform     VARCHAR(100),

    difficulty   VARCHAR(50),

    solved       BOOLEAN      NOT NULL DEFAULT TRUE,

    revise_count INTEGER      NOT NULL DEFAULT 0,

    created_at   TIMESTAMPTZ  NOT NULL DEFAULT now(),

    updated_at   TIMESTAMPTZ  NOT NULL DEFAULT now()
);

CREATE UNIQUE INDEX uk_questions_problem_link
    ON questions (problem_link);

CREATE INDEX idx_questions_difficulty
    ON questions (difficulty);

CREATE INDEX idx_questions_platform
    ON questions (platform);


/* =========================================================
   QUESTION ↔ TOPIC (MANY-TO-MANY)
   ========================================================= */

CREATE TABLE question_topics
(
    question_id BIGINT NOT NULL,
    topic_id    BIGINT NOT NULL,

    CONSTRAINT pk_question_topics
        PRIMARY KEY (question_id, topic_id),

    CONSTRAINT fk_qt_question
        FOREIGN KEY (question_id)
            REFERENCES questions (id)
            ON DELETE CASCADE,

    CONSTRAINT fk_qt_topic
        FOREIGN KEY (topic_id)
            REFERENCES topics (id)
            ON DELETE CASCADE
);


/* =========================================================
   QUESTION ↔ PATTERN (MANY-TO-MANY)
   ========================================================= */

CREATE TABLE question_patterns
(
    question_id BIGINT NOT NULL,
    pattern_id  BIGINT NOT NULL,

    CONSTRAINT pk_question_patterns
        PRIMARY KEY (question_id, pattern_id),

    CONSTRAINT fk_qp_question
        FOREIGN KEY (question_id)
            REFERENCES questions (id)
            ON DELETE CASCADE,

    CONSTRAINT fk_qp_pattern
        FOREIGN KEY (pattern_id)
            REFERENCES patterns (id)
            ON DELETE CASCADE
);

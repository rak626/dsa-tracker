ALTER TABLE questions
    ADD COLUMN last_attempted_at TIMESTAMPTZ NULL;
CREATE INDEX idx_question_last_attempted_at
    ON questions(last_attempted_at);

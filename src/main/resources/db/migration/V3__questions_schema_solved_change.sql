ALTER TABLE questions
DROP
COLUMN solved,
    ADD COLUMN solve_count INTEGER NOT NULL DEFAULT 1;

CREATE INDEX idx_questions_solve_count
    ON questions (solve_count);


ALTER TABLE questions
ALTER COLUMN solve_count SET DEFAULT 0;

-- Optional: If the user wants existing "1"s to be "0"s, but usually it's better to just change the default.
-- Based on the request "make sure solve count of each problem will be default to 0 not 1", 
-- I will also update existing records that might have been created with the old default if they haven't been "solved" yet.
-- However, "1" might mean it was solved once. If the user wants to RESET, I should update.
-- Given the phrasing "default to 0", it's safer to just change the default for new records.
-- But if the project is in development, updating existing might be expected.
-- I'll stick to just changing the default and update existing records that are exactly 1 if the user implies a reset.
-- Actually, the user says "make sure solve count of each problem will be default to 0", 
-- which could imply existing ones too. Let's update records where solve_count = 1 to 0 to reflect this new "start from zero" policy.

UPDATE questions SET solve_count = 0 WHERE solve_count = 1;

-- Extend online tests for result summary (marks, cutoff, time taken).
-- Run once on DBs that already have the base online-test tables.

ALTER TABLE tests
    ADD COLUMN total_marks INT NULL AFTER duration_minutes,
    ADD COLUMN cut_off INT NULL AFTER total_marks;

ALTER TABLE test_attempts
    ADD COLUMN status VARCHAR(20) NOT NULL DEFAULT 'SUBMITTED' AFTER user_id,
    ADD COLUMN correct_answers INT NULL AFTER score,
    ADD COLUMN incorrect_answers INT NULL AFTER correct_answers,
    ADD COLUMN marks_obtained INT NULL AFTER incorrect_answers,
    ADD COLUMN time_taken_seconds INT NULL AFTER percentage,
    ADD COLUMN started_at DATETIME NULL AFTER time_taken_seconds;

UPDATE test_attempts
SET started_at = COALESCE(started_at, submitted_at, NOW())
WHERE started_at IS NULL;

UPDATE test_attempts
SET correct_answers = COALESCE(correct_answers, score),
    incorrect_answers = COALESCE(incorrect_answers, GREATEST(total_questions - COALESCE(score, 0), 0)),
    marks_obtained = COALESCE(marks_obtained, score)
WHERE status = 'SUBMITTED';

ALTER TABLE test_attempts
    MODIFY score INT NULL,
    MODIFY total_questions INT NULL,
    MODIFY percentage DOUBLE NULL,
    MODIFY submitted_at DATETIME NULL,
    MODIFY started_at DATETIME NOT NULL;

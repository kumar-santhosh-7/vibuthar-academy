CREATE TABLE IF NOT EXISTS tests (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    test_id VARCHAR(20) UNIQUE,
    title VARCHAR(200) NOT NULL,
    description TEXT,
    pdf_url VARCHAR(500),
    duration_minutes INT,
    status VARCHAR(20) NOT NULL,
    created_at DATETIME NOT NULL,
    updated_at DATETIME NOT NULL
);

CREATE TABLE IF NOT EXISTS test_questions (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    test_pk BIGINT NOT NULL,
    question_text TEXT NOT NULL,
    order_index INT NOT NULL,
    created_at DATETIME NOT NULL,
    updated_at DATETIME NOT NULL,
    INDEX idx_test_questions_test_pk (test_pk),
    CONSTRAINT fk_test_questions_test
        FOREIGN KEY (test_pk) REFERENCES tests(id)
);

CREATE TABLE IF NOT EXISTS question_options (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    question_id BIGINT NOT NULL,
    option_text VARCHAR(1000) NOT NULL,
    is_correct BOOLEAN NOT NULL,
    order_index INT NOT NULL,
    created_at DATETIME NOT NULL,
    updated_at DATETIME NOT NULL,
    INDEX idx_question_options_question_id (question_id),
    CONSTRAINT fk_question_options_question
        FOREIGN KEY (question_id) REFERENCES test_questions(id)
);

CREATE TABLE IF NOT EXISTS test_attempts (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    test_pk BIGINT NOT NULL,
    user_id VARCHAR(20) NOT NULL,
    score INT NOT NULL,
    total_questions INT NOT NULL,
    percentage DOUBLE NOT NULL,
    submitted_at DATETIME NOT NULL,
    UNIQUE KEY uk_test_attempt_user (test_pk, user_id),
    INDEX idx_test_attempts_test_pk (test_pk),
    CONSTRAINT fk_test_attempts_test
        FOREIGN KEY (test_pk) REFERENCES tests(id)
);

CREATE TABLE IF NOT EXISTS attempt_answers (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    attempt_id BIGINT NOT NULL,
    question_id BIGINT NOT NULL,
    selected_option_id BIGINT NOT NULL,
    is_correct BOOLEAN NOT NULL,
    INDEX idx_attempt_answers_attempt_id (attempt_id),
    CONSTRAINT fk_attempt_answers_attempt
        FOREIGN KEY (attempt_id) REFERENCES test_attempts(id)
);

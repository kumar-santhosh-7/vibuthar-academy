CREATE TABLE IF NOT EXISTS video_watch_progress (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id VARCHAR(20) NOT NULL,
    video_id BIGINT NOT NULL,
    course_id VARCHAR(20) NOT NULL,
    watched_seconds INT NOT NULL DEFAULT 0,
    duration_seconds INT,
    progress_percent DECIMAL(5,2) NOT NULL DEFAULT 0.00,
    completed TINYINT(1) NOT NULL DEFAULT 0,
    last_watched_at DATETIME NOT NULL,
    created_at DATETIME NOT NULL,
    updated_at DATETIME NOT NULL,
    UNIQUE KEY uk_user_video (user_id, video_id),
    INDEX idx_vwp_course_id (course_id),
    INDEX idx_vwp_user_course (user_id, course_id),
    INDEX idx_vwp_video_completed (video_id, completed),
    CONSTRAINT fk_vwp_video FOREIGN KEY (video_id) REFERENCES course_videos(id) ON DELETE CASCADE
);

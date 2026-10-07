CREATE TABLE IF NOT EXISTS course_payments (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id VARCHAR(20) NOT NULL,
    course_id VARCHAR(20) NOT NULL,
    amount DECIMAL(10,2) NOT NULL,
    amount_paise BIGINT NOT NULL,
    currency VARCHAR(10) NOT NULL,
    razorpay_order_id VARCHAR(100) NOT NULL UNIQUE,
    razorpay_payment_id VARCHAR(100) UNIQUE,
    status VARCHAR(20) NOT NULL,
    payment_method VARCHAR(50),
    subscription_id BIGINT,
    receipt VARCHAR(100),
    created_at DATETIME NOT NULL,
    updated_at DATETIME NOT NULL,
    INDEX idx_course_payments_user_id (user_id),
    INDEX idx_course_payments_course_id (course_id),
    INDEX idx_course_payments_status (status)
);

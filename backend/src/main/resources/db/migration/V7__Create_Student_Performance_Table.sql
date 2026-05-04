CREATE TABLE student_performances (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT UNIQUE NOT NULL,
    assigned_sp INTEGER,
    accomplished_sp INTEGER,
    performance_ratio DOUBLE PRECISION,
    last_updated_at TIMESTAMP,
    CONSTRAINT fk_student_performance_user FOREIGN KEY (user_id) REFERENCES users(user_id)
);

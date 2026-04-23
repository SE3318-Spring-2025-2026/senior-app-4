CREATE TABLE IF NOT EXISTS deliverables (
    id BIGSERIAL PRIMARY KEY,
    group_id BIGINT NOT NULL,
    deliverable_type VARCHAR(50) NOT NULL,
    content TEXT NOT NULL,
    file_url TEXT,
    status VARCHAR(50) NOT NULL,
    committee_id BIGINT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

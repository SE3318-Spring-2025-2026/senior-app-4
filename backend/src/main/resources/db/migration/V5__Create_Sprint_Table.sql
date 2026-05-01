CREATE TABLE IF NOT EXISTS sprint (
    id BIGSERIAL PRIMARY KEY,
    sprint_name VARCHAR(255) NOT NULL,
    start_date DATE NOT NULL,
    end_date DATE NOT NULL,
    status VARCHAR(50) NOT NULL DEFAULT 'Active',
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_sprint_status ON sprint(status);
CREATE INDEX idx_sprint_dates ON sprint(start_date, end_date);
CREATE INDEX idx_sprint_active_date ON sprint(start_date, end_date) WHERE status = 'Active';

CREATE TABLE IF NOT EXISTS committees (
    committee_id BIGSERIAL PRIMARY KEY,
    committee_name VARCHAR(100) NOT NULL,
    description TEXT,
    status VARCHAR(50) NOT NULL DEFAULT 'ACTIVE',
    created_by BIGINT,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    version INTEGER DEFAULT 0
);

CREATE INDEX idx_committee_id ON committees(committee_id);
CREATE INDEX idx_committee_status ON committees(status);
CREATE INDEX idx_committee_created_by ON committees(created_by);

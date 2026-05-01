CREATE TABLE IF NOT EXISTS github_integrations (
    id BIGSERIAL PRIMARY KEY,
    group_id BIGINT NOT NULL UNIQUE,
    organization_name VARCHAR(255),
    github_pat_encrypted TEXT,
    status VARCHAR(50) NOT NULL DEFAULT 'INACTIVE',
    last_error TEXT,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_github_integrations_group FOREIGN KEY (group_id) REFERENCES groups(id)
);

CREATE TABLE IF NOT EXISTS jira_integrations (
    id BIGSERIAL PRIMARY KEY,
    group_id BIGINT NOT NULL UNIQUE,
    jira_space_url VARCHAR(255) NOT NULL,
    api_key TEXT,
    project_key VARCHAR(255) NOT NULL,
    status VARCHAR(50) NOT NULL DEFAULT 'ACTIVE',
    last_error TEXT,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_jira_integrations_group FOREIGN KEY (group_id) REFERENCES groups(id)
);

CREATE INDEX idx_github_integrations_group ON github_integrations(group_id);
CREATE INDEX idx_jira_integrations_group ON jira_integrations(group_id);

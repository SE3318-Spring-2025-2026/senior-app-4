-- Add committee_id column to system_logs table for P5.9 Audit Integration (Retry)
ALTER TABLE system_logs ADD COLUMN IF NOT EXISTS committee_id BIGINT;

-- Create index for committee_id queries
CREATE INDEX IF NOT EXISTS idx_system_logs_committee_id_v4 ON system_logs(committee_id);

-- Add description column to sprint_issue_tracking table
-- Only if the table exists (V8 migration must have run first)
DO $$
BEGIN
  -- Check if table exists
  IF EXISTS (SELECT 1 FROM information_schema.tables WHERE table_name='sprint_issue_tracking') THEN
    -- Check if description column already exists
    IF NOT EXISTS (SELECT 1 FROM information_schema.columns
                   WHERE table_name='sprint_issue_tracking' AND column_name='description') THEN
      ALTER TABLE sprint_issue_tracking ADD COLUMN description TEXT;
    END IF;
  END IF;
END $$;

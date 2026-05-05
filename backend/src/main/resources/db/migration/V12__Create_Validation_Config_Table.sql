-- V12__Create_Validation_Config_Table.sql
-- Singleton table for AI validation runtime configuration (Process 7 / Issue #298).
-- id is always 1; enforced by PRIMARY KEY + CHECK constraint.
-- excludedFilePatterns is stored as a comma-separated string (converted in Java).

CREATE TABLE IF NOT EXISTS public.validation_config (
    id                       BIGINT          PRIMARY KEY,
    review_weight            INTEGER         NOT NULL DEFAULT 40,
    implementation_weight    INTEGER         NOT NULL DEFAULT 60,
    openai_model             VARCHAR(50)     NOT NULL DEFAULT 'gpt-4o',
    max_diff_lines           INTEGER         NOT NULL DEFAULT 500,
    excluded_file_patterns   TEXT            NOT NULL DEFAULT ''
);

-- Guarantee the singleton row is present with defaults.
INSERT INTO public.validation_config (id, review_weight, implementation_weight,
                                      openai_model, max_diff_lines, excluded_file_patterns)
VALUES (1, 40, 60, 'gpt-4o', 500, '')
ON CONFLICT (id) DO NOTHING;

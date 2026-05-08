-- Final grading model from the project definition PDF.
-- Idempotent because several Supabase environments already contain some of these tables.

ALTER TABLE public.sprint
    ADD COLUMN IF NOT EXISTS required_story_points INTEGER;

DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1
        FROM pg_constraint
        WHERE conname = 'chk_sprint_required_story_points_nonnegative'
    ) THEN
        ALTER TABLE public.sprint
            ADD CONSTRAINT chk_sprint_required_story_points_nonnegative
            CHECK (required_story_points IS NULL OR required_story_points >= 0);
    END IF;
END $$;

CREATE TABLE IF NOT EXISTS public.grading_criteria (
    id               BIGSERIAL PRIMARY KEY,
    deliverable_type VARCHAR(50) NOT NULL,
    grading_type     VARCHAR(20) NOT NULL DEFAULT 'SOFT',
    name             VARCHAR(255) NOT NULL,
    description      TEXT,
    weight           DOUBLE PRECISION NOT NULL,
    created_at       TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    created_by       BIGINT NOT NULL DEFAULT 0
);

ALTER TABLE public.grading_criteria
    ADD COLUMN IF NOT EXISTS id BIGSERIAL,
    ADD COLUMN IF NOT EXISTS deliverable_type VARCHAR(50),
    ADD COLUMN IF NOT EXISTS grading_type VARCHAR(20) DEFAULT 'SOFT',
    ADD COLUMN IF NOT EXISTS name VARCHAR(255),
    ADD COLUMN IF NOT EXISTS description TEXT,
    ADD COLUMN IF NOT EXISTS weight DOUBLE PRECISION,
    ADD COLUMN IF NOT EXISTS created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    ADD COLUMN IF NOT EXISTS created_by BIGINT DEFAULT 0;

UPDATE public.grading_criteria SET grading_type = 'SOFT' WHERE grading_type IS NULL;
UPDATE public.grading_criteria SET created_at = NOW() WHERE created_at IS NULL;
UPDATE public.grading_criteria SET created_by = 0 WHERE created_by IS NULL;

DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1
        FROM pg_constraint
        WHERE conrelid = 'public.grading_criteria'::regclass
          AND contype = 'p'
    ) THEN
        ALTER TABLE public.grading_criteria ADD PRIMARY KEY (id);
    END IF;
END $$;

CREATE INDEX IF NOT EXISTS idx_grading_criteria_deliverable ON public.grading_criteria(deliverable_type);

DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM pg_constraint WHERE conname = 'chk_grading_criteria_type'
    ) THEN
        ALTER TABLE public.grading_criteria
            ADD CONSTRAINT chk_grading_criteria_type
            CHECK (grading_type IN ('BINARY', 'SOFT'));
    END IF;
    IF NOT EXISTS (
        SELECT 1 FROM pg_constraint WHERE conname = 'chk_grading_criteria_weight'
    ) THEN
        ALTER TABLE public.grading_criteria
            ADD CONSTRAINT chk_grading_criteria_weight
            CHECK (weight >= 0 AND weight <= 100);
    END IF;
END $$;

CREATE TABLE IF NOT EXISTS public.grades (
    id            BIGSERIAL PRIMARY KEY,
    submission_id BIGINT NOT NULL,
    professor_id  BIGINT NOT NULL,
    score         DOUBLE PRECISION NOT NULL,
    feedback      TEXT,
    graded_at     TIMESTAMP NOT NULL DEFAULT NOW()
);

ALTER TABLE public.grades
    ADD COLUMN IF NOT EXISTS id BIGSERIAL,
    ADD COLUMN IF NOT EXISTS submission_id BIGINT,
    ADD COLUMN IF NOT EXISTS professor_id BIGINT,
    ADD COLUMN IF NOT EXISTS score DOUBLE PRECISION,
    ADD COLUMN IF NOT EXISTS feedback TEXT,
    ADD COLUMN IF NOT EXISTS graded_at TIMESTAMP DEFAULT NOW();

UPDATE public.grades SET graded_at = NOW() WHERE graded_at IS NULL;

DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1
        FROM pg_constraint
        WHERE conrelid = 'public.grades'::regclass
          AND contype = 'p'
    ) THEN
        ALTER TABLE public.grades ADD PRIMARY KEY (id);
    END IF;
END $$;

CREATE INDEX IF NOT EXISTS idx_grades_submission_id ON public.grades(submission_id);
CREATE INDEX IF NOT EXISTS idx_grades_professor_id ON public.grades(professor_id);

CREATE TABLE IF NOT EXISTS public.sprint_advisor_grades (
    id                 BIGSERIAL PRIMARY KEY,
    group_id           BIGINT NOT NULL REFERENCES public.groups(id) ON DELETE CASCADE,
    sprint_id          BIGINT NOT NULL REFERENCES public.sprint(id) ON DELETE CASCADE,
    advisor_id         BIGINT NOT NULL REFERENCES public.users(user_id),
    scrum_grade        VARCHAR(1) NOT NULL CHECK (scrum_grade IN ('A', 'B', 'C', 'D', 'F')),
    code_review_grade  VARCHAR(1) NOT NULL CHECK (code_review_grade IN ('A', 'B', 'C', 'D', 'F')),
    created_at         TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    updated_at         TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    CONSTRAINT uq_sprint_advisor_grade UNIQUE (group_id, sprint_id, advisor_id)
);

CREATE INDEX IF NOT EXISTS idx_sag_group_id ON public.sprint_advisor_grades(group_id);
CREATE INDEX IF NOT EXISTS idx_sag_sprint_id ON public.sprint_advisor_grades(sprint_id);
CREATE INDEX IF NOT EXISTS idx_sag_advisor_id ON public.sprint_advisor_grades(advisor_id);

CREATE TABLE IF NOT EXISTS public.sprint_deliverable_weights (
    id               BIGSERIAL PRIMARY KEY,
    sprint_id        BIGINT NOT NULL REFERENCES public.sprint(id) ON DELETE CASCADE,
    deliverable_type VARCHAR(50) NOT NULL CHECK (deliverable_type IN (
        'PROPOSAL', 'REVISED_PROPOSAL', 'STATEMENT_OF_WORK', 'DEMONSTRATION'
    )),
    weight           NUMERIC(5,2) NOT NULL CHECK (weight >= 0 AND weight <= 100),
    created_at       TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    CONSTRAINT uq_sprint_deliverable UNIQUE (sprint_id, deliverable_type)
);

CREATE INDEX IF NOT EXISTS idx_sdw_sprint_id ON public.sprint_deliverable_weights(sprint_id);
CREATE INDEX IF NOT EXISTS idx_sdw_deliverable_type ON public.sprint_deliverable_weights(deliverable_type);

CREATE TABLE IF NOT EXISTS public.team_final_grades (
    id           BIGSERIAL PRIMARY KEY,
    group_id     BIGINT NOT NULL UNIQUE REFERENCES public.groups(id) ON DELETE CASCADE,
    team_grade   NUMERIC(6,2),
    published    BOOLEAN NOT NULL DEFAULT FALSE,
    computed_at  TIMESTAMP WITH TIME ZONE,
    published_at TIMESTAMP WITH TIME ZONE
);

CREATE INDEX IF NOT EXISTS idx_tfg_group_id ON public.team_final_grades(group_id);

CREATE TABLE IF NOT EXISTS public.student_final_grades (
    id          BIGSERIAL PRIMARY KEY,
    group_id    BIGINT NOT NULL REFERENCES public.groups(id) ON DELETE CASCADE,
    user_id     BIGINT NOT NULL REFERENCES public.users(user_id) ON DELETE CASCADE,
    final_grade NUMERIC(6,2),
    sp_ratio    NUMERIC(5,4),
    published   BOOLEAN NOT NULL DEFAULT FALSE,
    computed_at TIMESTAMP WITH TIME ZONE,
    CONSTRAINT uq_student_final_grade UNIQUE (group_id, user_id)
);

CREATE INDEX IF NOT EXISTS idx_sfg_user_id ON public.student_final_grades(user_id);
CREATE INDEX IF NOT EXISTS idx_sfg_group_id ON public.student_final_grades(group_id);

CREATE TABLE IF NOT EXISTS public.grade_criteria_scores (
    id             BIGSERIAL PRIMARY KEY,
    grade_id       BIGINT NOT NULL REFERENCES public.grades(id) ON DELETE CASCADE,
    criterion_id   BIGINT NOT NULL REFERENCES public.grading_criteria(id) ON DELETE CASCADE,
    raw_score      DOUBLE PRECISION NOT NULL,
    weighted_score DOUBLE PRECISION NOT NULL,
    created_at     TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    CONSTRAINT uq_grade_criterion UNIQUE (grade_id, criterion_id)
);

CREATE INDEX IF NOT EXISTS idx_gcs_grade_id ON public.grade_criteria_scores(grade_id);
CREATE INDEX IF NOT EXISTS idx_gcs_criterion_id ON public.grade_criteria_scores(criterion_id);

-- Some schemas were created with excluded_file_patterns as text[] while the
-- Java converter intentionally stores a comma-separated TEXT value.
DO $$
BEGIN
    IF EXISTS (
        SELECT 1
        FROM information_schema.columns
        WHERE table_schema = 'public'
          AND table_name = 'validation_config'
          AND column_name = 'excluded_file_patterns'
          AND data_type = 'ARRAY'
    ) THEN
        ALTER TABLE public.validation_config
            ALTER COLUMN excluded_file_patterns TYPE TEXT
            USING array_to_string(excluded_file_patterns, ',');
    END IF;
END $$;

ALTER TABLE public.validation_config
    ALTER COLUMN excluded_file_patterns SET DEFAULT '';

UPDATE public.validation_config
SET excluded_file_patterns = ''
WHERE excluded_file_patterns = '{}';

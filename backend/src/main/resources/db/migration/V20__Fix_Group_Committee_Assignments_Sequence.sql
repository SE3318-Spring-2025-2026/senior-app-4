-- Fix sequence for group_committee_assignments table
-- Resets the sequence to be greater than the current max ID to prevent duplicate key violations
SELECT setval(
    pg_get_serial_sequence('group_committee_assignments', 'assignment_id'),
    COALESCE((SELECT MAX(assignment_id) FROM group_committee_assignments), 0) + 1,
    false
);

-- Also fix other committee-related sequences that may be out of sync
SELECT setval(
    pg_get_serial_sequence('committee_advisors', 'committee_advisor_id'),
    COALESCE((SELECT MAX(committee_advisor_id) FROM committee_advisors), 0) + 1,
    false
);

SELECT setval(
    pg_get_serial_sequence('committee_jury_members', 'jury_member_id'),
    COALESCE((SELECT MAX(jury_member_id) FROM committee_jury_members), 0) + 1,
    false
);

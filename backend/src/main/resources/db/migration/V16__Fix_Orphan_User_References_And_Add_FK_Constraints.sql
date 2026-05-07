-- Clean up orphaned advisor/leader references in groups table
UPDATE groups
SET advisor_id = NULL
WHERE advisor_id IS NOT NULL
  AND advisor_id NOT IN (SELECT user_id FROM users);

-- leader_id is NOT NULL so we can't null it out — delete the group instead
-- (a group without a leader is invalid data)
DELETE FROM groups
WHERE leader_id NOT IN (SELECT user_id FROM users);

-- Add FK constraints so this can never happen again
ALTER TABLE groups
    ADD CONSTRAINT fk_groups_advisor
        FOREIGN KEY (advisor_id) REFERENCES users(user_id) ON DELETE SET NULL;

ALTER TABLE groups
    ADD CONSTRAINT fk_groups_leader
        FOREIGN KEY (leader_id) REFERENCES users(user_id) ON DELETE CASCADE;

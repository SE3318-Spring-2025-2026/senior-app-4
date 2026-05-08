-- Remove group_members rows that reference non-existent users
DELETE FROM group_members
WHERE user_id NOT IN (SELECT user_id FROM users);

-- Add FK constraint so this cannot happen again
ALTER TABLE group_members
    ADD CONSTRAINT fk_group_members_user
        FOREIGN KEY (user_id) REFERENCES users(user_id) ON DELETE CASCADE;

BEGIN;
ALTER TABLE user_profiles ADD COLUMN IF NOT EXISTS avatar_path VARCHAR(300);
-- Retained as a read-only source until legacy images have been migrated and verified.
CREATE TABLE IF NOT EXISTS user_avatars (
 user_id BIGINT PRIMARY KEY REFERENCES users(id) ON DELETE CASCADE,
 image_data BYTEA NOT NULL
);
COMMIT;

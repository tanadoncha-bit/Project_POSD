-- Stop every running app instance before applying; start only the updated version afterwards.
-- Run the return-damage migration first if not already applied.
BEGIN;
CREATE TABLE IF NOT EXISTS app_migrations (version VARCHAR(100) PRIMARY KEY);
CREATE TABLE IF NOT EXISTS role_management_lock (id BIGINT PRIMARY KEY);
INSERT INTO role_management_lock(id) SELECT 1 WHERE NOT EXISTS (SELECT 1 FROM role_management_lock WHERE id=1);
CREATE TABLE IF NOT EXISTS role_audit (
 id BIGSERIAL PRIMARY KEY, actor_username VARCHAR(50) NOT NULL,
 target_username VARCHAR(50) NOT NULL, old_role VARCHAR(20), new_role VARCHAR(20) NOT NULL,
 changed_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);
SELECT id FROM role_management_lock WHERE id=1 FOR UPDATE;
UPDATE users SET role=CASE role WHEN 'STAFF' THEN 'USER' WHEN 'MANAGER' THEN 'STAFF' ELSE role END
WHERE NOT EXISTS (SELECT 1 FROM app_migrations WHERE version='user_roles_v2');
ALTER TABLE users ALTER COLUMN role SET DEFAULT 'USER';
INSERT INTO app_migrations(version) SELECT 'user_roles_v2'
WHERE NOT EXISTS (SELECT 1 FROM app_migrations WHERE version='user_roles_v2');
COMMIT;

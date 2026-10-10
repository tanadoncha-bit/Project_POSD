-- Existing installations must have completed the user_roles_v2 upgrade.
DO $$ BEGIN
 IF NOT EXISTS (SELECT 1 FROM app_migrations WHERE version='user_roles_v2') THEN
  RAISE EXCEPTION 'Apply the legacy user role migration before enabling Flyway';
 END IF;
END $$;

CREATE TABLE IF NOT EXISTS borrow_workflow_audit (
 id BIGSERIAL PRIMARY KEY,
 request_id BIGINT NOT NULL,
 actor_username VARCHAR(100) NOT NULL,
 action VARCHAR(30) NOT NULL,
 changed_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);
CREATE INDEX IF NOT EXISTS idx_workflow_audit_request ON borrow_workflow_audit(request_id, id);


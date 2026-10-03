BEGIN;
CREATE TABLE IF NOT EXISTS borrow_workflow_audit (
 id BIGSERIAL PRIMARY KEY,
 request_id BIGINT NOT NULL,
 actor_username VARCHAR(100) NOT NULL,
 action VARCHAR(30) NOT NULL,
 changed_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);
CREATE INDEX IF NOT EXISTS idx_workflow_audit_request ON borrow_workflow_audit(request_id, id);
COMMIT;

ALTER TABLE users ADD COLUMN security_version BIGINT NOT NULL DEFAULT 0;
ALTER TABLE delivery_jobs ADD COLUMN delivery_key VARCHAR(36) DEFAULT gen_random_uuid()::text;
UPDATE delivery_jobs SET delivery_key=gen_random_uuid()::text WHERE delivery_key IS NULL;
ALTER TABLE delivery_jobs ALTER COLUMN delivery_key SET NOT NULL;
ALTER TABLE delivery_jobs ADD COLUMN lease_token VARCHAR(36);
ALTER TABLE delivery_jobs ADD COLUMN lease_until TIMESTAMP WITH TIME ZONE;
CREATE TABLE email_changes (
    user_id BIGINT PRIMARY KEY REFERENCES users(id) ON DELETE CASCADE,
    old_email VARCHAR(100) NOT NULL,
    new_email VARCHAR(100) NOT NULL,
    token_hash VARCHAR(64) NOT NULL UNIQUE,
    security_version BIGINT NOT NULL,
    expires_at TIMESTAMP WITH TIME ZONE NOT NULL,
    requested_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- Existing naive values were written in the database session zone; preserve that interpretation.
ALTER TABLE delivery_jobs ALTER COLUMN next_attempt TYPE TIMESTAMP WITH TIME ZONE USING next_attempt AT TIME ZONE current_setting('TimeZone');
ALTER TABLE email_verifications ALTER COLUMN expires_at TYPE TIMESTAMP WITH TIME ZONE USING expires_at AT TIME ZONE current_setting('TimeZone');
ALTER TABLE email_verifications ALTER COLUMN requested_at TYPE TIMESTAMP WITH TIME ZONE USING requested_at AT TIME ZONE current_setting('TimeZone');
ALTER TABLE email_verifications ALTER COLUMN verified_at TYPE TIMESTAMP WITH TIME ZONE USING verified_at AT TIME ZONE current_setting('TimeZone');

CREATE TABLE external_identities (
    provider VARCHAR(30) NOT NULL,
    subject VARCHAR(255) NOT NULL,
    user_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (provider,subject),
    UNIQUE (provider,user_id)
);

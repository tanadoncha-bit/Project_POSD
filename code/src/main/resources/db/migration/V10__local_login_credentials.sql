ALTER TABLE users ADD COLUMN local_password_enabled BOOLEAN NOT NULL DEFAULT true;
-- Earlier Google-only accounts were assigned opaque generated usernames and unusable passwords.
UPDATE users SET local_password_enabled=false
WHERE username ~ '^google_[0-9a-f]{32}$'
AND EXISTS (SELECT 1 FROM external_identities i WHERE i.user_id=users.id AND i.provider='google');

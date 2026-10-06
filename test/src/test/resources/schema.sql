DROP TABLE IF EXISTS external_identities;
DROP TABLE IF EXISTS user_avatars;
DROP TABLE IF EXISTS role_audit;
DROP TABLE IF EXISTS role_management_lock;
DROP TABLE IF EXISTS app_migrations;
DROP TABLE IF EXISTS return_inspections;
DROP TABLE IF EXISTS return_records;
DROP TABLE IF EXISTS borrow_items;
DROP TABLE IF EXISTS borrow_requests;
DROP TABLE IF EXISTS equipment;
DROP TABLE IF EXISTS equipment_categories;
DROP TABLE IF EXISTS user_profiles;
DROP TABLE IF EXISTS users;

-- ========== USERS ==========
CREATE TABLE users (
    id          BIGSERIAL PRIMARY KEY,
    username    VARCHAR(50)  NOT NULL UNIQUE,
    email       VARCHAR(100) NOT NULL UNIQUE,
    password    VARCHAR(255) NOT NULL,
    role        VARCHAR(20)  NOT NULL DEFAULT 'USER',
    created_at  TIMESTAMP    NOT NULL DEFAULT now()
);

-- ========== USER_PROFILES (1:1 กับ users) ==========
CREATE TABLE user_profiles (
    id          BIGSERIAL PRIMARY KEY,
    user_id     BIGINT       NOT NULL UNIQUE REFERENCES users(id) ON DELETE CASCADE,
    full_name   VARCHAR(150) NOT NULL,
    phone       VARCHAR(20),
    avatar_path VARCHAR(300),
    department  VARCHAR(100)
);

-- ========== EQUIPMENT_CATEGORIES ==========
CREATE TABLE equipment_categories (
    id          BIGSERIAL PRIMARY KEY,
    name        VARCHAR(100) NOT NULL UNIQUE,
    description VARCHAR(300)
);

-- ========== EQUIPMENT (1:N จาก equipment_categories) ==========
CREATE TABLE equipment (
    id              BIGSERIAL PRIMARY KEY,
    asset_code      VARCHAR(30)  NOT NULL UNIQUE,
    name            VARCHAR(150) NOT NULL,
    category_id     BIGINT       NOT NULL REFERENCES equipment_categories(id),
    status          VARCHAR(20)  NOT NULL DEFAULT 'AVAILABLE',
    purchase_price NUMERIC(12,2) CHECK (purchase_price >= 0),
    purchase_date   DATE
);
CREATE INDEX idx_equipment_status ON equipment(status);

-- ========== BORROW_REQUESTS (1:N จาก users) ==========
CREATE TABLE borrow_requests (
    id          BIGSERIAL PRIMARY KEY,
    user_id     BIGINT      NOT NULL REFERENCES users(id),
    borrow_date DATE        NOT NULL,
    due_date    DATE        NOT NULL,
    status      VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    note        VARCHAR(500),
    created_at  TIMESTAMP   NOT NULL DEFAULT now()
);
CREATE INDEX idx_borrow_request_status ON borrow_requests(status);
CREATE INDEX idx_borrow_request_due_date ON borrow_requests(due_date);

-- ========== BORROW_ITEMS (1:N จาก borrow_requests และ equipment) ==========
CREATE TABLE borrow_items (
    id                  BIGSERIAL PRIMARY KEY,
    borrow_request_id   BIGINT NOT NULL REFERENCES borrow_requests(id) ON DELETE CASCADE,
    equipment_id        BIGINT NOT NULL REFERENCES equipment(id),
    quantity            INT    NOT NULL DEFAULT 1,
    condition_on_borrow VARCHAR(200)
);

-- ========== RETURN_RECORDS (1:0..1 กับ borrow_requests) ==========
CREATE TABLE return_records (
    id                  BIGSERIAL PRIMARY KEY,
    borrow_request_id   BIGINT NOT NULL UNIQUE REFERENCES borrow_requests(id) ON DELETE CASCADE,
    return_date         DATE           NOT NULL,
    condition           VARCHAR(50)    NOT NULL,
    fine_amount         NUMERIC(10,2)  DEFAULT 0,
    damage_amount       NUMERIC(14,2) NOT NULL DEFAULT 0,
    remark              VARCHAR(500)
);

CREATE TABLE IF NOT EXISTS return_inspections (
    return_record_id BIGINT NOT NULL REFERENCES return_records(id) ON DELETE CASCADE,
    item_order INTEGER NOT NULL,
    equipment_id BIGINT NOT NULL,
    equipment_name VARCHAR(150) NOT NULL,
    condition VARCHAR(50) NOT NULL,
    purchase_price NUMERIC(12,2),
    damage_rate NUMERIC(3,2) NOT NULL,
    damage_amount NUMERIC(14,2) NOT NULL,
    remark VARCHAR(500),
    PRIMARY KEY (return_record_id, item_order)
);


CREATE TABLE IF NOT EXISTS role_management_lock (id BIGINT PRIMARY KEY);
INSERT INTO role_management_lock(id) SELECT 1 WHERE NOT EXISTS (SELECT 1 FROM role_management_lock WHERE id=1);
CREATE TABLE IF NOT EXISTS role_audit (
 id BIGSERIAL PRIMARY KEY, actor_username VARCHAR(50) NOT NULL,
 target_username VARCHAR(50) NOT NULL, old_role VARCHAR(20), new_role VARCHAR(20) NOT NULL,
 changed_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);




ALTER TABLE equipment ADD COLUMN IF NOT EXISTS image_url VARCHAR(1000);
ALTER TABLE equipment ADD COLUMN IF NOT EXISTS specifications TEXT;

ALTER TABLE equipment ADD COLUMN IF NOT EXISTS storage_slot VARCHAR(100);

CREATE TABLE IF NOT EXISTS borrow_workflow_audit (
 id BIGSERIAL PRIMARY KEY,
 request_id BIGINT NOT NULL,
 actor_username VARCHAR(100) NOT NULL,
 action VARCHAR(30) NOT NULL,
 changed_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);
CREATE INDEX IF NOT EXISTS idx_workflow_audit_request ON borrow_workflow_audit(request_id, id);

ALTER TABLE borrow_items ADD COLUMN IF NOT EXISTS snapshot_name VARCHAR(150);
ALTER TABLE borrow_items ADD COLUMN IF NOT EXISTS snapshot_asset_code VARCHAR(30);
ALTER TABLE borrow_items ADD COLUMN IF NOT EXISTS snapshot_storage_slot VARCHAR(100);
ALTER TABLE borrow_items ADD COLUMN IF NOT EXISTS snapshot_image_url VARCHAR(1000);
ALTER TABLE borrow_items ADD COLUMN IF NOT EXISTS snapshot_category_name VARCHAR(150);
CREATE UNIQUE INDEX IF NOT EXISTS uq_equipment_storage_slot ON equipment(storage_slot);

CREATE INDEX IF NOT EXISTS idx_borrow_item_snapshot_image ON borrow_items(snapshot_image_url);

ALTER TABLE borrow_requests ADD COLUMN IF NOT EXISTS daily_fine NUMERIC(12,2);
ALTER TABLE borrow_requests ADD COLUMN IF NOT EXISTS grace_days INT;
ALTER TABLE borrow_requests ADD COLUMN IF NOT EXISTS scratch_rate NUMERIC(5,4);
ALTER TABLE borrow_requests ADD COLUMN IF NOT EXISTS damage_rate NUMERIC(5,4);
ALTER TABLE borrow_requests ADD COLUMN IF NOT EXISTS loss_rate NUMERIC(5,4);
ALTER TABLE borrow_items ADD COLUMN IF NOT EXISTS snapshot_purchase_price NUMERIC(12,2);

ALTER TABLE borrow_items ADD COLUMN IF NOT EXISTS returned_on DATE;

ALTER TABLE borrow_requests ADD COLUMN IF NOT EXISTS rejection_reason VARCHAR(500);
CREATE TABLE IF NOT EXISTS settlements (request_id BIGINT PRIMARY KEY REFERENCES borrow_requests(id), amount NUMERIC(16,2) NOT NULL, reference VARCHAR(500) NOT NULL, actor_username VARCHAR(50) NOT NULL, paid_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP);
CREATE TABLE IF NOT EXISTS equipment_repairs (id BIGSERIAL PRIMARY KEY, equipment_id BIGINT NOT NULL REFERENCES equipment(id), actor_username VARCHAR(50) NOT NULL, note VARCHAR(500) NOT NULL, repaired_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP);

CREATE TABLE IF NOT EXISTS delivery_jobs (id BIGSERIAL PRIMARY KEY, kind VARCHAR(30) NOT NULL, recipient VARCHAR(320) NOT NULL, subject VARCHAR(200) NOT NULL, payload TEXT NOT NULL, completed BOOLEAN NOT NULL DEFAULT false, attempts INT NOT NULL DEFAULT 0, next_attempt TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP, last_error VARCHAR(150));
CREATE INDEX IF NOT EXISTS idx_delivery_jobs_pending ON delivery_jobs(completed,next_attempt);
CREATE TABLE IF NOT EXISTS email_verifications (user_id BIGINT PRIMARY KEY REFERENCES users(id) ON DELETE CASCADE, email VARCHAR(320) NOT NULL, token_hash VARCHAR(64), expires_at TIMESTAMP, requested_at TIMESTAMP NOT NULL, verified_at TIMESTAMP);

CREATE TABLE IF NOT EXISTS locker_access (
 request_id BIGINT PRIMARY KEY REFERENCES borrow_requests(id) ON DELETE CASCADE,
 pin VARCHAR(6), slots TEXT NOT NULL, opened BOOLEAN NOT NULL DEFAULT false
);

CREATE TABLE external_identities (
    provider VARCHAR(30) NOT NULL,
    subject VARCHAR(255) NOT NULL,
    user_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (provider,subject),
    UNIQUE (provider,user_id)
);

ALTER TABLE users ADD COLUMN local_password_enabled BOOLEAN NOT NULL DEFAULT true;

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
    role        VARCHAR(20)  NOT NULL DEFAULT 'STAFF',
    created_at  TIMESTAMP    NOT NULL DEFAULT now()
);

-- ========== USER_PROFILES (1:1 กับ users) ==========
CREATE TABLE user_profiles (
    id          BIGSERIAL PRIMARY KEY,
    user_id     BIGINT       NOT NULL UNIQUE REFERENCES users(id) ON DELETE CASCADE,
    full_name   VARCHAR(150) NOT NULL,
    phone       VARCHAR(20),
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
    remark              VARCHAR(500)
);

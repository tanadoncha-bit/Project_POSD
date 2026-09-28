-- Run once before starting the updated app. Preserves existing equipment and returns.
BEGIN;
ALTER TABLE equipment ADD COLUMN IF NOT EXISTS purchase_price NUMERIC(12,2) CHECK (purchase_price >= 0);
ALTER TABLE return_records ADD COLUMN IF NOT EXISTS damage_amount NUMERIC(14,2) NOT NULL DEFAULT 0;
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
COMMIT;

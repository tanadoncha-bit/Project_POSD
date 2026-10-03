BEGIN;
ALTER TABLE borrow_requests ADD COLUMN IF NOT EXISTS daily_fine NUMERIC(12,2);
ALTER TABLE borrow_requests ADD COLUMN IF NOT EXISTS grace_days INT;
ALTER TABLE borrow_requests ADD COLUMN IF NOT EXISTS scratch_rate NUMERIC(5,4);
ALTER TABLE borrow_requests ADD COLUMN IF NOT EXISTS damage_rate NUMERIC(5,4);
ALTER TABLE borrow_requests ADD COLUMN IF NOT EXISTS loss_rate NUMERIC(5,4);
ALTER TABLE borrow_items ADD COLUMN IF NOT EXISTS snapshot_purchase_price NUMERIC(12,2);
-- Legacy backfill uses current values; original historical prices cannot be recovered.
UPDATE borrow_requests b SET daily_fine=CASE WHEN u.role='VIP' THEN 30 ELSE 50 END,
 grace_days=CASE WHEN u.role='VIP' THEN 2 ELSE 0 END, scratch_rate=0.20, damage_rate=0.50, loss_rate=1.00
FROM users u WHERE b.user_id=u.id AND b.daily_fine IS NULL;
UPDATE borrow_items b SET snapshot_purchase_price=e.purchase_price FROM equipment e
WHERE b.equipment_id=e.id AND b.snapshot_purchase_price IS NULL;
ALTER TABLE borrow_items ADD COLUMN IF NOT EXISTS returned_on DATE;
UPDATE borrow_items b SET returned_on=r.return_date FROM return_records r
WHERE b.borrow_request_id=r.borrow_request_id AND b.returned_on IS NULL;
ALTER TABLE borrow_requests ADD COLUMN IF NOT EXISTS rejection_reason VARCHAR(500);
CREATE TABLE IF NOT EXISTS settlements (request_id BIGINT PRIMARY KEY REFERENCES borrow_requests(id), amount NUMERIC(16,2) NOT NULL, reference VARCHAR(500) NOT NULL, actor_username VARCHAR(50) NOT NULL, paid_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP);
CREATE TABLE IF NOT EXISTS equipment_repairs (id BIGSERIAL PRIMARY KEY, equipment_id BIGINT NOT NULL REFERENCES equipment(id), actor_username VARCHAR(50) NOT NULL, note VARCHAR(500) NOT NULL, repaired_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP);
COMMIT;

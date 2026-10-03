-- Stop all app instances before running. Apply previous migrations first.
-- Inspect duplicates first; clear/reassign conflicting slots intentionally, then rerun.
-- This query must return no rows before the migration can finish:
SELECT lower(trim(storage_slot)) AS slot, string_agg(asset_code, ', ') AS assets
FROM equipment WHERE nullif(trim(storage_slot), '') IS NOT NULL
GROUP BY lower(trim(storage_slot)) HAVING count(*) > 1;

BEGIN;
ALTER TABLE borrow_items ADD COLUMN IF NOT EXISTS snapshot_name VARCHAR(150);
ALTER TABLE borrow_items ADD COLUMN IF NOT EXISTS snapshot_asset_code VARCHAR(30);
ALTER TABLE borrow_items ADD COLUMN IF NOT EXISTS snapshot_storage_slot VARCHAR(100);
ALTER TABLE borrow_items ADD COLUMN IF NOT EXISTS snapshot_image_url VARCHAR(1000);
ALTER TABLE borrow_items ADD COLUMN IF NOT EXISTS snapshot_category_name VARCHAR(150);
-- Legacy rows can only capture current data; this cannot recover overwritten history.
UPDATE borrow_items b SET snapshot_name=e.name, snapshot_asset_code=e.asset_code,
 snapshot_storage_slot=e.storage_slot, snapshot_image_url=e.image_url,
 snapshot_category_name=c.name
FROM equipment e JOIN equipment_categories c ON c.id=e.category_id
WHERE b.equipment_id=e.id AND b.snapshot_name IS NULL;
UPDATE equipment SET storage_slot=nullif(upper(trim(storage_slot)), '');
-- NULL/unassigned slots may repeat; every actual slot is unique, ignoring case/spaces.
CREATE UNIQUE INDEX IF NOT EXISTS uq_equipment_storage_slot
 ON equipment (lower(trim(storage_slot)));
CREATE INDEX IF NOT EXISTS idx_borrow_item_snapshot_image ON borrow_items(snapshot_image_url);
COMMIT;

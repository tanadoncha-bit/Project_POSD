-- Allow locker descriptions to exceed the original 2000-character limit.
ALTER TABLE locker_access ALTER COLUMN slots TYPE TEXT;

-- Retire legacy metadata only after avatar bytes have been migrated and verified in Storage.
DO $$ BEGIN
 IF EXISTS (SELECT 1 FROM users WHERE role NOT IN ('USER','STAFF','ADMIN','VIP')) THEN
  RAISE EXCEPTION 'Unsupported legacy roles remain; complete the role upgrade before retiring markers';
 END IF;
 IF to_regclass('user_avatars') IS NOT NULL THEN
  IF EXISTS (SELECT 1 FROM user_avatars a LEFT JOIN user_profiles p ON p.user_id=a.user_id
             WHERE p.avatar_path IS NULL OR trim(p.avatar_path)='') THEN
   RAISE EXCEPTION 'Legacy avatars still need a verified Storage copy; refusing to drop user_avatars';
  END IF;
 END IF;
END $$;
DROP TABLE IF EXISTS user_avatars;
DROP TABLE IF EXISTS app_migrations;

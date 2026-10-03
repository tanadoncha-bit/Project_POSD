# Running the completed borrowing workflow

Keep `spring.sql.init.mode=never` for the existing Supabase database. Do not rerun `schema.sql` on data you want to keep: that original initialization script drops tables.

The application now authenticates against `users`. The previous in-memory user/manager/admin accounts have been removed. On startup, `LegacyPasswordUpgrade` converts the original seed's plaintext passwords to BCrypt, preserving account IDs and passwords. Already encoded passwords are unchanged. No tables are recreated. Users can then change their password from Profile.

Registration always creates USER borrowers and their profile. Submitted role/id fields cannot grant privileges. ADMIN and STAFF are operators; USER and VIP can view/create/cancel their own requests. The personal history page always shows only the signed-in user's history, including for an administrator. The management page lists all requests.

Workflow: register -> sign in -> /borrow -> operator approves -> operator confirms physical pickup -> operator records return. Each equipment record is a unique asset; quantity must be one. Approval is not a reservation: the atomic AVAILABLE -> IN_USE transition at pickup determines the winner if multiple approved requests share an asset. The losing transaction returns a conflict and rolls back all assets in that request. Cancellation is recorded as CANCELLED.

Returns are recorded on the server's current date; backdated/future returns are rejected. Operators inspect every item separately. NORMAL/GOOD costs 0%, SCRATCH/MINOR_SCRATCHES costs 20%, DAMAGED costs 50%, and LOST costs 100% of the stored purchase price. Normal/scratched assets become AVAILABLE, damaged assets become MAINTENANCE, and lost assets become DISPOSED. Damage/loss requires a note and a known purchase price. All items must be returned together; partial returns are not supported. The existing `fineAmount` remains the late fee; `damageAmount` is separate and `totalAmount` is their sum. Each receipt snapshots the item price, rate, condition and charge. Both borrower and operator can use View charges on returned requests.

`OverdueScheduler` checks borrowed requests every 60 seconds. Configure `borrow.overdue.interval-ms` and `borrow.overdue.initial-delay-ms` as needed. All date operations use the server's local timezone; run production in Asia/Bangkok for Thai calendar dates. Notifications are an overdue banner in My Requests plus after-commit application logging. Outbound email/SMS and email verification are not configured; the verification button is disabled rather than claiming delivery.

Verification: `.\mvnw.cmd test` runs unit tests and full Spring MVC/security/JPA integration tests against an isolated H2 database in PostgreSQL mode using the project's schema.sql. It does not clear or seed Supabase. Tests cover registration/login/profile, password changes, ownership, CSRF, concurrent pickups, transaction rollback, inventory invariants, return conditions, fines, overdue transitions and server-rendered pages. PostgreSQL-specific contention under production load is not simulated by H2.


## September 2026 update: deploy before running the new code

Run `doc/migrations/20260926_return_damage.sql` against the existing database before starting this version. It adds columns and the inspection table, and does not drop or reseed any data. This migration is not run automatically (`spring.sql.init.mode=never`). Do not use `schema.sql` for an existing database.

Existing equipment prices stay NULL (unknown), not zero. Enter actual prices in Management > Inventory > Edit. New equipment requires a price in THB with at most two decimal places; zero is allowed only when the asset really has zero value. A charged return is blocked if its price is unknown. Existing return records retain their late fees, with damage amount zero and no historical per-item breakdown. The 20%/50% policy is implemented in `ReturnCondition`; it is a project rule, not a market valuation.

## Roles and first administrator setup

Roles are now USER (borrower), VIP (borrower with discounted late fees), STAFF (equipment operator), and ADMIN (operator plus user administration). Public registration always creates USER.

1. Stop all app instances. Run `doc/migrations/20260926_return_damage.sql` if not already applied, then `doc/migrations/20260926_user_roles.sql` in Supabase SQL Editor. The latter moves old STAFF borrowers to USER and old MANAGER operators to STAFF. Existing ADMIN/VIP accounts stay unchanged. A version marker makes rerunning safe. Do not start old application versions against the migrated database.
2. Start the updated app. Startup checks the migration marker and refuses to start if it is missing, preventing old STAFF borrowers from gaining operator rights.
3. If an ADMIN already exists, sign in with that account. If none exists, set these environment variables in the application's run configuration before startup:

   - `APP_BOOTSTRAP_ADMIN_USERNAME`: a new dedicated username (3-50 letters/numbers/dot/underscore/hyphen)
   - `APP_BOOTSTRAP_ADMIN_EMAIL`: a new email address
   - `APP_BOOTSTRAP_ADMIN_PASSWORD`: a unique password of at least 12 characters, at most 72 UTF-8 bytes

   Alternatively use Spring properties `app.bootstrap-admin.username`, `app.bootstrap-admin.email`, and `app.bootstrap-admin.password` in a private local configuration, never in committed source. Bootstrap creates a new account only when no ADMIN exists. It refuses to take over an existing username/email, stores a BCrypt hash and records a SYSTEM audit event. Remove these bootstrap variables after successful setup so they cannot recreate an account on a future empty database. No default password is provided.
4. Sign in through the normal login form, then open Management > Users & roles (`/admin/users`). Have colleagues register normally and assign STAFF or ADMIN from this page. The last ADMIN cannot be demoted, including concurrent attempts. Saving the same role is a no-op.
5. The role history records actor, target, old/new roles and server time (latest 100 entries shown). Permissions refresh from the database on every request, so already signed-in users gain/lose privileges without signing out. STAFF cannot change roles, delete equipment, or access the user list.

No production roles, passwords or database schema were changed automatically during development. The bootstrap password is set by the deployment operator; do not paste it in chat.


## Profile pictures: Supabase Storage

1. Stop the app and run `doc/migrations/20260927_avatar_storage.sql` in Supabase SQL Editor. This adds `user_profiles.avatar_path` and preserves the old binary table. It is safe to repeat.
2. In Supabase > Storage, create a **private** bucket named `avatars`. Allow `image/png`; a 2 MB bucket limit is enough because the app stores normalized 256 x 256 PNGs. No public upload/read policies are needed: the backend uses a service-role key.
3. Set server environment variables in the IDE run configuration:
   - `SUPABASE_URL`: your project API URL, e.g. `https://your-project.supabase.co` (not the database JDBC URL)
   - `SUPABASE_SERVICE_ROLE_KEY`: the legacy `service_role` API key from project settings. This must stay on the server, never in templates, JavaScript, Git or chat.
   - `SUPABASE_AVATAR_BUCKET`: `avatars` (default)
4. Start the app. Clicking the Profile portrait still selects and saves a JPG/PNG up to 2 MB. The server validates and normalizes it; the browser loads the image directly from Storage using a signed URL valid for one hour. Reload the page to renew an expired URL. The database stores only the object path for new uploads. Cached links remain usable until expiry; signing requires a small Storage API request when rendering a page.
5. To migrate existing images, set `MIGRATE_LEGACY_AVATARS=true` for one startup. It copies each unmigrated image to Storage and updates its path only after upload succeeds. Disable the flag afterwards. On failure, correct configuration and restart with the flag; completed records are skipped. Concurrent user uploads take priority. The old table remains a backup and provides a read fallback until each user is migrated. It is never deleted automatically.
6. Once every image has been migrated and checked, back up and clean up the legacy binary rows separately if you need to reclaim database space. Until then, historical bytes still occupy space. Keep the empty legacy table while the compatibility reader remains in the code.

Supabase outages do not prevent a page loading: the navbar falls back to initials, and failed uploads display a friendly message. Successful replacement deletes the previous Storage object after committing metadata. Failed metadata writes attempt to remove the newly uploaded object. Cleanup failures are logged without credentials; reconcile orphan objects against `user_profiles.avatar_path` if this happens. Database and Storage do not share an atomic transaction, so crash recovery is a documented operational limitation.

The bucket, keys and real data migration are deployment steps and have not been run automatically.

References: [Supabase Storage REST API](https://supabase.com/docs/reference/self-hosting-storage), [private buckets and signed URLs](https://supabase.com/docs/guides/storage/buckets/fundamentals).


## Toast notifications

Shared notifications live in `static/js/toast.js` and use `toast.success(message)`, `toast.error(message)`, or `toast.info(message)`. Default durations are configured in `config`: success/info 3500 ms, error 5500 ms. Override per message with `{ duration: 8000 }`; use zero for manual dismissal. Notifications appear at the top centre, pause while hovered/focused, deduplicate identical active messages, and keep at most four active notifications. Text is inserted as text, never HTML. Reduced-motion preferences are respected.

Server flash messages are marked `data-toast="success"` or `data-toast="error"` and consumed once when the page loads. Persistent domain information such as overdue loans and fee estimates stays on the page. No new dependency or database migration is required.


## Borrowing list

- Search results and catalog cards use the same equipment detail dialog.
- Add available assets to the borrowing list; the side panel supports removing assets and checkout.
- The list persists in sessionStorage for the current browser tab, scoped by username. It is not a reservation.
- Checkout checks current availability and uses the existing server-rendered borrowing form and API. All selected assets share one due date.
- Successful submission removes submitted assets from the list; failures preserve the selection.
- The server remains responsible for authorization and availability checks during submission.


## Equipment images and specifications

Run `doc/migrations/20260927_equipment_details.sql` before starting the updated application against an existing database. In Admin, add/edit equipment to supply an HTTPS image URL (or local `/images/` path) and specifications. Existing records retain their data and show an explicit placeholder until these optional fields are filled. Catalog and Search cards display the category; asset codes remain visible in the details and borrowing list.


## Management overview

Overview groups equipment by database category on horizontally scrollable shelves. Select an asset to edit it. The work queue prioritizes overdue, pending, then approved requests and links to the corresponding request in the Requests panel. Inventory uses a searchable status-filtered table. Browse Equipment in the top bar returns to the public catalog. Missing images use an explicit placeholder instead of an unrelated device image. On mobile the shelves become category grids.


### Equipment image uploads
Create a Private Supabase Storage bucket named `equipment`. The server uses the existing storage URL and service key. Optionally override `app.storage.equipment-bucket`. Operators choose a JPG/PNG (up to 2 MB, 4096px per side) in Add/Edit equipment. Images preserve aspect ratio and are resized to at most 1200px. Paths use the existing image_url column; no new migration is needed. If uploading fails after equipment is saved, retry Save in the same form; the created equipment ID is retained to avoid a duplicate.


### Workflow corrections (review follow-up)
Approval, pickup and return require a different operator from the borrower. Approval locks assets and rejects overlapping approved/active loans (both boundary dates inclusive); equipment currently unavailable cannot be approved. Approval does not guarantee physical availability if a prior borrower returns late. Management request pages contain 20 rows; summary counts are global. The first remediation pass required no new database migration. The second pass requires the workflow audit migration below; previously supplied equipment storage-slot migration is still required. Partial returns, settlements and valuation snapshots remain unimplemented.


### Pagination and workflow audit (2026-09-29)
Before starting the updated application against an existing database, run `doc/migrations/20260929_workflow_audit.sql` in the same database/schema used by the application. This adds an audit table and index without changing existing borrowing data. Restart the server after the migration. An absent audit table will cause borrowing actions to roll back rather than silently lose their audit records.

Catalog pages contain 12 assets; My Requests contains 10 requests and supports server-side status/keyword filtering with global owner-specific counts. Profile active loans and past requests have independent pages of 5 records. Checkout from the borrowing list requests only the selected assets. The direct legacy `/borrow` page without IDs still lists all available equipment.

Audit records capture request ID, actor username, action and timestamp in the same transaction as successful creation, approval, pickup, cancellation and return. Scheduled transitions use actor `SYSTEM`. The scheduled check cancels PENDING/APPROVED requests only after the due date (not on the due date), with audit action EXPIRED; BORROWED requests become OVERDUE. Existing overdue uncollected requests will be cancelled when the scheduled job next runs. The audit table is retained independently of request deletion; no audit viewer is provided yet.


### Equipment identity and unique slots (2026-09-29)
1. Before deploying this version, inspect duplicate slots using the SELECT at the top of `doc/migrations/20260929_equipment_history_slots.sql`. In the previous app, clear or reassign conflicting slots deliberately; a storage slot identifies one physical asset, including assets currently on loan or disposed. Clear a retired asset's slot explicitly before reusing it.
2. Stop every app instance, run the migration, then start the updated app. A duplicate slot aborts the migration; roll back, resolve the duplicates and rerun. This migration has not been run automatically against the live database.
3. Edit equipment now includes Category. Slot labels are trimmed and normalized to uppercase; blank means unassigned. Database uniqueness also protects concurrent writes.
4. New requests snapshot equipment name, asset code, category name, slot and image URL at submission. Request history, search and management queues read this snapshot. Updating inventory therefore does not rewrite the submitted identity. Referenced Storage images remain available after replacement. Externally hosted images can still change outside this system.
5. Legacy requests are backfilled with current inventory values only. Previously overwritten Dell/name/category information cannot be reconstructed without a backup or other reliable record. Never present this backfill as the original submission values.
6. Correcting a typo/category for the same physical asset is supported. Replacing a Dell laptop with a different Razer device should use a NEW equipment record/asset code; retire the old asset instead of renaming it. Both need independent histories.

This change snapshots identity, not fee policies or purchase valuation. Loan-time fee snapshots remain separate follow-up work.


## Final workflow upgrade (2026-10-03)

This section supersedes the historical rollout notes above, including statements that partial returns, settlements and fee snapshots are unimplemented, that the legacy borrow selector loads all assets, and the earlier manual migration startup instructions.

### Enable versioned migrations
- Existing installations must already have the earlier return-damage, roles v2, avatar storage and equipment details/slot upgrades. Resolve duplicate slots before enabling this profile. Keep a database backup.
- Stop old app instances. Set `APP_PROFILE=migrations` in the local `.env` (or activate the `migrations` Spring profile in the IDE), then start the updated application. Flyway baselines an existing schema at version 1, applies V2-V5 in order and verifies checksums on later starts. Leave this profile enabled for subsequent upgrades.
- A fresh database is created by V1 without seeded accounts or destructive SQL. Existing schemas skip V1. Flyway cleaning is disabled. Do not run `schema.sql` against production: it is the legacy H2 test initializer.
- V4 snapshots fee policy/valuation and adds partial returns, rejection reasons, payment records and repair logs. V5 adds delivery jobs and email verification. Do not run standalone manual migrations and edit already-applied Flyway scripts interchangeably.
- The migrations were exercised against a separate local PostgreSQL 18 cluster for fresh creation, existing-schema upgrade and repeated startup. No production migration was run.

### Operational rules
- New requests record prices and the standard/VIP daily rate, grace period and damage percentages at submission. Later inventory price or role changes do not alter these amounts. Legacy backfill uses currently available values; unknown historical prices cannot be reconstructed.
- Operators can select a subset in Return. Returned items become available/maintenance/disposed immediately. The request remains active until every item is returned. Late fees remain one fee per request and continue until its final return; intermediate totals are provisional. Repeated return of the same item is rejected.
- Reject requires a reason, stores the request as cancelled with the rejection reason, and preserves its audit history.
- Record payment is an acknowledgement of a payment already received, with a receipt/reference. It does not initiate a transfer. Settlement is full-amount only, after all returns, and duplicates/self-settlement are rejected. Partial payments, refunds and payment gateway integration are not included.
- Complete repair requires a maintenance asset and a repair note; the repair is recorded before availability changes. Direct maintenance-to-available metadata edits are rejected.

### Email and persistent delivery
Configure locally (never commit credentials): `SMTP_HOST`, `SMTP_PORT` (default 587), `SMTP_USERNAME`, `SMTP_PASSWORD`, `SMTP_FROM`. Optional `SMTP_AUTH` and `SMTP_STARTTLS` default true. Sending uses Spring Mail with bounded connection/read/write timeouts.
- Email verification sends a random code valid for 30 minutes. It can be consumed once by the signed-in account and is tied to its current email. Requests are limited to one per minute. The button stays disabled if mail is unconfigured.
- Notifications and equipment cleanup failures use `delivery_jobs`. A scheduled worker retries once a minute, up to 10 attempts. Disable with `APP_JOBS_ENABLED=false` during local diagnostic work. SMTP has at-least-once delivery semantics; a crash after send but before commit can duplicate an email.
- Completed jobs clear their payload. For exhausted jobs inspect `id`, `kind`, `attempts`, `last_error` (not payload) and fix the provider issue before deliberately resetting attempts/next_attempt. Jobs whose mail configuration is missing must not block storage cleanup jobs.
- The current local environment did not have SMTP configured when inspected. All tests mock email and storage; no real email was sent.

### Validation and limits
Server-side catalog/history/inventory pagination is in place; management loads 12 assets per page with database filtering and separate global counts. The legacy borrow selector is capped at 12 assets with a link to the complete catalog. A generated H2 inventory sample measured 3 SQL statements; timings are written to `target/performance-sample.txt` and are not browser/production benchmarks.
The browser integration could not start because its Windows sandbox failed. Browser visual checks, slow-network interactions and live SMTP delivery still require verification in a working environment.

References: [Spring Mail setup](https://docs.spring.io/spring-boot/reference/io/email.html), [Spring Boot 4 migration modules](https://github.com/spring-projects/spring-boot/wiki/Spring-Boot-4.0-Migration-Guide), [Flyway PostgreSQL module](https://github.com/flyway/flyway/blob/main/documentation/Reference/Database%20Driver%20Reference/PostgreSQL%20Database.md).


### Windows startup troubleshooting
Use the repository Maven wrapper from the project directory:

```powershell
.\mvnw.cmd spring-boot:run
```

After adding dependencies to `pom.xml`, stop the previous application process completely before starting this command. A DevTools restart alone can retain the old dependency classpath and report `ClassNotFoundException: org.springframework.mail.javamail.JavaMailSender`; the mail starter is already declared in this project. This error does not mean SMTP credentials are missing.

If plain `mvn` fails with `RepositorySystem.createSessionBuilder()` and its library listing contains multiple Maven/Resolver versions, that Maven installation is inconsistent. The wrapper uses a separate pinned distribution and bypasses the extension-managed Maven installation. Do not delete the project's dependencies or change application code to work around that launcher failure. For IDE Run/Debug, reload the Maven project before relaunching.


### User navigation performance (2026-10-04)
Home/catalog no longer fetch the unused full profile. My Requests summarizes status counts in one query, fetches the borrower and optional return record with the page, and batch-loads request items. Database pagination remains in place; collections are not fetch-joined across pagination. The profile page continues to receive its editable profile fields.

The authenticated H2 fixture (12 requests, first page of 10) reduced Hibernate statements from 24 to 6 for My Requests and 5 to 4 for Home/catalog. These counts exclude JdbcTemplate avatar queries and do not measure remote database latency, image downloads or browser rendering. `userNavigationQueryBudget` checks query ceilings; its sample is written to `target/user-navigation-performance.txt`.

Home, Equipment and My Requests opt into native cross-document view transitions where supported, with a stable header and reduced-motion opt-out. This is still server-rendered navigation, not an SPA or cached history: authentication, form scripts, direct URLs and browser back/forward keep their existing lifecycle. Unsupported browsers navigate normally. Visual browser verification remains outstanding.

# Project review ? 2026-09-28

Scope: static review of application workflows, authorization, persistence, image storage, shared UI, page loading and project structure; complete existing Maven suite; read-only anonymous HTTP probes of localhost:8080. No production records or application code changed during this review. Browser rendering and authenticated performance were not measured. Findings describe current workspace; the running app may not include every latest edit.

## High priority

1. Self-service approval separation is incomplete. BorrowRequestServiceImpl.approveBorrowRequest prevents self-approval, but pickUpEquipment and ReturnRecordServiceImpl.returnEquipment only require operator status. An operator borrower can confirm their own handover and classify their own damage after another operator approves. Apply a consistent policy for independent handover/inspection and store actor IDs and timestamps. The approval-only restriction is implemented correctly; this is the remaining workflow gap.
2. Approval does not reserve equipment or check date overlap. Multiple requests for the same asset and period can be approved; pickup only permits the first available-to-in-use transition. The existing parallel pickup test proves double pickup is prevented, not that both approvals can be fulfilled. Decide explicitly whether approval guarantees a reservation; if so, validate overlapping intervals transactionally and expire uncollected reservations.
3. Image replacement can revert to a deleted image. Equipment update accepts a client imageUrl and overwrites the stored value. If two editors opened the old image, one uploads a replacement (deleting the old object), then the other saves ordinary metadata with the stale URL, the equipment points to a deleted object. Keep storage-managed paths out of ordinary metadata updates; add optimistic version checks. A separate upload endpoint is already present and should own this field.
4. The full test suite is red. `mvnw.cmd test -q`: 52 tests, 0 assertion failures, 1 error. BorrowRequestServiceTest.approveBorrowRequest_success_delegatesToStatePattern does not configure current.require() after the new owner check. Fix the test fixture with a distinct operator; do not remove the production authorization check.

## Confirmed functional gaps / bugs

5. Slot does not reach borrower history: BorrowItemResponseDto and BorrowRequestMapper do not include storageSlot; my-history.html reads equipmentLockerNames, which DashboardController.history does not populate. Assigning a slot in management still leaves this view at Slot pending.
6. Date-picker lifecycle leaks: form-pickers.js registers document/window listeners per control. borrow-modal.js removes dialogs on close, but listeners retain detached controls; the WeakSet does not release references held by listener closures. Use AbortController/dispose on dialog close, or delegated shared listeners. Calendar month navigation also replaces the focused button without restoring focus.
7. Escape in a picker also reaches admin.js's document Escape handler, closing the entire equipment form. Consume picker Escape before it reaches the modal handler; add focus containment and focus restoration to custom admin modals.
8. Partial image-save retry state is ambiguous after Cancel. admin.js stores savedEquipmentId after creating equipment, retaining it until success. Closing and reopening Add can later PUT the previous equipment rather than create a new one. Make the partial-save state explicit, or switch to Edit after creation and reset Add state on a deliberate new creation. Also report metadata success vs image failure separately.
9. Deleting an equipment record does not delete its Storage object. EquipmentServiceImpl.deleteEquipment deletes the row only; replacement cleanup exists in EquipmentImageService. Arrange after-commit cleanup with retry/reconciliation. Never delete the object before a potentially failing database delete.

## Workflow decisions needed

- Fees use current purchase price and current borrower role at return time. Editing price or changing VIP status mid-loan changes charges. Define whether to snapshot policy and valuation at approval/pickup or deliberately use current values, then persist that policy for audit.
- There is no complete payment/settlement workflow: recording charges should not imply they were paid. Also decide partial returns, request rejection reason, no-show expiry and damaged-item repair flow before real operational use.
- NotificationServiceimpl logs notifications rather than delivering them. profile.js navigates to /profile/verify-email but no matching controller endpoint was found. Do not present these as complete production features.
- Storage slot is a free-text label, not a validated locker allocation. If physical capacity/exclusivity matters, introduce actual slot records and allocation rules.

## Performance observations

Anonymous localhost probes (three sequential requests each, TTFB, not full browser load):

| Route | TTFB ms | Response bytes |
| --- | --- | --- |
| / | 692, 689, 697 | 40249 |
| /equipment | 555, 426, 516 | 34184 |
| /api/v1/equipment?size=20 | 309, 218, 220 | 1070 |
| /css/style.css | 29, 12, 23 | 192488 |

/profile requires authentication (401 in anonymous probe); no performance claim for admin/profile. Concurrent test activity and the current dev environment affect these figures. They are a small sample, not a production benchmark.

- admin.js allPages fetches every equipment/request page before render; every update runs loadData again. Fetch only the active view with server pagination/filtering; obtain summary counts from a small aggregate endpoint.
- EquipmentPageController, DashboardController.borrow/history/profile use unpaged results. Query only needed records, status and fields; paginate history and catalog.
- BorrowRequestMapper traverses lazy items and equipment per request without a fetch plan. Likely N+1 behavior; measure SQL counts before choosing a two-stage ID-page + fetch query/projection. Do not paginate a collection fetch join blindly.
- EquipmentImageService.read signs through Supabase on each uncached image request. Cache signed URLs by immutable object path with a TTL shorter than expiry. Browser redirect caching helps repeat requests but not a cold device/session.
- CurrentRoleFilter queries the user for authenticated static resource requests too. AccountModelAdvice also performs profile/avatar reads for image controller requests in its web package. Narrow advice and static-resource filter scope while preserving immediate role revocation for protected requests.

## Structure and design principles

Existing strengths: controller/service/repository separation; borrowing State pattern; fine Strategy; image storage port; transactional pickup/return and role-change locking; CSRF; server-side image validation and aspect-preserving resizing.

Incremental cleanup recommended:
- Split accumulated style.css overrides into shared tokens/components plus scoped page styles. Resolve duplicate selectors before moving rules. CSS size is secondary to data loading, but repeated overrides are a major visual regression source.
- Split admin.js into inventory, requests, user management and shared modal/data utilities; retain behavior tests around selectors/events.
- Inject a qualified ImageStorage for equipment rather than constructing SupabaseImageStorage inside EquipmentImageService. Move provider classes out of service/avatar into a shared storage package; share image decoding validation while keeping avatar crop and equipment fit policies separate.
- Use request/response DTOs consistently: EquipmentController currently binds and returns entities despite separate equipment DTO/mapper classes.
- Adopt versioned database migrations instead of manually running SQL alongside schema.sql; current production initialization is disabled. Keep deployment migration verification separate from H2 schema tests.
- Remove unused static-preview branches/templates deliberately, standardize naming (layouut, NotificationServiceimpl, FinestrategyResolver), and format compact Java code for reviewability.

## Suggested order

1. Restore green tests; close authorization gaps; prevent stale image overwrite; fix Slot propagation and picker lifecycle/Escape.
2. Agree reservation, cancellation/expiry, damage valuation and settlement policies; add audit fields and workflow tests.
3. Paginate and lazy-load data; measure query counts and authenticated TTFB; add signed-URL caching.
4. Consolidate CSS/JS, storage abstractions, DTOs and migration tooling with focused regression checks.


## Implementation follow-up

Completed in the first remediation pass:
- Independent operator policy for approval, pickup and return, enforced in the service layer and reflected in UI actions.
- Transactional asset locking and overlapping approved/active reservation checks during approval; expired requests cannot be approved.
- Metadata edits cannot overwrite Storage-managed images. Storage-managed URLs cannot be supplied on creation.
- Storage slot is included in borrowing DTO/history.
- Picker global listeners and observers are disposed on borrow dialog close; Escape no longer closes its containing equipment form; month navigation restores focus.
- Opening Add starts a new form; retry ID remains only while continuing that form.
- Equipment delete triggers object cleanup after commit. Provider failure is logged; a durable cleanup retry queue remains future work.
- Equipment Storage is injected; signed URLs have bounded 50-minute caching.
- Static public resources bypass database role-refresh lookup; account model advice no longer runs for image endpoints.
- Hibernate secondary queries are batched (50).
- Management requests use 20-item server pages and status filtering, with independent summary counts and a bounded attention queue.
- Full existing tests pass; integration coverage added for self-pickup/return, overlapping approval, slot propagation, stale image paths and paginated request access.

Not completed by this pass: catalog/profile/history server pagination, whole-project CSS/JS consolidation, versioned migration runner, actor audit records and loan-time fee snapshots, real email verification/notification delivery, partial returns/payment settlement, durable cleanup retries, and controlled browser performance comparison. These remain explicit follow-up work; this pass is not a claim that every review item is closed.

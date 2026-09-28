# Software design principles in this project

The application uses Spring MVC layers: controllers translate HTTP requests, services enforce use cases, repositories persist state, and domain types represent business rules. Patterns support concrete responsibilities rather than adding indirection everywhere.

| Principle | Concrete implementation | Reason |
|---|---|---|
| Single Responsibility | `AvatarController` handles HTTP/flash messages; `AvatarImageProcessor` validates/crops/encodes; `AvatarService` coordinates replacement; `AvatarRepository` handles metadata; `SupabaseImageStorage` implements provider HTTP calls | Storage credentials and image processing do not belong in controllers or entities |
| Dependency Inversion | `AvatarService` depends on `ImageStorage`, supplied by constructor injection | Provider changes and tests do not change the use case |
| Open/Closed | Another `ImageStorage` implementation can replace Supabase; existing fine strategies implement `FineStrategyService` | Add another provider or fine policy without branching throughout controllers |
| Interface Segregation | `ImageStorage` exposes only upload, read URL and delete | Application services do not depend on bucket administration, authentication SDKs or unrelated cloud operations |
| Liskov Substitution | Storage implementations must accept normalized PNG bytes, return a usable read URL, and report failures as `StorageException` | Test doubles and production adapters honor the same contract; not every provider has identical caching guarantees |
| Encapsulation | The server derives the signed-in account, validates bytes, and computes fees using database prices | Clients cannot choose another upload owner or supply authoritative monetary totals |
| Separation of concerns | Profile tables hold object paths; Storage holds binary content | Ordinary profile reads do not load image bytes |
| KISS / YAGNI | One small storage port and one adapter; no new cloud SDK, generic media framework or distributed transaction system | Keeps the student project reviewable while supporting its actual requirements |

Existing patterns: borrow lifecycle uses State (`BorrowStateResolver` and state classes); late fees use Strategy (`FineStrategyService`, standard/VIP implementations); repositories abstract persistence. Avatar storage uses an Adapter (`SupabaseImageStorage`) behind a port (`ImageStorage`). These are patterns; SRP/DIP/OCP are the principles they help satisfy.

## Replacement workflow and consistency

Validate/re-encode -> upload to a unique object path -> lock the user row in a short database transaction -> save the new path -> commit -> delete the previous object. Concurrent updates are serialized only for metadata. If metadata fails, attempt deletion of the newly uploaded object. Provider and database transactions cannot be made atomic; orphan cleanup after crashes is explicitly documented rather than claiming guaranteed exactly-once writes.

Legacy migration keeps the original bytes and skips users already holding a Storage path. Its transaction rechecks the path to avoid replacing a newly uploaded image with an old one.

## Verification

Integration tests exercise authenticated uploads, CSRF, isolation between accounts, normalization, error preservation and legacy migration using a mocked storage port. Adapter contract tests run against a local HTTP server (no real credentials/network dependency). A service test verifies rollback cleanup. Existing borrowing, role, monetary and rendering tests remain in place.

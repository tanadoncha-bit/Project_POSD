# LeadIT - IT Equipment Borrow and Return

LeadIT manages equipment inventory, borrowing requests, pickup and return inspections.
Users can track requests and edit their profiles. Staff approve requests and record returns.
Administrators manage accounts, roles and equipment categories.
Frozen fee policies and inspection records preserve the history of each loan.

## Team members

| No. | Name | Student ID | Section | Existing branch | Responsibility |
|---|---|---|---|---|---|
| 1 | นางสาวกัญญาภัค ทองวิเศษ | 673380391-3  | 3 | `kanyaphak_673380391_3_Sec3` | Frontend, Integration, DevOps & Documentation |
| 2 | นางสาวอลิชา ชนะบุญ | 673380431-7 | 3 | `alicha_673380431_7_Sec3` | Master Data & User Module (User, UserProfile, Equipment, EquipmentCategory) + Builder/Factory Pattern |
| 3 | นายธนดล ไชยศิลา | 673380585-0 | 3 | `tanadon_673380585_0_Sec3` | Borrow/Return Business Logic (BorrowRequest, BorrowItem, ReturnRecord) + State/Strategy/Observer Pattern |

Existing branch names above do not match the assignment's exact three-part naming rule.
Confirm the required interpretation with the instructor before renaming published branches.
Do not manufacture commits or change authorship to meet contribution requirements.

## Tech Stack

Java 17, Spring Boot 4.1.1, Maven Wrapper, PostgreSQL, Spring Data JPA,
Thymeleaf, Spring Security, Flyway, springdoc OpenAPI 3.1.1, JUnit 5 and Mockito.
H2 is used only for isolated tests, not as the deployed database.

## System Architecture

Controller -> Service / Query interface -> Repository -> Entity / PostgreSQL.
REST responses use DTOs and mappers. See [architecture](doc/diagrams/component.md),
[SOLID analysis](doc/solid-analysis.md) and [design patterns](doc/design-patterns.md).
Some legacy auxiliary services still use JdbcTemplate directly; see the audit for remaining work.

## Database Design (ER Diagram)

See [ER diagram](doc/diagrams/er.md) and [data dictionary](doc/data-dictionary.md).
User/Profile is one-to-one; User/Request and Request/Item are one-to-many.
Request/ReturnRecord is one-to-zero-or-one, not one-to-many.
Versioned schema migrations are in code/src/main/resources/db/migration/.

## Installation & Setup

1. Install JDK 17 and PostgreSQL (or Docker Compose).
2. Copy .env.example to .env and fill database credentials locally.
3. For a fresh database use APP_PROFILE=migrations. For an existing database read
   [migration guidance](doc/WORKFLOW_SETUP.md) and back up before applying migrations.
4. Images require configured Supabase storage. Google login and email verification are optional; see [provider setup](doc/GOOGLE_AND_EMAIL_SETUP.md).
5. Fee policies and page sizes can be configured in .env; see [configuration](doc/CONFIGURATION.md).

## How to Run

Windows: `./mvnw.cmd spring-boot:run`
Linux/macOS: `./mvnw spring-boot:run`
Docker: set LOCAL_DB_PASSWORD in .env, then `docker compose up --build`.
Open http://localhost:8080. A fresh database has no predefined administrator;
follow the bootstrap instructions indoc/WORKFLOW_SETUP.md.
Docker configuration is prepared but has not yet been run in this review.

## API Documentation

Swagger UI: http://localhost:8080/swagger-ui.html
OpenAPI: http://localhost:8080/v3/api-docs
Equipment and Categories provide GET/POST/PUT/DELETE CRUD.
Category changes are ADMIN-only. Equipment changes require STAFF/ADMIN;
equipment deletion is ADMIN-only. Session-authenticated mutations require CSRF.
Borrowing actions use PATCH approve/pickup/cancel and POST return/settlement.

## How to Run Tests

Windows: `./mvnw.cmd test` or `./mvnw.cmd verify`
Linux/macOS: `./mvnw test` or `./mvnw verify`
Reports: target/surefire-reports and [review test summary](doc/test-report.md).
The real PostgreSQL migration test requires a disposable local database and
TEST_POSTGRES_URL; it is skipped without that environment variable.
CI builds and tests PRs; it does not deploy automatically yet.

## Deployment URL

Not supplied / not verified. A reachable public deployment and deployed Swagger
must be supplied before submission. Preparing Docker files is not deployment.

## Project Structure

```text
code/src/main/       Application Java, templates, CSS, JS and migrations
test/src/test/      Unit and integration tests
img/                 Image assets copied to static/images by Maven
doc/                Audit, design documentation and diagrams
.github/workflows/   Build and test automation
pom.xml              Root build; maps code/, test/ and img/ into Maven
Dockerfile           Container build
docker-compose.yml  Application and local PostgreSQL
```

See [assignment audit](doc/ASSIGNMENT_AUDIT.md) for verified requirements,
remaining gaps, optional features and submission exclusions.

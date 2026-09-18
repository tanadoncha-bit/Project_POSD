# IT Equipment Borrow-Return Tracking System

ระบบจัดการและติดตามสถานะการยืม-คืนอุปกรณ์ IT พัฒนาด้วย Spring Boot รองรับการลงทะเบียนอุปกรณ์ การขอยืม การอนุมัติ การติดตามสถานะแบบเรียลไทม์ (State Pattern) และการแจ้งเตือนอัตโนมัติเมื่อเกินกำหนดคืน (Observer Pattern) ระบบออกแบบตามหลัก SOLID และใช้ Design Pattern หลายรูปแบบเพื่อให้โค้ดยืดหยุ่นและดูแลรักษาง่าย พร้อม REST API และหน้าเว็บสำหรับผู้ใช้งานจริง

## สมาชิกกลุ่ม

| ลำดับ | ชื่อ-นามสกุล | รหัสนักศึกษา | Section | Branch | หน้าที่รับผิดชอบ |
|:---:|---|:---:|:---:|---|---|
| 1 | นางสาวกัญญาภัค ทองวิเศษ | 673380391-3  | 3 | `kanyaphak_673380391_3_Sec3` | Frontend, Integration, DevOps & Documentation |
| 2 | นางสาวอลิชา ชนะบุญ | 673380431-7 | 3 | `alicha_673380431_7_Sec3` | Master Data & User Module (User, UserProfile, Equipment, EquipmentCategory) + Builder/Factory Pattern |
| 3 | นายธนดล ไชยศิลา | 673380585-0 | 3 | `tanadon_673380585_0_Sec3` | Borrow/Return Business Logic (BorrowRequest, BorrowItem, ReturnRecord) + State/Strategy/Observer Pattern |

## Tech Stack

**Backend**
- Java 17+
- Spring Boot 4.1.1
- Hibernate / JPA
- MySQL / PostgreSQL

**Frontend**
- Thymeleaf

**Testing**
- JUnit 5
- Mockito
- Spring Boot Test (Integration Test)

**API Documentation**
- Swagger / OpenAPI (springdoc-openapi)

**DevOps**
- Docker, Docker Compose
- Git / GitHub (Branch + Pull Request Workflow)
- CI/CD (ถ้ามี — ระบุ GitHub Actions หรือเครื่องมือที่ใช้)

**Design Patterns ที่ใช้**
- Builder Pattern (สร้าง Response DTO)
- Factory Method Pattern (สร้างอุปกรณ์ตามประเภท)
- State Pattern (สถานะการยืม: PENDING / APPROVED / BORROWED / RETURNED / OVERDUE)
- Strategy Pattern (คำนวณค่าปรับ/ค่าเสียหาย)
- Observer Pattern (แจ้งเตือนเมื่อเกินกำหนดคืน ผ่าน Spring `ApplicationEvent`)

## System Architecture

ระบบออกแบบเป็น Layered Architecture ประกอบด้วย:

```
Client (Browser)
      │
      ▼
Controller Layer (REST API: /api/v1/equipment, /api/v1/borrow-requests)
      │
      ▼
Service Layer (Business Logic + Design Patterns)
      │
      ▼
Repository Layer (Spring Data JPA)
      │
      ▼
Database (MySQL/PostgreSQL)
```

## Database Design (ER Diagram)

ระบบประกอบด้วยตารางหลัก 7 ตาราง:

| Entity | ความสัมพันธ์ |
|---|---|
| `User` — `UserProfile` | 1:1 |
| `User` — `BorrowRequest` | 1:N |
| `EquipmentCategory` — `Equipment` | 1:N |
| `BorrowRequest` — `BorrowItem` | 1:N |
| `Equipment` — `BorrowItem` | 1:N |
| `BorrowRequest` — `ReturnRecord` | 1:N |

## Installation & Setup

### ข้อกำหนดเบื้องต้น (Prerequisites)
- JDK 17 ขึ้นไป
- Maven 3.8+
- MySQL/PostgreSQL (หรือใช้ Docker Compose)
- Docker & Docker Compose (ถ้าต้องการรันผ่าน container)

### ขั้นตอนติดตั้ง

```bash
# 1. Clone repository
git clone <repository-url>
cd it-equipment-borrow-system

# 2. ตั้งค่าฐานข้อมูลใน src/main/resources/application.properties (หรือ application.yml)
spring.datasource.url=jdbc:mysql://localhost:3306/it_borrow_db
spring.datasource.username=your_username
spring.datasource.password=your_password

# 3. ติดตั้ง dependencies
mvn clean install
```

## How to Run

**รันแบบ local (Maven):**
```bash
mvn spring-boot:run
```

**รันแบบ Docker Compose:**
```bash
docker-compose up --build
```

แอปพลิเคชันจะรันที่: `http://localhost:8080`

## API Documentation

หลังจากรันแอปพลิเคชันแล้ว สามารถเข้าดู API Documentation (Swagger UI) ได้ที่:

```
http://localhost:8080/swagger-ui/index.html
```

**Resource หลัก:**
| Method | Endpoint | คำอธิบาย |
|---|---|---|
| GET | `/api/v1/equipment` | ดึงรายการอุปกรณ์ทั้งหมด (พร้อม Pagination/Sorting) |
| GET | `/api/v1/equipment/{id}` | ดึงข้อมูลอุปกรณ์ตาม ID |
| POST | `/api/v1/equipment` | เพิ่มอุปกรณ์ใหม่ |
| PUT | `/api/v1/equipment/{id}` | แก้ไขข้อมูลอุปกรณ์ |
| DELETE | `/api/v1/equipment/{id}` | ลบอุปกรณ์ |
| GET | `/api/v1/borrow-requests` | ดึงรายการคำขอยืมทั้งหมด |
| POST | `/api/v1/borrow-requests` | สร้างคำขอยืมใหม่ |
| PUT | `/api/v1/borrow-requests/{id}/approve` | อนุมัติคำขอยืม |
| PUT | `/api/v1/borrow-requests/{id}/return` | บันทึกการคืนอุปกรณ์ |

## How to Run Tests

**รัน Unit Test ทั้งหมด:**
```bash
mvn test
```

**รัน Integration Test:**
```bash
mvn verify
```

**ดู Test Coverage Report** (ถ้าตั้งค่า JaCoCo ไว้):
```bash
mvn jacoco:report
# เปิดดูที่ target/site/jacoco/index.html
```

## Deployment URL

- **Production/Demo URL:** _(ใส่ลิงก์ที่ deploy จริง เช่น Railway, Render, AWS)_
- **Swagger UI (Production):** _(ใส่ลิงก์ Swagger บน production)_

## Project Structure

```
it-equipment-borrow-system/
├── src/
│   ├── main/
│   │   ├── java/com/example/itborrowsystem/
│   │   │   ├── controller/          # REST Controllers
│   │   │   ├── service/             # Business Logic (Interfaces)
│   │   │   │   └── impl/            # Service Implementations
│   │   │   ├── repository/          # Spring Data JPA Repositories
│   │   │   ├── entity/              # JPA Entities
│   │   │   ├── dto/                 # Request/Response DTOs
│   │   │   ├── common/
│   │   │   │   ├── event/           # Observer Pattern (OverdueEvent, Listener)
│   │   │   │   ├── exception/       # Global Exception Handler
│   │   │   │   └── pattern/         # State/Strategy/Factory/Builder implementations
│   │   │   ├── config/              # Security, Swagger, Async config
│   │   │   └── ItBorrowSystemApplication.java
│   │   └── resources/
│   │       ├── application.properties
│   │       └── db/migration/        # Migration scripts
│   └── test/
│       └── java/com/example/itborrowsystem/
│           ├── service/             # Unit Tests (JUnit + Mockito)
│           └── integration/         # Integration Tests
├── docs/
│   ├── er-diagram.png
│   ├── class-diagram.png
│   ├── sequence-diagram.png
│   ├── solid-analysis.md
│   └── design-patterns.md
├── Dockerfile
├── docker-compose.yml
├── pom.xml
└── README.md
```
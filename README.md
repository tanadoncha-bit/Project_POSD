# LeadIT - ระบบจัดการการยืมและคืนอุปกรณ์ไอที

LeadIT เป็นระบบสำหรับจัดการอุปกรณ์ไอทีและกระบวนการยืม-คืนอุปกรณ์ภายในองค์กร  
ผู้ใช้สามารถดูอุปกรณ์ ส่งคำขอยืม ติดตามสถานะ และตรวจสอบประวัติการยืมของตนเองได้  
เจ้าหน้าที่สามารถอนุมัติคำขอยืม จัดการการรับ-คืนอุปกรณ์ และตรวจสอบสภาพอุปกรณ์ได้  
ผู้ดูแลระบบสามารถจัดการบัญชีผู้ใช้ สิทธิ์การใช้งาน อุปกรณ์ และหมวดหมู่อุปกรณ์ได้  

---

## สมาชิกกลุ่ม

| ลำดับ | ชื่อ-นามสกุล | รหัสนักศึกษา | Section | Branch | หน้าที่รับผิดชอบ |
|---|---|---|---|---|---|
| 1 | นางสาวกัญญาภัค ทองวิเศษ | 673380391-3 | 3 | `kanyaphak_673380391_3_Sec3` | Frontend, Integration, DevOps และ Documentation |
| 2 | นางสาวอลิชา ชนะบุญ | 673380431-7 | 3 | `alicha_673380431_7_Sec3` | Master Data และ User Module ได้แก่ User, UserProfile, Equipment, EquipmentCategory และ Builder/Factory Pattern |
| 3 | นายธนดล ไชยศิลา | 673380585-0 | 3 | `tanadon_673380585_0_Sec3` | Borrow/Return Business Logic ได้แก่ BorrowRequest, BorrowItem, ReturnRecord และ State/Strategy/Observer Pattern |

---

## Tech Stack

### Programming Language
- Java 17

### Backend
- Spring Boot 4.1.1
- Spring MVC
- Spring Data JPA
- Spring Security
- Spring Validation
- Thymeleaf

### Database
- PostgreSQL
- Flyway สำหรับจัดการ Database Migration
- H2 สำหรับการทดสอบ

### API
- REST API
- OpenAPI 3
- Swagger UI

### Testing
- JUnit 5
- Mockito
- Spring Boot Test
- Spring Security Test

### Build & Deployment
- Maven
- Docker
- Docker Compose
- Render

---

## System Architecture

ระบบใช้ Layered Architecture โดยแบ่งระบบออกเป็นหลาย Layer
เพื่อแยกหน้าที่ของแต่ละส่วนและทำให้ระบบสามารถดูแลและพัฒนาต่อได้ง่าย
```text
Client
  |
  v
Controller
  |
  v
Service
  |
  v
Repository
  |
  v
PostgreSQL Database 
```

### รายละเอียดแต่ละ Layer
- Controller
รับ HTTP Request จากผู้ใช้งาน และส่ง Response กลับไปยัง Client

- Service
จัดการ Business Logic และกระบวนการทำงานหลักของระบบ

- Repository
ทำหน้าที่ติดต่อและจัดการข้อมูลใน Database

- Domain / Entity
แทนข้อมูลหลักของระบบและความสัมพันธ์ระหว่างข้อมูล

- DTO / Mapper
ใช้สำหรับรับส่งและแปลงข้อมูลระหว่าง API กับ Entity

- Security
จัดการ Authentication และ Authorization รวมถึงสิทธิ์ของผู้ใช้งาน

- Thymeleaf
ใช้สำหรับสร้างหน้า Web Application
---
## Database Design (ER Diagram)


---
## Installation & Setup

### Requirements

ก่อนเริ่มใช้งานระบบต้องติดตั้ง

- JDK 17
- PostgreSQL
- Git
- Docker และ Docker Compose (กรณีต้องการรันด้วย Docker)

### Clone Project

```bash
git clone https://github.com/tanadoncha-bit/Project_POSD.git
cd Project_POSD ```

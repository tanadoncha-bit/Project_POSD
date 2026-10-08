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

ระบบใช้สถาปัตยกรรมแบบแบ่งเป็น Layer เพื่อแยกหน้าที่ของแต่ละส่วน
และทำให้ระบบสามารถพัฒนา แก้ไข และดูแลรักษาได้ง่าย

```text
User / Client
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
PostgreSQL

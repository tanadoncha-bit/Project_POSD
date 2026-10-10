# พจนานุกรมข้อมูล

จัดทำจากไฟล์ SQL ของ Flyway ในโปรเจค ไม่ใช่การตรวจฐานข้อมูลที่รันอยู่ รายการนี้รวบรวม 18 ตารางจาก migration เดิม รวมตารางที่ถูกลบภายหลัง และระบุส่วนเพิ่มเติมของ V11 ไว้ท้ายเอกสาร

## users

| คอลัมน์ | ชนิดข้อมูลและข้อกำหนด SQL |
|---|---|
| id | BIGSERIAL PRIMARY KEY |
| username | VARCHAR(50) NOT NULL UNIQUE |
| email | VARCHAR(100) NOT NULL UNIQUE |
| password | VARCHAR(255) NOT NULL |
| role | VARCHAR(20) NOT NULL DEFAULT 'USER' |
| created_at | TIMESTAMP NOT NULL DEFAULT now() |

## user_profiles

| คอลัมน์ | ชนิดข้อมูลและข้อกำหนด SQL |
|---|---|
| id | BIGSERIAL PRIMARY KEY |
| user_id | BIGINT NOT NULL UNIQUE REFERENCES users(id) ON DELETE CASCADE |
| full_name | VARCHAR(150) NOT NULL |
| phone | VARCHAR(20) |
| avatar_path | VARCHAR(300) |
| department | VARCHAR(100) |

## equipment_categories

| คอลัมน์ | ชนิดข้อมูลและข้อกำหนด SQL |
|---|---|
| id | BIGSERIAL PRIMARY KEY |
| name | VARCHAR(100) NOT NULL UNIQUE |
| description | VARCHAR(300) |

## equipment

| คอลัมน์ | ชนิดข้อมูลและข้อกำหนด SQL |
|---|---|
| id | BIGSERIAL PRIMARY KEY |
| asset_code | VARCHAR(30) NOT NULL UNIQUE |
| name | VARCHAR(150) NOT NULL |
| category_id | BIGINT NOT NULL REFERENCES equipment_categories(id) |
| status | VARCHAR(20) NOT NULL DEFAULT 'AVAILABLE' |
| purchase_price | NUMERIC(12,2) CHECK (purchase_price >= 0) |
| purchase_date | DATE |
| image_url | VARCHAR(1000) |
| specifications | TEXT |
| storage_slot | VARCHAR(100) |

## borrow_requests

| คอลัมน์ | ชนิดข้อมูลและข้อกำหนด SQL |
|---|---|
| id | BIGSERIAL PRIMARY KEY |
| user_id | BIGINT NOT NULL REFERENCES users(id) |
| borrow_date | DATE NOT NULL |
| due_date | DATE NOT NULL |
| status | VARCHAR(20) NOT NULL DEFAULT 'PENDING' |
| note | VARCHAR(500) |
| created_at | TIMESTAMP NOT NULL DEFAULT now() |
| daily_fine | NUMERIC(12,2) |
| grace_days | INT |
| scratch_rate | NUMERIC(5,4) |
| damage_rate | NUMERIC(5,4) |
| loss_rate | NUMERIC(5,4) |
| rejection_reason | VARCHAR(500) |

## borrow_items

| คอลัมน์ | ชนิดข้อมูลและข้อกำหนด SQL |
|---|---|
| id | BIGSERIAL PRIMARY KEY |
| borrow_request_id | BIGINT NOT NULL REFERENCES borrow_requests(id) ON DELETE CASCADE |
| equipment_id | BIGINT NOT NULL REFERENCES equipment(id) |
| quantity | INT NOT NULL DEFAULT 1 |
| condition_on_borrow | VARCHAR(200) |
| snapshot_name | VARCHAR(150) |
| snapshot_asset_code | VARCHAR(30) |
| snapshot_storage_slot | VARCHAR(100) |
| snapshot_image_url | VARCHAR(1000) |
| snapshot_category_name | VARCHAR(150) |
| snapshot_purchase_price | NUMERIC(12,2) |
| returned_on | DATE |

## return_records

| คอลัมน์ | ชนิดข้อมูลและข้อกำหนด SQL |
|---|---|
| id | BIGSERIAL PRIMARY KEY |
| borrow_request_id | BIGINT NOT NULL UNIQUE REFERENCES borrow_requests(id) ON DELETE CASCADE |
| return_date | DATE NOT NULL |
| condition | VARCHAR(50) NOT NULL |
| fine_amount | NUMERIC(10,2) DEFAULT 0 |
| damage_amount | NUMERIC(14,2) NOT NULL DEFAULT 0 |
| remark | VARCHAR(500) |

## return_inspections

| คอลัมน์ | ชนิดข้อมูลและข้อกำหนด SQL |
|---|---|
| return_record_id | BIGINT NOT NULL REFERENCES return_records(id) ON DELETE CASCADE |
| item_order | INTEGER NOT NULL |
| equipment_id | BIGINT NOT NULL |
| equipment_name | VARCHAR(150) NOT NULL |
| condition | VARCHAR(50) NOT NULL |
| purchase_price | NUMERIC(12,2) |
| damage_rate | NUMERIC(3,2) NOT NULL |
| damage_amount | NUMERIC(14,2) NOT NULL |
| remark | VARCHAR(500) |

## app_migrations (ตารางเดิมที่ถูกลบใน V8)

| คอลัมน์ | ชนิดข้อมูลและข้อกำหนด SQL |
|---|---|
| version | VARCHAR(100) PRIMARY KEY |

## role_management_lock

| คอลัมน์ | ชนิดข้อมูลและข้อกำหนด SQL |
|---|---|
| id | BIGINT PRIMARY KEY |

## role_audit

| คอลัมน์ | ชนิดข้อมูลและข้อกำหนด SQL |
|---|---|
| id | BIGSERIAL PRIMARY KEY |
| actor_username | VARCHAR(50) NOT NULL |
| target_username | VARCHAR(50) NOT NULL |
| old_role | VARCHAR(20) |
| new_role | VARCHAR(20) NOT NULL |
| changed_at | TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP |

## user_avatars (ตารางเดิมที่ถูกลบใน V8)

| คอลัมน์ | ชนิดข้อมูลและข้อกำหนด SQL |
|---|---|
| user_id | BIGINT PRIMARY KEY REFERENCES users(id) ON DELETE CASCADE |
| image_data | BYTEA NOT NULL |

## borrow_workflow_audit

| คอลัมน์ | ชนิดข้อมูลและข้อกำหนด SQL |
|---|---|
| id | BIGSERIAL PRIMARY KEY |
| request_id | BIGINT NOT NULL |
| actor_username | VARCHAR(100) NOT NULL |
| action | VARCHAR(30) NOT NULL |
| changed_at | TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP |

## settlements

| คอลัมน์ | ชนิดข้อมูลและข้อกำหนด SQL |
|---|---|
| request_id | BIGINT PRIMARY KEY REFERENCES borrow_requests(id) |
| amount | NUMERIC(16,2) NOT NULL |
| reference | VARCHAR(500) NOT NULL |
| actor_username | VARCHAR(50) NOT NULL |
| paid_at | TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP |

## equipment_repairs

| คอลัมน์ | ชนิดข้อมูลและข้อกำหนด SQL |
|---|---|
| id | BIGSERIAL PRIMARY KEY |
| equipment_id | BIGINT NOT NULL REFERENCES equipment(id) |
| actor_username | VARCHAR(50) NOT NULL |
| note | VARCHAR(500) NOT NULL |
| repaired_at | TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP |

## delivery_jobs

| คอลัมน์ | ชนิดข้อมูลและข้อกำหนด SQL |
|---|---|
| id | BIGSERIAL PRIMARY KEY |
| kind | VARCHAR(30) NOT NULL |
| recipient | VARCHAR(320) NOT NULL |
| subject | VARCHAR(200) NOT NULL |
| payload | TEXT NOT NULL |
| completed | BOOLEAN NOT NULL DEFAULT false |
| attempts | INT NOT NULL DEFAULT 0 |
| next_attempt | TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP |
| last_error | VARCHAR(150) |

## email_verifications

| คอลัมน์ | ชนิดข้อมูลและข้อกำหนด SQL |
|---|---|
| user_id | BIGINT PRIMARY KEY REFERENCES users(id) ON DELETE CASCADE |
| email | VARCHAR(320) NOT NULL |
| token_hash | VARCHAR(64) |
| expires_at | TIMESTAMP |
| requested_at | TIMESTAMP NOT NULL |
| verified_at | TIMESTAMP |

## locker_access

| คอลัมน์ | ชนิดข้อมูลและข้อกำหนด SQL |
|---|---|
| request_id | BIGINT PRIMARY KEY REFERENCES borrow_requests(id) ON DELETE CASCADE |
| pin | VARCHAR(6) |
| slots | TEXT (ปรับชนิดข้อมูลใน V7__locker_slots_text.sql) |
| opened | BOOLEAN NOT NULL DEFAULT false |

## เหตุผลในการเลือก Fetch และ Cascade

ความสัมพันธ์ UserProfile/User, Request/User, Item/Equipment และ ReturnRecord/Request ใช้ LAZY เพื่อโหลดข้อมูลเมื่อจำเป็น คำขอยืมเป็นเจ้าของรายการอุปกรณ์และบันทึกคืน จึงใช้ cascade ALL และ orphan removal ไม่ลบอุปกรณ์ย้อนหลังตามคำขอ และไม่อนุญาตให้ลบหมวดที่ยังมีอุปกรณ์อ้างถึง ใช้ foreign key ป้องกันข้อมูลอ้างอิงหลัก ส่วนรหัสใน snapshot และประวัติบางรายการเก็บไว้โดยไม่ผูกเป็นความสัมพันธ์ Entity รายละเอียดข้อกำหนดดูได้จาก SQL migration


## ข้อมูลด้านความปลอดภัยของบัญชีและงานส่งข้อมูลที่เพิ่มใน V11

| ตาราง | ฟิลด์ | หน้าที่ |
| --- | --- | --- |
| users | security_version BIGINT NOT NULL DEFAULT 0 | ยกเลิก session ที่สร้างก่อนเปลี่ยนข้อมูลสำคัญของบัญชี |
| email_changes | user_id PK/FK, old_email, new_email, token_hash UNIQUE, security_version, expires_at, requested_at | เก็บคำขอเปลี่ยนอีเมลแบบใช้ครั้งเดียว อีเมลหลักยังไม่เปลี่ยนจนกว่าจะยืนยัน |
| delivery_jobs | delivery_key VARCHAR(36) NOT NULL | ใช้ UUID เดิมของงานส่งข้อมูลในการลองส่งแต่ละครั้ง |
| delivery_jobs | lease_token, lease_until | ระบุผู้ประมวลผลงานและเวลาที่มีสิทธิ์ครอบครองงาน |
| delivery_jobs | next_attempt TIMESTAMP WITH TIME ZONE | เวลาที่สามารถลองส่งครั้งถัดไป |
| email_verifications | expires_at/requested_at/verified_at TIMESTAMP WITH TIME ZONE | เวลาของวงจร token ที่ไม่ขึ้นกับเขตเวลาของเครื่อง |

V11 แปลงเวลางานส่งข้อมูลและยืนยันอีเมลเดิมโดยใช้เขตเวลาของ session ฐานข้อมูล ส่วนวันยืมและวันครบกำหนดยังใช้ DATE ตามปฏิทินที่ระบบกำหนด

# SOLID Analysis — ส่วนของคนที่ 2 (Borrow/Return Business Logic)

เอกสารนี้ระบุว่าหลักการ SOLID แต่ละข้อปรากฏอยู่ที่ไฟล์ใด บรรทัดใด ในส่วนงานที่รับผิดชอบ
(BorrowRequest, BorrowItem, ReturnRecord, State/Strategy/Observer Pattern, Service Layer)

---

## S — Single Responsibility Principle

| ไฟล์ | บรรทัด | เหตุผล |
|---|---|---|
| `service/impl/BorrowRequestServiceImpl.java` | 30–149 | ทำหน้าที่ **orchestrate workflow** การยืมเท่านั้น (รับ request, ประสาน Repository/State/Event) ไม่มีตรรกะการเปลี่ยนสถานะหรือคำนวณค่าปรับปนอยู่ในคลาสนี้เลย — งานเหล่านั้นถูกส่งต่อให้ `BorrowState` และ `FineStrategyService` รับผิดชอบแทน |
| `domain/state/PendingState.java` | 13–43 | รับผิดชอบพฤติกรรมของสถานะ `PENDING` เพียงสถานะเดียว ไม่รู้จักหรือจัดการ logic ของสถานะอื่น |
| `common/event/OverdueEventListener.java` | 16–30 | ทำหน้าที่เดียวคือ "ฟัง event แล้วสั่งแจ้งเตือน" ไม่ยุ่งกับการเปลี่ยนสถานะหรือบันทึกข้อมูลใดๆ ในฐานข้อมูล |
| `service/impl/ReturnRecordServiceImpl.java` | 44–77 | รับผิดชอบเฉพาะ flow การคืนอุปกรณ์ (เปลี่ยนสถานะ → คำนวณค่าปรับ → คืนอุปกรณ์ → บันทึก) แยกขาดจาก flow การยืมใน `BorrowRequestServiceImpl` โดยสิ้นเชิง |

---

## O — Open/Closed Principle

| ไฟล์ | บรรทัด | เหตุผล |
|---|---|---|
| `domain/state/BorrowState.java` | 11–27 | Interface กลางที่กำหนดพฤติกรรมของทุกสถานะ — เพิ่มสถานะใหม่ทำได้โดยสร้างคลาส implement ตัวนี้เพิ่ม ไม่ต้องแก้โค้ดของสถานะเดิมแม้แต่บรรทัดเดียว |
| `domain/state/BorrowStateResolver.java` | 30–38 | จุดเดียวที่ต้องแก้เมื่อเพิ่มสถานะใหม่ (เพิ่ม `case` ใน switch) ส่วน `BorrowRequestServiceImpl` และ state class เดิมทั้งหมดไม่กระทบ |
| `service/FineStrategyService.java` | 12–24 | Interface ของ Strategy Pattern — เพิ่มกฎการคิดค่าปรับแบบใหม่ (เช่น role เพิ่มเติมในอนาคต) ทำได้โดยสร้างคลาสใหม่ implement interface นี้ |
| `service/impl/strategy/FineStrategyResolver.java` | 23–32 | ใช้ `strategies.stream().filter(s -> s.supports(role))` ไล่หาใน `List<FineStrategyService>` ที่ Spring inject ให้อัตโนมัติ — เพิ่ม strategy ใหม่แล้ว resolver เห็นเองทันที **ไม่ต้องแก้โค้ดในไฟล์นี้เลย** |

---

## L — Liskov Substitution Principle

| ไฟล์ | บรรทัด | เหตุผล |
|---|---|---|
| `domain/state/PendingState.java` | 16–42 | implement ครบทุก method ของ `BorrowState` แม้บาง method ทำไม่ได้จริงในสถานะนี้ (เช่น `pickUp()`) ก็ **throw `InvalidBorrowStateException` ที่มีความหมายทางธุรกิจ** แทนที่จะ throw `UnsupportedOperationException` — ทำให้ใช้แทน `BorrowState` ที่ใดก็ได้โดยพฤติกรรม (throw exception ที่คาดเดาได้) ยังคงสอดคล้องกับสัญญาของ interface |
| `domain/state/ApprovedState.java`, `BorrowedState.java`, `OverdueState.java`, `ReturnedState.java` | ทั้งไฟล์ | ยึดหลักเดียวกับ `PendingState` ทุกคลาส — ทุก method คืนค่า/throw exception ที่มีความหมาย ไม่มีคลาสไหน throw `UnsupportedOperationException` |

---

## I — Interface Segregation Principle

| ไฟล์ | บรรทัด | เหตุผล |
|---|---|---|
| `domain/state/BorrowState.java` | 11–27 | มีแค่ 5 method ที่เกี่ยวกับการเปลี่ยนสถานะการยืมโดยตรง (`approve`, `pickUp`, `returnEquipment`, `cancel`, `markOverdue`) ไม่ยัดรวม method ที่ไม่เกี่ยวข้อง เช่น การคำนวณค่าปรับหรือการแจ้งเตือน |
| `service/FineStrategyService.java` | 12–24 | มีแค่ 2 method (`calculate`, `supports`) เฉพาะเรื่องคำนวณค่าปรับเท่านั้น ไม่รวม method อื่นที่ไม่เกี่ยวกับ Strategy Pattern นี้ |
| `service/NotificationService.java` | ทั้งไฟล์ | แยก interface การแจ้งเตือนออกจาก `BorrowRequestService` โดยสิ้นเชิง — คลาสที่ต้องการแค่ส่งแจ้งเตือน (เช่น `OverdueEventListener`) ไม่ต้องรู้จัก method อื่นของระบบยืม-คืนเลย |

---

## D — Dependency Inversion Principle

| ไฟล์ | บรรทัด | เหตุผล |
|---|---|---|
| `service/impl/BorrowRequestServiceImpl.java` | 33–53 | Field ทุกตัวเป็น **interface หรือ resolver** (`BorrowRequestRepository`, `UserRepository`, `EquipmentRepository`, `BorrowStateResolver`, `BorrowRequestMapper`, `ApplicationEventPublisher`) ไม่มี field ไหนเป็น concrete class โดยตรง และรับเข้ามาผ่าน **constructor เท่านั้น** ไม่มี field injection (`@Autowired` บน field) |
| `service/impl/ReturnRecordServiceImpl.java` | 26–42 | เช่นเดียวกัน — พึ่งพา `FineStrategyResolver` และ `BorrowStateResolver` ผ่าน constructor ไม่รู้จัก `StandardFineStrategy`/`VipFineStrategy`/`PendingState` ฯลฯ โดยตรงแม้แต่คลาสเดียว |
| `common/event/OverdueEventListener.java` | 18 | พึ่งพา `NotificationService` (interface) ไม่ใช่ `NotificationServiceImpl` (concrete class) — ถ้าเปลี่ยนวิธีส่งแจ้งเตือนจาก log เป็น email จริง ไฟล์นี้ไม่ต้องแก้เลย |
| `service/impl/strategy/FineStrategyResolver.java` | 17–21 | รับ `List<FineStrategyService>` (interface) ผ่าน constructor แทนที่จะ `new StandardFineStrategy()` ตรงๆ — Spring เป็นคนประกอบ (compose) ให้ทั้งหมดตอน runtime |

---

## สรุป

ทุกหลักการ SOLID ถูกนำไปใช้จริงในโค้ด ไม่ใช่แค่ตั้งชื่อคลาสให้ดูดี:
- **S** ทำให้แต่ละคลาสแก้ไขได้อย่างอิสระโดยไม่กระทบกัน
- **O** ทำให้เพิ่มสถานะ/กลยุทธ์ใหม่โดยไม่ต้องแก้โค้ดเดิม (พิสูจน์ได้จาก `BorrowStateResolver` และ `FineStrategyResolver`)
- **L** ทำให้ state class ทุกตัวใช้แทนกันได้ผ่าน interface เดียวกันโดยไม่มี exception ที่ผิดสัญญา
- **I** ทำให้ interface แต่ละตัวเล็กและตรงจุด ไม่มี "fat interface"
- **D** ทำให้ Service ทุกตัวขึ้นกับ abstraction ไม่ใช่ implementation ตรวจสอบได้จาก constructor ของทุกคลาส

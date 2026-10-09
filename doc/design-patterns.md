# ตาราง GoF Patterns

| Pattern | ปัญหาที่แก้ |ไฟล์/คลาสที่ใช้|
|---|---|---|
|State|การยืมมีหลายสถานะ แต่ละสถานะอนุญาตการทำงานไม่เหมือนกัน เช่น คำขอที่ยังไม่อนุมัติไม่ควรรับหรือคืนอุปกรณ์ จึงแยกกฎของแต่ละสถานะออกจากกัน|`domain/state/BorrowState.java, PendingState.java, ApprovedState.java, BorrowedState.java, OverdueState.java, ReturnedState.java, BorrowStateResolver.java`|
|Strategy|ผู้ยืมแต่ละประเภทอาจมีหลักเกณฑ์คำนวณค่าปรับต่างกัน จึงแยกวิธีคำนวณค่าปรับตามบทบาทออกเป็นกลยุทธ์ และเลือกใช้ให้เหมาะกับผู้ยืม|`service/FineStrategyService.java, service/strategy/StandardFineStrategy.java, VipFineStrategy.java, FineStrategyResolver.java`|
|Observer|แยกการเผยแพร่ Event ออกจากการจัดการ Event เมื่อเกิดเหตุการณ์อุปกรณ์เกินกำหนดคืน ระบบสามารถแจ้งเตือนผู้ใช้ผ่าน Listener โดยไม่ต้องรวมขั้นตอนแจ้งเตือนไว้ใน Business Logic ทั้งหมด|`BorrowRequestServiceImpl.java, OverdueEvent.java, OverdueEventListener.java` และ `NotificationService` โดยใช้ `ApplicationEventPublisher` จาก Spring Framework เพื่อเผยแพร่ Event|

## Class Diagram
### State Pattern 
![State Pattern](img/Pattern/State.png)
### State Pattern 
![Strategy Pattern](img/Pattern/Strategy.png)
### Observer Pattern 
![Observer Pattern](img/Pattern/Observer.png)

# ส่วน Enterprise / Architectural Patterns ต้องครบ 6 แบบ

| Pattern | ปัญหาที่แก้ |ไฟล์/คลาสที่ใช้|
|---|---|---|
|Layered Architecture|ป้องกันการรวมโค้ดทุกหน้าที่ไว้ในชั้นเดียว|โฟลเดอร์ `controller/, service/, repository/, domain/, mapper/`|
|MVC|แยกการรับ Request/ควบคุมการทำงานออกจากข้อมูลและหน้าจอ|`controller/web/, controller/api/, domain/entity/`, ไฟล์ View ใน `src/main/resources/templates/`|
|Repository Pattern|แยกการเข้าถึงฐานข้อมูลออกจาก Business Logic|`repository/BorrowRequestRepository.java, EquipmentRepository.java, UserRepository.java`|
|Service Layer Pattern|รวมกฎและขั้นตอนการทำงานของระบบไว้ใน Service|`service/impl/BorrowRequestServiceImpl.java, EquipmentServiceImpl.java, ReturnRecordServiceImpl.java`|
|DTO + Mapper|ไม่เปิดเผย Entity โดยตรงเป็น API Contract และควบคุมข้อมูลที่ส่งกลับ|`dto/request/, dto/response/, mapper/BorrowRequestMapper.java, UserMapper.java, CategoryMapper.java`|
|Dependency Injection|ลดการสร้าง Dependency ด้วย new เอง ทำให้แยกส่วนและทดสอบง่ายขึ้น|Constructor ของ `BorrowRequestServiceImpl, BorrowRequestMapper` และคลาสที่รับ Dependency ผ่าน Constructor|

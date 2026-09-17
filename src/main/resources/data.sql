INSERT INTO users (username, email, password, role) VALUES
('admin', 'admin@itborrow.com', 'admin123', 'ADMIN'),
('somchai', 'somchai@itborrow.com', 'password123', 'STAFF'),
('malee', 'malee@itborrow.com', 'password123', 'VIP');

INSERT INTO user_profiles (user_id, full_name, phone, department) VALUES
(1, 'Admin System', '0800000000', 'IT Department'),
(2, 'Somchai Jaidee', '0812345678', 'Sales'),
(3, 'Malee Suksan', '0898765432', 'Marketing');

INSERT INTO equipment_categories (name, description) VALUES
('Laptop', 'โน้ตบุ๊กสำหรับใช้งานทั่วไปและงานเฉพาะทาง'),
('Monitor', 'จอแสดงผลภายนอก'),
('Accessory', 'อุปกรณ์เสริม เช่น เมาส์ คีย์บอร์ด สาย HDMI');

INSERT INTO equipment (asset_code, name, category_id, status) VALUES
('LAP-001', 'Dell Latitude 5420', 1, 'AVAILABLE'),
('LAP-002', 'MacBook Pro 14"', 1, 'AVAILABLE'),
('MON-001', 'LG 24" FHD Monitor', 2, 'AVAILABLE'),
('ACC-001', 'Logitech Wireless Mouse', 3, 'AVAILABLE');

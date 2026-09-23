-- Flyway Migration V2__seed_demo_data.sql
-- Seeding dữ liệu thực tế chất lượng cao cho hệ thống Quản lý Vé Sự kiện

-- ============================================================
-- 1. SEEDING USERS & ROLES
-- Password mã hóa BCrypt cho '123456': $2a$10$R9h/cIPz0gi.URNNX3kh2OPST9/PgBkqquzi.Ss7KIUgO2t0jWMUW
-- Password mã hóa BCrypt cho 'admin': $2a$10$e8wFAfN204GfE1v57/0d/OQpEaM8FzZkU0Q.0G6h2F.v0Zz5sX0sW
-- ============================================================

-- Đảm bảo có các Role cơ bản
INSERT INTO roles (name, description) VALUES
('ADMIN', 'Quản trị viên hệ thống'),
('ORGANIZER', 'Ban tổ chức sự kiện'),
('STAFF', 'Nhân viên soát vé / quản lý của BTC'),
('CUSTOMER', 'Khách hàng mua vé')
ON CONFLICT (name) DO NOTHING;

-- Tài khoản Admin
INSERT INTO users (id, username, password, full_name, email, phone, address, enabled, created_at, updated_at) VALUES
(1, 'admin', '$2a$10$Yh/F0RRuLqSg6HnsmSAPB.OI/Y5fHzpiQt1nCEpjQqCkBQr3TuhIu', 'Quản Trị Viên Hệ Thống', 'admin@eventmng.com', '0901234567', 'Tòa nhà Bitexco, Q.1, TP. Hồ Chí Minh', true, NOW(), NOW())
ON CONFLICT (username) DO UPDATE SET password = EXCLUDED.password, enabled = true;

-- Ban Tổ Chức (Organizers)
INSERT INTO users (id, username, password, full_name, email, phone, address, enabled, created_at, updated_at) VALUES
(2, 'saigon_concerts', '$2a$10$Yh/F0RRuLqSg6HnsmSAPB.OI/Y5fHzpiQt1nCEpjQqCkBQr3TuhIu', 'Công ty Giải trí Saigon Concerts', 'contact@saigonconcerts.vn', '0988776655', '123 Nguyễn Huệ, Phường Bến Nghé, Quận 1, TP. Hồ Chí Minh', true, NOW(), NOW()),
(3, 'techfest_vn', '$2a$10$Yh/F0RRuLqSg6HnsmSAPB.OI/Y5fHzpiQt1nCEpjQqCkBQr3TuhIu', 'Hiệp hội Công nghệ Vietnam Tech Community', 'events@techfest.vn', '0912345678', 'Tầng 5, Keangnam Landmark 72, Nam Từ Liêm, Hà Nội', true, NOW(), NOW()),
(4, 'vn_marathon', '$2a$10$Yh/F0RRuLqSg6HnsmSAPB.OI/Y5fHzpiQt1nCEpjQqCkBQr3TuhIu', 'Ban Tổ Chức Giải Chạy Việt Nam', 'info@vnmarathon.org', '0933445566', '45 Lê Duẩn, Quận 1, TP. Hồ Chí Minh', true, NOW(), NOW())
ON CONFLICT (username) DO UPDATE SET password = EXCLUDED.password, enabled = true;

-- Khách hàng (Customers)
INSERT INTO users (id, username, password, full_name, email, phone, address, enabled, created_at, updated_at) VALUES
(5, 'hoanganh', '$2a$10$Yh/F0RRuLqSg6HnsmSAPB.OI/Y5fHzpiQt1nCEpjQqCkBQr3TuhIu', 'Nguyễn Hoàng Anh', 'hoanganh@gmail.com', '0971112233', '15 Lê Văn Sỹ, Quận 3, TP. Hồ Chí Minh', true, NOW(), NOW()),
(6, 'thuylinh', '$2a$10$Yh/F0RRuLqSg6HnsmSAPB.OI/Y5fHzpiQt1nCEpjQqCkBQr3TuhIu', 'Phạm Thùy Linh', 'thuylinh.pham@gmail.com', '0962223344', '88 Cầu Giấy, Hà Nội', true, NOW(), NOW()),
(7, 'quangminh', '$2a$10$Yh/F0RRuLqSg6HnsmSAPB.OI/Y5fHzpiQt1nCEpjQqCkBQr3TuhIu', 'Đặng Quang Minh', 'quangminh.dang@gmail.com', '0953334455', '102 Hải Phòng, Q. Thanh Khê, Đà Nẵng', true, NOW(), NOW())
ON CONFLICT (username) DO UPDATE SET password = EXCLUDED.password, enabled = true;

-- Gán Role cho từng User
INSERT INTO user_roles (user_id, role_id) VALUES
(1, 'ADMIN'),
(2, 'ORGANIZER'),
(3, 'ORGANIZER'),
(4, 'ORGANIZER'),
(5, 'CUSTOMER'),
(6, 'CUSTOMER'),
(7, 'CUSTOMER')
ON CONFLICT DO NOTHING;


-- ============================================================
-- 2. SEEDING CATEGORIES (DANH MỤC SỰ KIỆN)
-- ============================================================

INSERT INTO categories (id, name, description, created_at, updated_at) VALUES
(1, 'Âm Nhạc & Concert', 'Các đêm nhạc live concert, festival âm nhạc hoành tráng và đại nhạc hội ngoài trời', NOW(), NOW()),
(2, 'Công Nghệ & Startup', 'Hội thảo công nghệ, diễn đàn trí tuệ nhân tạo AI, điện toán đám mây và triển lãm khởi nghiệp', NOW(), NOW()),
(3, 'Thể Thao & Giải Chạy', 'Các giải chạy Marathon, giải đấu bóng đá, pickleball và sự kiện thể thao cộng đồng', NOW(), NOW()),
(4, 'Văn Hóa & Nghệ Thuật', 'Triển lãm mỹ thuật, kịch nói, hòa nhạc thính phòng và biểu diễn nghệ thuật truyền thống', NOW(), NOW()),
(5, 'Ẩm Thực & Lễ Hội', 'Lễ hội ẩm thực đường phố, festival bia thủ công và các hội chợ văn hóa vùng miền', NOW(), NOW())
ON CONFLICT (id) DO UPDATE SET name = EXCLUDED.name, description = EXCLUDED.description;


-- ============================================================
-- 3. SEEDING EVENTS (SỰ KIỆN THỰC TẾ)
-- ============================================================

INSERT INTO events (id, name, category_id, organizer_id, location, province, start_time, end_time, sale_start_date, sale_end_date, description, status, created_at, updated_at) VALUES
(1, 'Đại Nhạc Hội Mùa Hè - Live Concert 2026', 1, 2, 'Sân vận động Quân khu 7, 202 Hoàng Văn Thụ, Phường 9, Phú Nhuận', 'TP. Hồ Chí Minh', '2026-07-15 18:00:00', '2026-07-15 23:00:00', '2026-05-01 00:00:00', '2026-07-14 23:59:59', 'Đêm đại nhạc hội mùa hè hoành tráng quy tụ hơn 20 ca sĩ hàng đầu Việt Nam. Hệ thống âm thanh ánh sáng chuẩn quốc tế mang lại trải nghiệm đỉnh cao.', 'OPENING', NOW(), NOW()),
(2, 'Vietnam AI & Cloud Tech Summit 2026', 2, 3, 'Trung tâm Hội nghị Quốc gia, Đại lộ Thăng Long, Nam Từ Liêm', 'Hà Nội', '2026-08-20 08:00:00', '2026-08-20 17:30:00', '2026-06-01 00:00:00', '2026-08-19 23:59:59', 'Diễn đàn công nghệ quy mô lớn nhất năm 2026 về AI Generative, Cloud Infrastructure và Data Engineering với hơn 50 diễn giả từ Google, AWS, Microsoft.', 'UPCOMING', NOW(), NOW()),
(3, 'Giải Chạy Marathon Quốc Tế TP.HCM 2026', 3, 4, 'Công viên Tao Đàn & Các cung đường trung tâm Quận 1', 'TP. Hồ Chí Minh', '2026-09-10 04:30:00', '2026-09-10 11:00:00', '2026-05-15 00:00:00', '2026-09-01 23:59:59', 'Giải chạy marathon quốc tế quy mô 10.000 vận động viên với 4 cự ly chính: 5km, 10km, 21km (Half Marathon) và 42km (Full Marathon).', 'UPCOMING', NOW(), NOW()),
(4, 'Triển Lãm Nghệ Thuật Đương Đại "Sắc Màu Sài Gòn"', 4, 2, 'Bảo tàng Mỹ thuật TP.HCM, 97A Phó Đức Chính, Quận 1', 'TP. Hồ Chí Minh', '2026-06-01 09:00:00', '2026-06-30 18:00:00', '2026-05-01 00:00:00', '2026-06-30 17:00:00', 'Không gian nghệ thuật trưng bày hơn 100 tác phẩm hội họa, điêu khắc và nghệ thuật sắp đặt tương tác của các nghệ sĩ trẻ hàng đầu Việt Nam.', 'OPENING', NOW(), NOW())
ON CONFLICT (id) DO UPDATE SET name = EXCLUDED.name, location = EXCLUDED.location, description = EXCLUDED.description;


-- ============================================================
-- 4. SEEDING EVENT IMAGES (URL ẢNH CHẤT LƯỢNG CAO)
-- ============================================================

INSERT INTO event_images (id, event_id, image_url, created_at, updated_at) VALUES
(1, 1, 'https://images.unsplash.com/photo-1470225620780-dba8ba36b745?auto=format&fit=crop&w=1200&q=80', NOW(), NOW()),
(2, 1, 'https://images.unsplash.com/photo-1514525253161-7a46d19cd819?auto=format&fit=crop&w=1200&q=80', NOW(), NOW()),
(3, 2, 'https://images.unsplash.com/photo-1540575467063-178a50c2df87?auto=format&fit=crop&w=1200&q=80', NOW(), NOW()),
(4, 2, 'https://images.unsplash.com/photo-1505373877841-8d25f7d46678?auto=format&fit=crop&w=1200&q=80', NOW(), NOW()),
(5, 3, 'https://images.unsplash.com/photo-1452626038306-9aae5e071dd3?auto=format&fit=crop&w=1200&q=80', NOW(), NOW()),
(6, 4, 'https://images.unsplash.com/photo-1561214115-f2f134cc4912?auto=format&fit=crop&w=1200&q=80', NOW(), NOW())
ON CONFLICT (id) DO UPDATE SET image_url = EXCLUDED.image_url;


-- ============================================================
-- 5. SEEDING TICKET TYPES (LOẠI VÉ SỰ KIỆN)
-- ============================================================

INSERT INTO ticket_types (id, event_id, name, price, total_quantity, remaining_quantity, description, created_at, updated_at) VALUES
-- Event 1: Concert Mùa Hè
(1, 1, 'Vé SVIP (Sát Sân Khấu + Gift Set)', 2500000.00, 100, 85, 'Hàng ghế VIP sát sân khấu, bao gồm áo thun lưu niệm, vòng tay phát sáng và lối đi riêng qua cửa ưu tiên.', NOW(), NOW()),
(2, 1, 'Vé VIP (Khu Vực A)', 1500000.00, 300, 240, 'Khu vực chính giữa đối diện sân khấu, có chỗ ngồi cố định và nước uống miễn phí.', NOW(), NOW()),
(3, 1, 'Vé Phổ Thông (GA)', 500000.00, 1000, 750, 'Vé đứng khu vực sôi động phía sau khu vực VIP.', NOW(), NOW()),

-- Event 2: Tech Summit
(4, 2, 'Vé Chuyên Gia (Full Pass + Lunch)', 1200000.00, 200, 180, 'Quyền tham dự tất cả các phiên thảo luận, buffet trưa cao cấp và tài liệu độc quyền từ diễn giả.', NOW(), NOW()),
(5, 2, 'Vé Sinh Viên / Khách Tham Dự', 300000.00, 500, 420, 'Quyền tham dự các phiên hội thảo chung và gian hàng triển lãm công nghệ.', NOW(), NOW()),

-- Event 3: Marathon
(6, 3, 'Vé 42KM Full Marathon', 9500000.00, 2000, 1500, 'Race kit đầy đủ: Áo Finisher, Huy chương hoàn thành, Balo, Gel năng lượng và bảo hiểm chạy bộ.', NOW(), NOW()),
(7, 3, 'Vé 21KM Half Marathon', 750000.00, 3000, 2100, 'Race kit bao gồm Áo chạy, Huy chương hoàn thành và nước điện giải hỗ trợ dọc đường đua.', NOW(), NOW()),

-- Event 4: Triển Lãm Nghệ Thuật
(8, 4, 'Vé Tham Quan Thường', 150000.00, 500, 380, 'Vé vào cổng tự do tham quan toàn bộ các phòng triển lãm trong ngày.', NOW(), NOW())
ON CONFLICT (id) DO UPDATE SET name = EXCLUDED.name, price = EXCLUDED.price;


-- ============================================================
-- 6. SEEDING VOUCHERS (MÃ GIẢM GIÁ)
-- ============================================================

INSERT INTO vouchers (id, code, discount_type, amount, max_discount, min_order_amount, quantity, start_date, end_date, event_id, created_by, created_at, updated_at) VALUES
(1, 'HE2026', 'PERCENTAGE', 20.00, 100000.00, 300000.00, 500, '2026-05-01 00:00:00', '2026-08-31 23:59:59', NULL, 1, NOW(), NOW()),
(2, 'SUMMERVIP', 'AMOUNT', 150000.00, 150000.00, 1000000.00, 100, '2026-05-01 00:00:00', '2026-07-31 23:59:59', 1, 2, NOW(), NOW()),
(3, 'TECH50K', 'AMOUNT', 50000.00, 50000.00, 200000.00, 200, '2026-06-01 00:00:00', '2026-08-20 23:59:59', 2, 3, NOW(), NOW())
ON CONFLICT (code) DO NOTHING;


-- ============================================================
-- 7. SEEDING BLOG TAGS & BLOG POSTS
-- ============================================================

INSERT INTO blog_tags (id, name, slug, created_at, updated_at) VALUES
(1, 'Tin Tức Sự Kiện', 'tin-tuc-su-kien', NOW(), NOW()),
(2, 'Bí Kíp Mua Vé', 'bi-kip-mua-ve', NOW(), NOW()),
(3, 'Công Nghệ & AI', 'cong-nghe-ai', NOW(), NOW()),
(4, 'Giải Trí & Âm Nhạc', 'giai-tri-am-nhac', NOW(), NOW())
ON CONFLICT (slug) DO NOTHING;

INSERT INTO blog_posts (id, title, slug, summary, content, thumbnail, meta_title, meta_description, author_id, status, published_at, created_at, updated_at) VALUES
(1, 'Bí kíp săn vé Đại Nhạc Hội Mùa Hè 2026 thành công 100%', 'bi-kip-san-ve-dai-nhac-hoi-mua-he-2026', 'Hướng dẫn chi tiết các bước chuẩn bị tài khoản, chọn vị trí ngồi và thanh toán nhanh chóng để sở hữu tấm vé mơ ước.', 'Để săn được tấm vé SVIP của Đại Nhạc Hội Mùa Hè 2026, bạn cần chuẩn bị kết nối mạng ổn định, đăng nhập sẵn tài khoản và liên kết cổng thanh toán PayOS trước giờ mở bán.', 'https://images.unsplash.com/photo-1470225620780-dba8ba36b745?auto=format&fit=crop&w=800&q=80', 'Bí kíp săn vé Concert Mùa Hè 2026', 'Hướng dẫn săn vé concert mùa hè nhanh chóng không nghẽn mạng', 1, 'PUBLISHED', NOW(), NOW(), NOW()),
(2, 'Top 5 điểm nổi bật tại Vietnam AI & Cloud Tech Summit 2026', 'top-5-diem-noi-bat-vietnam-ai-cloud-tech-summit-2026', 'Khám phá những xu hướng công nghệ hàng đầu và cơ hội kết nối với các diễn giả quốc tế tại sự kiện.', 'Vietnam AI & Cloud Tech Summit 2026 năm nay quy tụ các chuyên gia hàng đầu từ Google Cloud, AWS và Microsoft. Người tham dự sẽ được trải nghiệm các mô hình AI trực tiếp.', 'https://images.unsplash.com/photo-1540575467063-178a50c2df87?auto=format&fit=crop&w=800&q=80', 'Điểm nổi bật Vietnam AI Tech Summit 2026', 'Tổng quan sự kiện công nghệ AI lớn nhất năm 2026', 1, 'PUBLISHED', NOW(), NOW(), NOW())
ON CONFLICT (slug) DO NOTHING;

INSERT INTO blog_post_tags (post_id, tag_id) VALUES
(1, 2),
(1, 4),
(2, 1),
(2, 3)
ON CONFLICT DO NOTHING;


-- ============================================================
-- 8. SEEDING ORDERS & TICKETS (ĐƠN HÀNG MẪU ĐỂ THỐNG KÊ RÕ RÀNG)
-- ============================================================

INSERT INTO orders (id, order_code, customer_id, organizer_amount, platform_fee_rate, service_fee, total_amount, discount_amount, voucher_code, payment_method, payment_status, order_status, order_date, paid_at, created_at, updated_at) VALUES
('ORD-2026-0001', 100001, 5, 2375000.00, 0.05, 125000.00, 2500000.00, 0.00, NULL, 'PAYOS', 'PAID', 'COMPLETED', NOW() - INTERVAL '2 days', NOW() - INTERVAL '2 days', NOW(), NOW()),
('ORD-2026-0002', 100002, 6, 1425000.00, 0.05, 75000.00, 1500000.00, 0.00, NULL, 'PAYOS', 'PAID', 'COMPLETED', NOW() - INTERVAL '1 day', NOW() - INTERVAL '1 day', NOW(), NOW()),
('ORD-2026-0003', 100003, 7, 950000.00, 0.05, 50000.00, 1000000.00, 0.00, NULL, 'PAYOS', 'PAID', 'COMPLETED', NOW(), NOW(), NOW(), NOW())
ON CONFLICT (id) DO NOTHING;

INSERT INTO order_items (id, order_id, ticket_type_id, quantity, unit_price, subtotal, created_at, updated_at) VALUES
(1, 'ORD-2026-0001', 1, 1, 2500000.00, 2500000.00, NOW(), NOW()),
(2, 'ORD-2026-0002', 2, 1, 1500000.00, 1500000.00, NOW(), NOW()),
(3, 'ORD-2026-0003', 3, 2, 500000.00, 1000000.00, NOW(), NOW())
ON CONFLICT (id) DO NOTHING;

INSERT INTO tickets (id, order_id, ticket_type_id, ticket_code, qr_code, status, used_at, created_at, updated_at) VALUES
(1, 'ORD-2026-0001', 1, 'TCK-SVIP-001', 'https://api.qrserver.com/v1/create-qr-code/?size=150x150&data=TCK-SVIP-001', 'VALID', NULL, NOW(), NOW()),
(2, 'ORD-2026-0002', 2, 'TCK-VIP-002', 'https://api.qrserver.com/v1/create-qr-code/?size=150x150&data=TCK-VIP-002', 'VALID', NULL, NOW(), NOW()),
(3, 'ORD-2026-0003', 3, 'TCK-GA-003', 'https://api.qrserver.com/v1/create-qr-code/?size=150x150&data=TCK-GA-003', 'VALID', NULL, NOW(), NOW()),
(4, 'ORD-2026-0003', 3, 'TCK-GA-004', 'https://api.qrserver.com/v1/create-qr-code/?size=150x150&data=TCK-GA-004', 'VALID', NULL, NOW(), NOW())
ON CONFLICT (id) DO NOTHING;

-- Đồng bộ sequence cho các bảng để tránh lỗi ID trùng khi chèn dữ liệu mới sau khi seed
SELECT setval(pg_get_serial_sequence('users', 'id'), (SELECT MAX(id) FROM users));
SELECT setval(pg_get_serial_sequence('categories', 'id'), (SELECT MAX(id) FROM categories));
SELECT setval(pg_get_serial_sequence('events', 'id'), (SELECT MAX(id) FROM events));
SELECT setval(pg_get_serial_sequence('event_images', 'id'), (SELECT MAX(id) FROM event_images));
SELECT setval(pg_get_serial_sequence('ticket_types', 'id'), (SELECT MAX(id) FROM ticket_types));
SELECT setval(pg_get_serial_sequence('vouchers', 'id'), (SELECT MAX(id) FROM vouchers));
SELECT setval(pg_get_serial_sequence('blog_tags', 'id'), (SELECT MAX(id) FROM blog_tags));
SELECT setval(pg_get_serial_sequence('blog_posts', 'id'), (SELECT MAX(id) FROM blog_posts));
SELECT setval(pg_get_serial_sequence('order_items', 'id'), (SELECT MAX(id) FROM order_items));
SELECT setval(pg_get_serial_sequence('tickets', 'id'), (SELECT MAX(id) FROM tickets));

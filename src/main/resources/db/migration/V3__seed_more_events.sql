-- Flyway Migration V3__seed_more_events.sql
-- Seeding 5 sự kiện đang mở bán với hạn ngừng bán là 22:00:00 ngày 26/09/2026

-- ============================================================
-- 1. SEEDING 5 SỰ KIỆN ĐANG MỞ BÁN (HẠN BÁN: 22h NGÀY 26/09/2026)
-- ============================================================

INSERT INTO events (id, name, category_id, organizer_id, location, province, start_time, end_time, sale_start_date, sale_end_date, description, status, created_at, updated_at) VALUES
-- Event 5: Âm Nhạc (Bán chạy gần hết vé #1)
(5, 'Đại Hội Âm Nhạc Electronic Future Sound 2026', 1, 2, 'Trung tâm Hội chợ & Triển lãm Sài Gòn (SECC), 799 Nguyễn Văn Linh, Q.7', 'TP. Hồ Chí Minh', '2026-09-27 19:00:00', '2026-09-27 23:30:00', '2026-09-01 09:00:00', '2026-09-26 22:00:00', 'Lễ hội âm nhạc điện tử EDM quy tụ dàn DJ quốc tế cực khủng. Số lượng vé cực kỳ giới hạn và đang bán chạy gần hết!', 'OPENING', NOW(), NOW()),

-- Event 6: Công Nghệ (Bán chạy gần hết vé #2)
(6, 'Giải Đấu Game Esports Vietnam Championship 2026', 2, 3, 'Nhà thi đấu Nguyễn Du, 116 Nguyễn Du, Phường Bến Thành, Quận 1', 'TP. Hồ Chí Minh', '2026-09-28 13:00:00', '2026-09-28 21:00:00', '2026-09-05 08:00:00', '2026-09-26 22:00:00', 'Chung kết giải đấu Esports quốc gia mùa thu. Các chỗ ngồi khán đài đã được đặt mua gần hết sạch!', 'OPENING', NOW(), NOW()),

-- Event 7: Ẩm Thực (Sự kiện có vé giá 5.000 VNĐ)
(7, 'Lễ Hội Ẩm Thực & Trải Nghiệm Văn Hóa 5K', 5, 2, 'Công viên Văn hóa Lê Thị Riêng, 875 Cách Mạng Tháng 8, Quận 10', 'TP. Hồ Chí Minh', '2026-09-27 08:00:00', '2026-09-27 21:00:00', '2026-09-10 10:00:00', '2026-09-26 22:00:00', 'Chương trình lễ hội ẩm thực đặc biệt với vé vào cổng siêu ưu đãi chỉ 5.000 đồng/vé cho tất cả khách tham quan.', 'OPENING', NOW(), NOW()),

-- Event 8: Workshop Công Nghệ
(8, 'Workshop Lập Trình AI & Cloud Native Systems', 2, 3, 'Tầng 3, Toà nhà Dreamplex, 195 Điện Biên Phủ, Bình Thạnh', 'TP. Hồ Chí Minh', '2026-09-29 09:00:00', '2026-09-29 17:00:00', '2026-09-15 00:00:00', '2026-09-26 22:00:00', 'Buổi trải nghiệm thực hành xây dựng ứng dụng AI tích hợp Cloud Native dành cho lập trình viên và sinh viên IT.', 'OPENING', NOW(), NOW()),

-- Event 9: Thể Thao
(9, 'Giải Chạy Đêm TP.HCM Night Run 2026', 3, 4, 'Tuyến đường Bến Vân Đồn - Cầu Ánh Sao - Phú Mỹ Hưng', 'TP. Hồ Chí Minh', '2026-10-02 20:00:00', '2026-10-03 02:00:00', '2026-09-20 00:00:00', '2026-09-26 22:00:00', 'Giải chạy đêm ngắm thành phố về đêm cực kỳ sôi động với không khí âm nhạc dọc các trạm tiếp nước.', 'OPENING', NOW(), NOW())
ON CONFLICT (id) DO UPDATE SET 
    name = EXCLUDED.name, 
    sale_end_date = EXCLUDED.sale_end_date, 
    status = EXCLUDED.status;


-- ============================================================
-- 2. SEEDING ẢNH CHO 5 SỰ KIỆN MỚI
-- ============================================================

INSERT INTO event_images (id, event_id, image_url, created_at, updated_at) VALUES
(7, 5, 'https://images.unsplash.com/photo-1516450360452-9312f5e86fc7?auto=format&fit=crop&w=1200&q=80', NOW(), NOW()),
(8, 6, 'https://images.unsplash.com/photo-1542751371-adc38448a05e?auto=format&fit=crop&w=1200&q=80', NOW(), NOW()),
(9, 7, 'https://images.unsplash.com/photo-1555396273-367ea4eb4db5?auto=format&fit=crop&w=1200&q=80', NOW(), NOW()),
(10, 8, 'https://images.unsplash.com/photo-1531482615713-2afd69097998?auto=format&fit=crop&w=1200&q=80', NOW(), NOW()),
(11, 9, 'https://images.unsplash.com/photo-1476480862126-209bfaa8edc8?auto=format&fit=crop&w=1200&q=80', NOW(), NOW())
ON CONFLICT (id) DO UPDATE SET image_url = EXCLUDED.image_url;


-- ============================================================
-- 3. SEEDING VÉ SỰ KIỆN (BAO GỒM VÉ GẦN HẾT & VÉ 5.000 ĐỒNG)
-- ============================================================

INSERT INTO ticket_types (id, event_id, name, price, total_quantity, remaining_quantity, description, created_at, updated_at) VALUES
-- Event 5: EDM Concert (Gần hết vé #1 - Còn đúng 3 vé VIP và 15 vé GA)
(9, 5, 'Vé VIP Future Sound (Sát Sân Khấu)', 800000.00, 500, 3, 'Khu vực VIP sát sân khấu EDM. Hiện chỉ còn lại đúng 3 vé cuối cùng!', NOW(), NOW()),
(10, 5, 'Vé Phổ Thông GA EDM', 300000.00, 2000, 15, 'Khu vực đứng sôi động. Đã bán 1985/2000 vé!', NOW(), NOW()),

-- Event 6: Esports Championship (Gần hết vé #2 - Còn đúng 8 vé)
(11, 6, 'Vé Khán Đài Độc Quyền Esports', 250000.00, 800, 8, 'Ghế ngồi trung tâm khán đài theo dõi trận chung kết. Chỉ còn lại 8 vé duy nhất!', NOW(), NOW()),

-- Event 7: Lễ Hội Ẩm Thực (Có vé siêu rẻ 5.000 VNĐ)
(12, 7, 'Vé Vào Cổng Ưu Đãi 5K', 5000.00, 5000, 3400, 'Vé vào cổng tự do trải nghiệm và thưởng thức các gian hàng ẩm thực.', NOW(), NOW()),

-- Event 8: Workshop AI
(13, 8, 'Vé Tham Dự Workshop AI', 150000.00, 200, 120, 'Vé bao gồm nước uống và tài liệu thực hành lab.', NOW(), NOW()),

-- Event 9: Night Run
(14, 9, 'Vé Cự Ly 10KM Night Run', 450000.00, 1500, 980, 'Race kit áo chạy đêm phát sáng và huy chương.', NOW(), NOW()),
(15, 9, 'Vé Cự Ly 5KM Fun Run', 250000.00, 1000, 650, 'Race kit cự ly 5km vui nhộn.', NOW(), NOW())
ON CONFLICT (id) DO UPDATE SET price = EXCLUDED.price, remaining_quantity = EXCLUDED.remaining_quantity;


-- Đồng bộ lại sequence tự tăng cho các bảng
SELECT setval(pg_get_serial_sequence('events', 'id'), (SELECT MAX(id) FROM events));
SELECT setval(pg_get_serial_sequence('event_images', 'id'), (SELECT MAX(id) FROM event_images));
SELECT setval(pg_get_serial_sequence('ticket_types', 'id'), (SELECT MAX(id) FROM ticket_types));

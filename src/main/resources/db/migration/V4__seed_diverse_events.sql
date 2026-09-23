-- Flyway Migration V4__seed_diverse_events.sql
-- Bổ sung 35 sự kiện đa dạng: 8 danh mục, 20 tỉnh/thành, giá vé từ 20.000đ đến 12.000.000đ.
-- Trạng thái: phần lớn OPENING (mở bán quanh 09-10/2026), kèm UPCOMING, CLOSED, COMPLETED,
-- một số hạng vé / sự kiện đã bán hết để test giao diện.
--
-- Không hardcode ID: các bảng dùng sequence tự tăng, liên kết qua username / tên danh mục / tên sự kiện,
-- để không ghi đè lên dữ liệu người dùng đã tạo trên môi trường đã deploy.
-- Ảnh được host trên Cloudinary (folder event-mng/seed-v4); nguồn và giấy phép ghi ở cuối file.


-- ============================================================
-- 1. BAN TỔ CHỨC MỚI (mật khẩu: 123456)
-- ============================================================

INSERT INTO users (username, password, full_name, email, phone, address, enabled, created_at, updated_at) VALUES
('hanoi_arts', '$2a$10$R9h/cIPz0gi.URNNX3kh2OPST9/PgBkqquzi.Ss7KIUgO2t0jWMUW', 'Trung Tâm Nghệ Thuật Biểu Diễn Hà Nội', 'booking@hanoiarts.vn', '0243826888', '22 Tràng Tiền, Phường Tràng Tiền, Quận Hoàn Kiếm, Hà Nội', true, NOW(), NOW()),
('mientrung_events', '$2a$10$R9h/cIPz0gi.URNNX3kh2OPST9/PgBkqquzi.Ss7KIUgO2t0jWMUW', 'Công ty Sự Kiện & Truyền Thông Miền Trung', 'hello@mientrungevents.vn', '0905123789', '98 Bạch Đằng, Phường Hải Châu 1, Quận Hải Châu, Đà Nẵng', true, NOW(), NOW()),
('vietxplore', '$2a$10$R9h/cIPz0gi.URNNX3kh2OPST9/PgBkqquzi.Ss7KIUgO2t0jWMUW', 'Công ty Du Lịch Trải Nghiệm VietXplore', 'tour@vietxplore.vn', '0939456123', '56 Hai Bà Trưng, Phường Tân An, Quận Ninh Kiều, Cần Thơ', true, NOW(), NOW())
ON CONFLICT (username) DO NOTHING;

INSERT INTO user_roles (user_id, role_id)
SELECT id, 'ORGANIZER' FROM users WHERE username IN ('hanoi_arts', 'mientrung_events', 'vietxplore')
ON CONFLICT DO NOTHING;


-- ============================================================
-- 2. DANH MỤC MỚI
-- ============================================================

INSERT INTO categories (name, description, created_at, updated_at)
SELECT v.name, v.description, NOW(), NOW()
FROM (VALUES
    ('Sân Khấu & Hài Kịch', 'Kịch nói, nhạc kịch, hài độc thoại stand-up comedy và các chương trình sân khấu giải trí'),
    ('Workshop & Kỹ Năng', 'Lớp học trải nghiệm, workshop kỹ năng, thủ công mỹ nghệ và thiết kế sáng tạo'),
    ('Du Lịch & Trải Nghiệm', 'Tour trải nghiệm, khám phá thiên nhiên, cắm trại và nghỉ dưỡng chăm sóc sức khỏe')
) AS v(name, description)
WHERE NOT EXISTS (SELECT 1 FROM categories c WHERE c.name = v.name);


-- ============================================================
-- 3. SỰ KIỆN
-- ============================================================

INSERT INTO events (name, category_id, organizer_id, location, province, start_time, end_time, sale_start_date, sale_end_date, description, status, created_at, updated_at)
SELECT v.name,
       (SELECT MIN(c.id) FROM categories c WHERE c.name = v.category),
       (SELECT u.id FROM users u WHERE u.username = v.organizer),
       v.location, v.province,
       v.start_time::timestamp, v.end_time::timestamp, v.sale_start::timestamp, v.sale_end::timestamp,
       v.description, v.status, NOW(), NOW()
FROM (VALUES
    -- Âm Nhạc & Concert
    ('Đêm Nhạc Acoustic Mùa Sương Đà Lạt 2026', 'Âm Nhạc & Concert', 'saigon_concerts',
     'Quảng trường Lâm Viên, Đường Trần Quốc Toản, Phường 10, TP. Đà Lạt', 'Lâm Đồng',
     '2026-10-10 19:30:00', '2026-10-10 22:30:00', '2026-09-01 09:00:00', '2026-10-09 23:59:59', 'OPENING',
     'Đêm nhạc acoustic ngoài trời giữa tiết trời se lạnh Đà Lạt với guitar mộc, violin và những bản tình ca quen thuộc. Khán giả được phục vụ trà nóng, khoai nướng và chăn len miễn phí.'),
    ('Hà Nội Rock Festival 2026', 'Âm Nhạc & Concert', 'hanoi_arts',
     'Công viên Thống Nhất, 354A Lê Duẩn, Quận Hai Bà Trưng', 'Hà Nội',
     '2026-10-17 16:00:00', '2026-10-17 23:00:00', '2026-08-25 10:00:00', '2026-10-16 23:59:59', 'OPENING',
     'Lễ hội rock ngoài trời lớn nhất miền Bắc với 12 ban nhạc rock, metal và indie. Hệ thống âm thanh công suất 100.000W cùng khu ẩm thực và merchandise ngay trong công viên.'),
    ('Hòa Nhạc Giao Hưởng Mùa Thu Vienna', 'Âm Nhạc & Concert', 'hanoi_arts',
     'Nhà hát Lớn Hà Nội, 1 Tràng Tiền, Quận Hoàn Kiếm', 'Hà Nội',
     '2026-10-24 20:00:00', '2026-10-24 22:00:00', '2026-09-05 09:00:00', '2026-10-24 12:00:00', 'OPENING',
     'Dàn nhạc giao hưởng 60 nghệ sĩ trình diễn các tác phẩm của Mozart, Strauss và Beethoven. Quy định trang phục lịch sự, không cho trẻ em dưới 8 tuổi vào khán phòng.'),
    ('Đêm Nhạc Jazz Trên Du Thuyền Sông Sài Gòn', 'Âm Nhạc & Concert', 'saigon_concerts',
     'Bến Bạch Đằng, Đường Tôn Đức Thắng, Phường Bến Nghé, Quận 1', 'TP. Hồ Chí Minh',
     '2026-10-03 19:00:00', '2026-10-03 22:00:00', '2026-09-10 09:00:00', '2026-10-02 18:00:00', 'OPENING',
     'Ba giờ du ngoạn sông Sài Gòn về đêm cùng ban nhạc jazz sống, ngắm toàn cảnh Thủ Thiêm và Landmark 81 lên đèn. Tàu khởi hành đúng 19h00, vui lòng có mặt trước 30 phút.'),
    ('K-Pop Dance Festival Đà Nẵng 2026', 'Âm Nhạc & Concert', 'mientrung_events',
     'Cung Thể thao Tiên Sơn, Đường Phan Đăng Lưu, Quận Hải Châu', 'Đà Nẵng',
     '2026-10-11 17:00:00', '2026-10-11 22:00:00', '2026-09-01 10:00:00', '2026-10-10 23:59:59', 'OPENING',
     'Đại hội nhảy cover K-Pop với 30 nhóm nhảy toàn miền Trung, xen kẽ phần Random Play Dance cho khán giả. Tổng giải thưởng 200 triệu đồng.'),
    ('Vũng Tàu Beach Music Festival 2026', 'Âm Nhạc & Concert', 'saigon_concerts',
     'Bãi Sau (Bãi Thùy Vân), Đường Thùy Vân, Phường Thắng Tam, TP. Vũng Tàu', 'Bà Rịa - Vũng Tàu',
     '2026-10-31 15:00:00', '2026-11-01 00:30:00', '2026-09-15 10:00:00', '2026-10-30 23:59:59', 'OPENING',
     'Lễ hội âm nhạc trên bãi biển với DJ, ban nhạc và màn pháo hoa lúc nửa đêm. Khu cắm trại, ẩm thực hải sản và trò chơi bãi biển mở cửa từ 15h.'),
    ('Countdown Party 2027 – Chào Năm Mới Hà Nội', 'Âm Nhạc & Concert', 'hanoi_arts',
     'Quảng trường Đông Kinh Nghĩa Thục, Hồ Hoàn Kiếm, Quận Hoàn Kiếm', 'Hà Nội',
     '2026-12-31 20:00:00', '2027-01-01 01:00:00', '2026-10-15 10:00:00', '2026-12-30 23:59:59', 'UPCOMING',
     'Đêm nhạc đếm ngược chào năm mới 2027 bên Hồ Gươm với dàn ca sĩ khách mời và màn bắn pháo hoa nghệ thuật lúc 0h. Vé mở bán từ ngày 15/10/2026.'),
    -- Công Nghệ & Startup
    ('Hà Nội Developer Day 2026', 'Công Nghệ & Startup', 'techfest_vn',
     'Khách sạn Melia Hà Nội, 44B Lý Thường Kiệt, Quận Hoàn Kiếm', 'Hà Nội',
     '2026-10-18 08:00:00', '2026-10-18 17:30:00', '2026-09-01 09:00:00', '2026-10-16 23:59:59', 'OPENING',
     'Ngày hội lập trình viên với 4 track song song: Backend & Cloud, Frontend & Mobile, AI/ML và DevOps. Hơn 30 diễn giả từ các công ty công nghệ hàng đầu Việt Nam.'),
    ('Đà Nẵng Startup Pitching Day 2026', 'Công Nghệ & Startup', 'mientrung_events',
     'Cung Hội nghị Ariyana, 107 Võ Nguyên Giáp, Quận Ngũ Hành Sơn', 'Đà Nẵng',
     '2026-10-09 13:30:00', '2026-10-09 18:00:00', '2026-09-10 09:00:00', '2026-10-08 23:59:59', 'OPENING',
     '20 startup tiềm năng nhất miền Trung trình bày trước hội đồng quỹ đầu tư trong và ngoài nước. Khán giả được bình chọn giải Startup được yêu thích nhất.'),
    ('Vietnam Fintech & Blockchain Forum 2026', 'Công Nghệ & Startup', 'techfest_vn',
     'GEM Center, 8 Nguyễn Bỉnh Khiêm, Phường Đa Kao, Quận 1', 'TP. Hồ Chí Minh',
     '2026-11-20 08:30:00', '2026-11-20 17:00:00', '2026-10-05 09:00:00', '2026-11-18 23:59:59', 'UPCOMING',
     'Diễn đàn về thanh toán số, ngân hàng mở, tài sản số và khung pháp lý blockchain tại Việt Nam. Vé mở bán từ ngày 05/10/2026.'),
    ('Hội Thảo Cloud & DevOps Mùa Hè 2026', 'Công Nghệ & Startup', 'techfest_vn',
     'Khách sạn Pan Pacific Hà Nội, 1 Thanh Niên, Quận Ba Đình', 'Hà Nội',
     '2026-08-15 08:00:00', '2026-08-15 17:00:00', '2026-07-01 09:00:00', '2026-08-14 23:59:59', 'COMPLETED',
     'Hội thảo chuyên sâu về Kubernetes, CI/CD và tối ưu chi phí cloud. Sự kiện đã diễn ra thành công với hơn 500 kỹ sư tham dự.'),
    -- Workshop & Kỹ Năng
    ('Workshop Thiết Kế UI/UX Cho Người Mới Bắt Đầu', 'Workshop & Kỹ Năng', 'techfest_vn',
     'Đại học Cần Thơ – Khu II, Đường 3 Tháng 2, Phường Xuân Khánh, Quận Ninh Kiều', 'Cần Thơ',
     '2026-10-04 08:30:00', '2026-10-04 16:30:00', '2026-09-12 09:00:00', '2026-10-03 20:00:00', 'OPENING',
     'Một ngày thực hành thiết kế giao diện ứng dụng di động bằng Figma, từ wireframe đến prototype. Học viên tự mang laptop, được cấp chứng nhận hoàn thành.'),
    ('Workshop Làm Gốm Bát Tràng Cuối Tuần', 'Workshop & Kỹ Năng', 'hanoi_arts',
     'Làng gốm Bát Tràng, Xã Bát Tràng, Huyện Gia Lâm', 'Hà Nội',
     '2026-10-04 08:00:00', '2026-10-04 11:30:00', '2026-09-15 09:00:00', '2026-10-03 22:00:00', 'OPENING',
     'Trải nghiệm tự tay nặn gốm trên bàn xoay cùng nghệ nhân làng nghề 500 năm tuổi. Sản phẩm được nung và gửi về tận nhà sau 7 ngày.'),
    -- Thể Thao & Giải Chạy
    ('Sa Pa Mountain Trail 2026', 'Thể Thao & Giải Chạy', 'vn_marathon',
     'Quảng trường Sa Pa, Thị xã Sa Pa', 'Lào Cai',
     '2026-11-14 04:00:00', '2026-11-15 18:00:00', '2026-08-20 09:00:00', '2026-10-31 23:59:59', 'OPENING',
     'Giải chạy địa hình băng qua ruộng bậc thang, bản làng và đỉnh núi quanh Sa Pa. Cung đường 70km có tổng độ cao tích lũy hơn 4.000m, yêu cầu chứng nhận hoàn thành giải trail trước đó.'),
    ('Giải Pickleball Mở Rộng Bình Dương 2026', 'Thể Thao & Giải Chạy', 'vn_marathon',
     'Trung tâm TDTT tỉnh Bình Dương, Đường 30 Tháng 4, Phường Phú Hòa, TP. Thủ Dầu Một', 'Bình Dương',
     '2026-10-10 07:00:00', '2026-10-11 18:00:00', '2026-09-05 09:00:00', '2026-10-05 23:59:59', 'OPENING',
     'Giải pickleball mở rộng dành cho vận động viên nghiệp dư và chuyên nghiệp với 16 sân thi đấu. Khán giả vào xem miễn phí khu vực ngoài, vé khán đài có ghế ngồi và mái che.'),
    ('Nha Trang Triathlon 2026', 'Thể Thao & Giải Chạy', 'vn_marathon',
     'Quảng trường 2 Tháng 4, Đường Trần Phú, TP. Nha Trang', 'Khánh Hòa',
     '2026-11-08 05:30:00', '2026-11-08 13:00:00', '2026-08-25 09:00:00', '2026-10-25 23:59:59', 'OPENING',
     'Giải ba môn phối hợp bơi biển – đạp xe – chạy bộ dọc vịnh Nha Trang. Có cự ly Sprint cho người mới và nội dung tiếp sức đồng đội.'),
    ('Giải Đua Thuyền Rồng Sông Hương 2026', 'Thể Thao & Giải Chạy', 'mientrung_events',
     'Bờ Nam sông Hương, đoạn cầu Trường Tiền – cầu Phú Xuân, TP. Huế', 'Huế',
     '2026-10-25 07:30:00', '2026-10-25 11:30:00', '2026-09-15 09:00:00', '2026-10-24 23:59:59', 'OPENING',
     'Giải đua thuyền truyền thống với 24 đội đến từ các phường xã ven sông Hương. Khán đài dựng dọc bờ sông, có bình luận trực tiếp và biểu diễn trống hội.'),
    ('Chung Kết Giải Bóng Đá Phủi Hải Phòng Cup 2026', 'Thể Thao & Giải Chạy', 'vn_marathon',
     'Sân vận động Lạch Tray, 15 Lạch Tray, Quận Ngô Quyền', 'Hải Phòng',
     '2026-10-04 15:30:00', '2026-10-04 18:00:00', '2026-09-15 09:00:00', '2026-10-04 12:00:00', 'OPENING',
     'Trận chung kết giải bóng đá phong trào lớn nhất đất Cảng, quy tụ 32 đội bóng nghiệp dư. Trước trận có màn cổ vũ của các hội cổ động viên.'),
    -- Văn Hóa & Nghệ Thuật
    ('Múa Rối Nước Thăng Long – Suất Diễn Đặc Biệt Mùa Thu', 'Văn Hóa & Nghệ Thuật', 'hanoi_arts',
     'Nhà hát Múa rối Thăng Long, 57B Đinh Tiên Hoàng, Quận Hoàn Kiếm', 'Hà Nội',
     '2026-10-03 18:30:00', '2026-10-03 19:30:00', '2026-09-01 09:00:00', '2026-10-03 17:00:00', 'OPENING',
     'Suất diễn đặc biệt với 17 tiết mục rối nước truyền thống như Múa rồng, Chú Tễu, Đánh cá, kèm phần giao lưu với nghệ nhân sau buổi diễn.'),
    ('Đêm Phố Cổ Hội An – Lễ Hội Đèn Lồng & Hoa Đăng', 'Văn Hóa & Nghệ Thuật', 'mientrung_events',
     'Phố cổ Hội An, Đường Nguyễn Phúc Chu – Chùa Cầu, TP. Hội An', 'Quảng Nam',
     '2026-10-25 18:00:00', '2026-10-25 22:00:00', '2026-09-20 09:00:00', '2026-10-25 17:00:00', 'OPENING',
     'Đêm rằm phố cổ tắt đèn điện, chỉ thắp sáng bằng đèn lồng. Du khách được thả hoa đăng trên sông Hoài, xem biểu diễn bài chòi và hát hò khoan.'),
    ('Đêm Nhã Nhạc Cung Đình Tại Đại Nội Huế', 'Văn Hóa & Nghệ Thuật', 'mientrung_events',
     'Duyệt Thị Đường, Đại Nội Huế, Đường 23 Tháng 8, Phường Thuận Hòa', 'Huế',
     '2026-10-17 19:30:00', '2026-10-17 21:00:00', '2026-09-01 09:00:00', '2026-10-17 17:00:00', 'OPENING',
     'Chương trình Nhã nhạc cung đình Huế — Di sản văn hóa phi vật thể được UNESCO công nhận — biểu diễn tại nhà hát cổ nhất Việt Nam trong Đại Nội.'),
    ('Đêm Dân Ca Ví Giặm Xứ Nghệ', 'Văn Hóa & Nghệ Thuật', 'mientrung_events',
     'Quảng trường Hồ Chí Minh, Đại lộ Lê Nin, TP. Vinh', 'Nghệ An',
     '2026-09-27 19:30:00', '2026-09-27 22:00:00', '2026-09-01 09:00:00', '2026-09-26 23:59:59', 'OPENING',
     'Đêm diễn dân ca Ví Giặm Nghệ Tĩnh với sự tham gia của các nghệ nhân ưu tú và câu lạc bộ dân ca các huyện. Vé sắp ngừng bán.'),
    ('Vietnam Comic & Cosplay Festival 2026', 'Văn Hóa & Nghệ Thuật', 'saigon_concerts',
     'Nhà Văn hóa Thanh niên, 4 Phạm Ngọc Thạch, Phường Bến Nghé, Quận 1', 'TP. Hồ Chí Minh',
     '2026-10-24 09:00:00', '2026-10-25 20:00:00', '2026-09-15 10:00:00', '2026-10-23 23:59:59', 'OPENING',
     'Hai ngày lễ hội truyện tranh, anime và cosplay với hơn 150 gian hàng artist, khu trưng bày mô hình và cuộc thi cosplay toàn quốc.'),
    -- Sân Khấu & Hài Kịch
    ('Kịch Nói: Tấm Cám – Phiên Bản Đương Đại', 'Sân Khấu & Hài Kịch', 'saigon_concerts',
     'Sân khấu kịch IDECAF, 28 Lê Thánh Tôn, Phường Bến Nghé, Quận 1', 'TP. Hồ Chí Minh',
     '2026-10-10 19:30:00', '2026-10-10 22:00:00', '2026-09-10 09:00:00', '2026-10-10 17:00:00', 'OPENING',
     'Câu chuyện cổ tích Tấm Cám được kể lại trong bối cảnh Sài Gòn hiện đại, pha trộn hài kịch và nhạc sống. Phù hợp khán giả từ 13 tuổi.'),
    ('Stand-up Comedy Night: Chuyện Người Hà Nội', 'Sân Khấu & Hài Kịch', 'hanoi_arts',
     'Sân khấu Hầm Rượu, 18 Tông Đản, Quận Hoàn Kiếm', 'Hà Nội',
     '2026-10-02 20:00:00', '2026-10-02 22:00:00', '2026-09-05 10:00:00', '2026-10-02 18:00:00', 'OPENING',
     'Đêm hài độc thoại với 6 comedian trẻ kể chuyện tắc đường, trà đá vỉa hè và đời sống văn phòng Hà Nội. Nội dung dành cho khán giả 18+. Toàn bộ vé đã bán hết.'),
    -- Ẩm Thực & Lễ Hội
    ('Đêm Hội Trăng Rằm Phố Cổ Hà Nội 2026', 'Ẩm Thực & Lễ Hội', 'hanoi_arts',
     'Phố Hàng Mã & Phố đi bộ Hồ Gươm, Quận Hoàn Kiếm', 'Hà Nội',
     '2026-09-25 17:00:00', '2026-09-25 23:00:00', '2026-09-10 09:00:00', '2026-09-25 16:00:00', 'OPENING',
     'Đêm hội Trung thu đúng rằm tháng Tám với rước đèn, múa lân, phá cỗ và workshop làm đèn ông sao, bánh trung thu cho cả gia đình.'),
    ('Saigon Craft Beer Festival 2026', 'Ẩm Thực & Lễ Hội', 'saigon_concerts',
     'Công viên bờ sông Sài Gòn, Đường Trần Bạch Đằng, Phường Thủ Thiêm, TP. Thủ Đức', 'TP. Hồ Chí Minh',
     '2026-10-17 15:00:00', '2026-10-18 23:00:00', '2026-09-01 10:00:00', '2026-10-17 12:00:00', 'OPENING',
     'Hơn 40 nhà nấu bia thủ công trong nước và quốc tế, food truck và nhạc sống suốt 2 ngày. Chỉ dành cho khách từ 18 tuổi, vui lòng mang theo giấy tờ tùy thân.'),
    ('Lễ Hội Hải Sản & Du Thuyền Đêm Vịnh Hạ Long', 'Ẩm Thực & Lễ Hội', 'vietxplore',
     'Cảng tàu khách Quốc tế Tuần Châu, TP. Hạ Long', 'Quảng Ninh',
     '2026-10-11 17:30:00', '2026-10-11 21:30:00', '2026-09-10 09:00:00', '2026-10-10 23:59:59', 'OPENING',
     'Bốn giờ trên du thuyền ngắm hoàng hôn vịnh Hạ Long, thưởng thức buffet hải sản tươi sống và câu mực đêm.'),
    ('Lễ Hội Ẩm Thực Biển Sầm Sơn 2026', 'Ẩm Thực & Lễ Hội', 'vietxplore',
     'Quảng trường biển Sầm Sơn, Đường Hồ Xuân Hương, TP. Sầm Sơn', 'Thanh Hóa',
     '2026-09-26 16:00:00', '2026-09-27 22:00:00', '2026-09-01 09:00:00', '2026-09-21 23:59:59', 'CLOSED',
     'Lễ hội ẩm thực biển với 100 gian hàng đặc sản xứ Thanh như nem chua, chả tôm, gỏi cá nhệch. Sự kiện đã ngừng bán vé online, khách có thể mua vé trực tiếp tại cổng.'),
    -- Du Lịch & Trải Nghiệm
    ('Tour Bình Minh Chợ Nổi Cái Răng & Vườn Trái Cây', 'Du Lịch & Trải Nghiệm', 'vietxplore',
     'Bến Ninh Kiều, Đường Hai Bà Trưng, Quận Ninh Kiều', 'Cần Thơ',
     '2026-10-04 05:00:00', '2026-10-04 11:30:00', '2026-09-01 09:00:00', '2026-10-03 20:00:00', 'OPENING',
     'Đi thuyền đón bình minh trên chợ nổi Cái Răng, ăn sáng hủ tiếu trên ghe và tham quan vườn trái cây, lò hủ tiếu truyền thống.'),
    ('Đêm Tràng An – Du Thuyền Ngắm Hang Động Kỳ Ảo', 'Du Lịch & Trải Nghiệm', 'vietxplore',
     'Khu du lịch sinh thái Tràng An, Xã Trường Yên, Huyện Hoa Lư', 'Ninh Bình',
     '2026-10-18 18:00:00', '2026-10-18 21:00:00', '2026-09-15 09:00:00', '2026-10-17 23:59:59', 'OPENING',
     'Hành trình thuyền đêm qua các hang động được chiếu sáng nghệ thuật trong quần thể Di sản thế giới Tràng An, kết hợp biểu diễn hát chèo trên sông.'),
    ('Phú Quốc Sunset Yoga & Wellness Retreat', 'Du Lịch & Trải Nghiệm', 'vietxplore',
     'Bãi Trường, Xã Dương Tơ, TP. Phú Quốc', 'Kiên Giang',
     '2026-11-06 06:00:00', '2026-11-08 12:00:00', '2026-09-01 09:00:00', '2026-10-30 23:59:59', 'OPENING',
     'Chương trình nghỉ dưỡng 3 ngày 2 đêm với yoga bình minh và hoàng hôn trên biển, thiền, spa và ẩm thực healthy. Có lớp yoga lẻ cho khách không lưu trú.'),
    ('Khám Phá Rừng Tràm Trà Sư Mùa Nước Nổi', 'Du Lịch & Trải Nghiệm', 'vietxplore',
     'Rừng tràm Trà Sư, Xã Văn Giáo, Thị xã Tịnh Biên', 'An Giang',
     '2026-10-11 07:00:00', '2026-10-11 15:00:00', '2026-09-10 09:00:00', '2026-10-10 20:00:00', 'OPENING',
     'Đi xuồng ba lá xuyên rừng tràm phủ bèo xanh mùa nước nổi, ngắm chim trời và thưởng thức đặc sản cá linh, bông điên điển.'),
    ('Hành Trình Về Đất Mũi Cà Mau', 'Du Lịch & Trải Nghiệm', 'vietxplore',
     'Khu du lịch Mũi Cà Mau, Xã Đất Mũi, Huyện Ngọc Hiển', 'Cà Mau',
     '2026-11-01 06:00:00', '2026-11-02 18:00:00', '2026-09-15 09:00:00', '2026-10-28 23:59:59', 'OPENING',
     'Hành trình đến điểm cực Nam Tổ quốc: cột mốc tọa độ GPS 0001, rừng đước ngập mặn và làng chài Khai Long.'),
    ('Camping Music Night Đồng Nai', 'Du Lịch & Trải Nghiệm', 'saigon_concerts',
     'Khu du lịch Thác Giang Điền, Xã Giang Điền, Huyện Trảng Bom', 'Đồng Nai',
     '2026-10-17 15:00:00', '2026-10-18 10:00:00', '2026-09-10 09:00:00', '2026-10-15 23:59:59', 'OPENING',
     'Một đêm cắm trại bên thác Giang Điền với nhạc acoustic quanh lửa trại, chiếu phim ngoài trời và BBQ. Cách TP.HCM khoảng 50km.')
) AS v(name, category, organizer, location, province, start_time, end_time, sale_start, sale_end, status, description)
WHERE NOT EXISTS (SELECT 1 FROM events e WHERE e.name = v.name);


-- ============================================================
-- 4. ẢNH SỰ KIỆN (Cloudinary) — ảnh đầu tiên của mỗi sự kiện là ảnh bìa
-- ============================================================

INSERT INTO event_images (event_id, image_url, created_at, updated_at)
SELECT e.id, v.image_url, NOW(), NOW()
FROM (VALUES
    (1, 'Đêm Nhạc Acoustic Mùa Sương Đà Lạt 2026', 'https://res.cloudinary.com/n9oo62kx/image/upload/v1790150239/event-mng/seed-v4/dalat-acoustic-1.jpg'),
    (2, 'Đêm Nhạc Acoustic Mùa Sương Đà Lạt 2026', 'https://res.cloudinary.com/n9oo62kx/image/upload/v1790150242/event-mng/seed-v4/dalat-acoustic-2.jpg'),
    (3, 'Hà Nội Rock Festival 2026', 'https://res.cloudinary.com/n9oo62kx/image/upload/v1790150244/event-mng/seed-v4/hanoi-rock-1.jpg'),
    (4, 'Hà Nội Rock Festival 2026', 'https://res.cloudinary.com/n9oo62kx/image/upload/v1790150245/event-mng/seed-v4/hanoi-rock-2.jpg'),
    (5, 'Hòa Nhạc Giao Hưởng Mùa Thu Vienna', 'https://res.cloudinary.com/n9oo62kx/image/upload/v1790150247/event-mng/seed-v4/hanoi-symphony-1.jpg'),
    (6, 'Hòa Nhạc Giao Hưởng Mùa Thu Vienna', 'https://res.cloudinary.com/n9oo62kx/image/upload/v1790150249/event-mng/seed-v4/hanoi-symphony-2.jpg'),
    (7, 'Đêm Nhạc Jazz Trên Du Thuyền Sông Sài Gòn', 'https://res.cloudinary.com/n9oo62kx/image/upload/v1790150251/event-mng/seed-v4/saigon-jazz-1.jpg'),
    (8, 'Đêm Nhạc Jazz Trên Du Thuyền Sông Sài Gòn', 'https://res.cloudinary.com/n9oo62kx/image/upload/v1790150253/event-mng/seed-v4/saigon-jazz-2.jpg'),
    (9, 'K-Pop Dance Festival Đà Nẵng 2026', 'https://res.cloudinary.com/n9oo62kx/image/upload/v1790150254/event-mng/seed-v4/danang-kpop-1.jpg'),
    (10, 'K-Pop Dance Festival Đà Nẵng 2026', 'https://res.cloudinary.com/n9oo62kx/image/upload/v1790150257/event-mng/seed-v4/danang-kpop-2.jpg'),
    (11, 'Vũng Tàu Beach Music Festival 2026', 'https://res.cloudinary.com/n9oo62kx/image/upload/v1790150258/event-mng/seed-v4/vungtau-beach-1.jpg'),
    (12, 'Vũng Tàu Beach Music Festival 2026', 'https://res.cloudinary.com/n9oo62kx/image/upload/v1790150260/event-mng/seed-v4/vungtau-beach-2.jpg'),
    (13, 'Countdown Party 2027 – Chào Năm Mới Hà Nội', 'https://res.cloudinary.com/n9oo62kx/image/upload/v1790150262/event-mng/seed-v4/hanoi-countdown-1.jpg'),
    (14, 'Countdown Party 2027 – Chào Năm Mới Hà Nội', 'https://res.cloudinary.com/n9oo62kx/image/upload/v1790150264/event-mng/seed-v4/hanoi-countdown-2.jpg'),
    (15, 'Hà Nội Developer Day 2026', 'https://res.cloudinary.com/n9oo62kx/image/upload/v1790150265/event-mng/seed-v4/hanoi-devday-1.jpg'),
    (16, 'Hà Nội Developer Day 2026', 'https://res.cloudinary.com/n9oo62kx/image/upload/v1790150267/event-mng/seed-v4/hanoi-devday-2.jpg'),
    (17, 'Đà Nẵng Startup Pitching Day 2026', 'https://res.cloudinary.com/n9oo62kx/image/upload/v1790150269/event-mng/seed-v4/danang-startup-1.jpg'),
    (18, 'Đà Nẵng Startup Pitching Day 2026', 'https://res.cloudinary.com/n9oo62kx/image/upload/v1790150271/event-mng/seed-v4/danang-startup-2.jpg'),
    (19, 'Vietnam Fintech & Blockchain Forum 2026', 'https://res.cloudinary.com/n9oo62kx/image/upload/v1790150274/event-mng/seed-v4/hcm-fintech-1.jpg'),
    (20, 'Vietnam Fintech & Blockchain Forum 2026', 'https://res.cloudinary.com/n9oo62kx/image/upload/v1790150275/event-mng/seed-v4/hcm-fintech-2.jpg'),
    (21, 'Hội Thảo Cloud & DevOps Mùa Hè 2026', 'https://res.cloudinary.com/n9oo62kx/image/upload/v1790150278/event-mng/seed-v4/hanoi-cloud-summer-1.jpg'),
    (22, 'Workshop Thiết Kế UI/UX Cho Người Mới Bắt Đầu', 'https://res.cloudinary.com/n9oo62kx/image/upload/v1790150280/event-mng/seed-v4/cantho-uiux-1.jpg'),
    (23, 'Workshop Thiết Kế UI/UX Cho Người Mới Bắt Đầu', 'https://res.cloudinary.com/n9oo62kx/image/upload/v1790150284/event-mng/seed-v4/cantho-uiux-2.jpg'),
    (24, 'Workshop Làm Gốm Bát Tràng Cuối Tuần', 'https://res.cloudinary.com/n9oo62kx/image/upload/v1790150286/event-mng/seed-v4/battrang-pottery-1.jpg'),
    (25, 'Workshop Làm Gốm Bát Tràng Cuối Tuần', 'https://res.cloudinary.com/n9oo62kx/image/upload/v1790150287/event-mng/seed-v4/battrang-pottery-2.jpg'),
    (26, 'Sa Pa Mountain Trail 2026', 'https://res.cloudinary.com/n9oo62kx/image/upload/v1790150289/event-mng/seed-v4/sapa-trail-1.jpg'),
    (27, 'Sa Pa Mountain Trail 2026', 'https://res.cloudinary.com/n9oo62kx/image/upload/v1790150291/event-mng/seed-v4/sapa-trail-2.jpg'),
    (28, 'Giải Pickleball Mở Rộng Bình Dương 2026', 'https://res.cloudinary.com/n9oo62kx/image/upload/v1790150294/event-mng/seed-v4/binhduong-pickleball-1.jpg'),
    (29, 'Giải Pickleball Mở Rộng Bình Dương 2026', 'https://res.cloudinary.com/n9oo62kx/image/upload/v1790150296/event-mng/seed-v4/binhduong-pickleball-2.jpg'),
    (30, 'Nha Trang Triathlon 2026', 'https://res.cloudinary.com/n9oo62kx/image/upload/v1790150298/event-mng/seed-v4/nhatrang-triathlon-1.jpg'),
    (31, 'Nha Trang Triathlon 2026', 'https://res.cloudinary.com/n9oo62kx/image/upload/v1790150300/event-mng/seed-v4/nhatrang-triathlon-2.jpg'),
    (32, 'Giải Đua Thuyền Rồng Sông Hương 2026', 'https://res.cloudinary.com/n9oo62kx/image/upload/v1790150302/event-mng/seed-v4/hue-dragonboat-1.jpg'),
    (33, 'Giải Đua Thuyền Rồng Sông Hương 2026', 'https://res.cloudinary.com/n9oo62kx/image/upload/v1790150417/event-mng/seed-v4/hue-dragonboat-2.jpg'),
    (34, 'Chung Kết Giải Bóng Đá Phủi Hải Phòng Cup 2026', 'https://res.cloudinary.com/n9oo62kx/image/upload/v1790150305/event-mng/seed-v4/haiphong-football-1.jpg'),
    (35, 'Múa Rối Nước Thăng Long – Suất Diễn Đặc Biệt Mùa Thu', 'https://res.cloudinary.com/n9oo62kx/image/upload/v1790150307/event-mng/seed-v4/thanglong-waterpuppet-1.jpg'),
    (36, 'Múa Rối Nước Thăng Long – Suất Diễn Đặc Biệt Mùa Thu', 'https://res.cloudinary.com/n9oo62kx/image/upload/v1790150309/event-mng/seed-v4/thanglong-waterpuppet-2.jpg'),
    (37, 'Đêm Phố Cổ Hội An – Lễ Hội Đèn Lồng & Hoa Đăng', 'https://res.cloudinary.com/n9oo62kx/image/upload/v1790150311/event-mng/seed-v4/hoian-lantern-1.jpg'),
    (38, 'Đêm Phố Cổ Hội An – Lễ Hội Đèn Lồng & Hoa Đăng', 'https://res.cloudinary.com/n9oo62kx/image/upload/v1790150313/event-mng/seed-v4/hoian-lantern-2.jpg'),
    (39, 'Đêm Nhã Nhạc Cung Đình Tại Đại Nội Huế', 'https://res.cloudinary.com/n9oo62kx/image/upload/v1790150314/event-mng/seed-v4/hue-nhanhac-1.jpg'),
    (40, 'Đêm Nhã Nhạc Cung Đình Tại Đại Nội Huế', 'https://res.cloudinary.com/n9oo62kx/image/upload/v1790150316/event-mng/seed-v4/hue-nhanhac-2.jpg'),
    (41, 'Đêm Dân Ca Ví Giặm Xứ Nghệ', 'https://res.cloudinary.com/n9oo62kx/image/upload/v1790150318/event-mng/seed-v4/nghean-vigiam-1.jpg'),
    (42, 'Đêm Dân Ca Ví Giặm Xứ Nghệ', 'https://res.cloudinary.com/n9oo62kx/image/upload/v1790150321/event-mng/seed-v4/nghean-vigiam-2.jpg'),
    (43, 'Vietnam Comic & Cosplay Festival 2026', 'https://res.cloudinary.com/n9oo62kx/image/upload/v1790150348/event-mng/seed-v4/hcm-cosplay-1.jpg'),
    (44, 'Vietnam Comic & Cosplay Festival 2026', 'https://res.cloudinary.com/n9oo62kx/image/upload/v1790150351/event-mng/seed-v4/hcm-cosplay-2.jpg'),
    (45, 'Kịch Nói: Tấm Cám – Phiên Bản Đương Đại', 'https://res.cloudinary.com/n9oo62kx/image/upload/v1790150326/event-mng/seed-v4/hcm-tamcam-theatre-1.jpg'),
    (46, 'Stand-up Comedy Night: Chuyện Người Hà Nội', 'https://res.cloudinary.com/n9oo62kx/image/upload/v1790150328/event-mng/seed-v4/hanoi-standup-1.jpg'),
    (47, 'Stand-up Comedy Night: Chuyện Người Hà Nội', 'https://res.cloudinary.com/n9oo62kx/image/upload/v1790150330/event-mng/seed-v4/hanoi-standup-2.jpg'),
    (48, 'Đêm Hội Trăng Rằm Phố Cổ Hà Nội 2026', 'https://res.cloudinary.com/n9oo62kx/image/upload/v1790150323/event-mng/seed-v4/hanoi-midautumn-1.jpg'),
    (49, 'Đêm Hội Trăng Rằm Phố Cổ Hà Nội 2026', 'https://res.cloudinary.com/n9oo62kx/image/upload/v1790150325/event-mng/seed-v4/hanoi-midautumn-2.jpg'),
    (50, 'Saigon Craft Beer Festival 2026', 'https://res.cloudinary.com/n9oo62kx/image/upload/v1790150333/event-mng/seed-v4/saigon-craftbeer-1.jpg'),
    (51, 'Saigon Craft Beer Festival 2026', 'https://res.cloudinary.com/n9oo62kx/image/upload/v1790150334/event-mng/seed-v4/saigon-craftbeer-2.jpg'),
    (52, 'Lễ Hội Hải Sản & Du Thuyền Đêm Vịnh Hạ Long', 'https://res.cloudinary.com/n9oo62kx/image/upload/v1790150336/event-mng/seed-v4/halong-seafood-1.jpg'),
    (53, 'Lễ Hội Hải Sản & Du Thuyền Đêm Vịnh Hạ Long', 'https://res.cloudinary.com/n9oo62kx/image/upload/v1790150339/event-mng/seed-v4/halong-seafood-2.jpg'),
    (54, 'Lễ Hội Ẩm Thực Biển Sầm Sơn 2026', 'https://res.cloudinary.com/n9oo62kx/image/upload/v1790150341/event-mng/seed-v4/samson-food-1.jpg'),
    (55, 'Lễ Hội Ẩm Thực Biển Sầm Sơn 2026', 'https://res.cloudinary.com/n9oo62kx/image/upload/v1790150346/event-mng/seed-v4/samson-food-2.jpg'),
    (56, 'Tour Bình Minh Chợ Nổi Cái Răng & Vườn Trái Cây', 'https://res.cloudinary.com/n9oo62kx/image/upload/v1790150354/event-mng/seed-v4/cantho-floating-market-1.jpg'),
    (57, 'Tour Bình Minh Chợ Nổi Cái Răng & Vườn Trái Cây', 'https://res.cloudinary.com/n9oo62kx/image/upload/v1790150357/event-mng/seed-v4/cantho-floating-market-2.jpg'),
    (58, 'Đêm Tràng An – Du Thuyền Ngắm Hang Động Kỳ Ảo', 'https://res.cloudinary.com/n9oo62kx/image/upload/v1790150359/event-mng/seed-v4/ninhbinh-trangan-1.jpg'),
    (59, 'Đêm Tràng An – Du Thuyền Ngắm Hang Động Kỳ Ảo', 'https://res.cloudinary.com/n9oo62kx/image/upload/v1790150361/event-mng/seed-v4/ninhbinh-trangan-2.jpg'),
    (60, 'Phú Quốc Sunset Yoga & Wellness Retreat', 'https://res.cloudinary.com/n9oo62kx/image/upload/v1790150364/event-mng/seed-v4/phuquoc-yoga-1.jpg'),
    (61, 'Phú Quốc Sunset Yoga & Wellness Retreat', 'https://res.cloudinary.com/n9oo62kx/image/upload/v1790150367/event-mng/seed-v4/phuquoc-yoga-2.jpg'),
    (62, 'Khám Phá Rừng Tràm Trà Sư Mùa Nước Nổi', 'https://res.cloudinary.com/n9oo62kx/image/upload/v1790150369/event-mng/seed-v4/angiang-trasu-1.jpg'),
    (63, 'Khám Phá Rừng Tràm Trà Sư Mùa Nước Nổi', 'https://res.cloudinary.com/n9oo62kx/image/upload/v1790150371/event-mng/seed-v4/angiang-trasu-2.jpg'),
    (64, 'Hành Trình Về Đất Mũi Cà Mau', 'https://res.cloudinary.com/n9oo62kx/image/upload/v1790150373/event-mng/seed-v4/camau-datmui-1.jpg'),
    (65, 'Hành Trình Về Đất Mũi Cà Mau', 'https://res.cloudinary.com/n9oo62kx/image/upload/v1790150376/event-mng/seed-v4/camau-datmui-2.jpg'),
    (66, 'Camping Music Night Đồng Nai', 'https://res.cloudinary.com/n9oo62kx/image/upload/v1790150378/event-mng/seed-v4/dongnai-camping-1.jpg'),
    (67, 'Camping Music Night Đồng Nai', 'https://res.cloudinary.com/n9oo62kx/image/upload/v1790150380/event-mng/seed-v4/dongnai-camping-2.jpg')
) AS v(sort_order, event_name, image_url)
JOIN events e ON e.name = v.event_name
WHERE NOT EXISTS (SELECT 1 FROM event_images i WHERE i.event_id = e.id AND i.image_url = v.image_url)
ORDER BY v.sort_order;


-- ============================================================
-- 5. LOẠI VÉ
-- ============================================================

INSERT INTO ticket_types (event_id, name, price, total_quantity, remaining_quantity, description, created_at, updated_at)
SELECT e.id, v.name, v.price, v.total_quantity, v.remaining_quantity, v.description, NOW(), NOW()
FROM (VALUES
    -- Đêm Nhạc Acoustic Mùa Sương Đà Lạt 2026
    (1, 'Đêm Nhạc Acoustic Mùa Sương Đà Lạt 2026', 'Vé Thường (Khu C)', 390000.00, 600, 214, 'Ghế ngồi khu C phía sau, tầm nhìn bao quát sân khấu.'),
    (2, 'Đêm Nhạc Acoustic Mùa Sương Đà Lạt 2026', 'Vé VIP (Bàn Tròn + Trà Nóng)', 690000.00, 200, 37, 'Bàn tròn 4 người khu B, kèm trà atiso nóng và bánh ngọt Đà Lạt.'),
    (3, 'Đêm Nhạc Acoustic Mùa Sương Đà Lạt 2026', 'Vé Couple Sát Sân Khấu (2 Người)', 1290000.00, 50, 6, 'Cặp ghế sofa đôi ngay hàng đầu, tặng hoa và ảnh chụp lưu niệm.'),
    -- Hà Nội Rock Festival 2026
    (4, 'Hà Nội Rock Festival 2026', 'Vé Early Bird GA', 299000.00, 1000, 0, 'Vé mở bán sớm số lượng giới hạn — đã bán hết.'),
    (5, 'Hà Nội Rock Festival 2026', 'Vé GA', 449000.00, 3000, 1650, 'Khu vực đứng chung trước sân khấu chính.'),
    (6, 'Hà Nội Rock Festival 2026', 'Vé Fanzone Sát Sân Khấu', 799000.00, 800, 120, 'Khu vực moshpit sát sân khấu, lối vào riêng.'),
    (7, 'Hà Nội Rock Festival 2026', 'Vé VIP Lounge (Đồ Uống Không Giới Hạn)', 1690000.00, 150, 44, 'Khán đài VIP có mái che, đồ uống không giới hạn và nhà vệ sinh riêng.'),
    -- Hòa Nhạc Giao Hưởng Mùa Thu Vienna
    (8, 'Hòa Nhạc Giao Hưởng Mùa Thu Vienna', 'Ghế Tầng 3 (Balcony)', 450000.00, 200, 88, 'Ghế tầng 3, âm thanh tốt, tầm nhìn từ trên cao.'),
    (9, 'Hòa Nhạc Giao Hưởng Mùa Thu Vienna', 'Ghế Tầng 2', 950000.00, 180, 60, 'Ghế tầng 2 chính diện sân khấu.'),
    (10, 'Hòa Nhạc Giao Hưởng Mùa Thu Vienna', 'Ghế Hạng A Tầng 1', 1800000.00, 250, 31, 'Ghế tầng 1 hàng 1 đến 12, vị trí đẹp nhất khán phòng.'),
    (11, 'Hòa Nhạc Giao Hưởng Mùa Thu Vienna', 'Lô Box Riêng (4 Ghế)', 6800000.00, 12, 0, 'Lô riêng 4 ghế bên hông sân khấu kèm champagne — đã bán hết.'),
    -- Đêm Nhạc Jazz Trên Du Thuyền Sông Sài Gòn
    (12, 'Đêm Nhạc Jazz Trên Du Thuyền Sông Sài Gòn', 'Vé Boong Mở (Không Kèm Ăn Tối)', 590000.00, 120, 47, 'Chỗ đứng/ngồi tự do trên boong, kèm 1 ly cocktail.'),
    (13, 'Đêm Nhạc Jazz Trên Du Thuyền Sông Sài Gòn', 'Vé Dinner Jazz (Set Menu Âu)', 1450000.00, 80, 19, 'Bàn ăn trong khoang, set menu Âu 4 món.'),
    (14, 'Đêm Nhạc Jazz Trên Du Thuyền Sông Sài Gòn', 'Bàn VIP Mũi Tàu (4 Người)', 6500000.00, 5, 1, 'Bàn riêng ở mũi tàu cho 4 người, set menu cao cấp và rượu vang.'),
    -- K-Pop Dance Festival Đà Nẵng 2026
    (15, 'K-Pop Dance Festival Đà Nẵng 2026', 'Vé Khán Đài', 199000.00, 2500, 1230, 'Ghế khán đài tự do.'),
    (16, 'K-Pop Dance Festival Đà Nẵng 2026', 'Vé Standing Sát Sân Khấu', 499000.00, 800, 0, 'Khu đứng sát sân khấu kèm lightstick — đã bán hết.'),
    (17, 'K-Pop Dance Festival Đà Nẵng 2026', 'Vé Thí Sinh Random Play Dance', 99000.00, 300, 85, 'Đăng ký tham gia phần Random Play Dance, kèm vé vào cổng.'),
    -- Vũng Tàu Beach Music Festival 2026
    (18, 'Vũng Tàu Beach Music Festival 2026', 'Vé 1 Ngày GA', 350000.00, 5000, 3890, 'Vé vào cổng khu vực chung trên bãi biển.'),
    (19, 'Vũng Tàu Beach Music Festival 2026', 'Vé Combo Nhóm 4 Người', 1200000.00, 400, 305, '4 vé GA, tiết kiệm 200.000đ so với mua lẻ.'),
    (20, 'Vũng Tàu Beach Music Festival 2026', 'Vé VIP Cabana Bãi Biển', 1150000.00, 300, 210, 'Ghế cabana có dù che, khu vực bar riêng và 2 đồ uống.'),
    -- Countdown Party 2027 – Chào Năm Mới Hà Nội
    (21, 'Countdown Party 2027 – Chào Năm Mới Hà Nội', 'Vé GA', 250000.00, 8000, 8000, 'Khu vực đứng chung quanh quảng trường.'),
    (22, 'Countdown Party 2027 – Chào Năm Mới Hà Nội', 'Vé VIP Khu Sát Sân Khấu', 990000.00, 1000, 1000, 'Khu vực VIP có rào chắn riêng sát sân khấu.'),
    (23, 'Countdown Party 2027 – Chào Năm Mới Hà Nội', 'Vé VVIP Rooftop (Buffet + Champagne)', 4500000.00, 100, 100, 'Sân thượng nhìn thẳng ra hồ, buffet tối và champagne lúc giao thừa.'),
    -- Hà Nội Developer Day 2026
    (24, 'Hà Nội Developer Day 2026', 'Vé Early Bird', 249000.00, 300, 0, 'Vé ưu đãi mở bán sớm — đã bán hết.'),
    (25, 'Hà Nội Developer Day 2026', 'Vé Sinh Viên (Xác Thực Thẻ SV)', 99000.00, 400, 112, 'Xuất trình thẻ sinh viên khi check-in.'),
    (26, 'Hà Nội Developer Day 2026', 'Vé Tiêu Chuẩn', 349000.00, 800, 390, 'Tham dự tất cả các track và khu triển lãm.'),
    (27, 'Hà Nội Developer Day 2026', 'Vé Business (Lunch + Networking)', 1290000.00, 150, 66, 'Kèm buffet trưa và phiên networking riêng với diễn giả.'),
    -- Đà Nẵng Startup Pitching Day 2026
    (28, 'Đà Nẵng Startup Pitching Day 2026', 'Vé Khán Giả', 50000.00, 500, 233, 'Tham dự toàn bộ phiên pitching.'),
    (29, 'Đà Nẵng Startup Pitching Day 2026', 'Đăng Ký Startup Pitching', 500000.00, 30, 4, 'Suất trình bày 5 phút trước hội đồng, cần qua vòng duyệt hồ sơ.'),
    (30, 'Đà Nẵng Startup Pitching Day 2026', 'Vé Nhà Đầu Tư (Bàn Riêng + Hồ Sơ Startup)', 2000000.00, 40, 17, 'Bàn riêng hàng đầu, nhận bộ hồ sơ chi tiết của các startup.'),
    -- Vietnam Fintech & Blockchain Forum 2026
    (31, 'Vietnam Fintech & Blockchain Forum 2026', 'Vé Sinh Viên', 390000.00, 150, 150, 'Dành cho sinh viên các ngành tài chính, CNTT.'),
    (32, 'Vietnam Fintech & Blockchain Forum 2026', 'Vé Standard', 1500000.00, 600, 600, 'Tham dự toàn bộ phiên thảo luận, kèm tea-break.'),
    (33, 'Vietnam Fintech & Blockchain Forum 2026', 'Vé VIP (Gala Dinner + Networking)', 4900000.00, 120, 120, 'Kèm gala dinner buổi tối và khu networking riêng với diễn giả.'),
    -- Hội Thảo Cloud & DevOps Mùa Hè 2026
    (34, 'Hội Thảo Cloud & DevOps Mùa Hè 2026', 'Vé Tiêu Chuẩn', 299000.00, 500, 37, 'Tham dự các phiên hội thảo chính.'),
    (35, 'Hội Thảo Cloud & DevOps Mùa Hè 2026', 'Vé Workshop Hands-on', 690000.00, 100, 0, 'Thực hành trực tiếp trên cluster Kubernetes.'),
    -- Workshop Thiết Kế UI/UX Cho Người Mới Bắt Đầu
    (36, 'Workshop Thiết Kế UI/UX Cho Người Mới Bắt Đầu', 'Vé Tham Dự (Kèm Tài Liệu Figma)', 120000.00, 60, 23, 'Tham dự workshop, kèm file tài liệu và UI kit.'),
    (37, 'Workshop Thiết Kế UI/UX Cho Người Mới Bắt Đầu', 'Vé Mentoring 1-1 Sau Workshop', 350000.00, 15, 3, 'Thêm 30 phút review portfolio 1-1 với mentor.'),
    -- Workshop Làm Gốm Bát Tràng Cuối Tuần
    (38, 'Workshop Làm Gốm Bát Tràng Cuối Tuần', 'Vé Người Lớn', 180000.00, 40, 15, 'Một lượt nặn gốm và vẽ men, kèm đất sét.'),
    (39, 'Workshop Làm Gốm Bát Tràng Cuối Tuần', 'Vé Trẻ Em (Dưới 12 Tuổi)', 120000.00, 30, 12, 'Có hướng dẫn viên hỗ trợ riêng cho bé.'),
    (40, 'Workshop Làm Gốm Bát Tràng Cuối Tuần', 'Combo Gia Đình (2 Lớn + 1 Trẻ)', 420000.00, 15, 6, 'Tiết kiệm 60.000đ so với mua lẻ.'),
    -- Sa Pa Mountain Trail 2026
    (41, 'Sa Pa Mountain Trail 2026', 'Cự Ly 10KM Discovery', 990000.00, 1200, 702, 'Race kit, huy chương và nước uống tại 2 trạm.'),
    (42, 'Sa Pa Mountain Trail 2026', 'Cự Ly 21KM Trail', 1790000.00, 1000, 436, 'Race kit, huy chương, 4 trạm tiếp nước và bảo hiểm.'),
    (43, 'Sa Pa Mountain Trail 2026', 'Cự Ly 42KM Mountain Marathon', 3200000.00, 600, 211, 'Race kit đầy đủ, chip tính giờ và xe hỗ trợ y tế.'),
    (44, 'Sa Pa Mountain Trail 2026', 'Cự Ly 70KM Ultra Trail', 5900000.00, 300, 58, 'Yêu cầu chứng nhận hoàn thành giải trail từ 42km.'),
    -- Giải Pickleball Mở Rộng Bình Dương 2026
    (45, 'Giải Pickleball Mở Rộng Bình Dương 2026', 'Vé Khán Giả (2 Ngày)', 50000.00, 1000, 640, 'Ghế khán đài có mái che trong 2 ngày thi đấu.'),
    (46, 'Giải Pickleball Mở Rộng Bình Dương 2026', 'Đăng Ký Đôi Nam/Nữ Nghiệp Dư', 600000.00, 64, 9, 'Phí đăng ký cho 1 cặp đôi, kèm áo thi đấu.'),
    (47, 'Giải Pickleball Mở Rộng Bình Dương 2026', 'Đăng Ký Đôi Nam/Nữ Chuyên Nghiệp', 1200000.00, 32, 0, 'Phí đăng ký hạng chuyên nghiệp — đã đủ số lượng.'),
    -- Nha Trang Triathlon 2026
    (48, 'Nha Trang Triathlon 2026', 'Cự Ly Sprint (750m - 20km - 5km)', 2200000.00, 700, 402, 'Dành cho người mới tham gia ba môn phối hợp.'),
    (49, 'Nha Trang Triathlon 2026', 'Cự Ly Olympic (1.5km - 40km - 10km)', 3500000.00, 500, 187, 'Cự ly tiêu chuẩn Olympic, tính giờ bằng chip.'),
    (50, 'Nha Trang Triathlon 2026', 'Tiếp Sức Đồng Đội 3 Người', 5400000.00, 100, 41, 'Mỗi thành viên đảm nhận một môn, cự ly Olympic.'),
    -- Giải Đua Thuyền Rồng Sông Hương 2026
    (51, 'Giải Đua Thuyền Rồng Sông Hương 2026', 'Vé Khán Đài Thường', 30000.00, 3000, 1890, 'Khán đài dọc bờ sông, không có mái che.'),
    (52, 'Giải Đua Thuyền Rồng Sông Hương 2026', 'Vé Khán Đài VIP (Có Mái Che)', 200000.00, 400, 150, 'Khán đài VIP gần đích, có mái che và nước uống.'),
    -- Chung Kết Giải Bóng Đá Phủi Hải Phòng Cup 2026
    (53, 'Chung Kết Giải Bóng Đá Phủi Hải Phòng Cup 2026', 'Vé Khán Đài B & C', 20000.00, 8000, 5230, 'Khán đài hai đầu sân.'),
    (54, 'Chung Kết Giải Bóng Đá Phủi Hải Phòng Cup 2026', 'Vé Khán Đài A (Có Mái Che)', 60000.00, 3000, 1180, 'Khán đài chính có mái che.'),
    -- Múa Rối Nước Thăng Long – Suất Diễn Đặc Biệt Mùa Thu
    (55, 'Múa Rối Nước Thăng Long – Suất Diễn Đặc Biệt Mùa Thu', 'Vé Hạng Thường', 100000.00, 300, 97, 'Ghế ngồi từ hàng E trở về sau.'),
    (56, 'Múa Rối Nước Thăng Long – Suất Diễn Đặc Biệt Mùa Thu', 'Vé Hạng Nhất (Hàng A - D)', 200000.00, 80, 0, 'Bốn hàng ghế đầu gần sân khấu nước — đã bán hết.'),
    -- Đêm Phố Cổ Hội An – Lễ Hội Đèn Lồng & Hoa Đăng
    (57, 'Đêm Phố Cổ Hội An – Lễ Hội Đèn Lồng & Hoa Đăng', 'Vé Tham Quan Phố Cổ + Hoa Đăng', 80000.00, 5000, 3200, 'Vé tham quan phố cổ kèm 1 đèn hoa đăng.'),
    (58, 'Đêm Phố Cổ Hội An – Lễ Hội Đèn Lồng & Hoa Đăng', 'Vé Thuyền Thả Đèn Trên Sông Hoài', 250000.00, 600, 284, '20 phút đi thuyền trên sông Hoài, kèm 3 đèn hoa đăng.'),
    -- Đêm Nhã Nhạc Cung Đình Tại Đại Nội Huế
    (59, 'Đêm Nhã Nhạc Cung Đình Tại Đại Nội Huế', 'Vé Thường', 250000.00, 200, 88, 'Ghế ngồi khán phòng Duyệt Thị Đường.'),
    (60, 'Đêm Nhã Nhạc Cung Đình Tại Đại Nội Huế', 'Vé VIP (Kèm Trà Cung Đình & Bánh Huế)', 600000.00, 60, 12, 'Hàng ghế đầu, thưởng thức trà cung đình và bánh Huế.'),
    -- Đêm Dân Ca Ví Giặm Xứ Nghệ
    (61, 'Đêm Dân Ca Ví Giặm Xứ Nghệ', 'Vé Ghế Ngồi Khán Đài', 50000.00, 2000, 780, 'Ghế khán đài ngoài trời.'),
    (62, 'Đêm Dân Ca Ví Giặm Xứ Nghệ', 'Vé Ghế VIP Hàng Đầu', 150000.00, 200, 19, 'Hàng ghế đầu có bàn trà.'),
    -- Vietnam Comic & Cosplay Festival 2026
    (63, 'Vietnam Comic & Cosplay Festival 2026', 'Vé Thi Cosplay Contest', 80000.00, 120, 33, 'Đăng ký thi cosplay, kèm vé vào cổng ngày thi.'),
    (64, 'Vietnam Comic & Cosplay Festival 2026', 'Vé 1 Ngày', 120000.00, 4000, 2380, 'Vào cổng 1 ngày tùy chọn.'),
    (65, 'Vietnam Comic & Cosplay Festival 2026', 'Vé 2 Ngày + Túi Quà', 220000.00, 2000, 1010, 'Vào cổng cả 2 ngày, kèm túi quà và poster.'),
    (66, 'Vietnam Comic & Cosplay Festival 2026', 'Vé Fast Lane + Gặp Gỡ Khách Mời', 690000.00, 200, 0, 'Lối vào ưu tiên và buổi fan-meeting — đã bán hết.'),
    -- Kịch Nói: Tấm Cám – Phiên Bản Đương Đại
    (67, 'Kịch Nói: Tấm Cám – Phiên Bản Đương Đại', 'Vé Lầu 1', 180000.00, 150, 72, 'Ghế lầu 1 nhìn xuống sân khấu.'),
    (68, 'Kịch Nói: Tấm Cám – Phiên Bản Đương Đại', 'Vé Trệt Hạng B', 330000.00, 200, 95, 'Ghế tầng trệt từ hàng 6 trở về sau.'),
    (69, 'Kịch Nói: Tấm Cám – Phiên Bản Đương Đại', 'Vé Trệt Hạng A (Hàng 1 - 5)', 500000.00, 80, 11, 'Năm hàng ghế đầu tầng trệt.'),
    -- Stand-up Comedy Night: Chuyện Người Hà Nội
    (70, 'Stand-up Comedy Night: Chuyện Người Hà Nội', 'Vé Đứng', 150000.00, 80, 0, 'Khu vực đứng phía sau — đã bán hết.'),
    (71, 'Stand-up Comedy Night: Chuyện Người Hà Nội', 'Vé Ghế Ngồi (Kèm 1 Đồ Uống)', 300000.00, 60, 0, 'Ghế ngồi kèm 1 đồ uống — đã bán hết.'),
    -- Đêm Hội Trăng Rằm Phố Cổ Hà Nội 2026
    (72, 'Đêm Hội Trăng Rằm Phố Cổ Hà Nội 2026', 'Vé Tham Gia Rước Đèn (Kèm Đèn Ông Sao)', 60000.00, 2000, 340, 'Tham gia đoàn rước đèn, tặng 1 đèn ông sao.'),
    (73, 'Đêm Hội Trăng Rằm Phố Cổ Hà Nội 2026', 'Vé Workshop Làm Đèn Lồng + Bánh Trung Thu', 199000.00, 150, 8, 'Tự làm 1 đèn lồng và 2 bánh trung thu mang về.'),
    -- Saigon Craft Beer Festival 2026
    (74, 'Saigon Craft Beer Festival 2026', 'Vé Vào Cổng (Kèm Ly Nếm Thử)', 150000.00, 3000, 1760, 'Vào cổng 1 ngày, tặng ly nếm thử khắc logo.'),
    (75, 'Saigon Craft Beer Festival 2026', 'Vé Tasting 10 Loại Bia', 450000.00, 1500, 620, '10 phiếu nếm thử bia tại các quầy tùy chọn.'),
    (76, 'Saigon Craft Beer Festival 2026', 'Vé Unlimited VIP (2 Ngày)', 1100000.00, 200, 37, 'Nếm thử không giới hạn cả 2 ngày, khu lounge riêng.'),
    -- Lễ Hội Hải Sản & Du Thuyền Đêm Vịnh Hạ Long
    (77, 'Lễ Hội Hải Sản & Du Thuyền Đêm Vịnh Hạ Long', 'Vé Trẻ Em (Dưới 1m3)', 450000.00, 100, 61, 'Áp dụng cho trẻ em cao dưới 1m3.'),
    (78, 'Lễ Hội Hải Sản & Du Thuyền Đêm Vịnh Hạ Long', 'Vé Du Thuyền + Buffet Hải Sản', 890000.00, 250, 104, 'Buffet hải sản và 1 đồ uống chào mừng.'),
    (79, 'Lễ Hội Hải Sản & Du Thuyền Đêm Vịnh Hạ Long', 'Vé Phòng Riêng VIP (Set Tôm Hùm)', 2450000.00, 30, 9, 'Phòng ăn riêng trên tầng thượng, set tôm hùm cho 1 người.'),
    -- Lễ Hội Ẩm Thực Biển Sầm Sơn 2026
    (80, 'Lễ Hội Ẩm Thực Biển Sầm Sơn 2026', 'Vé Vào Cổng', 20000.00, 5000, 1290, 'Vé vào cổng khu lễ hội.'),
    (81, 'Lễ Hội Ẩm Thực Biển Sầm Sơn 2026', 'Vé Buffet Hải Sản Không Giới Hạn', 399000.00, 400, 57, 'Buffet hải sản 90 phút tại khu nhà hàng.'),
    -- Tour Bình Minh Chợ Nổi Cái Răng & Vườn Trái Cây
    (82, 'Tour Bình Minh Chợ Nổi Cái Răng & Vườn Trái Cây', 'Vé Trẻ Em', 150000.00, 60, 31, 'Áp dụng cho trẻ từ 5 đến 11 tuổi.'),
    (83, 'Tour Bình Minh Chợ Nổi Cái Răng & Vườn Trái Cây', 'Vé Tour Ghép (Thuyền Chung)', 290000.00, 120, 46, 'Thuyền 10-12 khách, kèm hướng dẫn viên.'),
    (84, 'Tour Bình Minh Chợ Nổi Cái Răng & Vườn Trái Cây', 'Vé Tour Riêng Gia Đình (Tối Đa 6 Người)', 1500000.00, 20, 7, 'Thuyền riêng cho nhóm tối đa 6 người.'),
    -- Đêm Tràng An – Du Thuyền Ngắm Hang Động Kỳ Ảo
    (85, 'Đêm Tràng An – Du Thuyền Ngắm Hang Động Kỳ Ảo', 'Vé Thuyền Trẻ Em', 180000.00, 400, 233, 'Trẻ em từ 1m đến 1m3.'),
    (86, 'Đêm Tràng An – Du Thuyền Ngắm Hang Động Kỳ Ảo', 'Vé Thuyền Người Lớn', 350000.00, 800, 421, 'Thuyền 4 khách, có áo phao và đèn pin.'),
    -- Phú Quốc Sunset Yoga & Wellness Retreat
    (87, 'Phú Quốc Sunset Yoga & Wellness Retreat', 'Vé Lớp Yoga Hoàng Hôn (1 Buổi)', 250000.00, 100, 58, 'Một buổi yoga hoàng hôn 90 phút trên bãi biển.'),
    (88, 'Phú Quốc Sunset Yoga & Wellness Retreat', 'Gói Retreat 3N2Đ (Phòng Đôi)', 8900000.00, 30, 12, 'Giá cho 1 khách ở phòng đôi, trọn gói ăn ở và lớp học.'),
    (89, 'Phú Quốc Sunset Yoga & Wellness Retreat', 'Gói Retreat 3N2Đ (Phòng Đơn)', 12000000.00, 10, 2, 'Phòng đơn hướng biển, trọn gói ăn ở, lớp học và 1 liệu trình spa.'),
    -- Khám Phá Rừng Tràm Trà Sư Mùa Nước Nổi
    (90, 'Khám Phá Rừng Tràm Trà Sư Mùa Nước Nổi', 'Vé Tham Quan + Xuồng Ba Lá', 150000.00, 300, 166, 'Vé vào rừng tràm và 1 lượt xuồng ba lá.'),
    (91, 'Khám Phá Rừng Tràm Trà Sư Mùa Nước Nổi', 'Combo Tour + Ăn Trưa Đặc Sản', 390000.00, 120, 47, 'Kèm xe đưa đón từ TP. Long Xuyên và bữa trưa đặc sản.'),
    -- Hành Trình Về Đất Mũi Cà Mau
    (92, 'Hành Trình Về Đất Mũi Cà Mau', 'Vé Tham Quan Trong Ngày', 450000.00, 150, 91, 'Ca nô khứ hồi từ TP. Cà Mau và vé tham quan.'),
    (93, 'Hành Trình Về Đất Mũi Cà Mau', 'Vé Tour 2N1Đ (Ghép Đoàn)', 1650000.00, 60, 28, 'Tour 2 ngày 1 đêm, homestay và các bữa ăn.'),
    -- Camping Music Night Đồng Nai
    (94, 'Camping Music Night Đồng Nai', 'Vé Mang Lều Tự Túc', 199000.00, 500, 260, 'Khu cắm lều tự túc, kèm vé nghe nhạc và xem phim.'),
    (95, 'Camping Music Night Đồng Nai', 'Vé Glamping (Lều + Nệm + BBQ)', 1390000.00, 80, 22, 'Lều glamping 2 người dựng sẵn, nệm, đèn và set BBQ.')
) AS v(sort_order, event_name, name, price, total_quantity, remaining_quantity, description)
JOIN events e ON e.name = v.event_name
WHERE NOT EXISTS (SELECT 1 FROM ticket_types t WHERE t.event_id = e.id AND t.name = v.name)
ORDER BY v.sort_order;


-- ============================================================
-- 6. MÃ GIẢM GIÁ
-- ============================================================

INSERT INTO vouchers (code, discount_type, amount, max_discount, min_order_amount, quantity, start_date, end_date, event_id, created_by, created_at, updated_at)
SELECT v.code, v.discount_type, v.amount, v.max_discount, v.min_order_amount, v.quantity,
       v.start_date::timestamp, v.end_date::timestamp,
       (SELECT MAX(e.id) FROM events e WHERE e.name = v.event_name),
       (SELECT u.id FROM users u WHERE u.username = v.created_by),
       NOW(), NOW()
FROM (VALUES
    ('ROCK2026', 'AMOUNT', 50000.00, 50000.00, 400000.00, 300, '2026-09-01 00:00:00', '2026-10-16 23:59:59', 'Hà Nội Rock Festival 2026', 'hanoi_arts'),
    ('DALAT15', 'PERCENTAGE', 15.00, 150000.00, 600000.00, 100, '2026-09-01 00:00:00', '2026-10-09 23:59:59', 'Đêm Nhạc Acoustic Mùa Sương Đà Lạt 2026', 'saigon_concerts'),
    ('TRAIL300K', 'AMOUNT', 300000.00, 300000.00, 3000000.00, 80, '2026-09-01 00:00:00', '2026-10-31 23:59:59', 'Sa Pa Mountain Trail 2026', 'vn_marathon'),
    ('KHAMPHA10', 'PERCENTAGE', 10.00, 200000.00, 300000.00, 500, '2026-09-15 00:00:00', '2026-12-31 23:59:59', NULL, 'admin')
) AS v(code, discount_type, amount, max_discount, min_order_amount, quantity, start_date, end_date, event_name, created_by)
ON CONFLICT (code) DO NOTHING;


-- ============================================================
-- NGUỒN ẢNH (Openverse — Flickr / Wikimedia Commons, giấy phép Creative Commons cho phép dùng thương mại)
-- ============================================================
-- dalat-acoustic-1: "Tallest Man on Earth 1" — linspiration01, CC BY 2.0, https://www.flickr.com/photos/91429305@N07/9465789061
-- dalat-acoustic-2: "Twin Atlantic" — baileykd, CC BY 2.0, https://www.flickr.com/photos/64422319@N07/6890107739
-- hanoi-rock-1: "Tori was there too" — kevin dooley, CC BY 2.0, https://www.flickr.com/photos/12836528@N00/2108795296
-- hanoi-rock-2: "Green Day Concert Crowd - Put Your Hands Up For Green Day" — Anirudh Koul, CC BY 2.0, https://www.flickr.com/photos/84856173@N00/3734360895
-- hanoi-symphony-1: "Film Score Philharmonic Orchestra 'THE MAGIC' in Muza Kawasaki Symphony Hall" — Dick Thomas Johnson, CC BY 2.0, https://www.flickr.com/photos/31029865@N06/51878918226
-- hanoi-symphony-2: "Conductor - Frederik Magle conducting a symphony orchestra 10" — Frederik Magle Music, CC BY 2.0, https://www.flickr.com/photos/78550404@N08/7026769687
-- saigon-jazz-1: "Page 4 out of 365. Simi & Friends" — Cristian Ştefănescu, CC BY 2.0, https://www.flickr.com/photos/46145615@N02/16199696935
-- saigon-jazz-2: "Anthony Jackson" — ArtBrom, CC BY-SA 2.0, https://www.flickr.com/photos/17277074@N00/2096626775
-- danang-kpop-1: "Dance" — Lisa Padilla, CC BY 2.0, https://www.flickr.com/photos/43885961@N00/605984800
-- danang-kpop-2: "Party" — abulhussain, CC BY 2.0, https://www.flickr.com/photos/7678586@N06/5594387099
-- vungtau-beach-1: "'Suicide Silence' 'Mitch Lucker' Mitch Lucker is dead at 28" — Ted Van Pelt, CC BY 2.0, https://www.flickr.com/photos/28914673@N02/6841141731
-- vungtau-beach-2: "4th of July Fireworks at Miller Outdoor Theatre" — AlphaTangoBravo / Adam Baker, CC BY 2.0, https://www.flickr.com/photos/44124479650@N01/3690987934
-- hanoi-countdown-1: "Fireworks" — nigelhowe, CC BY 2.0, https://www.flickr.com/photos/40939157@N03/6700336625
-- hanoi-countdown-2: "Happy 2012!" — laszlo-photo, CC BY 2.0, https://www.flickr.com/photos/40467171@N00/6612450499
-- hanoi-devday-1: "Monday Sessions, David Cage of Quantic Dream" — Official GDC, CC BY 2.0, https://www.flickr.com/photos/46982319@N06/4897801184
-- hanoi-devday-2: "GDC Europe 2010 Talks, Conversations, Presentations" — Official GDC, CC BY 2.0, https://www.flickr.com/photos/46982319@N06/4894730690
-- danang-startup-1: "ESB - Spark of Genius 2015 17/09/15" — Web Summit, CC BY 2.0, https://www.flickr.com/photos/74711243@N06/15272932221
-- danang-startup-2: "Stephen Fleming Paul Freet introduces Startup Gauntlet" — MikeSchinkel, CC BY 2.0, https://www.flickr.com/photos/13838874@N00/2887223014
-- hcm-fintech-1: "DONGBAI GUO CTO AT ALIEXPRESS [DUBLIN TECH SUMMIT 2017]-125090" — infomatique, CC BY-SA 2.0, https://www.flickr.com/photos/80824546@N00/32877530466
-- hcm-fintech-2: "DONGBAI GUO CTO AT ALIEXPRESS [DUBLIN TECH SUMMIT 2017]-125089" — infomatique, CC BY-SA 2.0, https://www.flickr.com/photos/80824546@N00/32537862860
-- hanoi-cloud-summer-1: "Monday Sessions, Gunnar Lott of IDG" — Official GDC, CC BY 2.0, https://www.flickr.com/photos/46982319@N06/4897517864
-- cantho-uiux-1: "VFS Summer Intensives 2014" — vancouverfilmschool, CC BY 2.0, https://www.flickr.com/photos/38174668@N05/14651060440
-- cantho-uiux-2: "Flipped Learning Design in VET Workshop - Gold Coast Institute of TAFE (GCIT)" — Vanguard Visions, CC BY 2.0, https://www.flickr.com/photos/77018488@N03/9497316159
-- battrang-pottery-1: "Potter's Wheel, Avanos" — twiga_swala, CC BY-SA 2.0, https://www.flickr.com/photos/21013862@N08/2285973556
-- battrang-pottery-2: "homemade tools and curly wire from whisk handle" — bptakoma, CC BY 2.0, https://www.flickr.com/photos/8010145@N08/4413121131
-- sapa-trail-1: "Sapa Rice Terrace" — My Aching Head, CC BY-SA 2.0, https://www.flickr.com/photos/29981276@N05/7394593418
-- sapa-trail-2: "Running" — Lake Mead National Recreation Area, CC BY-SA 2.0, https://www.flickr.com/photos/30839029@N05/8982516896
-- binhduong-pickleball-1: "Pickleball Players" — TheVillagesFL, CC BY-SA 4.0, https://commons.wikimedia.org/w/index.php?curid=65975778
-- binhduong-pickleball-2: "Pickleball Pros" — Picklerpeej, CC BY-SA 4.0, https://commons.wikimedia.org/w/index.php?curid=107275576
-- nhatrang-triathlon-1: "White Lake Half Ironman Triathlon Swim Start 054" — cygnus921, CC BY 2.0, https://www.flickr.com/photos/11726702@N07/2465623669
-- nhatrang-triathlon-2: "White Lake Half Ironman Triathlon Swim Start 055" — cygnus921, CC BY 2.0, https://www.flickr.com/photos/11726702@N07/2465624525
-- hue-dragonboat-1: "Penang International Dragon Boat Race" — Ben's Optic, CC BY-SA 2.0, https://www.flickr.com/photos/16932563@N06/2901614981
-- hue-dragonboat-2: "Dragon boat racing in Hong Kong" — Atmhk, CC BY-SA 3.0, https://commons.wikimedia.org/w/index.php?curid=18534263
-- haiphong-football-1: "Amateur football match" — 遠望jupiter, CC BY-SA 2.0, https://www.flickr.com/photos/94015160@N03/8878950165
-- thanglong-waterpuppet-1: "Water puppets Hanoi" — sixty4coupe, CC BY-SA 2.0, https://www.flickr.com/photos/79893993@N00/2097670824
-- thanglong-waterpuppet-2: "Vietnamese water puppets, Hanoi, Vietnam" — Paul Mannix, CC BY 2.0, https://www.flickr.com/photos/19511776@N00/33700298506
-- hoian-lantern-1: "Hoi An." — Nguyen Duc Loi, CC BY 2.0, https://www.flickr.com/photos/85675121@N04/12810464585
-- hoian-lantern-2: "Colorful Lanterns in Hoi An, #MidAutumn2015" — Khánh Hmoong, CC BY 2.0, https://www.flickr.com/photos/7997148@N05/21493179628
-- hue-nhanhac-1: "Hue Imperial Citadel" — xiquinhosilva, CC BY 2.0, https://www.flickr.com/photos/7138083@N04/54086493277
-- hue-nhanhac-2: "Hue Imperial Citadel" — xiquinhosilva, CC BY 2.0, https://www.flickr.com/photos/7138083@N04/54087827360
-- nghean-vigiam-1: "DSC_1886" — Nguyen Vu Hung (vuhung), CC BY 2.0, https://www.flickr.com/photos/77987497@N00/2224891606
-- nghean-vigiam-2: "Local lasses play traditional Vietnamese music" — shankar s., CC BY 2.0, https://www.flickr.com/photos/77742560@N06/30646043573
-- hcm-cosplay-1: "Nekocon cosplay convention" — watts_photos, CC BY 2.0, https://www.flickr.com/photos/126288307@N05/40671546781
-- hcm-cosplay-2: "Nekocon cosplay convention" — watts_photos, CC BY 2.0, https://www.flickr.com/photos/126288307@N05/39776602895
-- hcm-tamcam-theatre-1: "Almaty" — torekhan, CC BY 2.0, https://www.flickr.com/photos/44148409@N04/15120492715
-- hanoi-standup-1: "Beth Stelling" — TheeErin, CC BY-SA 2.0, https://www.flickr.com/photos/27073477@N00/5044031622
-- hanoi-standup-2: "Marty DeRosa" — TheeErin, CC BY-SA 2.0, https://www.flickr.com/photos/27073477@N00/5043401471
-- hanoi-midautumn-1: "Lanterns at Mid-Autumn Festival, Hong Kong; September 2013 (03)" — doctorho, CC BY-SA 2.0, https://commons.wikimedia.org/w/index.php?curid=110540200
-- hanoi-midautumn-2: "Lanterns at Mid-Autumn Festival, Hong Kong; September 2013 (01)" — doctorho, CC BY-SA 2.0, https://commons.wikimedia.org/w/index.php?curid=110540202
-- saigon-craftbeer-1: "WP_20150105_19_19_40_Pro" — Nicola since 1972, CC BY 2.0, https://www.flickr.com/photos/15216811@N06/16026889030
-- saigon-craftbeer-2: "Shenzhen Craft Beer Festival" — rose_symotiuk, CC BY 2.0, https://www.flickr.com/photos/28469445@N02/16950582247
-- halong-seafood-1: "Night in Ha Long Bay" — radkuch.13, CC BY 2.0, https://www.flickr.com/photos/137294100@N08/42038000855
-- halong-seafood-2: "Ha Long Bay" — Ondřej Žváček, CC BY 2.5, https://commons.wikimedia.org/w/index.php?curid=2520505
-- samson-food-1: "Sam Son beach" — Kevin Walsh, CC BY 2.0, https://commons.wikimedia.org/w/index.php?curid=127961415
-- samson-food-2: "Sam Son beach 2" — Hungda, CC BY-SA 3.0, https://commons.wikimedia.org/w/index.php?curid=21006594
-- cantho-floating-market-1: "Cai Rang Floating Market 8" — Christophe95, CC BY-SA 4.0, https://commons.wikimedia.org/w/index.php?curid=86772814
-- cantho-floating-market-2: "Cai Rang Floating Market 19" — michael clarke stuff, CC BY-SA 2.0, https://www.flickr.com/photos/19646736@N00/8547098629
-- ninhbinh-trangan-1: "Trang An - 05" — Benjamin Smith, CC BY-SA 4.0, https://commons.wikimedia.org/w/index.php?curid=133533384
-- ninhbinh-trangan-2: "Ninh Binh, Vietnam" — Nguyen Duc Loi, CC BY 2.0, https://www.flickr.com/photos/85675121@N04/12811029564
-- phuquoc-yoga-1: "Sunset at Intercontinental Phu Quoc" — ToGa Wanderings, CC BY 2.0, https://www.flickr.com/photos/69031678@N00/52434049200
-- phuquoc-yoga-2: "Sunset Phu Quoc" — noramorgan, CC BY 2.0, https://www.flickr.com/photos/13963650@N00/376737319
-- angiang-trasu-1: "Go inside Tra Su Forest" — ePi.Longo, CC BY-SA 2.0, https://www.flickr.com/photos/56278354@N00/8153273333
-- angiang-trasu-2: "Tra Su Forest" — ePi.Longo, CC BY-SA 2.0, https://www.flickr.com/photos/56278354@N00/8153296328
-- camau-datmui-1: "Dat Mui National Park, Ca Mau" — The U.S. Consulate General Ho Chi Minh City, CC BY 2.0, https://www.flickr.com/photos/79629660@N03/10942080803
-- camau-datmui-2: "IMG_6228" — beggs, CC BY 2.0, https://www.flickr.com/photos/94509941@N00/1421929094
-- dongnai-camping-1: "Day I - III" — Robert Anders, CC BY 2.0, https://www.flickr.com/photos/50312443@N04/20577392712
-- dongnai-camping-2: "Drunken Steals" — tedge__, CC BY 2.0, https://www.flickr.com/photos/12505025@N08/4624859639

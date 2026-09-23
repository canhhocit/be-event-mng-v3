-- Flyway Migration V4__fix_user_passwords.sql
-- Sửa lại toàn bộ password mã hóa BCrypt chính xác cho 123456 và kích hoạt tất cả tài khoản user mẫu

UPDATE users SET 
    password = '$2a$10$Yh/F0RRuLqSg6HnsmSAPB.OI/Y5fHzpiQt1nCEpjQqCkBQr3TuhIu',
    enabled = true
WHERE username IN ('admin', 'saigon_concerts', 'techfest_vn', 'vn_marathon', 'hoanganh', 'thuylinh', 'quangminh');

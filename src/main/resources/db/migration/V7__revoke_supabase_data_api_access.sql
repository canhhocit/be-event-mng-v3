-- Flyway Migration V7__revoke_supabase_data_api_access.sql
-- Trên Supabase, bảng trong schema public mặc định được cấp quyền cho role anon/authenticated,
-- tức là đọc/sửa được qua Data API (REST tự sinh) chỉ cần anon key (vd. đọc users, sửa orders).
-- Backend truy cập DB trực tiếp bằng user postgres nên thu hồi các quyền đó, kể cả cho bảng tạo sau này.
-- Postgres thường (local, Neon) không có các role này -> bỏ qua.

DO $$
BEGIN
    IF EXISTS (SELECT 1 FROM pg_roles WHERE rolname = 'anon')
       AND EXISTS (SELECT 1 FROM pg_roles WHERE rolname = 'authenticated') THEN
        REVOKE ALL ON ALL TABLES IN SCHEMA public FROM anon, authenticated;
        REVOKE ALL ON ALL SEQUENCES IN SCHEMA public FROM anon, authenticated;
        ALTER DEFAULT PRIVILEGES IN SCHEMA public REVOKE ALL ON TABLES FROM anon, authenticated;
        ALTER DEFAULT PRIVILEGES IN SCHEMA public REVOKE ALL ON SEQUENCES FROM anon, authenticated;
    END IF;
END $$;

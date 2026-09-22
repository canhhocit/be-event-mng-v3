-- Flyway Database Migration Script V1__init_schema.sql
-- Target Database Engine: PostgreSQL / Neon Database

-- 1. Roles table
CREATE TABLE IF NOT EXISTS roles (
    name VARCHAR(255) PRIMARY KEY,
    description VARCHAR(255)
);

-- 2. Users table
CREATE TABLE IF NOT EXISTS users (
    id BIGSERIAL PRIMARY KEY,
    username VARCHAR(255) NOT NULL UNIQUE,
    email VARCHAR(255),
    password VARCHAR(255) NOT NULL,
    full_name VARCHAR(255),
    phone VARCHAR(255),
    address VARCHAR(255),
    enabled BOOLEAN NOT NULL DEFAULT TRUE,
    verification_token VARCHAR(255),
    otp VARCHAR(255),
    otp_expiry TIMESTAMP,
    organizer_id BIGINT REFERENCES users(id) ON DELETE SET NULL,
    created_at TIMESTAMP,
    updated_at TIMESTAMP
);

-- 3. User Roles join table
CREATE TABLE IF NOT EXISTS user_roles (
    user_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    role_id VARCHAR(255) NOT NULL REFERENCES roles(name) ON DELETE CASCADE,
    PRIMARY KEY (user_id, role_id)
);

-- 4. Invalidated Tokens table
CREATE TABLE IF NOT EXISTS invalidated_tokens (
    id VARCHAR(255) PRIMARY KEY,
    expiry_time TIMESTAMP
);

-- 5. Categories table
CREATE TABLE IF NOT EXISTS categories (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    description TEXT,
    created_at TIMESTAMP,
    updated_at TIMESTAMP
);

-- 6. Events table
CREATE TABLE IF NOT EXISTS events (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    category_id BIGINT REFERENCES categories(id) ON DELETE SET NULL,
    organizer_id BIGINT REFERENCES users(id) ON DELETE SET NULL,
    location VARCHAR(255),
    province VARCHAR(255),
    start_time TIMESTAMP,
    end_time TIMESTAMP,
    sale_start_date TIMESTAMP,
    sale_end_date TIMESTAMP,
    description TEXT,
    status VARCHAR(50),
    created_at TIMESTAMP,
    updated_at TIMESTAMP
);

-- 7. Event Images table
CREATE TABLE IF NOT EXISTS event_images (
    id BIGSERIAL PRIMARY KEY,
    event_id BIGINT REFERENCES events(id) ON DELETE CASCADE,
    image_url VARCHAR(500) NOT NULL,
    created_at TIMESTAMP,
    updated_at TIMESTAMP
);

-- 8. Ticket Types table
CREATE TABLE IF NOT EXISTS ticket_types (
    id BIGSERIAL PRIMARY KEY,
    event_id BIGINT REFERENCES events(id) ON DELETE CASCADE,
    name VARCHAR(100) NOT NULL,
    price NUMERIC(12, 2) NOT NULL,
    total_quantity INT NOT NULL,
    remaining_quantity INT NOT NULL,
    description TEXT,
    created_at TIMESTAMP,
    updated_at TIMESTAMP
);

-- 9. Vouchers table
CREATE TABLE IF NOT EXISTS vouchers (
    id BIGSERIAL PRIMARY KEY,
    code VARCHAR(100) NOT NULL UNIQUE,
    discount_type VARCHAR(50) NOT NULL,
    amount NUMERIC(12, 2) NOT NULL,
    max_discount NUMERIC(12, 2),
    min_order_amount NUMERIC(12, 2),
    quantity INT,
    start_date TIMESTAMP,
    end_date TIMESTAMP,
    event_id BIGINT REFERENCES events(id) ON DELETE SET NULL,
    created_by BIGINT REFERENCES users(id) ON DELETE SET NULL,
    created_at TIMESTAMP,
    updated_at TIMESTAMP
);

-- 10. Carts table
CREATE TABLE IF NOT EXISTS carts (
    id BIGSERIAL PRIMARY KEY,
    customer_id BIGINT REFERENCES users(id) ON DELETE CASCADE,
    status VARCHAR(50),
    created_at TIMESTAMP,
    updated_at TIMESTAMP
);

-- 11. Cart Items table
CREATE TABLE IF NOT EXISTS cart_items (
    id BIGSERIAL PRIMARY KEY,
    cart_id BIGINT REFERENCES carts(id) ON DELETE CASCADE,
    ticket_type_id BIGINT REFERENCES ticket_types(id) ON DELETE CASCADE,
    quantity INT NOT NULL,
    unit_price NUMERIC(12, 2) NOT NULL,
    subtotal NUMERIC(12, 2) NOT NULL,
    created_at TIMESTAMP,
    updated_at TIMESTAMP
);

-- 12. Orders table
CREATE TABLE IF NOT EXISTS orders (
    id VARCHAR(255) PRIMARY KEY,
    order_code BIGINT UNIQUE,
    customer_id BIGINT REFERENCES users(id) ON DELETE SET NULL,
    organizer_amount NUMERIC(12, 2) NOT NULL,
    platform_fee_rate REAL NOT NULL,
    service_fee NUMERIC(12, 2) NOT NULL,
    total_amount NUMERIC(12, 2) NOT NULL,
    discount_amount NUMERIC(12, 2),
    voucher_code VARCHAR(255),
    payment_method VARCHAR(50),
    payment_status VARCHAR(50),
    order_status VARCHAR(50),
    order_date TIMESTAMP,
    paid_at TIMESTAMP,
    created_at TIMESTAMP,
    updated_at TIMESTAMP
);

-- 13. Order Items table
CREATE TABLE IF NOT EXISTS order_items (
    id BIGSERIAL PRIMARY KEY,
    order_id VARCHAR(255) REFERENCES orders(id) ON DELETE CASCADE,
    ticket_type_id BIGINT REFERENCES ticket_types(id) ON DELETE SET NULL,
    quantity INT NOT NULL,
    unit_price NUMERIC(12, 2) NOT NULL,
    subtotal NUMERIC(12, 2) NOT NULL,
    created_at TIMESTAMP,
    updated_at TIMESTAMP
);

-- 14. Tickets table
CREATE TABLE IF NOT EXISTS tickets (
    id BIGSERIAL PRIMARY KEY,
    order_id VARCHAR(255) REFERENCES orders(id) ON DELETE CASCADE,
    ticket_type_id BIGINT REFERENCES ticket_types(id) ON DELETE SET NULL,
    ticket_code VARCHAR(255) NOT NULL UNIQUE,
    qr_code TEXT,
    status VARCHAR(50),
    used_at TIMESTAMP,
    created_at TIMESTAMP,
    updated_at TIMESTAMP
);

-- 15. Blog Tags table
CREATE TABLE IF NOT EXISTS blog_tags (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(255) NOT NULL UNIQUE,
    slug VARCHAR(255) NOT NULL UNIQUE,
    created_at TIMESTAMP,
    updated_at TIMESTAMP
);

-- 16. Blog Posts table
CREATE TABLE IF NOT EXISTS blog_posts (
    id BIGSERIAL PRIMARY KEY,
    title VARCHAR(255) NOT NULL,
    slug VARCHAR(255) NOT NULL UNIQUE,
    summary TEXT,
    content TEXT,
    thumbnail VARCHAR(500),
    meta_title VARCHAR(255),
    meta_description TEXT,
    author_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    status VARCHAR(50),
    published_at TIMESTAMP,
    created_at TIMESTAMP,
    updated_at TIMESTAMP
);

-- 17. Blog Post Tags join table
CREATE TABLE IF NOT EXISTS blog_post_tags (
    post_id BIGINT NOT NULL REFERENCES blog_posts(id) ON DELETE CASCADE,
    tag_id BIGINT NOT NULL REFERENCES blog_tags(id) ON DELETE CASCADE,
    PRIMARY KEY (post_id, tag_id)
);

-- 18. Blog Post Event IDs element collection table
CREATE TABLE IF NOT EXISTS blog_post_event_ids (
    post_id BIGINT NOT NULL REFERENCES blog_posts(id) ON DELETE CASCADE,
    event_id BIGINT NOT NULL
);

-- Initial Roles Seeding
INSERT INTO roles (name, description) VALUES
('ADMIN', 'Administrator role'),
('CUSTOMER', 'Customer user role'),
('ORGANIZER', 'Event Organizer role'),
('STAFF', 'Organizer staff role')
ON CONFLICT (name) DO NOTHING;

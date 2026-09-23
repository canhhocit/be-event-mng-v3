-- Flyway Migration V5__add_performance_indexes.sql
-- Thêm các Index tối ưu hóa tốc độ truy vấn trên PostgreSQL Cloud

CREATE INDEX IF NOT EXISTS idx_events_status_category ON events(status, category_id);
CREATE INDEX IF NOT EXISTS idx_events_organizer_status ON events(organizer_id, status);
CREATE INDEX IF NOT EXISTS idx_events_start_time ON events(start_time);

CREATE INDEX IF NOT EXISTS idx_ticket_types_event ON ticket_types(event_id);
CREATE INDEX IF NOT EXISTS idx_order_items_ticket_type ON order_items(ticket_type_id);
CREATE INDEX IF NOT EXISTS idx_orders_customer_status ON orders(customer_id, payment_status);
CREATE INDEX IF NOT EXISTS idx_orders_payment_status_paid ON orders(payment_status, paid_at);
CREATE INDEX IF NOT EXISTS idx_tickets_order_status ON tickets(order_id, status);
CREATE INDEX IF NOT EXISTS idx_tickets_code ON tickets(ticket_code);

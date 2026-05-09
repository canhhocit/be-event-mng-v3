# TÀI LIỆU CHI TIẾT LUỒNG NGHIỆP VỤ ORDERING & TICKETING

Tài liệu này mô tả chi tiết cách hệ thống vận hành từ lúc người dùng thêm vé vào giỏ hàng cho đến khi vé được quét tại cổng sự kiện.

---

## 1. Luồng Giỏ hàng (Cart Flow)

### Bước 1: Thêm vé vào giỏ hàng
*   **API**: `POST /cart/add`
*   **Tham số**: `ticketTypeId`, `quantity`
*   **Hoạt động**: Kiểm tra User hiện tại đã có giỏ hàng chưa. Nếu chưa thì tạo mới. Kiểm tra xem loại vé có tồn tại không. Thêm `CartItem` vào giỏ.
*   **SQL Minh họa**:
    ```sql
    -- Tìm giỏ hàng của user
    SELECT * FROM carts WHERE customer_id = ?;
    
    -- Thêm món vào giỏ
    INSERT INTO cart_items (cart_id, ticket_type_id, quantity, unit_price, subtotal) 
    VALUES (?, ?, ?, ?, ?);
    ```

---

## 2. Luồng Đặt hàng & Thanh toán (Ordering & Payment Flow)

### Bước 2: Checkout (Thanh toán toàn bộ hoặc một phần)
*   **API**: 
    *   Thanh toán hết: `POST /bookings/checkout`
    *   Thanh toán món chọn lọc: `POST /bookings/checkout-selected`
*   **Xử lý tại OrderService**:
    1.  **Validate**: Kiểm tra số lượng vé trong kho (`TicketType.remaining_quantity`).
    2.  **Tính toán**: Tính tổng tiền, phí sàn (25%), tiền cho BTC (75%). Áp dụng Voucher (nếu có).
    3.  **Lưu Order**: Tạo bản ghi `Order` (trạng thái `PENDING`) và các `OrderItem`.
    4.  **Gọi PayOS**: Gọi API `createPayOSPaymentLink` để lấy mã QR.
*   **SQL Minh họa**:
    ```sql
    -- Tạo đơn hàng mới
    INSERT INTO orders (order_code, total_amount, service_fee, organizer_amount, payment_status, order_status, ...) 
    VALUES (?, ?, ?, ?, 'PENDING', 'PENDING', ...);
    
    -- Trừ món khỏi giỏ hàng
    DELETE FROM cart_items WHERE id IN (...);
    ```

### Bước 3: Xử lý Webhook (Khi tiền đã về)
*   **API**: `POST /api/v1/payments/payos-webhook` (Gọi từ PayOS Server)
*   **Xử lý tại PaymentController & OrderService**:
    1.  **Verify**: Kiểm tra dữ liệu từ PayOS gửi về.
    2.  **Cập nhật Đơn hàng**: Đổi trạng thái `payment_status` sang `PAID`, `order_status` sang `CONFIRMED`.
    3.  **Trừ kho (Inventory)**: Giảm `remaining_quantity` của `TicketType`.
    4.  **Sinh Vé (Ticketing)**: Với mỗi số lượng mua, tạo 1 bản ghi trong bảng `tickets`.
    5.  **Gửi Mail**: Gọi `PdfService` tạo hóa đơn và `EmailService` gửi mail.
*   **SQL Minh họa**:
    ```sql
    -- Cập nhật đơn hàng
    UPDATE orders SET payment_status = 'PAID', order_status = 'CONFIRMED' WHERE order_code = ?;
    
    -- Trừ kho thực tế
    UPDATE ticket_types SET remaining_quantity = remaining_quantity - ? WHERE id = ?;
    
    -- Tạo bản ghi vé cho khách
    INSERT INTO tickets (order_id, ticket_type_id, ticket_code, qr_code, status) 
    VALUES (?, ?, ?, ?, 'VALID');
    ```

---

## 3. Luồng Check-in Vé (Check-in Flow)

### Bước 4: Nhân viên quét mã QR
*   **API**: `POST /tickets/check-in`
*   **Tham số**: `ticketCode` (Lấy từ mã QR)
*   **Phân quyền (@PreAuthorize)**: Chỉ có `ADMIN`, `ORGANIZER`, hoặc `STAFF` của đúng sự kiện đó mới được gọi.
*   **Logic tại TicketService**:
    1.  **Tìm vé**: Kiểm tra mã vé có tồn tại không.
    2.  **Verify Quyền**: 
        *   Nếu là STAFF: Chỉ được quét vé của sự kiện do BAN TỔ CHỨC mình quản lý.
        *   Nếu là ORGANIZER: Chỉ quét được vé của sự kiện mình tạo ra.
    3.  **Validate trạng thái**: Vé phải đang ở trạng thái `VALID`. Nếu là `USED` thì báo lỗi (vé đã dùng).
    4.  **Cập nhật**: Đổi trạng thái vé sang `USED`, ghi nhận thời gian `used_at`.
*   **SQL Minh họa**:
    ```sql
    -- Tìm vé theo mã quét
    SELECT * FROM tickets WHERE ticket_code = ?;
    
    -- Cập nhật trạng thái check-in thành công
    UPDATE tickets SET status = 'USED', used_at = NOW() WHERE id = ?;
    ```

---

## TỔNG KẾT CÁC CÂU HỎI THƯỜNG GẶP (FAQ)

1.  **Tại sao không sinh vé ngay lúc đặt hàng?**
    *   *Trả lời*: Để tránh việc "giữ chỗ ảo". Vé chỉ chính thức được tạo ra và trừ kho thực tế khi tiền đã về hệ thống (Trạng thái PAID).
2.  **Tại sao dùng Webhook thay vì đợi Client báo thành công?**
    *   *Trả lời*: Vì Client (Trình duyệt/Mobile) có thể bị tắt, mất mạng ngay sau khi thanh toán. Webhook là giao tiếp Server-to-Server nên độ tin cậy là 100%.
3.  **Làm sao để đảm bảo an toàn khi trừ kho?**
    *   *Trả lời*: Dùng `@Transactional`. Nếu trong quá trình sinh 10 cái vé mà cái thứ 11 bị lỗi, hệ thống sẽ hủy bỏ (rollback) toàn bộ các vé trước đó và trả lại số lượng kho, đảm bảo dữ liệu luôn khớp.

# EventHub RESTFUL API


## 🛠️ Tech Stack

### Công nghệ
- **Java 17 + Spring Boot 3.4.5**
- **Spring Security + JWT**: Bảo mật và phân quyền đa lớp (RBAC).
- **Spring Data JPA + MySQL**: Quản trị dữ liệu quan hệ và hiệu năng truy vấn.
- **MapStruct & Lombok**: Tối ưu hóa code và mapping dữ liệu DTO.
- **Swagger (OpenAPI)**: Tài liệu hóa API chuyên nghiệp.
- **Integrations**: PayOS (VietQR), Brevo (Email API), iText (PDF Generation).

---

## ✨ Tính năng chính

### 🔐 Authentication & Identity
- **Đăng ký/Đăng nhập**: Nhận JWT token, xác thực tài khoản qua Email & OTP.
- **Phân quyền**: Quản lý 4 vai trò chính: `ADMIN`, `ORGANIZER`, `STAFF`, `CUSTOMER`.

### 📅 Event Management
- **Quản lý sự kiện**: Thêm/Sửa/Xóa và duyệt sự kiện công khai.
- **Ticket Inventory**: Quản lý đa dạng hạng vé (VIP/Thường) và tồn kho thời gian thực.
- **Tìm kiếm**: Công cụ tìm kiếm và lọc sự kiện theo địa điểm, tên và khoảng giá.

### 🛒 Cart & Ordering
- **Giỏ hàng**: Quản lý giỏ hàng riêng biệt cho từng khách hàng.
- **Checkout**: Xử lý logic đặt hàng, kiểm tra tính hợp lệ và trừ tồn kho vé.

### 💳 Payment & Fulfillment
- **Thanh toán trực tuyến**: Tích hợp cổng thanh toán **PayOS (VietQR)** và **MoMo**.
- **Tự động hóa**: Xử lý Webhook ngân hàng, tự động sinh vé QR Code, xuất hóa đơn PDF và gửi Email xác nhận ngay lập tức.

### 📊 Statistics & Marketing
- **Voucher**: Hệ thống mã giảm giá theo số tiền hoặc phần trăm.
- **Dashboard**: Thống kê doanh thu và báo cáo hiệu quả bán vé cho Admin/Organizer.

---

## 🚀 Hướng dẫn chạy project

### Yêu cầu
- Java 17+
- MySQL 8.0
- Maven

### Cấu hình `application.yml`
```properties
spring.datasource.url=jdbc:mysql://localhost:3306/event_mng
spring.datasource.username=root
spring.datasource.password=your_password
jwt.signerKey=your_secret_key
payos.clientId=your_payos_id
```

### Chạy ứng dụng
```bash
mvn spring-boot:run
```

### Xem API docs (Swagger UI)
```
http://localhost:8080/swagger-ui/index.html
```

---

## 📁 Cấu trúc thư mục (Modular Monolith)

```text
src/main/java/com/sa/event_mng/
├── modules/
│   ├── identity/     # Xác thực & Quản lý người dùng
│   ├── event/        # Quản lý sự kiện & Hạng vé
│   ├── ordering/     # Giỏ hàng & Thanh toán
│   ├── ticketing/    # Vé điện tử & QR Code
│   ├── marketing/    # Voucher & Khuyến mãi
│   └── blog/         # Tin tức & Hướng dẫn
└── shared/           # Cấu hình dùng chung (Exception, Email, PDF...)
```

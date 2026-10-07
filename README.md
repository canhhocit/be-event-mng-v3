# 🎪 EventHub Backend RESTful API

[![Java](https://img.shields.io/badge/Java-17-orange.svg)](https://www.oracle.com/java/)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.4.5-brightgreen.svg)](https://spring.io/projects/spring-boot)
[![PostgreSQL](https://img.shields.io/badge/PostgreSQL-16-blue.svg)](https://www.postgresql.org/)
[![Flyway](https://img.shields.io/badge/Flyway-Migration-red.svg)](https://flywaydb.org/)
[![License](https://img.shields.io/badge/License-MIT-green.svg)](LICENSE)

**EventHub Backend** là hệ thống máy chủ xử lý toàn bộ logic nghiệp vụ, quản lý bán vé sự kiện trực tuyến, thanh toán VietQR tự động, quản lý giỏ hàng, xuất vé điện tử tích hợp QR Code và phân quyền đa lớp cho nền tảng **EventHub**.

Hệ thống được thiết kế theo kiến trúc **Domain-Driven Layered Modular Monolith**, giúp đảm bảo tính cô lập nghiệp vụ, dễ bảo trì, mở rộng và kiểm thử.

---

## 📑 Mục lục
- [1. Kiến trúc hệ thống (System Architecture)](#1-kiến-trúc-hệ-thống-system-architecture)
- [2. Các luồng nghiệp vụ cốt lõi (Core Sequence Workflows)](#2-các-luồng-nghiệp-vụ-cốt-lõi-core-sequence-workflows)
  - [2.1. Luồng Thanh toán VietQR (PayOS) & Tự động phát hành vé QR](#21-luồng-thanh-toán-vietqr-payos--tự-động-phát-hành-vé-qr)
  - [2.2. Luồng Kiểm tra & Soát vé (Event Check-in)](#22-luồng-kiểm-tra--soát-vé-event-check-in)
  - [2.3. Luồng Xác thực & Bảo mật JWT (Authentication & Security)](#23-luồng-xác-thực--bảo-mật-jwt-authentication--security)
- [3. Cơ sở dữ liệu & Database Migration (Flyway)](#3-cơ-sở-dữ-liệu--database-migration-flyway)
- [4. Danh sách các Module nghiệp vụ](#4-danh-sách-các-module-nghiệp-vụ)
- [5. Công nghệ & Thư viện sử dụng (Tech Stack)](#5-công-nghệ--thư-viện-sử-dụng-tech-stack)
- [6. Cấu trúc thư mục mã nguồn](#6-cấu-trúc-thư-mục-mã-nguồn)
- [7. Hướng dẫn cài đặt & Chạy dự án](#7-hướng-dẫn-cài-đặt--chạy-dự-án)

---

## 1. Kiến trúc hệ thống (System Architecture)

Hệ thống áp dụng kiến trúc **Modular Monolith** kết hợp với **Layered Domain-Driven Design (DDD)**. Mỗi miền nghiệp vụ (Module) được đóng gói độc lập với 4 lớp kiến trúc chuẩn hóa:

```mermaid
flowchart TD
    subgraph ClientLayer ["Client Layer (Giao diện người dùng)"]
        FE_Customer["FE Customer (ReactJS + Vite)"]
        FE_Admin["FE Admin & Organizer (ReactJS)"]
        FE_Blog["FE Blog & Guide (ReactJS)"]
        Mobile_Staff["Mobile App Staff (Android Kotlin)"]
    end

    subgraph SecurityLayer ["Security & Routing Layer"]
        Gateway["Spring Security + OAuth2 Resource Server"]
        JWTDecoder["Custom JWT Decoder & Token Invalidator"]
    end

    subgraph BusinessModules ["Business Modules (Modular Monolith)"]
        subgraph ModIdentity ["Identity Module"]
            AuthService["Auth & User Service"]
            TokenCleanup["Token Cleanup Task (@Scheduled)"]
        end

        subgraph ModEvent ["Event Module"]
            EventService["Event & Category Service"]
            EventTask["Event Status Task (@Scheduled)"]
        end

        subgraph ModOrdering ["Ordering Module"]
            CartService["Cart Service"]
            OrderService["Order & Checkout Service"]
            PaymentService["Payment & PayOS Webhook"]
        end

        subgraph ModTicketing ["Ticketing Module"]
            TicketService["Ticket & QR Check-in Service"]
        end

        subgraph ModMarketing ["Marketing Module"]
            VoucherService["Voucher Service"]
        end

        subgraph ModBlog ["Blog Module"]
            BlogService["Blog & Tag Service"]
        end
    end

    subgraph SharedInfra ["Shared Infrastructure & External Integrations"]
        Flyway["Flyway Migration Engine"]
        PostgreSQL[("PostgreSQL 16 Database")]
        PayOS["PayOS Payment Gateway (VietQR)"]
        Cloudinary["Cloudinary CDN (Image Storage)"]
        BrevoMail["Brevo Mail API / SMTP (Thymeleaf)"]
        OpenPDF["OpenPDF Service (Invoice PDF Generator)"]
    end

    ClientLayer --> Gateway
    Gateway --> JWTDecoder
    JWTDecoder --> BusinessModules
    BusinessModules --> SharedInfra
```

---

## 2. Các luồng nghiệp vụ cốt lõi (Core Sequence Workflows)

### 2.1. Luồng Thanh toán VietQR (PayOS) & Tự động phát hành vé QR

```mermaid
sequenceDiagram
    autonumber
    actor Customer as Khách hàng (Client)
    participant FE as Frontend Customer
    participant BE as Backend OrderService
    participant PayOS as PayOS Gateway
    participant DB as PostgreSQL Database
    participant Mail as Brevo Email Service

    Customer->>FE: Chọn mua vé & Nhấn Checkout
    FE->>BE: POST /bookings/checkout {ticketTypeId, quantity, voucherCode}
    activate BE
    BE->>DB: Kiểm tra tồn kho vé & Áp dụng Voucher
    BE->>DB: Tạo Đơn hàng (Order status: PENDING)
    BE->>PayOS: Gọi API PayOS sinh link thanh toán & mã VietQR
    PayOS-->>BE: Trả về checkoutUrl & QR Code String
    BE-->>FE: Trả về ApiResponse chứa link thanh toán PayOS
    deactivate BE

    FE->>Customer: Hiển thị mã VietQR thanh toán
    Customer->>PayOS: Chuyển khoản ngân hàng qua VietQR

    PayOS->>BE: Webhook HTTP POST /payment/payos-webhook (Kèm Checksum Header)
    activate BE
    BE->>BE: Xác thực Checksum Signature (Chống giả mạo)
    BE->>DB: Cập nhật Order.paymentStatus = PAID
    BE->>DB: Tự động giảm remainingQuantity trong TicketType
    BE->>DB: Tự động khởi tạo danh sách Vé điện tử (Ticket) + Mã QR Code duy nhất
    BE->>BE: Tự động xuất file PDF Hóa đơn (OpenPDF)
    BE->>Mail: Render Email HTML (Thymeleaf) đính kèm QR Code vé & gửi cho Khách hàng
    BE-->>PayOS: HTTP 200 OK (Xác nhận Webhook thành công)
    deactivate BE

    FE->>BE: Polling / GET /bookings/{id}
    BE-->>FE: Trả về Đơn hàng đã thanh toán thành công
    FE->>Customer: Hiển thị màn hình PaymentSuccess & Danh sách vé QR
```

---

### 2.2. Luồng Kiểm tra & Soát vé (Event Check-in)

```mermaid
sequenceDiagram
    autonumber
    actor Staff as Nhân viên (Staff Mobile App)
    participant App as Mobile Kotlin App
    participant BE as Backend TicketService
    participant DB as PostgreSQL Database

    Staff->>App: Mở Camera ứng dụng & Quét mã QR trên vé khách
    App->>BE: POST /tickets/check-in {qrCode, eventId}
    activate BE
    BE->>DB: Truy vấn thông tin vé bằng qrCode
    
    alt Vé không tồn tại hoặc sai sự kiện
        BE-->>App: 404/400 Error (ErrorCode: TICKET_INVALID / TICKET_NOT_OWNED)
        App->>Staff: Hiển thị màn hình Đỏ: "Vé không hợp lệ"
    else Vé đã được sử dụng trước đó (isUsed == true)
        BE-->>App: 400 Error (ErrorCode: TICKET_USED)
        App->>Staff: Hiển thị màn hình Cảnh báo: "Vé đã check-in lúc HH:mm"
    else Vé hợp lệ & Chưa sử dụng
        BE->>BE: Kiểm tra Staff có quyền soát vé tại Event này không
        BE->>DB: Cập nhật Ticket.isUsed = true & Ghi nhận thời gian check-in
        BE-->>App: 200 OK (Ticket details: Hạng vé, Họ tên khách)
        App->>Staff: Hiển thị màn hình Xanh: "Check-in Thành công!"
    end
    deactivate BE
```

---

### 2.3. Luồng Xác thực & Bảo mật JWT (Authentication & Security)

```mermaid
sequenceDiagram
    autonumber
    actor User as Người dùng
    participant FE as Web Client
    participant Sec as Spring Security / CustomJwtDecoder
    participant BE as AuthService
    participant DB as PostgreSQL Database

    User->>FE: Nhập Username & Password
    FE->>BE: POST /auth/login
    activate BE
    BE->>DB: Kiểm tra thông tin người dùng & Mật khẩu BCrypt
    BE->>BE: Sinh mã JWT Access Token (Signed by HMAC-SHA512)
    BE-->>FE: Trả về Access Token + Refresh Token
    deactivate BE

    FE->>Sec: Gửi HTTP Request + Header `Authorization: Bearer <token>`
    activate Sec
    Sec->>DB: Kiểm tra Token trong bảng `invalidated_tokens`
    alt Token nằm trong danh sách đen (Đã Logout)
        Sec-->>FE: 401 Unauthorized (ErrorCode: UNAUTHENTICATED)
    else Token hợp lệ & Còn hiệu lực
        Sec->>BE: Cho phép Request đi tiếp vào Controller nghiệp vụ
        BE-->>FE: Trả về kết quả nghiệp vụ thành công
    end
    deactivate Sec

    User->>FE: Nhấn nút Đăng xuất (Logout)
    FE->>BE: POST /auth/logout {token}
    BE->>DB: Lưu Token vào bảng `invalidated_tokens`
    BE-->>FE: 200 OK (Đăng xuất thành công)
```

---

## 3. Cơ sở dữ liệu & Database Migration (Flyway)

Hệ thống sử dụng **PostgreSQL 16** làm cơ sở dữ liệu quan hệ chính. Toàn bộ cấu trúc bảng và dữ liệu khởi tạo được quản lý tự động bằng **Flyway Migration** tại thư mục `src/main/resources/db/migration`:

* `V1__init_schema.sql`: Khởi tạo 17 bảng dữ liệu quan hệ, thiết lập khóa chính (`PK`), khóa ngoại (`FK`), các chỉ mục B-Tree (`INDEX`) tối ưu tìm kiếm và ràng buộc toàn vẹn dữ liệu.
* `V2__seed_data.sql`: Cung cấp dữ liệu mẫu khởi tạo (Tài khoản Admin mặc định, các vai trò `ADMIN`, `ORGANIZER`, `STAFF`, `CUSTOMER`, các danh mục sự kiện mẫu).

```mermaid
erDiagram
    users ||--o{ user_roles : "has"
    roles ||--o{ user_roles : "belongs"
    users ||--o{ invalidated_tokens : "owns"
    users ||--o{ events : "organizes"
    users ||--o{ orders : "places"
    users ||--|| carts : "owns"
    users ||--o{ blog_posts : "writes"

    categories ||--o{ events : "classifies"
    events ||--o{ event_images : "contains"
    events ||--|{ ticket_types : "offers"
    events ||--o{ blog_posts : "relates_to"

    ticket_types ||--o{ tickets : "generates"
    ticket_types ||--o{ order_items : "ordered_in"
    ticket_types ||--o{ cart_items : "added_in"

    orders ||--|{ order_items : "contains"
    orders ||--o{ tickets : "issues"
    vouchers ||--o{ orders : "applied_to"

    carts ||--o{ cart_items : "contains"

    blog_posts ||--o{ blog_post_tags : "has"
    blog_tags ||--o{ blog_post_tags : "tagged_in"
```

---

## 4. Danh sách các Module nghiệp vụ

| STT | Module Name | Package Path | Chức năng chính |
| :-: | :--- | :--- | :--- |
| **1** | **Identity** | `modules.identity` | Đăng ký, Đăng nhập, Xác thực Email OTP, Quên mật khẩu, Thu hồi JWT Token, Phân quyền RBAC (`ADMIN`, `ORGANIZER`, `STAFF`, `CUSTOMER`). |
| **2** | **Event** | `modules.event` | Quản lý danh mục, Tạo/Sửa sự kiện, Upload banner Cloudinary, Tìm kiếm linh hoạt JPA Specification, Tác vụ tự động cập nhật trạng thái sự kiện (`@Scheduled`). |
| **3** | **Ordering** | `modules.ordering` | Quản lý giỏ hàng (`Cart`), Thanh toán đơn hàng, Tích hợp PayOS VietQR Webhook, Thống kê doanh thu đa chiều cho Admin & Organizer. |
| **4** | **Ticketing** | `modules.ticketing` | Tự động phát hành vé điện tử có chứa mã QR duy nhất, API soát vé (`/check-in`) cho ứng dụng Mobile. |
| **5** | **Marketing** | `modules.marketing` | Quản lý mã giảm giá (`Voucher`), tính toán chiết khấu theo % hoặc số tiền cố định, kiểm tra điều kiện áp dụng. |
| **6** | **Blog** | `modules.blog` | Quản lý bài viết tin tức, gắn thẻ phân loại (`BlogTag`), quy trình duyệt bài từ Ban tổ chức. |

---

## 5. Công nghệ & Thư viện sử dụng (Tech Stack)

* **Core Framework**: Java 17, Spring Boot 3.4.5
* **Database & Migration**: PostgreSQL 16, Flyway Database Migration
* **Security & Auth**: Spring Security, OAuth2 Resource Server, JWT (HMAC-SHA512), BCrypt
* **API Documentation**: SpringDoc OpenAPI v2.8.0, Swagger UI
* **Integrations & Third-party Services**:
  * **PayOS Java SDK (v1.0.1)**: Thanh toán trực tuyến VietQR & Webhook
  * **Cloudinary (v1.36.0)**: Quản lý CDN và lưu trữ hình ảnh
  * **Brevo (Sendinblue) / Spring Mail**: Gửi email OTP & Vé điện tử với mẫu Thymeleaf HTML
  * **OpenPDF (v1.3.30)**: Tạo và xuất tệp PDF Hóa đơn / Vé tự động
* **Tools & Utilities**: MapStruct (v1.5.5), Lombok, Dotenv, JavaFaker

---

## 6. Cấu trúc thư mục mã nguồn

```text
BE-event-mng/
├── src/main/java/com/sa/event_mng/
│   ├── modules/                    # Kiến trúc Modular Monolith
│   │   ├── identity/               # Module Xác thực & Phân quyền
│   │   ├── event/                  # Module Quản lý Sự kiện
│   │   ├── ordering/               # Module Đơn hàng & Thanh toán
│   │   ├── ticketing/              # Module Vé điện tử & Check-in
│   │   ├── marketing/              # Module Mã giảm giá (Voucher)
│   │   └── blog/                   # Module Bài viết & Tin tức
│   ├── shared/                     # Dùng chung toàn hệ thống
│   │   ├── dto/                    # Wrapper ApiResponse<T>
│   │   ├── exception/              # GlobalExceptionHandler & ErrorCode
│   │   ├── infrastructure/         # Cloudinary, PayOS, PDF, Email
│   │   └── config/                 # SecurityConfig, WebConfig, OpenApiConfig
│   ├── task/                       # Scheduled Tasks (EventStatus, Ping)
│   └── EventMngApplication.java    # Application Main Class
└── src/main/resources/
    ├── db/migration/               # Script SQL của Flyway Migration
    └── templates/                  # Template Email HTML Thymeleaf
```

---

## 7. Hướng dẫn cài đặt & Chạy dự án

### Yêu cầu môi trường
* **Java**: JDK 17+
* **Database**: PostgreSQL 16+
* **Build tool**: Maven 3.8+

### 1. Cấu hình biến môi trường (`.env` hoặc `application.properties`)
Tạo tệp `.env` tại thư mục gốc của dự án `BE-event-mng/.env`:

```env
SPRING_DATASOURCE_URL=jdbc:postgresql://localhost:5432/event_mng
SPRING_DATASOURCE_USERNAME=postgres
SPRING_DATASOURCE_PASSWORD=your_postgres_password

# JWT Security
JWT_SIGNER_KEY=your_secret_key_minimum_64_characters_long_for_hmac_sha512

# PayOS Integration
PAYOS_CLIENT_ID=your_payos_client_id
PAYOS_API_KEY=your_payos_api_key
PAYOS_CHECKSUM_KEY=your_payos_checksum_key

# Cloudinary Integration
CLOUDINARY_CLOUD_NAME=your_cloudinary_name
CLOUDINARY_API_KEY=your_cloudinary_api_key
CLOUDINARY_API_SECRET=your_cloudinary_api_secret

# Email Configuration
SPRING_MAIL_HOST=smtp-relay.brevo.com
SPRING_MAIL_PORT=587
SPRING_MAIL_USERNAME=your_brevo_email
SPRING_MAIL_PASSWORD=your_brevo_smtp_key
```

### 2. Chạy ứng dụng
Biên dịch và khởi chạy dự án (Flyway sẽ tự động chạy migration tạo CSDL):

```bash
# Biên dịch dự án
mvn clean install -DskipTests

# Chạy ứng dụng Spring Boot
mvn spring-boot:run
```

### 3. Xem Tài liệu API (Swagger UI)
Sau khi ứng dụng khởi chạy thành công tại cổng `8080`, mở trình duyệt truy cập:

```text
http://localhost:8080/swagger-ui/index.html
```

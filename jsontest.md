# Postman API Testing Guide & JSON Data

Sử dụng tài liệu này để kiểm thử hệ thống qua Postman. Tất cả các API trả về định dạng `ApiResponse`.

**Base URL:** `http://localhost:8080`

**Swagger UI:** [http://localhost:8080/swagger-ui/index.html](http://localhost:8080/swagger-ui/index.html)

**API Docs:** [http://localhost:8080/v3/api-docs](http://localhost:8080/v3/api-docs)

---

## 1. Authentication (`/auth`)

### 🔑 Đăng nhập
- **POST** `/auth/login`
```json
{
  "username": "admin",
  "password": "admin"
}
```

### 📝 Đăng ký tài khoản Customer
- **POST** `/auth/register`
```json
{
  "username": "customer01",
  "password": "Password123",
  "email": "customer01@example.com",
  "fullName": "Nguyễn Văn A",
  "phone": "0971234567",
  "address": "123 Đường ABC, TP.HCM"
}
```

### 🏢 Đăng ký tài khoản Organizer
- **POST** `/auth/register`
```json
{
  "username": "organizer01",
  "password": "Password123",
  "email": "organizer01@example.com",
  "fullName": "Công ty Sự kiện X",
  "phone": "0981234567",
  "address": "456 Đường XYZ, Hà Nội",
  "role": "ORGANIZER"
}
```

### 👷 Organizer tạo tài khoản Staff
- **POST** `/auth/register-staff`
- 🔒 Yêu cầu token ORGANIZER
```json
{
  "username": "staff01",
  "password": "Password123",
  "email": "staff01@example.com",
  "fullName": "Nhân viên soát vé"
}
```

### 🔄 Quên mật khẩu (gửi OTP)
- **POST** `/auth/forgot-password`
```json
{
  "email": "customer01@example.com"
}
```

### 🔒 Đặt lại mật khẩu
- **POST** `/auth/reset-password`
```json
{
  "email": "customer01@example.com",
  "otp": "123456",
  "newPassword": "NewPassword123"
}
```

### 🚪 Đăng xuất
- **POST** `/auth/logout`
- 🔒 Yêu cầu token
```json
{
  "token": "{{jwt_token}}"
}
```

---

## 2. Event Module (`/events`, `/categories`, `/ticket-types`)

### 📁 Tạo Danh mục (ADMIN)
- **POST** `/categories`
- 🔒 Yêu cầu token ADMIN
```json
{
  "name": "Âm nhạc",
  "description": "Các buổi liveshow và concert"
}
```

### 📋 Lấy danh sách Danh mục
- **GET** `/categories`

### 📅 Tạo Sự kiện mới (ORGANIZER)
- **POST** `/events`
- 🔒 Yêu cầu token ORGANIZER
- **Body Type:** `form-data`

| Field | Type | Ví dụ |
|---|---|---|
| `name` | Text | Concert Chillies 2026 |
| `categoryId` | Text | 1 |
| `location` | Text | Nhà thi đấu Phú Thọ |
| `province` | Text | Ho Chi Minh |
| `startTime` | Text | 2026-06-20T19:00:00 |
| `endTime` | Text | 2026-06-20T22:00:00 |
| `saleStartDate` | Text | 2026-05-01T00:00:00 |
| `saleEndDate` | Text | 2026-06-19T23:59:59 |
| `description` | Text | Mô tả chi tiết |
| `status` | Text | UPCOMING |
| `files` | File | [Chọn ảnh] |

### 🔍 Tìm kiếm Sự kiện
- **GET** `/events?page=1&size=10&search=concert&province=Ho+Chi+Minh`

### 📄 Chi tiết Sự kiện
- **GET** `/events/{id}`

### 📊 Thống kê Doanh thu (ORGANIZER)
- **GET** `/events/organizer/stats`
- 🔒 Yêu cầu token ORGANIZER

### 🎫 Thiết lập Loại vé (ORGANIZER)
- **POST** `/ticket-types`
- 🔒 Yêu cầu token ORGANIZER
```json
{
  "eventId": 1,
  "name": "Vé VIP (Fanzone)",
  "price": 1500000,
  "totalQuantity": 500,
  "description": "Gần sân khấu, kèm quà tặng."
}
```

---

## 3. Ordering Module (`/cart`, `/bookings`)

### 🛒 Xem Giỏ hàng
- **GET** `/cart`
- 🔒 Yêu cầu token

### ➕ Thêm vé vào Giỏ hàng
- **POST** `/cart/add`
- 🔒 Yêu cầu token
```json
{
  "ticketTypeId": 1,
  "quantity": 2
}
```

### ✏️ Cập nhật số lượng
- **PUT** `/cart/items/{itemId}?quantity=3`
- 🔒 Yêu cầu token

### 🗑️ Xóa một vé khỏi Giỏ
- **DELETE** `/cart/items/{itemId}`
- 🔒 Yêu cầu token

### 🧹 Xóa toàn bộ Giỏ hàng
- **DELETE** `/cart/clear`
- 🔒 Yêu cầu token

### 💳 Thanh toán toàn bộ Giỏ hàng
- **POST** `/bookings/checkout?paymentMethod=BANKING`
- 🔒 Yêu cầu token
- **paymentMethod:** `MOMO` | `VNPAY` | `BANKING`
- **voucherCode** (optional): `?voucherCode=SUMMER10`
- **platform** (optional, default: `web`): `?platform=web` hoặc `?platform=mobile`
  - **Web Frontend**: `/bookings/checkout?paymentMethod=PAYOS`
  - **Mobile App**: `/bookings/checkout?paymentMethod=PAYOS&platform=mobile` ⭐ **PHẢI GỬI platform=mobile**

### 💳 Thanh toán các mục được chọn
- **POST** `/bookings/checkout-selected?paymentMethod=BANKING`
- 🔒 Yêu cầu token
- **platform** (optional, default: `web`): `?platform=web` hoặc `?platform=mobile`
  - **Mobile App**: `/bookings/checkout-selected?paymentMethod=PAYOS&platform=mobile` ⭐ **PHẢI GỬI platform=mobile**
```json
[1, 2, 3]
```

### 📜 Lịch sử Đơn hàng
- **GET** `/bookings?page=1&size=10`
- 🔒 Yêu cầu token

---

## 4. Ticketing Module (`/tickets`)

### 🎟️ Xem Vé đã mua
- **GET** `/tickets/my-tickets`
- 🔒 Yêu cầu token

### ✅ Check-in Vé (ORGANIZER/STAFF)
- **POST** `/tickets/check-in?ticketCode=TKT-XXXXXXX`
- 🔒 Yêu cầu token ORGANIZER hoặc STAFF

---

## 5. Marketing Module (`/vouchers`)

### 🏷️ Tạo mã giảm giá (ADMIN/ORGANIZER)
- **POST** `/vouchers`
- 🔒 Yêu cầu token ADMIN hoặc ORGANIZER
```json
{
  "code": "SUMMER10",
  "discountType": "PERCENTAGE",
  "amount": 10,
  "minOrderAmount": 500000,
  "maxDiscount": 200000,
  "startDate": "2026-05-01T00:00:00",
  "endDate": "2026-06-30T23:59:59",
  "quantity": 100,
  "eventId": null
}
```

### 📋 Danh sách mã giảm giá
- **GET** `/vouchers?page=1&size=10`
- 🔒 Yêu cầu token ADMIN hoặc ORGANIZER

---

## 💡 Mẹo dùng Postman

1. Sau khi đăng nhập, copy giá trị `result.token` từ response.
2. Tạo một **Environment** trong Postman, thêm biến `jwt_token` và dán token vào.
3. Trong tab **Authorization** của từng request: chọn `Bearer Token` → nhập `{{jwt_token}}`.
4. Dùng **Swagger UI** tại `http://localhost:8080/swagger-ui/index.html` để xem toàn bộ API schema trực quan hơn.

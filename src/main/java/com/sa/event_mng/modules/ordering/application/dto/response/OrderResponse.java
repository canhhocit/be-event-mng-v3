package com.sa.event_mng.modules.ordering.application.dto.response;

import com.sa.event_mng.model.enums.OrderStatus;
import com.sa.event_mng.model.enums.PaymentMethod;
import com.sa.event_mng.model.enums.PaymentStatus;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OrderResponse {
    // Id đơn hàng (UUID dạng chuỗi)
    private String id;

    // Số tiền sau khi trừ phí dịch vụ / phí nền tảng, tổ chức thu được.
    private BigDecimal organizerAmount; // tổng tiền btc ăn
    private Float platformFeeRate;  // phần trăm tiền admin ăn
    private BigDecimal serviceFee;  // tổng tiền admin ăn
    private BigDecimal totalAmount; // tổng tiền khách phải trả

    // Thông tin thanh toán.
    private PaymentMethod paymentMethod;
    private PaymentStatus paymentStatus;
    private OrderStatus orderStatus;

    // Thời điểm tạo đơn.
    private LocalDateTime orderDate;

    // Link thanh toán nếu dùng PAYOS hoặc MoMo.
    private String paymentUrl;
}

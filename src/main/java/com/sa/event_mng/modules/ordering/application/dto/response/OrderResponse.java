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
    private String id;
    private BigDecimal organizerAmount; // tổng tiền btc ăn
    private Float platformFeeRate;  //phần trăm tiền admin ăn
    private BigDecimal serviceFee;  //tổng tiền admin ăn
    private BigDecimal totalAmount; //tổng tiền khách phải trả
    private PaymentMethod paymentMethod;
    private PaymentStatus paymentStatus;
    private OrderStatus orderStatus;
    private LocalDateTime orderDate;
    private String paymentUrl;
}

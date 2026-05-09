package com.sa.event_mng.modules.ordering.application.dto.response;

import lombok.*;
import java.math.BigDecimal;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CartResponse {
    // Id của giỏ hàng.
    private Long id;

    // Danh sách sản phẩm trong giỏ.
    private List<CartItemResponse> items;

    // Tổng giá trị của giỏ hàng.
    private BigDecimal totalAmount;
}

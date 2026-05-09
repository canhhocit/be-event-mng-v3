package com.sa.event_mng.modules.ordering.application.dto.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CartItemRequest {
    // Mã loại vé cần thêm vào giỏ hàng.
    @NotNull(message = "UNCATEGORIZED_EXCEPTION")
    private Long ticketTypeId;

    // Số lượng vé muốn mua.
    @Min(value = 1, message = "UNCATEGORIZED_EXCEPTION")
    private Integer quantity;
}

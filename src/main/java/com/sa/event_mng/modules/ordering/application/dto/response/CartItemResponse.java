package com.sa.event_mng.modules.ordering.application.dto.response;

import lombok.*;
import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CartItemResponse {
    private Long id;

    // Loại vé và sự kiện tương ứng.
    private Long ticketTypeId;
    private String ticketTypeName;
    private String eventName;
    private Long eventId;

    // Ảnh đại diện của sự kiện.
    private String eventImage;

    // Số lượng và giá.
    private Integer quantity;
    private BigDecimal unitPrice;
    private BigDecimal subtotal;

    // Thời điểm kết thúc thời gian bán vé.
    private java.time.LocalDateTime saleEndDate;
}

package com.sa.event_mng.modules.event.application.dto.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TicketTypeRequest {
    @NotBlank(message = "TICKET_NAME_REQUIRED")
    private String name;

    private String description;

    @Min(value = 0, message = "TICKET_PRICE_INVALID")
    private BigDecimal price;

    @Min(value = 1, message = "TICKET_QUANTITY_INVALID")
    private Integer totalQuantity;  

    @NotNull(message = "EVENT_ID_REQUIRED")
    private Long eventId;
}

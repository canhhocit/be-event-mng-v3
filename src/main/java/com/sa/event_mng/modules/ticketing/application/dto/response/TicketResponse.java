package com.sa.event_mng.modules.ticketing.application.dto.response;

import com.sa.event_mng.model.enums.TicketStatus;
import lombok.*;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TicketResponse {
    private Long id;

    // Tên sự kiện và loại vé.
    private String eventName;
    private String ticketTypeName;

    // Mã vé và QR code dùng để check-in.
    private String ticketCode;
    private String qrCode;

    // Trạng thái vé (VALID, USED, ...)
    private TicketStatus status;
    private LocalDateTime usedAt;
}

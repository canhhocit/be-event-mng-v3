package com.sa.event_mng.modules.ticketing.presentation.controller;

import com.sa.event_mng.shared.dto.ApiResponse;
import com.sa.event_mng.modules.ticketing.application.dto.response.TicketResponse;
import com.sa.event_mng.modules.ticketing.application.service.TicketService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/tickets")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Tag(name = "Vé điện tử", description = "Xem vé và thực hiện check-in")
public class TicketController {

    // Dịch vụ xử lý logic vé
    TicketService ticketService;

    @GetMapping("/my-tickets")
    @Operation(summary = "Xem danh sách vé đã mua")
    public ApiResponse<List<TicketResponse>> getMyTickets() {
        // Gọi service trả về danh sách vé của người dùng hiện tại
        return ApiResponse.<List<TicketResponse>>builder()
                .result(ticketService.getMyTickets())
                .build();
    }

    @PostMapping("/check-in")
    @Operation(summary = "Quét mã vé để check-in (Chỉ Ban tổ chức/Admin)")
    public ApiResponse<TicketResponse> checkIn(@RequestParam String ticketCode) {
        // Gọi service kiểm tra và đổi trạng thái vé khi check-in
        return ApiResponse.<TicketResponse>builder()
                .result(ticketService.checkIn(ticketCode))
                .build();
    }
}

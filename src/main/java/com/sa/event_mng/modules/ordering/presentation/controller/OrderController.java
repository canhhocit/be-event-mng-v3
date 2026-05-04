package com.sa.event_mng.modules.ordering.presentation.controller;

import com.sa.event_mng.shared.dto.ApiResponse;
import com.sa.event_mng.modules.ordering.application.dto.response.OrderResponse;
import com.sa.event_mng.model.enums.PaymentMethod;
import com.sa.event_mng.modules.ordering.application.service.OrderService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/bookings")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Tag(name = "Đơn hàng", description = "Thanh toán và xem lịch sử mua vé")
public class OrderController {

    OrderService orderService;

    @PostMapping("/checkout")
    @Operation(summary = "Thanh toán toàn bộ giỏ hàng")
    public ApiResponse<OrderResponse> checkout(
            @RequestParam PaymentMethod paymentMethod,
            @RequestParam(required = false) String voucherCode,
            @RequestParam(defaultValue = "web") String platform) {
        return ApiResponse.<OrderResponse>builder()
                .result(orderService.checkout(paymentMethod, voucherCode, platform))
                .build();
    }

    @PostMapping("/checkout-selected")
    @Operation(summary = "Thanh toán các mục được chọn trong giỏ hàng")
    public ApiResponse<OrderResponse> checkoutSelected(
            @RequestBody java.util.List<Long> itemIds, 
            @RequestParam PaymentMethod paymentMethod,
            @RequestParam(required = false) String voucherCode,
            @RequestParam(defaultValue = "web") String platform) {
        return ApiResponse.<OrderResponse>builder()
                .result(orderService.checkoutSelected(itemIds, paymentMethod, voucherCode, platform))
                .build();
    }

    @GetMapping("/{id}/invoice")
    @Operation(summary = "Tải hóa đơn PDF của đơn hàng")
    public org.springframework.http.ResponseEntity<byte[]> downloadInvoice(@PathVariable String id) {
        byte[] pdf = orderService.getOrderInvoice(id);
        org.springframework.http.HttpHeaders headers = new org.springframework.http.HttpHeaders();
        headers.setContentType(org.springframework.http.MediaType.APPLICATION_PDF);
        headers.setContentDispositionFormData("attachment", "Invoice_" + id + ".pdf");
        return new org.springframework.http.ResponseEntity<>(pdf, headers, org.springframework.http.HttpStatus.OK);
    }

    @PostMapping("/cancel-and-restore-cart")
    @Operation(summary = "Hủy đơn hàng và hoàn tác các mục về giỏ hàng")
    public ApiResponse<Void> cancelAndRestoreCart(@RequestParam Long orderCode) {
        orderService.cancelAndRestoreCart(orderCode);
        return ApiResponse.<Void>builder()
                .message("Đã hoàn tác giỏ hàng")
                .build();
    }

    @GetMapping
    @Operation(summary = "Xem lịch sử đơn hàng của tôi")
    public ApiResponse<Page<OrderResponse>> getMyOrders(@RequestParam(defaultValue = "1") int page,
                                                        @RequestParam(defaultValue = "10") int size) {
        PageRequest pageRequest = PageRequest.of(
                page - 1, size,
                Sort.by("createdAt").descending());
        return ApiResponse.<Page<OrderResponse>>builder()
                .result(orderService.getMyOrders(pageRequest))
                .build();
    }
}

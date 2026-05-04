package com.sa.event_mng.modules.marketing.presentation.controller;

import com.sa.event_mng.modules.marketing.application.dto.request.VoucherRequest;
import com.sa.event_mng.shared.dto.ApiResponse;
import com.sa.event_mng.modules.marketing.application.dto.response.VoucherResponse;
import com.sa.event_mng.modules.marketing.application.service.VoucherService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/vouchers")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Tag(name = "Khuyến mãi", description = "Quản lý mã giảm giá")
public class VoucherController {

    VoucherService voucherService;

    @PostMapping
    @Operation(summary = "Tạo mã giảm giá mới (ADMIN/ORGANIZER)")
    public ApiResponse<VoucherResponse> create(@RequestBody @Valid VoucherRequest request) {
        return ApiResponse.<VoucherResponse>builder()
                .result(voucherService.createVoucher(request))
                .build();
    }

    @GetMapping
    @Operation(summary = "Lấy danh sách mã giảm giá")
    public ApiResponse<Page<VoucherResponse>> getAll(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size) {
        int safePage = Math.max(page, 1);
        int safeSize = Math.max(size, 1);
        PageRequest pageRequest = PageRequest.of(safePage - 1, safeSize, Sort.by("id").descending());
        return ApiResponse.<Page<VoucherResponse>>builder()
                .result(voucherService.getAllVouchers(pageRequest))
                .build();
    }
    @PostMapping("/validate")
    @Operation(summary = "Kiểm tra mã giảm giá")
    public ApiResponse<Double> validateVoucher(
            @RequestBody com.sa.event_mng.modules.marketing.application.dto.request.VoucherValidationRequest request) {
        return ApiResponse.<Double>builder()
                .result(voucherService.calculateDiscount(request.getCode(), request.getEventAmounts()))
                .build();
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Xóa mã giảm giá (Chủ sở hữu/ADMIN)")
    public ApiResponse<Void> delete(@PathVariable Long id) {
        voucherService.deleteVoucher(id);
        return ApiResponse.<Void>builder().build();
    }

    @GetMapping("/event/{eventId}")
    @Operation(summary = "Lấy mã giảm giá khả dụng cho sự kiện")
    public ApiResponse<java.util.List<VoucherResponse>> getByEvent(@PathVariable Long eventId) {
        return ApiResponse.<java.util.List<VoucherResponse>>builder()
                .result(voucherService.getActiveVouchersForEvent(eventId))
                .build();
    }
}

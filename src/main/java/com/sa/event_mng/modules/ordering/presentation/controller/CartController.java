package com.sa.event_mng.modules.ordering.presentation.controller;

import com.sa.event_mng.modules.ordering.application.dto.request.CartItemRequest;
import com.sa.event_mng.shared.dto.ApiResponse;
import com.sa.event_mng.modules.ordering.application.dto.response.CartResponse;
import com.sa.event_mng.modules.ordering.application.service.CartService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/cart")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Tag(name = "Giỏ hàng", description = "Quản lý giỏ hàng của người dùng")
public class CartController {

    // Dịch vụ xử lý logic giỏ hàng
    CartService cartService;

    @PostMapping("/add")
    @Operation(summary = "Thêm vé vào giỏ hàng")
    public ApiResponse<CartResponse> addToCart(@RequestBody @Valid CartItemRequest request) {
        // Thêm vé vào giỏ hàng của user hiện tại.
        // Request body chứa ticketTypeId và số lượng.
        return ApiResponse.<CartResponse>builder()
                .result(cartService.addToCart(request))
                .build();
    }

    @GetMapping
    @Operation(summary = "Xem giỏ hàng của tôi")
    public ApiResponse<CartResponse> getMyCart() {
        // Lấy thông tin giỏ hàng của user đang đăng nhập.
        return ApiResponse.<CartResponse>builder()
                .result(cartService.getMyCart())
                .build();
    }

    @PutMapping("/items/{itemId}")
    @Operation(summary = "Cập nhật số lượng vé trong giỏ")
    public ApiResponse<CartResponse> updateQuantity(@PathVariable Long itemId, @RequestParam Integer quantity) {
        // Cập nhật số lượng của item trong giỏ, nếu quantity <= 0 thì xóa item.
        return ApiResponse.<CartResponse>builder()
                .result(cartService.updateQuantity(itemId, quantity))
                .build();
    }

    @DeleteMapping("/items/{itemId}")
    @Operation(summary = "Xóa vé khỏi giỏ hàng")
    public ApiResponse<CartResponse> removeItem(@PathVariable Long itemId) {
        // Xóa 1 item khỏi giỏ hàng theo itemId.
        return ApiResponse.<CartResponse>builder()
                .result(cartService.removeItem(itemId))
                .build();
    }

    @DeleteMapping("/clear")
    @Operation(summary = "Xóa toàn bộ giỏ hàng")
    public ApiResponse<Void> clearCart() {
        // Xóa toàn bộ các item trong giỏ hàng.
        cartService.clearCart();
        return ApiResponse.<Void>builder().build();
    }
}

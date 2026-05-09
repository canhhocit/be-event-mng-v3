package com.sa.event_mng.modules.ordering.presentation.controller;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;

import java.util.Map;
import java.net.URI;
import java.util.concurrent.CompletableFuture;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.sa.event_mng.modules.ordering.application.service.OrderService;

@RestController
@RequestMapping("/api/v1/payments")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class PaymentController {

    // Dịch vụ xử lý logic thanh toán và redirect
    OrderService orderService;

    @org.springframework.beans.factory.annotation.Value("${app.payment.deep-link.scheme}")
    @lombok.experimental.NonFinal
    String deepLinkScheme;

    @org.springframework.beans.factory.annotation.Value("${app.payment.deep-link.host}")
    @lombok.experimental.NonFinal
    String deepLinkHost;

    @org.springframework.beans.factory.annotation.Value("${app.payment.deep-link.path}")
    @lombok.experimental.NonFinal
    String deepLinkPath;

    // Chuẩn hóa trạng thái trả về từ PayOS / frontend.
    // Ví dụ "paid", "success" đều được xử lý thành "success".
    private String normalizeStatus(String status) {
        if (status == null) {
            return "success";
        }

        String normalized = status.trim().toLowerCase();
        return switch (normalized) {
            case "paid", "success" -> "success";
            case "canceled", "cancelled", "cancel" -> "cancel";
            default -> normalized;
        };
    }

    @org.springframework.web.bind.annotation.GetMapping("/payos-webhook")
    public String validateWebhook() {
        // Endpoint kiểm tra xem webhook PayOS đã hoạt động chưa.
        return "Webhook is active!";
    }

    @org.springframework.beans.factory.annotation.Value("${app.frontend.url}")
    @lombok.experimental.NonFinal
    String frontendUrl;

    @GetMapping("/redirect")
    public ResponseEntity<Void> redirectToDeepLink(@RequestParam(name = "orderCode") Long orderCode,
                                                   @RequestParam(name = "status", required = false, defaultValue = "success") String status,
                                                   @RequestParam(name = "platform", required = false, defaultValue = "mobile") String platform) {
        try {
            String normalizedStatus = normalizeStatus(status);

            // Nếu trạng thái thanh toán thành công, gọi hoàn tất thanh toán.
            if ("success".equals(normalizedStatus)) {
                CompletableFuture.runAsync(() -> {
                    try {
                        orderService.completePaymentByOrderCode(orderCode);
                    } catch (Exception ex) {
                        System.err.println("ERROR: async completePaymentByOrderCode failed for " + orderCode + ": " + ex.getMessage());
                        ex.printStackTrace();
                    }
                });
            } else if ("cancel".equals(normalizedStatus)) {
                // Nếu khách hủy, gọi hủy đơn và trả về giỏ hàng.
                CompletableFuture.runAsync(() -> {
                    try {
                        orderService.cancelPaymentByOrderCode(orderCode);
                    } catch (Exception ex) {
                        System.err.println("ERROR: async cancelPaymentByOrderCode failed for " + orderCode + ": " + ex.getMessage());
                        ex.printStackTrace();
                    }
                });
            }

            String finalUrl;
            if ("web".equalsIgnoreCase(platform)) {
                // Redirect trở lại web frontend sau khi thanh toán hoặc hủy.
                String subPath = "success".equals(normalizedStatus) ? "/payment/success" : "/payment/cancel";
                finalUrl = frontendUrl + subPath + "?orderCode=" + orderCode;
            } else {
                // Redirect tới mobile deep link.
                String normalizedPath = deepLinkPath.startsWith("/") ? deepLinkPath : "/" + deepLinkPath;
                finalUrl = deepLinkScheme + "://" + deepLinkHost + normalizedPath + "?orderCode=" + orderCode + "&status=" + normalizedStatus;
            }

            HttpHeaders headers = new HttpHeaders();
            headers.setLocation(URI.create(finalUrl));
            return new ResponseEntity<>(headers, HttpStatus.SEE_OTHER);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @PostMapping("/payos-webhook")
    public void handlePayOSWebhook(@RequestBody Map<String, Object> body) {
        // Webhook PayOS gọi vào endpoint này sau khi thanh toán thành công bên PayOS.
        // Body JSON chứa trường "data" với orderCode.
        try {
            System.out.println("DEBUG: [WEBHOOK] Received call from PayOS. Body: " + body);
            
            @SuppressWarnings("unchecked")
            Map<String, Object> data = (Map<String, Object>) body.get("data");
            
            if (data != null) {
                Object orderCodeObj = data.get("orderCode");
                System.out.println("DEBUG: [WEBHOOK] OrderCode detected: " + orderCodeObj);
                
                if (orderCodeObj != null) {
                    Long orderCode = Long.valueOf(orderCodeObj.toString());
                    System.out.println("DEBUG: [WEBHOOK] Triggering OrderService.completePaymentByOrderCode for #" + orderCode);
                    orderService.completePaymentByOrderCode(orderCode);
                    System.out.println("DEBUG: [WEBHOOK] Process completed for #" + orderCode);
                }
            } else {
                System.out.println("DEBUG: [WEBHOOK] Data is not a Map or missing.");
            }
        } catch (Exception e) {
            System.err.println("CRITICAL ERROR: [WEBHOOK] Failed to process. Error: " + e.getMessage());
            e.printStackTrace();
        }
    }
}

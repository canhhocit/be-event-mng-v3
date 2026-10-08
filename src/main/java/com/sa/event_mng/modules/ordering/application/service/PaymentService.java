package com.sa.event_mng.modules.ordering.application.service;

import com.sa.event_mng.modules.ordering.domain.model.Order;
import com.sa.event_mng.modules.ordering.domain.repository.OrderRepository;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.HashMap;
import java.util.HexFormat;
import java.util.Map;
import java.util.TreeMap;
import java.util.stream.Collectors;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import vn.payos.PayOS;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@lombok.extern.slf4j.Slf4j
public class PaymentService {

    PayOS payOS;
    OrderRepository orderRepository;
    
    @org.springframework.beans.factory.annotation.Autowired
    @org.springframework.context.annotation.Lazy
    @lombok.experimental.NonFinal
    OrderService orderService;

    @org.springframework.beans.factory.annotation.Value("${payos.client-id}")
    @lombok.experimental.NonFinal
    String clientId;

    @org.springframework.beans.factory.annotation.Value("${payos.api-key}")
    @lombok.experimental.NonFinal
    String apiKey;

    @org.springframework.beans.factory.annotation.Value("${payos.checksum-key}")
    @lombok.experimental.NonFinal
    String checksumKey;

    @org.springframework.beans.factory.annotation.Value("${app.frontend.url}")
    @lombok.experimental.NonFinal
    String frontendUrl;
    
    @org.springframework.beans.factory.annotation.Value("${app.backend.url}")
    @lombok.experimental.NonFinal
    String backendUrl;

    public String createPayOSPaymentLink(Order order, String platform) throws Exception {
        String description = "Thanh toan #" + order.getOrderCode();
        if (description.length() > 25) {
            description = "TT don hang " + order.getOrderCode();
        }
        
        // 1. Prepare data for signature
        long amount = order.getTotalAmount().longValue();
        long orderCode = order.getOrderCode();

        // Use backend redirect endpoints which will forward to app deep link (customer://...)
        String returnUrl = backendUrl + "/api/v1/payments/redirect?orderCode=" + orderCode + "&status=success&platform=" + platform;
        String cancelUrl = backendUrl + "/api/v1/payments/redirect?orderCode=" + orderCode + "&status=cancel&platform=" + platform;
        
        // PayOS requires fields in alphabetical order for signature
        String signatureData = "amount=" + amount +
                "&cancelUrl=" + cancelUrl +
                "&description=" + description +
                "&orderCode=" + orderCode +
                "&returnUrl=" + returnUrl;

        String signature = hmacSHA256(signatureData, checksumKey);

        // 2. Create Request Body
        Map<String, Object> body = new HashMap<>();
        body.put("orderCode", orderCode);
        body.put("amount", amount);
        body.put("description", description);
        body.put("cancelUrl", cancelUrl);
        body.put("returnUrl", returnUrl);
        body.put("signature", signature);

        // 3. Call PayOS API directly using RestTemplate
        RestTemplate restTemplate = new RestTemplate();
        HttpHeaders headers = new HttpHeaders();
        headers.set("x-client-id", clientId);
        headers.set("x-api-key", apiKey);
        headers.setContentType(MediaType.APPLICATION_JSON);

        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(body, headers);
        
        try {
            @SuppressWarnings("unchecked")
            ResponseEntity<Map<String, Object>> response = restTemplate.postForEntity(
                "https://api-merchant.payos.vn/v2/payment-requests", entity, (Class<Map<String, Object>>) (Class<?>) Map.class);
            
            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                Map<String, Object> responseBody = response.getBody();
                String code = String.valueOf(responseBody.get("code"));
                String desc = String.valueOf(responseBody.get("desc"));
                
                @SuppressWarnings("unchecked")
                Map<String, Object> data = (Map<String, Object>) responseBody.get("data");
                if (data != null && data.get("checkoutUrl") != null) {
                    return (String) data.get("checkoutUrl");
                } else {
                    System.err.println("PayOS API Response Error: code=" + code + ", desc=" + desc + ", fullBody=" + responseBody);
                    throw new Exception("PayOS API Error [" + code + "]: " + desc);
                }
            }
        } catch (Exception e) {
            System.err.println("PayOS API Error: " + e.getMessage());
            throw e;
        }
        throw new Exception("Failed to create PayOS payment link");
    }

    /**
     * Trạng thái link thanh toán lấy trực tiếp từ PayOS: PENDING, PROCESSING, PAID, CANCELLED, EXPIRED...
     * Trả về null nếu không gọi được PayOS. Dùng cho redirect, vì tham số status trên URL ai cũng sửa được.
     */
    public String getPaymentStatus(Long orderCode) {
        try {
            return payOS.getPaymentLinkInformation(orderCode).getStatus();
        } catch (Exception e) {
            log.warn("Không lấy được trạng thái PayOS cho đơn {}: {}", orderCode, e.getMessage());
            return null;
        }
    }

    /**
     * Xử lý webhook PayOS: chỉ hoàn tất đơn khi chữ ký hợp lệ (ký bằng checksum key),
     * giao dịch thành công (code "00") và số tiền khớp với đơn.
     */
    @SuppressWarnings("unchecked")
    public void handlePayOSWebhook(Map<String, Object> body) {
        Object rawData = body.get("data");
        Object signature = body.get("signature");
        if (!(rawData instanceof Map) || signature == null) {
            log.warn("Webhook PayOS thiếu data/signature, bỏ qua");
            return;
        }
        Map<String, Object> data = (Map<String, Object>) rawData;
        if (!isValidWebhookSignature(data, signature.toString())) {
            log.warn("Webhook PayOS sai chữ ký, bỏ qua. orderCode={}", data.get("orderCode"));
            return;
        }
        if (!"00".equals(String.valueOf(body.get("code"))) || !"00".equals(String.valueOf(data.get("code")))) {
            log.info("Webhook PayOS không phải giao dịch thành công: code={}, data.code={}", body.get("code"), data.get("code"));
            return;
        }

        Long orderCode = Long.valueOf(String.valueOf(data.get("orderCode")));
        Order order = orderRepository.findByOrderCode(orderCode).orElse(null);
        if (order == null) {
            // PayOS gửi dữ liệu mẫu (orderCode 123) khi lưu Webhook URL trên dashboard
            log.info("Webhook PayOS cho đơn không tồn tại: {}", orderCode);
            return;
        }
        long paidAmount = Long.parseLong(String.valueOf(data.get("amount")));
        if (paidAmount != order.getTotalAmount().longValue()) {
            log.warn("Webhook PayOS số tiền không khớp cho đơn {}: nhận {}, đơn {}", orderCode, paidAmount, order.getTotalAmount());
            return;
        }
        orderService.completePaymentByOrderCode(orderCode);
    }

    // Theo tài liệu PayOS: sắp xếp các trường của data theo tên, nối "key=value" bằng "&" (null -> rỗng),
    // HMAC-SHA256 với checksum key rồi so với signature.
    boolean isValidWebhookSignature(Map<String, Object> data, String signature) {
        try {
            String raw = new TreeMap<>(data).entrySet().stream()
                    .map(e -> e.getKey() + "=" + (e.getValue() == null ? "" : e.getValue()))
                    .collect(Collectors.joining("&"));
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(checksumKey.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            byte[] expected = HexFormat.of().formatHex(mac.doFinal(raw.getBytes(StandardCharsets.UTF_8)))
                    .getBytes(StandardCharsets.UTF_8);
            return MessageDigest.isEqual(expected, signature.toLowerCase().getBytes(StandardCharsets.UTF_8));
        } catch (Exception e) {
            log.warn("Không kiểm tra được chữ ký webhook: {}", e.getMessage());
            return false;
        }
    }

    private String hmacSHA256(String data, String key) throws Exception {
        javax.crypto.spec.SecretKeySpec secretKeySpec = new javax.crypto.spec.SecretKeySpec(key.getBytes(), "HmacSHA256");
        javax.crypto.Mac mac = javax.crypto.Mac.getInstance("HmacSHA256");
        mac.init(secretKeySpec);
        byte[] rawHmac = mac.doFinal(data.getBytes());
        StringBuilder sb = new StringBuilder();
        for (byte b : rawHmac) {
            sb.append(String.format("%02x", b));
        }
        return sb.toString();
    }
}

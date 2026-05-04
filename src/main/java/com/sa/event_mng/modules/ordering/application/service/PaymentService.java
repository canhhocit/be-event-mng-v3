package com.sa.event_mng.modules.ordering.application.service;

import com.sa.event_mng.modules.ordering.domain.model.Order;
import com.sa.event_mng.modules.ordering.domain.repository.OrderRepository;
import com.sa.event_mng.shared.exception.AppException;
import com.sa.event_mng.shared.exception.ErrorCode;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;

import java.util.HashMap;
import java.util.Map;

import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import vn.payos.PayOS;
import vn.payos.type.Webhook;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
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
        org.springframework.web.client.RestTemplate restTemplate = new org.springframework.web.client.RestTemplate();
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
                @SuppressWarnings("unchecked")                Map<String, Object> data = (Map<String, Object>) responseBody.get("data");
                return (String) data.get("checkoutUrl");
            }
        } catch (Exception e) {
            System.err.println("PayOS API Error: " + e.getMessage());
            throw e;
        }
        throw new Exception("Failed to create PayOS payment link");
    }

    public void handlePayOSWebhook(Webhook webhook) {
        try {
            // 1. Verify Webhook Signature
            var data = payOS.verifyPaymentWebhookData(webhook);

            // 2. Extract Order Code
            Long orderCode = data.getOrderCode();

            // 3. Find Order and Complete Payment
            Order order = orderRepository.findByOrderCode(orderCode)
                    .orElseThrow(() -> new AppException(ErrorCode.ORDER_NOT_FOUND));

            // Only complete if not already paid
            orderService.completePayment(order.getId());
            
        } catch (Exception e) {
            e.printStackTrace();
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

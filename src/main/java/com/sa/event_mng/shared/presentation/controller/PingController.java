package com.sa.event_mng.shared.presentation.controller;

import com.sa.event_mng.shared.dto.ApiResponse;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequiredArgsConstructor
@Slf4j
public class PingController {

    @PersistenceContext
    private final EntityManager entityManager;

    @GetMapping({"/ping", "/api/v1/ping"})
    public ApiResponse<Map<String, Object>> ping() {
        Map<String, Object> response = new HashMap<>();
        String dbStatus = "CONNECTED";
        
        try {
            // Tác động trực tiếp vào DB (Neon PostgreSQL) để giữ connection & compute alive
            Object dbCheck = entityManager.createNativeQuery("SELECT 1").getSingleResult();
            response.put("dbPingResult", dbCheck);
        } catch (Exception e) {
            log.error("Database ping failed: {}", e.getMessage());
            dbStatus = "DISCONNECTED: " + e.getMessage();
        }

        response.put("status", "UP");
        response.put("dbStatus", dbStatus);
        response.put("message", "Server and DB ping active");
        response.put("timestamp", LocalDateTime.now().toString());

        return ApiResponse.<Map<String, Object>>builder()
                .code(1000)
                .result(response)
                .build();
    }
}

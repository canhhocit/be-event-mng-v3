package com.sa.event_mng.task;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.net.URI;

@Component
@Slf4j
public class PingTask {

    @Value("${app.backend.url:http://localhost:8080}")
    private String backendUrl;

    @Value("${server.port:8080}")
    private String serverPort;

    @PersistenceContext
    private EntityManager entityManager;

    private final RestTemplate restTemplate = new RestTemplate();

    /**
     * Tự động ping chính mình và DB 5 phút 1 lần để giữ cho server Render & Neon Database không bị chìm vào trạng thái sleep.
     * Fixed rate: 300000ms = 5 phút. Initial delay: 10000ms = 10s sau khi app khởi chạy.
     */
    @Scheduled(fixedRateString = "${app.ping.interval:300000}", initialDelay = 10000)
    public void selfPing() {
        // 1. Tác động trực tiếp vào Database
        try {
            Object result = entityManager.createNativeQuery("SELECT 1").getSingleResult();
            log.info("Direct DB ping successful, result: {}", result);
        } catch (Exception e) {
            log.warn("Direct DB ping failed: {}", e.getMessage());
        }

        // 2. HTTP Self-ping (Tác động vào HTTP server & Render container)
        String targetUrl = getPingUrl();
        try {
            log.info("Self-ping HTTP triggered to: {}", targetUrl);
            String response = restTemplate.getForObject(URI.create(targetUrl), String.class);
            log.info("Self-ping HTTP response successful: {}", response);
        } catch (Exception e) {
            log.warn("Self-ping HTTP to {} failed: {}", targetUrl, e.getMessage());
            if (!targetUrl.contains("localhost")) {
                String localUrl = "http://localhost:" + serverPort + "/api/v1/ping";
                try {
                    log.info("Attempting fallback self-ping HTTP to: {}", localUrl);
                    String localResponse = restTemplate.getForObject(URI.create(localUrl), String.class);
                    log.info("Fallback self-ping HTTP successful: {}", localResponse);
                } catch (Exception ex) {
                    log.warn("Fallback self-ping HTTP to {} also failed: {}", localUrl, ex.getMessage());
                }
            }
        }
    }

    private String getPingUrl() {
        String url = backendUrl;
        if (url == null || url.trim().isEmpty()) {
            url = "http://localhost:" + serverPort;
        }
        if (url.endsWith("/")) {
            url = url.substring(0, url.length() - 1);
        }
        return url + "/api/v1/ping";
    }
}

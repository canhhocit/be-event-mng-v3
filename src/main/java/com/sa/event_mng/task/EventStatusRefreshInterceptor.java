package com.sa.event_mng.task;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

/**
 * Cập nhật trạng thái sự kiện ngay trong request nếu EventStatusTask đã quá 1 phút chưa chạy.
 * Giỏ hàng/checkout kiểm tra status OPENING trong DB, nên khi @Scheduled không chạy đều
 * (CPU bị bóp, instance vừa thức dậy) vẫn phải có status đúng trước khi xử lý request.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class EventStatusRefreshInterceptor implements HandlerInterceptor {

    private final EventStatusTask eventStatusTask;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        if (eventStatusTask.claimIfStale()) {
            try {
                eventStatusTask.autoUpdateEventStatus();
            } catch (Exception e) {
                log.warn("Không cập nhật được trạng thái sự kiện: {}", e.getMessage());
            }
        }
        return true;
    }
}

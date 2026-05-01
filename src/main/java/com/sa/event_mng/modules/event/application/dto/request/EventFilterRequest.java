package com.sa.event_mng.modules.event.application.dto.request;

import lombok.Data;
import org.springframework.format.annotation.DateTimeFormat;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class EventFilterRequest {
    private int page = 1;
    private int size = 10;
    private String keyword;
    private Long categoryId;
    private String province;
    private BigDecimal minPrice;
    private BigDecimal maxPrice;

    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
    private LocalDateTime startDate;

    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
    private LocalDateTime endDate;
}

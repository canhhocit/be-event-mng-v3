package com.sa.event_mng.modules.event.infrastructure.specification;

import com.sa.event_mng.modules.event.domain.model.Event;
import com.sa.event_mng.modules.event.domain.model.TicketType;
import com.sa.event_mng.modules.event.domain.model.EventStatus;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class EventSpecification {

    public static Specification<Event> filterEvents(
            String keyword,
            String province,
            BigDecimal minPrice,
            BigDecimal maxPrice,
            LocalDateTime startDate,
            LocalDateTime endDate,
            List<EventStatus> statuses,
            Long categoryId,
            boolean onlySelling) {
        
        return (root, query, criteriaBuilder) -> {
            List<Predicate> predicates = new ArrayList<>();
            LocalDateTime now = LocalDateTime.now();

            // Lọc theo thời gian bán vé (nếu yêu cầu)
            if (onlySelling) {
                predicates.add(criteriaBuilder.lessThanOrEqualTo(root.get("saleStartDate"), now));
                predicates.add(criteriaBuilder.greaterThanOrEqualTo(root.get("saleEndDate"), now));
            }

            // Lọc theo trạng thái (chỉ lấy sự kiện đang public)
            if (statuses != null && !statuses.isEmpty()) {
                predicates.add(root.get("status").in(statuses));
            }

            // Lọc theo từ khóa (tìm trong tên sự kiện)
            if (keyword != null && !keyword.isBlank()) {
                predicates.add(criteriaBuilder.like(
                        criteriaBuilder.lower(root.get("name")),
                        "%" + keyword.toLowerCase() + "%"
                ));
            }

            // Lọc theo danh mục
            if (categoryId != null) {
                predicates.add(criteriaBuilder.equal(root.get("category").get("id"), categoryId));
            }

            // Lọc theo Tỉnh/Thành phố
            if (province != null && !province.isBlank()) {
                predicates.add(criteriaBuilder.equal(root.get("province"), province));
            }

            // Lọc theo thời gian diễn ra
            if (startDate != null) {
                predicates.add(criteriaBuilder.greaterThanOrEqualTo(root.get("startTime"), startDate));
            }
            if (endDate != null) {
                predicates.add(criteriaBuilder.lessThanOrEqualTo(root.get("endTime"), endDate));
            }

            // Lọc theo khoảng giá (liên kết với bảng TicketType)
            if (minPrice != null || maxPrice != null) {
                Join<Event, TicketType> ticketJoin = root.join("ticketTypes", JoinType.LEFT);
                
                if (minPrice != null) {
                    predicates.add(criteriaBuilder.greaterThanOrEqualTo(ticketJoin.get("price"), minPrice));
                }
                if (maxPrice != null) {
                    predicates.add(criteriaBuilder.lessThanOrEqualTo(ticketJoin.get("price"), maxPrice));
                }
                
                // Đảm bảo không bị duplicate kết quả khi join
                query.distinct(true);
            }

            return criteriaBuilder.and(predicates.toArray(new Predicate[0]));
        };
    }
}

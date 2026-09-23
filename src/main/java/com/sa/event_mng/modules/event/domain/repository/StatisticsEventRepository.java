package com.sa.event_mng.modules.event.domain.repository;

import com.sa.event_mng.modules.event.domain.model.Event;
import com.sa.event_mng.modules.event.domain.model.projection.EventStatusStatsProjection;
import com.sa.event_mng.modules.event.domain.model.projection.EventTemporalStatsProjection;
import com.sa.event_mng.modules.event.domain.model.projection.TopEventQuarterProjection;
import com.sa.event_mng.modules.event.domain.model.projection.EventYearlyOverviewProjection;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface StatisticsEventRepository extends JpaRepository<Event, Long> {

    @Query(value = """
            SELECT status AS status, COUNT(*) AS count
            FROM events
            WHERE EXTRACT(YEAR FROM start_time) = :year
            AND EXTRACT(QUARTER FROM start_time) = :quarter
            GROUP BY status
            """, nativeQuery = true)
    List<EventStatusStatsProjection> findEventStatusStats(@Param("quarter") Long quarter, @Param("year") Long year);

    @Query(value = """
    SELECT EXTRACT(HOUR FROM e.start_time) AS hourOfDay,
        CASE
            WHEN SUM(t.total_quantity) = 0 THEN 0
            ELSE SUM(t.total_quantity - t.remaining_quantity) * 100.0 / SUM(t.total_quantity)
        END AS percentageOfTicketsSold
    FROM events e
    JOIN ticket_types t ON e.id = t.event_id
    WHERE EXTRACT(ISODOW FROM e.start_time) = :dayOfWeek
    GROUP BY EXTRACT(HOUR FROM e.start_time)
    ORDER BY hourOfDay
    """, nativeQuery = true)
    List<EventTemporalStatsProjection> findEventTemporalStats(@Param("dayOfWeek") Integer dayOfWeek);

    @Query(value = """
    SELECT e.name AS eventName,
        SUM(tt.total_quantity - tt.remaining_quantity) AS ticketsSold,
        CASE
            WHEN SUM(tt.total_quantity) = 0 THEN 0
            ELSE SUM(tt.total_quantity - tt.remaining_quantity) * 100.0 / SUM(tt.total_quantity)
        END AS occupancyRate,
        SUM(o.total_amount) AS totalRevenue,
        e.status AS status
    FROM events e
    JOIN ticket_types tt ON e.id = tt.event_id
    LEFT JOIN order_items oi ON oi.ticket_type_id = tt.id
    LEFT JOIN orders o ON o.id = oi.order_id
    WHERE EXTRACT(QUARTER FROM e.start_time) = :quarter AND EXTRACT(YEAR FROM e.start_time) = :year
    AND o.payment_status = 'PAID'
    GROUP BY e.id, e.name, e.status
    ORDER BY totalRevenue DESC, ticketsSold DESC, occupancyRate DESC
    LIMIT 5
    """, nativeQuery = true)
    List<TopEventQuarterProjection> findTop5EventsByQuarter(@Param("quarter") int quarter, @Param("year") int year);

    @Query(value = """
    SELECT
        e.name as eventName,
        e.status as status,
        COALESCE(SUM(oi.quantity), 0) as ticketsSold,
        SUM(tt.total_quantity) as totalQuantity,
        COALESCE(SUM(o.organizer_amount), 0) as organizerAmount
    FROM events e
    JOIN ticket_types tt ON tt.event_id = e.id
    LEFT JOIN order_items oi ON oi.ticket_type_id = tt.id
    LEFT JOIN orders o ON o.id = oi.order_id AND o.payment_status = 'PAID'
    WHERE e.organizer_id = :organizerId
    AND EXTRACT(YEAR FROM e.start_time) = :year
    GROUP BY e.id, e.name, e.status
    ORDER BY e.start_time ASC
    """, nativeQuery = true)
    List<EventYearlyOverviewProjection> findEventYearlyOverview(@Param("organizerId") Long organizerId, @Param("year") int year);
}

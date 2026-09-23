package com.sa.event_mng.modules.ordering.domain.repository;

import com.sa.event_mng.modules.ordering.domain.model.Order;
import com.sa.event_mng.modules.ordering.domain.model.projection.EventRevenueStatsAdminProjection;

import com.sa.event_mng.modules.ordering.domain.model.projection.MonthlyRevenueProjection;
import com.sa.event_mng.modules.ordering.domain.model.projection.MonthlyRevenueOrganizerProjection;
import com.sa.event_mng.modules.ordering.domain.model.projection.OrganizerOverviewProjection;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface StatisticsOrderRepository extends JpaRepository<Order, String> {

//    @Query(value = """
//    SELECT e.name as eventName,
//        SUM(o.total_amount) as totalRevenue,
//        SUM(tt.total_quantity - tt.remaining_quantity) as ticketsSold,
//        CASE
//            WHEN SUM(tt.total_quantity) = 0 THEN 0
//            ELSE SUM(tt.total_quantity - tt.remaining_quantity) * 100.0 / SUM(tt.total_quantity)
//        END AS percentageOfTicketsSold
//    FROM orders o
//    JOIN order_items oi ON o.id = oi.order_id
//    JOIN ticket_types tt ON oi.ticket_type_id = tt.id
//    RIGHT JOIN events e ON e.id = tt.event_id
//    WHERE e.organizer_id = :id
//    AND o.payment_status = 'PAID'
//    GROUP BY e.id
//    """, nativeQuery = true)
//    List<EventRevenueStatsOrganizerProjection> findEventRevenueOrganizerStats(@Param("id") Long id);

    @Query(value = """
    SELECT SUM(service_fee) as totalRevenue
    FROM orders
    WHERE payment_status = 'PAID'
        AND EXTRACT(YEAR FROM paid_at) = :year
    """, nativeQuery = true)
    EventRevenueStatsAdminProjection findEventRevenueAdminStats(@Param("year") int year);

    @Query(value = """
    SELECT EXTRACT(YEAR FROM paid_at) as year, EXTRACT(MONTH FROM paid_at) as month, SUM(service_fee) as revenue
    FROM orders
    WHERE payment_status = 'PAID' AND EXTRACT(YEAR FROM paid_at) = :year
    GROUP BY EXTRACT(YEAR FROM paid_at), EXTRACT(MONTH FROM paid_at)
    ORDER BY month ASC
    """, nativeQuery = true)
    List<MonthlyRevenueProjection> findMonthlyRevenueAdmin(@Param("year") int year);

    @Query(value = """
    SELECT EXTRACT(YEAR FROM o.paid_at) as year, EXTRACT(MONTH FROM o.paid_at) as month, SUM(o.organizer_amount) as revenue
    FROM orders o
    JOIN order_items oi ON o.id = oi.order_id
    JOIN ticket_types tt ON oi.ticket_type_id = tt.id
    JOIN events e ON e.id = tt.event_id
    WHERE o.payment_status = 'PAID'
    AND e.organizer_id = :organizerId
    AND EXTRACT(YEAR FROM o.paid_at) = :year
    GROUP BY EXTRACT(YEAR FROM o.paid_at), EXTRACT(MONTH FROM o.paid_at)
    ORDER BY month ASC
    """, nativeQuery = true)
    List<MonthlyRevenueOrganizerProjection> findMonthlyRevenueOrganizer(@Param("organizerId") Long organizerId, @Param("year") int year);

    @Query(value = """
    SELECT
        SUM(o.organizer_amount) as totalOrganizerAmount,
        SUM(oi.quantity) as totalTicketsSold,
        COUNT(DISTINCT e.id) as totalEvents,
        SUM(o.service_fee) as totalServiceFee
    FROM orders o
    JOIN order_items oi ON o.id = oi.order_id
    JOIN ticket_types tt ON oi.ticket_type_id = tt.id
    JOIN events e ON e.id = tt.event_id
    WHERE o.payment_status = 'PAID'
    AND e.organizer_id = :organizerId
    """, nativeQuery = true)
    OrganizerOverviewProjection findOrganizerOverview(@Param("organizerId") Long organizerId);
}

package com.sa.event_mng.modules.event.application.service;

import com.sa.event_mng.modules.event.application.dto.response.*;
import com.sa.event_mng.modules.event.application.mapper.StatsMapper;
import com.sa.event_mng.modules.event.domain.model.projection.*;
import com.sa.event_mng.modules.event.domain.repository.*;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class StatisticsEventService {

    StatisticsEventRepository statisticsEventRepository;
    StatsMapper statsMapper;

    @PreAuthorize("hasRole('ORGANIZER') and @securityCustom.isCurrentUser(#idOrganizer, authentication)")
    public EventYearlyOverviewResponse getEventYearlyOverview(Long idOrganizer, int year) {
        List<EventYearlyOverviewProjection> projections = statisticsEventRepository.findEventYearlyOverview(idOrganizer, year);
        List<EventYearlyOverviewResponse.EventSummary> events = projections.stream().map(p -> {
            long sold = p.getTicketsSold() != null ? p.getTicketsSold() : 0L;
            long total = p.getTotalQuantity() != null ? p.getTotalQuantity() : 0L;
            double rate = total > 0 ? Math.round(sold * 10000.0 / total) / 100.0 : 0.0;
            return EventYearlyOverviewResponse.EventSummary.builder()
                    .eventName(p.getEventName())
                    .status(p.getStatus())
                    .ticketsSold(sold)
                    .totalQuantity(total)
                    .sellRate(rate)
                    .organizerAmount(p.getOrganizerAmount())
                    .build();
        }).toList();
        return EventYearlyOverviewResponse.builder().year(year).events(events).build();
    }

    @PreAuthorize("hasRole('ADMIN')")
    @Cacheable(value = "event_status_stats", key = "#quarter + '-' + #year")
    public EventStatusStatsResponse getEventStatusStats(Long quarter, Long year) {
        List<EventStatusStatsProjection> eventStatusStatsProjections = statisticsEventRepository.findEventStatusStats(quarter, year);
        long total = eventStatusStatsProjections.stream()
                .mapToLong(EventStatusStatsProjection::getCount)
                .sum();

        List<EventStatusStatsResponse.EventStatusStatsDetail> statusDetails = eventStatusStatsProjections.stream()
                .map(statsMapper::toEventStatusStatsDetail)
                .map(detail -> new EventStatusStatsResponse.EventStatusStatsDetail(
                        detail.getStatus(),
                        total == 0 ? 0.0 : (detail.getCountEvents() * 100.0) / total,
                        detail.getCountEvents()
                ))
                .toList();

        return EventStatusStatsResponse.builder()
                .quarter(quarter)
                .year(year)
                .total(total)
                .eventStatusStatsDetail(statusDetails)
                .build();
    }

    Map<Integer, String> dayOfWeekMap = Map.of(
            1, "Sunday",
            2, "Monday",
            3, "Tuesday",
            4, "Wednesday",
            5, "Thursday",
            6, "Friday",
            7, "Saturday"
    );

    @PreAuthorize("hasRole('ADMIN')")
    @Cacheable(value = "event_temporal_stats", key = "#dayOfWeek")
    public EventTemporalStatsResponse getEventTemporalStats(int dayOfWeek) {
        List<EventTemporalStatsProjection> eventTemporalStatsProjection = statisticsEventRepository.findEventTemporalStats(dayOfWeek);

        Map<Integer, Double> dataByHour = eventTemporalStatsProjection.stream()
                .collect(Collectors.toMap(
                        EventTemporalStatsProjection::getHourOfDay,
                        EventTemporalStatsProjection::getPercentageOfTicketsSold
                ));

        List<EventTemporalStatsResponse.EventTemporalStatsDetail> eventTemporalStatsDetails = IntStream.range(0, 24)
                .mapToObj(h -> new EventTemporalStatsResponse.EventTemporalStatsDetail(
                        h,
                        dataByHour.getOrDefault(h, 0.0)
                ))
                .toList();

        return EventTemporalStatsResponse.builder()
                .day(dayOfWeekMap.get(dayOfWeek))
                .eventTemporalStatsDetail(eventTemporalStatsDetails)
                .build();
    }

    @PreAuthorize("hasRole('ADMIN')")
    @Cacheable(value = "top_events")
    public TopEventQuarterResponse getTop5EventsByQuarter() {
        LocalDate now = LocalDate.now();
        int quarter = (now.getMonthValue() - 1) / 3 + 1;
        int year = now.getYear();
        List<TopEventQuarterResponse.EventItem> events = statisticsEventRepository.findTop5EventsByQuarter(quarter, year).stream()
                .map(p -> TopEventQuarterResponse.EventItem.builder()
                        .eventName(p.getEventName())
                        .ticketsSold(p.getTicketsSold())
                        .occupancyRate(p.getOccupancyRate())
                        .totalRevenue(p.getTotalRevenue())
                        .status(p.getStatus())
                        .build())
                .toList();
        return TopEventQuarterResponse.builder()
                .quarter(quarter)
                .year(year)
                .events(events)
                .build();
    }
}

package com.sa.event_mng.modules.event.application.service;

import com.sa.event_mng.modules.event.application.dto.request.EventFilterRequest;
import com.sa.event_mng.modules.event.application.dto.request.EventRequest;
import com.sa.event_mng.modules.event.application.dto.response.EventResponse;
import com.sa.event_mng.modules.event.application.dto.response.OrganizerStatsResponse;
import com.sa.event_mng.modules.ordering.domain.repository.StatisticsOrderRepository;

import com.sa.event_mng.shared.exception.AppException;
import com.sa.event_mng.shared.exception.ErrorCode;
import com.sa.event_mng.modules.event.application.mapper.EventMapper;
import com.sa.event_mng.modules.event.domain.model.*;
import com.sa.event_mng.modules.event.domain.repository.*;
import com.sa.event_mng.modules.identity.domain.model.User;
import com.sa.event_mng.modules.identity.domain.repository.UserRepository;
import com.sa.event_mng.modules.event.infrastructure.specification.EventSpecification;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import com.sa.event_mng.shared.infrastructure.cloudinary.CloudinaryService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Slf4j
public class EventService {

    EventRepository eventRepository;
    CategoryRepository categoryRepository;
    UserRepository userRepository;
    EventMapper eventMapper;
    TicketTypeRepository ticketTypeRepository;
    StatisticsOrderRepository statisticsOrderRepository;
    CloudinaryService cloudinaryService;
    @Value("${app.file.base-url}")
    @lombok.experimental.NonFinal
    String fileBaseUrl;

    @Transactional
    @PreAuthorize("hasRole('ORGANIZER')")
    public EventResponse create(EventRequest request) {
        log.info("Đang tạo sự kiện mới kèm vé: Name={}", request.getName());
        
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        User organizer = userRepository.findByUsername(username)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_EXISTED));

        Category category = categoryRepository.findById(request.getCategoryId())
                .orElseThrow(() -> new AppException(ErrorCode.CATEGORY_NOT_FOUND));

        validateEventDates(request);

        Event event = Event.builder()
                .name(request.getName())
                .category(category)
                .organizer(organizer)
                .location(request.getLocation())
                .province(request.getProvince())
                .startTime(request.getStartTime())
                .endTime(request.getEndTime())
                .saleStartDate(request.getSaleStartDate())
                .saleEndDate(request.getSaleEndDate())
                .description(request.getDescription())
                .status(request.getStatus() != null ? request.getStatus() : EventStatus.PENDING)
                .build();

        if (request.getFiles() != null && !request.getFiles().isEmpty()) {
            List<EventImage> images = saveImages(request.getFiles(), event);
            event.setImages(images);
        }

        Event savedEvent = eventRepository.save(event);

        if (request.getTicketTypes() != null && !request.getTicketTypes().isEmpty()) {
            request.getTicketTypes().forEach(ttReq -> {
                TicketType tt = TicketType.builder()
                        .event(savedEvent)
                        .name(ttReq.getName())
                        .price(ttReq.getPrice())
                        .totalQuantity(ttReq.getTotalQuantity())
                        .remainingQuantity(ttReq.getTotalQuantity())
                        .description(ttReq.getDescription())
                        .build();
                ticketTypeRepository.save(tt);
            });
        }

        return eventMapper.toEventResponse(savedEvent);
    }

    public Page<EventResponse> getAllPublished(EventFilterRequest filter, PageRequest pageRequest) {
            
            List<EventStatus> activeStatuses = List.of(
                            EventStatus.OPENING
            );
            
            Specification<Event> spec = EventSpecification.filterEvents(
                filter.getKeyword(), filter.getProvince(), filter.getMinPrice(), filter.getMaxPrice(),
                filter.getStartDate(), filter.getEndDate(), activeStatuses, filter.getCategoryId(),
                true
            );

            return eventRepository.findAll(spec, pageRequest).map(eventMapper::toEventResponse);
    }

    public EventResponse getById(Long id) {
        Event event = eventRepository.findById(id)
                .orElseThrow(() -> new AppException(ErrorCode.EVENT_NOT_FOUND));
        return eventMapper.toEventResponse(event);
    }

    @Transactional
    @PreAuthorize("hasRole('ADMIN') or (hasRole('ORGANIZER') and @securityCustom.isOwner(#id, authentication))")
    public EventResponse update(Long id, EventRequest request) {
        Event event = eventRepository.findById(id)
                .orElseThrow(() -> new AppException(ErrorCode.EVENT_NOT_FOUND));
        event.setName(request.getName());
        event.setLocation(request.getLocation());
        event.setProvince(request.getProvince());
        event.setStartTime(request.getStartTime());
        event.setEndTime(request.getEndTime());
        event.setSaleStartDate(request.getSaleStartDate());
        event.setSaleEndDate(request.getSaleEndDate());

        validateEventDates(request);

        event.setDescription(request.getDescription());
        if (request.getStatus() != null)
            event.setStatus(request.getStatus());

        if (request.getCategoryId() != null) {
            Category category = categoryRepository.findById(request.getCategoryId())
                    .orElseThrow(() -> new AppException(ErrorCode.CATEGORY_NOT_FOUND));
            event.setCategory(category);
        }

        if (request.getFiles() != null && !request.getFiles().isEmpty()) {
            List<EventImage> newImages = saveImages(request.getFiles(), event);
            if (event.getImages() == null) {
                event.setImages(new ArrayList<>());
            }
            event.getImages().addAll(newImages);
        }

        return eventMapper.toEventResponse(eventRepository.save(event));
    }

    private void validateEventDates(EventRequest request) {
        if (request.getSaleStartDate() == null || request.getSaleEndDate() == null ||
            request.getStartTime() == null || request.getEndTime() == null) {
            return; // DTO validation will catch this if required
        }

        if (request.getSaleEndDate().isBefore(request.getSaleStartDate().plusHours(12))) {
            throw new AppException(ErrorCode.EVENT_SALE_PERIOD_INVALID);
        }

        if (request.getStartTime().isBefore(request.getSaleEndDate().plusDays(1))) {
            throw new AppException(ErrorCode.EVENT_START_TIME_INVALID);
        }

        if (request.getEndTime().isBefore(request.getStartTime().plusHours(2))) {
            throw new AppException(ErrorCode.EVENT_DURATION_INVALID);
        }
    }

    private List<EventImage> saveImages(List<MultipartFile> files, Event event) {
        List<EventImage> images = new ArrayList<>();
        for (MultipartFile file : files) {
            if (file.isEmpty()) continue;
            String imageUrl = cloudinaryService.uploadFile(file);
            images.add(EventImage.builder().imageUrl(imageUrl).event(event).build());
        }
        return images;
    }

    public Page<EventResponse> getAllForAdmin(String search, String status, PageRequest pageRequest) {
        Page<Event> events;
        boolean hasSearch = search != null && !search.isBlank();
        boolean hasStatus = status != null && !status.isBlank();

        if (hasSearch && hasStatus) {
            events = eventRepository.findByNameContainingIgnoreCaseAndStatus(
                    search, EventStatus.valueOf(status), pageRequest);
        } else if (hasSearch) {
            events = eventRepository.findByNameContainingIgnoreCase(search, pageRequest);
        } else if (hasStatus) {
            events = eventRepository.findByStatus(EventStatus.valueOf(status), pageRequest);
        } else {
            events = eventRepository.findAll(pageRequest);
        }
        return events.map(eventMapper::toEventResponse);
    }

    @Transactional
    @PreAuthorize("hasRole('ADMIN')")
    public EventResponse updateStatus(Long id, EventStatus status) {
        Event event = eventRepository.findById(id)
                .orElseThrow(() -> new AppException(ErrorCode.EVENT_NOT_FOUND));
        
        // Nếu admin duyệt (chuyển sang UPCOMING/OPENING), ta tính toán lại trạng thái chuẩn theo thời gian hiện tại
        if (status == EventStatus.UPCOMING || status == EventStatus.OPENING) {
            event.setStatus(event.calculateStatus(LocalDateTime.now()));
        } else {
            event.setStatus(status);
        }
        
        return eventMapper.toEventResponse(eventRepository.save(event));
    }

    public Page<EventResponse> getMyEvents(PageRequest pageRequest) {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_EXISTED));
        return eventRepository.findByOrganizerId(user.getId(), pageRequest)
                .map(eventMapper::toEventResponse);
    }

    public OrganizerStatsResponse getOrganizerStats() {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        User organizer = userRepository.findByUsername(username)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_EXISTED));

        List<Event> myEvents = eventRepository.findByOrganizerId(organizer.getId());

        List<OrganizerStatsResponse.EventStat> eventStats = new ArrayList<>();
        double totalRev = 0;
        long totalSold = 0;

        for (Event event : myEvents) {
            long sold = event.getTicketTypes().stream()
                    .mapToLong(tt -> tt.getTotalQuantity() - tt.getRemainingQuantity())
                    .sum();

            double rev = event.getTicketTypes().stream()
                    .mapToDouble(tt -> (tt.getTotalQuantity() - tt.getRemainingQuantity()) * tt.getPrice().doubleValue())
                    .sum();

            long totalTickets = event.getTicketTypes().stream()
                    .mapToLong(tt -> tt.getTotalQuantity())
                    .sum();

            eventStats.add(OrganizerStatsResponse.EventStat.builder()
                    .eventId(event.getId())
                    .eventName(event.getName())
                    .totalTickets(totalTickets)
                    .ticketsSold(sold)
                    .revenue(rev)
                    .sellThroughRate(totalTickets > 0 ? (double) sold / totalTickets * 100 : 0)
                    .status(event.getStatus().name())
                    .imageUrl(event.getImages() != null && !event.getImages().isEmpty() ? event.getImages().get(0).getImageUrl() : null)
                    .build());

            totalRev += rev;
            totalSold += sold;
        }

        int currentYear = LocalDate.now().getYear();
        List<com.sa.event_mng.modules.ordering.domain.model.projection.MonthlyRevenueOrganizerProjection> dbStats = 
                statisticsOrderRepository.findMonthlyRevenueOrganizer(organizer.getId(), currentYear);
        
        List<OrganizerStatsResponse.MonthlyRevenue> monthlyRevenues = new ArrayList<>();
        
        for (int m = 1; m <= 12; m++) {
            final int month = m;
            java.math.BigDecimal monthlyRev = dbStats.stream()
                    .filter(p -> p.getMonth() == month)
                    .map(com.sa.event_mng.modules.ordering.domain.model.projection.MonthlyRevenueOrganizerProjection::getRevenue)
                    .findFirst()
                    .orElse(java.math.BigDecimal.ZERO);

            monthlyRevenues.add(new OrganizerStatsResponse.MonthlyRevenue(currentYear, month, monthlyRev));
        }

        return OrganizerStatsResponse.builder()
                .totalEvents(myEvents.size())
                .totalTicketsSold(totalSold)
                .totalRevenue(totalRev)
                .eventStats(eventStats)
                .monthlyRevenues(monthlyRevenues)
                .build();
    }

}

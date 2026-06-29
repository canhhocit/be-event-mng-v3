package com.sa.event_mng.modules.event.application.service;

import com.sa.event_mng.modules.event.application.dto.request.EventRequest;
import com.sa.event_mng.modules.event.application.dto.response.EventResponse;
import com.sa.event_mng.modules.event.application.mapper.EventMapper;
import com.sa.event_mng.modules.event.domain.model.Category;
import com.sa.event_mng.modules.event.domain.model.Event;
import com.sa.event_mng.modules.event.domain.model.EventStatus;
import com.sa.event_mng.modules.event.domain.repository.CategoryRepository;
import com.sa.event_mng.modules.event.domain.repository.EventRepository;
import com.sa.event_mng.modules.event.domain.repository.TicketTypeRepository;
import com.sa.event_mng.modules.ordering.domain.repository.StatisticsOrderRepository;
import com.sa.event_mng.modules.identity.domain.model.User;
import com.sa.event_mng.modules.identity.domain.repository.UserRepository;
import com.sa.event_mng.shared.exception.AppException;
import com.sa.event_mng.shared.exception.ErrorCode;
import com.sa.event_mng.shared.infrastructure.cloudinary.CloudinaryService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class EventServiceTest {

    @Mock EventRepository eventRepository;
    @Mock CategoryRepository categoryRepository;
    @Mock UserRepository userRepository;
    @Mock EventMapper eventMapper;
    @Mock TicketTypeRepository ticketTypeRepository;
    @Mock StatisticsOrderRepository statisticsOrderRepository;
    @Mock CloudinaryService cloudinaryService;

    @InjectMocks EventService eventService;

    @Test
    void getById_shouldReturnEvent_whenExists() {
        Event event = Event.builder().id(1L).name("Concert").build();
        EventResponse expected = EventResponse.builder().id(1L).name("Concert").build();

        when(eventRepository.findById(1L)).thenReturn(Optional.of(event));
        when(eventMapper.toEventResponse(event)).thenReturn(expected);

        EventResponse result = eventService.getById(1L);

        assertEquals(1L, result.getId());
        assertEquals("Concert", result.getName());
    }

    @Test
    void getById_shouldThrow_whenNotFound() {
        when(eventRepository.findById(99L)).thenReturn(Optional.empty());

        AppException ex = assertThrows(AppException.class, () -> eventService.getById(99L));
        assertEquals(ErrorCode.EVENT_NOT_FOUND, ex.getErrorCode());
    }

    @Test
    void validateEventDates_shouldThrow_whenSalePeriodTooShort() {
        LocalDateTime now = LocalDateTime.now();
        EventRequest request = EventRequest.builder()
                .name("Test")
                .categoryId(1L)
                .saleStartDate(now)
                .saleEndDate(now.plusHours(6)) // less than 12h
                .startTime(now.plusDays(10))
                .endTime(now.plusDays(10).plusHours(3))
                .build();

        User organizer = User.builder().username("organizer1").build();
        Category category = Category.builder().id(1L).build();

        when(userRepository.findByUsername(any())).thenReturn(Optional.of(organizer));
        when(categoryRepository.findById(1L)).thenReturn(Optional.of(category));

        org.springframework.security.core.Authentication auth =
                mock(org.springframework.security.core.Authentication.class);
        when(auth.getName()).thenReturn("organizer1");
        org.springframework.security.core.context.SecurityContextHolder.getContext().setAuthentication(auth);

        AppException ex = assertThrows(AppException.class, () -> eventService.create(request));
        assertEquals(ErrorCode.EVENT_SALE_PERIOD_INVALID, ex.getErrorCode());
    }

    @Test
    void validateEventDates_shouldThrow_whenStartTimeNotAfterSaleEnd() {
        LocalDateTime now = LocalDateTime.now();
        EventRequest request = EventRequest.builder()
                .name("Test")
                .categoryId(1L)
                .saleStartDate(now)
                .saleEndDate(now.plusHours(13))
                .startTime(now.plusHours(14)) // less than 1 day after saleEnd
                .endTime(now.plusHours(17))
                .build();

        User organizer = User.builder().username("organizer1").build();
        Category category = Category.builder().id(1L).build();

        when(userRepository.findByUsername(any())).thenReturn(Optional.of(organizer));
        when(categoryRepository.findById(1L)).thenReturn(Optional.of(category));

        org.springframework.security.core.Authentication auth =
                mock(org.springframework.security.core.Authentication.class);
        when(auth.getName()).thenReturn("organizer1");
        org.springframework.security.core.context.SecurityContextHolder.getContext().setAuthentication(auth);

        AppException ex = assertThrows(AppException.class, () -> eventService.create(request));
        assertEquals(ErrorCode.EVENT_START_TIME_INVALID, ex.getErrorCode());
    }

    @Test
    void validateEventDates_shouldThrow_whenEventDurationTooShort() {
        LocalDateTime now = LocalDateTime.now();
        EventRequest request = EventRequest.builder()
                .name("Test")
                .categoryId(1L)
                .saleStartDate(now)
                .saleEndDate(now.plusHours(13))
                .startTime(now.plusDays(2))
                .endTime(now.plusDays(2).plusHours(1)) // less than 2h duration
                .build();

        User organizer = User.builder().username("organizer1").build();
        Category category = Category.builder().id(1L).build();

        when(userRepository.findByUsername(any())).thenReturn(Optional.of(organizer));
        when(categoryRepository.findById(1L)).thenReturn(Optional.of(category));

        org.springframework.security.core.Authentication auth =
                mock(org.springframework.security.core.Authentication.class);
        when(auth.getName()).thenReturn("organizer1");
        org.springframework.security.core.context.SecurityContextHolder.getContext().setAuthentication(auth);

        AppException ex = assertThrows(AppException.class, () -> eventService.create(request));
        assertEquals(ErrorCode.EVENT_DURATION_INVALID, ex.getErrorCode());
    }

    @Test
    void update_shouldThrow_whenEventNotFound() {
        when(eventRepository.findById(99L)).thenReturn(Optional.empty());

        AppException ex = assertThrows(AppException.class,
                () -> eventService.update(99L, EventRequest.builder().build()));
        assertEquals(ErrorCode.EVENT_NOT_FOUND, ex.getErrorCode());
    }

    @Test
    void update_shouldThrow_whenCategoryNotFound() {
        LocalDateTime now = LocalDateTime.now();
        Event event = Event.builder().id(1L).build();
        EventRequest request = EventRequest.builder()
                .name("Test")
                .categoryId(99L)
                .saleStartDate(now)
                .saleEndDate(now.plusHours(13))
                .startTime(now.plusDays(2))
                .endTime(now.plusDays(2).plusHours(3))
                .build();

        when(eventRepository.findById(1L)).thenReturn(Optional.of(event));
        when(categoryRepository.findById(99L)).thenReturn(Optional.empty());

        AppException ex = assertThrows(AppException.class, () -> eventService.update(1L, request));
        assertEquals(ErrorCode.CATEGORY_NOT_FOUND, ex.getErrorCode());
    }
}

package com.sa.event_mng.modules.event.application.service;

import com.sa.event_mng.modules.event.application.dto.request.TicketTypeRequest;
import com.sa.event_mng.modules.event.application.dto.response.TicketTypeResponse;
import com.sa.event_mng.modules.event.application.mapper.TicketTypeMapper;
import com.sa.event_mng.modules.event.domain.model.Event;
import com.sa.event_mng.modules.event.domain.model.TicketType;
import com.sa.event_mng.modules.event.domain.repository.EventRepository;
import com.sa.event_mng.modules.event.domain.repository.TicketTypeRepository;
import com.sa.event_mng.modules.identity.domain.model.User;
import com.sa.event_mng.shared.exception.AppException;
import com.sa.event_mng.shared.exception.ErrorCode;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TicketTypeServiceTest {

    @Mock TicketTypeRepository ticketTypeRepository;
    @Mock EventRepository eventRepository;
    @Mock TicketTypeMapper ticketTypeMapper;

    @InjectMocks TicketTypeService ticketTypeService;

    private void mockAuth(String username, String role) {
        Authentication auth = mock(Authentication.class);
        lenient().when(auth.getName()).thenReturn(username);
        lenient().when(auth.getAuthorities()).thenAnswer(inv ->
                List.of(new SimpleGrantedAuthority("ROLE_" + role)));
        SecurityContextHolder.getContext().setAuthentication(auth);
    }

    @Test
    void create_shouldThrow_whenEventNotFound() {
        TicketTypeRequest request = TicketTypeRequest.builder().eventId(99L).name("VIP").build();
        when(eventRepository.findById(99L)).thenReturn(Optional.empty());

        AppException ex = assertThrows(AppException.class, () -> ticketTypeService.create(request));
        assertEquals(ErrorCode.EVENT_NOT_FOUND, ex.getErrorCode());
    }

    @Test
    void create_shouldThrow_whenOrganizerNotOwner() {
        User organizer = User.builder().username("other_organizer").build();
        Event event = Event.builder().id(1L).organizer(organizer).build();
        TicketTypeRequest request = TicketTypeRequest.builder().eventId(1L).name("VIP").build();

        when(eventRepository.findById(1L)).thenReturn(Optional.of(event));
        mockAuth("current_user", "ORGANIZER");

        AppException ex = assertThrows(AppException.class, () -> ticketTypeService.create(request));
        assertEquals(ErrorCode.UNAUTHORIZED, ex.getErrorCode());
    }

    @Test
    void create_shouldSucceed_whenOrganizerIsOwner() {
        User organizer = User.builder().username("organizer1").build();
        Event event = Event.builder().id(1L).organizer(organizer).build();
        TicketTypeRequest request = TicketTypeRequest.builder()
                .eventId(1L).name("VIP").price(BigDecimal.valueOf(100)).totalQuantity(50).build();
        TicketType savedTicketType = TicketType.builder().id(1L).name("VIP").build();
        TicketTypeResponse expected = TicketTypeResponse.builder().id(1L).name("VIP").build();

        when(eventRepository.findById(1L)).thenReturn(Optional.of(event));
        when(ticketTypeRepository.save(any())).thenReturn(savedTicketType);
        when(ticketTypeMapper.toTicketTypeResponse(savedTicketType)).thenReturn(expected);
        mockAuth("organizer1", "ORGANIZER");

        TicketTypeResponse result = ticketTypeService.create(request);

        assertEquals("VIP", result.getName());
        verify(ticketTypeRepository).save(any());
    }

    @Test
    void create_shouldSucceed_whenAdmin() {
        User organizer = User.builder().username("organizer1").build();
        Event event = Event.builder().id(1L).organizer(organizer).build();
        TicketTypeRequest request = TicketTypeRequest.builder()
                .eventId(1L).name("Standard").price(BigDecimal.valueOf(50)).totalQuantity(100).build();
        TicketType saved = TicketType.builder().id(2L).name("Standard").build();
        TicketTypeResponse expected = TicketTypeResponse.builder().id(2L).name("Standard").build();

        when(eventRepository.findById(1L)).thenReturn(Optional.of(event));
        when(ticketTypeRepository.save(any())).thenReturn(saved);
        when(ticketTypeMapper.toTicketTypeResponse(saved)).thenReturn(expected);
        mockAuth("admin", "ADMIN");

        TicketTypeResponse result = ticketTypeService.create(request);

        assertEquals("Standard", result.getName());
    }

    @Test
    void getByEvent_shouldReturnList() {
        TicketType tt = TicketType.builder().id(1L).name("VIP").build();
        TicketTypeResponse resp = TicketTypeResponse.builder().id(1L).name("VIP").build();

        when(ticketTypeRepository.findByEventId(1L)).thenReturn(List.of(tt));
        when(ticketTypeMapper.toTicketTypeResponse(tt)).thenReturn(resp);

        List<TicketTypeResponse> result = ticketTypeService.getByEvent(1L);

        assertEquals(1, result.size());
        assertEquals("VIP", result.get(0).getName());
    }
}

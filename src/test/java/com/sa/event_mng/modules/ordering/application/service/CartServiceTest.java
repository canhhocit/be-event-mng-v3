package com.sa.event_mng.modules.ordering.application.service;

import com.sa.event_mng.model.enums.CartStatus;
import com.sa.event_mng.modules.event.domain.model.Event;
import com.sa.event_mng.modules.event.domain.model.EventStatus;
import com.sa.event_mng.modules.event.domain.model.TicketType;
import com.sa.event_mng.modules.event.domain.repository.TicketTypeRepository;
import com.sa.event_mng.modules.identity.domain.model.User;
import com.sa.event_mng.modules.identity.domain.repository.UserRepository;
import com.sa.event_mng.modules.ordering.application.dto.request.CartItemRequest;
import com.sa.event_mng.modules.ordering.application.dto.response.CartResponse;
import com.sa.event_mng.modules.ordering.application.mapper.CartMapper;
import com.sa.event_mng.modules.ordering.domain.model.Cart;
import com.sa.event_mng.modules.ordering.domain.repository.CartRepository;
import com.sa.event_mng.shared.exception.AppException;
import com.sa.event_mng.shared.exception.ErrorCode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CartServiceTest {

    @Mock CartRepository cartRepository;
    @Mock UserRepository userRepository;
    @Mock TicketTypeRepository ticketTypeRepository;
    @Mock CartMapper cartMapper;

    @InjectMocks CartService cartService;

    private User mockUser;

    @BeforeEach
    void setUp() {
        mockUser = User.builder().id(1L).username("customer1").build();
        Authentication auth = mock(Authentication.class);
        when(auth.getName()).thenReturn("customer1");
        SecurityContextHolder.getContext().setAuthentication(auth);
        when(userRepository.findByUsername("customer1")).thenReturn(Optional.of(mockUser));
    }

    @Test
    void addToCart_shouldThrow_whenTicketTypeNotFound() {
        CartItemRequest request = new CartItemRequest(99L, 2);
        when(cartRepository.findByCustomerId(1L)).thenReturn(Optional.of(
                Cart.builder().customer(mockUser).status(CartStatus.ACTIVE).items(new ArrayList<>()).build()));
        when(ticketTypeRepository.findById(99L)).thenReturn(Optional.empty());

        AppException ex = assertThrows(AppException.class, () -> cartService.addToCart(request));
        assertEquals(ErrorCode.TICKET_TYPE_NOT_FOUND, ex.getErrorCode());
    }

    @Test
    void addToCart_shouldThrow_whenEventNotOpening() {
        Event event = Event.builder().id(1L).status(EventStatus.PENDING).build();
        TicketType ticketType = TicketType.builder().id(1L).event(event).remainingQuantity(100).build();
        CartItemRequest request = new CartItemRequest(1L, 2);

        when(cartRepository.findByCustomerId(1L)).thenReturn(Optional.of(
                Cart.builder().customer(mockUser).status(CartStatus.ACTIVE).items(new ArrayList<>()).build()));
        when(ticketTypeRepository.findById(1L)).thenReturn(Optional.of(ticketType));

        AppException ex = assertThrows(AppException.class, () -> cartService.addToCart(request));
        assertEquals(ErrorCode.EVENT_NOT_OPENING, ex.getErrorCode());
    }

    @Test
    void addToCart_shouldThrow_whenNotEnoughStock() {
        Event event = Event.builder().id(1L).status(EventStatus.OPENING).build();
        TicketType ticketType = TicketType.builder().id(1L).event(event)
                .price(BigDecimal.valueOf(100)).remainingQuantity(1).build();
        CartItemRequest request = new CartItemRequest(1L, 5); // request more than stock

        when(cartRepository.findByCustomerId(1L)).thenReturn(Optional.of(
                Cart.builder().customer(mockUser).status(CartStatus.ACTIVE).items(new ArrayList<>()).build()));
        when(ticketTypeRepository.findById(1L)).thenReturn(Optional.of(ticketType));

        AppException ex = assertThrows(AppException.class, () -> cartService.addToCart(request));
        assertEquals(ErrorCode.TICKET_NOT_ENOUGH, ex.getErrorCode());
    }

    @Test
    void addToCart_shouldSucceed_whenValidRequest() {
        Event event = Event.builder().id(1L).status(EventStatus.OPENING).build();
        TicketType ticketType = TicketType.builder().id(1L).event(event)
                .price(BigDecimal.valueOf(100)).remainingQuantity(10).build();
        CartItemRequest request = new CartItemRequest(1L, 2);
        Cart cart = Cart.builder().customer(mockUser).status(CartStatus.ACTIVE).items(new ArrayList<>()).build();
        CartResponse expected = CartResponse.builder().build();

        when(cartRepository.findByCustomerId(1L)).thenReturn(Optional.of(cart));
        when(ticketTypeRepository.findById(1L)).thenReturn(Optional.of(ticketType));
        when(cartRepository.save(any())).thenReturn(cart);
        when(cartMapper.toCartResponse(cart)).thenReturn(expected);

        CartResponse result = cartService.addToCart(request);

        assertNotNull(result);
        verify(cartRepository).save(cart);
    }

    @Test
    void updateQuantity_shouldThrow_whenNotEnoughStock() {
        Event event = Event.builder().id(1L).status(EventStatus.OPENING).build();
        TicketType ticketType = TicketType.builder().id(1L).event(event).remainingQuantity(3).build();
        com.sa.event_mng.modules.ordering.domain.model.CartItem item =
                com.sa.event_mng.modules.ordering.domain.model.CartItem.builder()
                        .id(1L).ticketType(ticketType).quantity(1).unitPrice(BigDecimal.valueOf(100)).build();
        Cart cart = Cart.builder().customer(mockUser).status(CartStatus.ACTIVE)
                .items(new ArrayList<>(java.util.List.of(item))).build();

        when(cartRepository.findByCustomerId(1L)).thenReturn(Optional.of(cart));

        AppException ex = assertThrows(AppException.class, () -> cartService.updateQuantity(1L, 10));
        assertEquals(ErrorCode.TICKET_NOT_ENOUGH, ex.getErrorCode());
    }

    @Test
    void getMyCart_shouldReturnEmptyCart_whenNoCartExists() {
        when(cartRepository.findByCustomerId(1L)).thenReturn(Optional.empty());
        CartResponse expected = CartResponse.builder().build();
        when(cartMapper.toCartResponse(any())).thenReturn(expected);

        CartResponse result = cartService.getMyCart();

        assertNotNull(result);
    }
}

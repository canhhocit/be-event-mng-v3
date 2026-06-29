package com.sa.event_mng.modules.marketing.application.service;

import com.sa.event_mng.modules.event.domain.model.Event;
import com.sa.event_mng.modules.event.domain.repository.EventRepository;
import com.sa.event_mng.modules.identity.domain.model.Role;
import com.sa.event_mng.modules.identity.domain.model.User;
import com.sa.event_mng.modules.identity.domain.repository.UserRepository;
import com.sa.event_mng.modules.marketing.application.dto.request.VoucherRequest;
import com.sa.event_mng.modules.marketing.application.mapper.VoucherMapper;
import com.sa.event_mng.modules.marketing.domain.model.Voucher;
import com.sa.event_mng.modules.marketing.domain.repository.VoucherRepository;
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
import java.time.LocalDateTime;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class VoucherServiceTest {

    @Mock VoucherRepository voucherRepository;
    @Mock EventRepository eventRepository;
    @Mock UserRepository userRepository;
    @Mock VoucherMapper voucherMapper;

    @InjectMocks VoucherService voucherService;

    private User organizer;

    @BeforeEach
    void setUp() {
        organizer = User.builder().id(1L).username("organizer1")
                .roles(Set.of(Role.builder().name("ORGANIZER").build())).build();

        Authentication auth = mock(Authentication.class);
        lenient().when(auth.getName()).thenReturn("organizer1");
        SecurityContextHolder.getContext().setAuthentication(auth);
        lenient().when(userRepository.findByUsername("organizer1")).thenReturn(Optional.of(organizer));
    }

    @Test
    void createVoucher_shouldThrow_whenCodeAlreadyExists() {
        VoucherRequest request = VoucherRequest.builder()
                .code("SALE10")
                .startDate(LocalDateTime.now())
                .endDate(LocalDateTime.now().plusDays(1))
                .build();

        when(voucherRepository.findByCode("SALE10")).thenReturn(Optional.of(new Voucher()));

        AppException ex = assertThrows(AppException.class, () -> voucherService.createVoucher(request));
        assertEquals(ErrorCode.VOUCHER_EXISTED, ex.getErrorCode());
    }

    @Test
    void createVoucher_shouldThrow_whenDateRangeInvalid() {
        LocalDateTime now = LocalDateTime.now();
        VoucherRequest request = VoucherRequest.builder()
                .code("SALE10")
                .startDate(now)
                .endDate(now.plusMinutes(30)) // less than 1 hour
                .build();

        when(voucherRepository.findByCode("SALE10")).thenReturn(Optional.empty());

        AppException ex = assertThrows(AppException.class, () -> voucherService.createVoucher(request));
        assertEquals(ErrorCode.VOUCHER_DATE_INVALID, ex.getErrorCode());
    }

    @Test
    void createVoucher_shouldThrow_whenOrganizerNotOwnerOfEvent() {
        User otherOrganizer = User.builder().id(99L).username("other").build();
        Event event = Event.builder().id(1L).organizer(otherOrganizer).build();
        LocalDateTime now = LocalDateTime.now();
        VoucherRequest request = VoucherRequest.builder()
                .code("SALE10").eventId(1L)
                .startDate(now).endDate(now.plusHours(2))
                .build();

        when(voucherRepository.findByCode("SALE10")).thenReturn(Optional.empty());
        when(eventRepository.findById(1L)).thenReturn(Optional.of(event));
        when(voucherMapper.toVoucher(any())).thenReturn(new Voucher());

        AppException ex = assertThrows(AppException.class, () -> voucherService.createVoucher(request));
        assertEquals(ErrorCode.VOUCHER_NOT_ALLOWED, ex.getErrorCode());
    }

    @Test
    void calculateDiscount_shouldThrow_whenVoucherExpired() {
        Voucher voucher = Voucher.builder()
                .code("OLD10")
                .startDate(LocalDateTime.now().minusDays(5))
                .endDate(LocalDateTime.now().minusDays(1)) // expired
                .discountType("PERCENTAGE")
                .amount(BigDecimal.valueOf(10))
                .build();

        when(voucherRepository.findByCode("OLD10")).thenReturn(Optional.of(voucher));

        AppException ex = assertThrows(AppException.class,
                () -> voucherService.calculateDiscount("OLD10", Map.of(1L, 500.0)));
        assertEquals(ErrorCode.VOUCHER_EXPIRED, ex.getErrorCode());
    }

    @Test
    void calculateDiscount_shouldThrow_whenVoucherNotYetActive() {
        Voucher voucher = Voucher.builder()
                .code("FUTURE10")
                .startDate(LocalDateTime.now().plusDays(1)) // not started
                .endDate(LocalDateTime.now().plusDays(5))
                .discountType("PERCENTAGE")
                .amount(BigDecimal.valueOf(10))
                .build();

        when(voucherRepository.findByCode("FUTURE10")).thenReturn(Optional.of(voucher));

        AppException ex = assertThrows(AppException.class,
                () -> voucherService.calculateDiscount("FUTURE10", Map.of(1L, 500.0)));
        assertEquals(ErrorCode.VOUCHER_NOT_ACTIVE, ex.getErrorCode());
    }

    @Test
    void calculateDiscount_shouldThrow_whenMinAmountNotMet() {
        Voucher voucher = Voucher.builder()
                .code("MIN100")
                .startDate(LocalDateTime.now().minusDays(1))
                .endDate(LocalDateTime.now().plusDays(5))
                .discountType("PERCENTAGE")
                .amount(BigDecimal.valueOf(10))
                .minOrderAmount(BigDecimal.valueOf(500))
                .build();

        when(voucherRepository.findByCode("MIN100")).thenReturn(Optional.of(voucher));

        AppException ex = assertThrows(AppException.class,
                () -> voucherService.calculateDiscount("MIN100", Map.of(1L, 100.0)));
        assertEquals(ErrorCode.VOUCHER_MIN_AMOUNT_NOT_MET, ex.getErrorCode());
    }

    @Test
    void calculateDiscount_shouldThrow_whenOutOfStock() {
        Voucher voucher = Voucher.builder()
                .code("GONE")
                .startDate(LocalDateTime.now().minusDays(1))
                .endDate(LocalDateTime.now().plusDays(5))
                .discountType("AMOUNT")
                .amount(BigDecimal.valueOf(50))
                .quantity(0)
                .build();

        when(voucherRepository.findByCode("GONE")).thenReturn(Optional.of(voucher));

        AppException ex = assertThrows(AppException.class,
                () -> voucherService.calculateDiscount("GONE", Map.of(1L, 500.0)));
        assertEquals(ErrorCode.VOUCHER_OUT_OF_STOCK, ex.getErrorCode());
    }

    @Test
    void deleteVoucher_shouldThrow_whenVoucherNotFound() {
        when(voucherRepository.findById(99L)).thenReturn(Optional.empty());

        AppException ex = assertThrows(AppException.class, () -> voucherService.deleteVoucher(99L));
        assertEquals(ErrorCode.VOUCHER_NOT_FOUND, ex.getErrorCode());
    }

    @Test
    void deleteVoucher_shouldThrow_whenNotOwner() {
        User otherUser = User.builder().id(99L).username("other").build();
        Voucher voucher = Voucher.builder().id(1L).creator(otherUser).build();

        when(voucherRepository.findById(1L)).thenReturn(Optional.of(voucher));

        AppException ex = assertThrows(AppException.class, () -> voucherService.deleteVoucher(1L));
        assertEquals(ErrorCode.UNAUTHORIZED, ex.getErrorCode());
    }
}

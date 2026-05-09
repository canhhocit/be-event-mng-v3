package com.sa.event_mng.modules.ticketing.application.service;

import com.sa.event_mng.modules.ticketing.application.dto.response.TicketResponse;
import com.sa.event_mng.shared.exception.AppException;
import com.sa.event_mng.shared.exception.ErrorCode;
import com.sa.event_mng.modules.ticketing.application.mapper.TicketMapper;
import com.sa.event_mng.modules.ticketing.domain.model.Ticket;
import com.sa.event_mng.modules.identity.domain.model.User;
import com.sa.event_mng.model.enums.TicketStatus;
import com.sa.event_mng.modules.ticketing.domain.repository.TicketRepository;
import com.sa.event_mng.modules.identity.domain.repository.UserRepository;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Dịch vụ xử lý logic liên quan đến vé (Ticket).
 * Controller gọi vào service này để thực hiện các nghiệp vụ như xem vé của user hoặc check-in vé.
 */
@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class TicketService {

    TicketRepository ticketRepository;
    UserRepository userRepository;
    TicketMapper ticketMapper;

    /**
     * Lấy danh sách vé đã mua của user đang đăng nhập.
     */
    public List<TicketResponse> getMyTickets() {
        User user = getCurrentUser();
        return ticketRepository.findAll().stream()
                // Lọc ra các vé thuộc đơn hàng của user hiện tại
                .filter(t -> t.getOrder().getCustomer().getId().equals(user.getId()))
                // Chuyển entity Ticket sang DTO trả về client
                .map(ticketMapper::toTicketResponse)
                .toList();
    }

    /**
     * Check-in vé dựa trên mã vé ticketCode.
     * Chỉ ADMIN, ORGANIZER hoặc STAFF mới có thể gọi.
     */
    @Transactional
    @PreAuthorize("hasRole('ADMIN') or hasRole('ORGANIZER') or hasRole('STAFF')")
    public TicketResponse checkIn(String ticketCode) {
        Ticket ticket = ticketRepository.findByTicketCode(ticketCode)
                .orElseThrow(() -> new AppException(ErrorCode.TICKET_INVALID));

        User user = getCurrentUser();
        boolean isAdmin = SecurityContextHolder.getContext().getAuthentication().getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));
        boolean isStaff = SecurityContextHolder.getContext().getAuthentication().getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_STAFF"));

        Long eventOrganizerId = ticket.getTicketType().getEvent().getOrganizer().getId();

        // Nếu không phải admin thì kiểm tra quyền dựa trên vai trò
        if (!isAdmin) {
            if (isStaff) {
                // Staff chỉ được check-in vé của ban tổ chức mình
                if (user.getOrganizer() == null || !user.getOrganizer().getId().equals(eventOrganizerId)) {
                    throw new AppException(ErrorCode.UNAUTHORIZED);
                }
            } else {
                // Organizer chỉ được check-in vé của chính mình
                if (!user.getId().equals(eventOrganizerId)) {
                    throw new AppException(ErrorCode.TICKET_NOT_OWNED);
                }
            }
        }

        if (ticket.getStatus() == TicketStatus.USED) {
            throw new AppException(ErrorCode.TICKET_USED);
        }

        if (ticket.getStatus() != TicketStatus.VALID) {
            throw new AppException(ErrorCode.TICKET_INVALID);
        }

        ticket.setStatus(TicketStatus.USED);
        ticket.setUsedAt(LocalDateTime.now());

        // Lưu lại trạng thái vé đã sử dụng và trả về thông tin vé
        return ticketMapper.toTicketResponse(ticketRepository.save(ticket));
    }

    /**
     * Lấy thông tin user hiện tại từ Spring Security.
     */
    private User getCurrentUser() {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_EXISTED));
    }

}

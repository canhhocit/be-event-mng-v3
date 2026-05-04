package com.sa.event_mng.modules.ordering.application.service;

import com.sa.event_mng.modules.event.domain.model.Event;
import com.sa.event_mng.modules.event.domain.model.EventStatus;
import com.sa.event_mng.modules.event.domain.model.TicketType;
import com.sa.event_mng.modules.event.domain.repository.TicketTypeRepository;
import com.sa.event_mng.modules.identity.domain.model.User;
import com.sa.event_mng.modules.identity.domain.repository.UserRepository;
import com.sa.event_mng.modules.marketing.application.service.VoucherService;
import com.sa.event_mng.modules.ordering.application.dto.response.OrderResponse;
import com.sa.event_mng.modules.ordering.application.mapper.OrderMapper;
import com.sa.event_mng.modules.ordering.domain.model.Cart;
import com.sa.event_mng.modules.ordering.domain.model.CartItem;
import com.sa.event_mng.modules.ordering.domain.model.Order;
import com.sa.event_mng.modules.ordering.domain.model.OrderItem;
import com.sa.event_mng.modules.ordering.domain.repository.CartRepository;
import com.sa.event_mng.modules.ordering.domain.repository.OrderRepository;
import com.sa.event_mng.modules.ticketing.domain.model.Ticket;
import com.sa.event_mng.modules.ticketing.domain.repository.TicketRepository;
import com.sa.event_mng.model.enums.OrderStatus;
import com.sa.event_mng.model.enums.PaymentMethod;
import com.sa.event_mng.model.enums.PaymentStatus;
import com.sa.event_mng.model.enums.TicketStatus;
import com.sa.event_mng.shared.exception.AppException;
import com.sa.event_mng.shared.exception.ErrorCode;
import com.sa.event_mng.shared.infrastructure.email.EmailService;
import com.sa.event_mng.shared.infrastructure.pdf.PdfService;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@lombok.extern.slf4j.Slf4j
public class OrderService {

    private final OrderRepository orderRepository;
    private final CartRepository cartRepository;
    private final UserRepository userRepository;
    private final TicketRepository ticketRepository;
    private final TicketTypeRepository ticketTypeRepository;
    private final OrderMapper orderMapper;
    private final EmailService emailService;
    private final VoucherService voucherService;
    private final PdfService pdfService;
    private final PaymentService paymentService;

    public OrderService(OrderRepository orderRepository, 
                        CartRepository cartRepository, 
                        UserRepository userRepository, 
                        TicketRepository ticketRepository, 
                        TicketTypeRepository ticketTypeRepository, 
                        OrderMapper orderMapper, 
                        EmailService emailService, 
                        VoucherService voucherService, 
                        PdfService pdfService, 
                        PaymentService paymentService) {
        this.orderRepository = orderRepository;
        this.cartRepository = cartRepository;
        this.userRepository = userRepository;
        this.ticketRepository = ticketRepository;
        this.ticketTypeRepository = ticketTypeRepository;
        this.orderMapper = orderMapper;
        this.emailService = emailService;
        this.voucherService = voucherService;
        this.pdfService = pdfService;
        this.paymentService = paymentService;
    }

    private User getCurrentUser() {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_EXISTED));
    }

    @Transactional
    public OrderResponse checkout(PaymentMethod paymentMethod, String voucherCode, String platform) {
        User user = getCurrentUser();
        Cart cart = cartRepository.findByCustomerId(user.getId())
                .orElseThrow(() -> new AppException(ErrorCode.CART_EMPTY));

        if (cart.getItems().isEmpty()) {
            throw new AppException(ErrorCode.CART_EMPTY);
        }

        return createOrderFromItems(user, cart.getItems(), paymentMethod, cart, voucherCode, platform);
    }

    @Transactional
    public OrderResponse checkoutSelected(List<Long> itemIds, PaymentMethod paymentMethod, String voucherCode, String platform) {
        User user = getCurrentUser();
        Cart cart = cartRepository.findByCustomerId(user.getId())
                .orElseThrow(() -> new AppException(ErrorCode.CART_EMPTY));

        List<CartItem> selectedItems = new ArrayList<>();
        for (CartItem item : cart.getItems()) {
            if (itemIds.contains(item.getId())) {
                selectedItems.add(item);
            }
        }

        if (selectedItems.isEmpty()) {
            throw new AppException(ErrorCode.CART_EMPTY);
        }

        return createOrderFromItems(user, selectedItems, paymentMethod, cart, voucherCode, platform);
    }

    private OrderResponse createOrderFromItems(User user, List<CartItem> items, PaymentMethod paymentMethod, Cart cart, String voucherCode, String platform) {
        BigDecimal subTotal = BigDecimal.ZERO;
        for (CartItem item : items) {
            subTotal = subTotal.add(item.getSubtotal());
            Event event = item.getTicketType().getEvent();
            if (event.getStatus() != EventStatus.OPENING) {
                throw new AppException(ErrorCode.EVENT_NOT_OPENING);
            }
            if (item.getTicketType().getRemainingQuantity() < item.getQuantity()) {
                throw new AppException(ErrorCode.TICKET_NOT_ENOUGH);
            }
        }

        BigDecimal discountAmount = BigDecimal.ZERO;
        if (voucherCode != null && !voucherCode.isBlank()) {
            Map<Long, Double> eventAmounts = items.stream()
                    .collect(Collectors.groupingBy(
                            item -> item.getTicketType().getEvent().getId(),
                            Collectors.summingDouble(item -> item.getSubtotal().doubleValue())
                    ));
            Double discount = voucherService.calculateDiscount(voucherCode, eventAmounts);
            discountAmount = BigDecimal.valueOf(discount);
        }

        BigDecimal totalAmount = subTotal.subtract(discountAmount);
        BigDecimal platformFeeRate = new BigDecimal("0.25");
        BigDecimal serviceFee = totalAmount.multiply(platformFeeRate);
        BigDecimal organizerAmount = totalAmount.subtract(serviceFee);

        Order order = Order.builder()
                .customer(user)
                .orderCode(System.currentTimeMillis() % 1000000000L)
                .organizerAmount(organizerAmount)
                .serviceFee(serviceFee)
                .platformFeeRate(platformFeeRate.floatValue())
                .totalAmount(totalAmount)
                .discountAmount(discountAmount)
                .voucherCode(voucherCode)
                .paymentMethod(paymentMethod)
                .paymentStatus(PaymentStatus.PENDING)
                .orderStatus(OrderStatus.PENDING)
                .orderDate(LocalDateTime.now())
                .build();

        List<OrderItem> orderItems = new ArrayList<>();
        for (CartItem cartItem : items) {
            OrderItem orderItem = OrderItem.builder()
                    .order(order)
                    .ticketType(cartItem.getTicketType())
                    .quantity(cartItem.getQuantity())
                    .unitPrice(cartItem.getUnitPrice())
                    .subtotal(cartItem.getSubtotal())
                    .build();
            orderItems.add(orderItem);
        }

        order.setItems(orderItems);
        Order savedOrder = orderRepository.save(order);
        OrderResponse response = orderMapper.toOrderResponse(savedOrder);

        // Simulation for MoMo: Complete immediately
        if (paymentMethod == PaymentMethod.MOMO) {
            this.completePayment(savedOrder.getId());
            response.setPaymentUrl("/payment/success?orderCode=" + savedOrder.getOrderCode());
        }

        if (paymentMethod == PaymentMethod.PAYOS) {
            try {
                String paymentUrl = paymentService.createPayOSPaymentLink(savedOrder, platform);
                response.setPaymentUrl(paymentUrl);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        if (voucherCode != null && !voucherCode.isBlank()) {
            voucherService.applyVoucher(voucherCode);
        }

        cart.getItems().removeAll(items);
        cartRepository.save(cart);

        return response;
    }

    @Transactional
    public void completePayment(String orderId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new AppException(ErrorCode.ORDER_NOT_FOUND));
        processCompletion(order);
    }

    @Transactional
    public void completePaymentByOrderCode(Long orderCode) {
        Order order = orderRepository.findByOrderCode(orderCode)
                .orElseThrow(() -> new AppException(ErrorCode.ORDER_NOT_FOUND));
        processCompletion(order);
    }

    @Transactional
    public void cancelPaymentByOrderCode(Long orderCode) {
        Order order = orderRepository.findByOrderCode(orderCode)
                .orElseThrow(() -> new AppException(ErrorCode.ORDER_NOT_FOUND));

        if (order.getPaymentStatus() == PaymentStatus.PAID) {
            return;
        }

        if (order.getPaymentStatus() == PaymentStatus.FAILED && order.getOrderStatus() == OrderStatus.CANCELLED) {
            return;
        }

        User customer = order.getCustomer();
        Cart cart = cartRepository.findByCustomerId(customer.getId())
                .orElseGet(() -> {
                    Cart newCart = new Cart();
                    newCart.setCustomer(customer);
                    newCart.setItems(new ArrayList<>());
                    return cartRepository.save(newCart);
                });

        if (cart.getItems() == null) {
            cart.setItems(new ArrayList<>());
        }

        for (OrderItem orderItem : order.getItems()) {
            CartItem existingItem = null;
            for (CartItem cartItem : cart.getItems()) {
                if (cartItem.getTicketType().getId().equals(orderItem.getTicketType().getId())) {
                    existingItem = cartItem;
                    break;
                }
            }

            if (existingItem != null) {
                int newQuantity = existingItem.getQuantity() + orderItem.getQuantity();
                existingItem.setQuantity(newQuantity);
                existingItem.setSubtotal(existingItem.getUnitPrice().multiply(new BigDecimal(newQuantity)));
            } else {
                CartItem newItem = new CartItem();
                newItem.setCart(cart);
                newItem.setTicketType(orderItem.getTicketType());
                newItem.setQuantity(orderItem.getQuantity());
                newItem.setUnitPrice(orderItem.getUnitPrice());
                newItem.setSubtotal(orderItem.getUnitPrice().multiply(new BigDecimal(orderItem.getQuantity())));
                cart.getItems().add(newItem);
            }
        }

        cartRepository.save(cart);
        order.setPaymentStatus(PaymentStatus.FAILED);
        order.setOrderStatus(OrderStatus.CANCELLED);
        orderRepository.save(order);
    }

    private void processCompletion(Order order) {
        log.info("DEBUG: [ORDER_PROCESS] Starting completion for OrderID: {}, OrderCode: {}", order.getId(), order.getOrderCode());
        
        // Tránh xử lý lặp nếu đã PAID
        if (order.getPaymentStatus() == PaymentStatus.PAID) {
            log.warn("DEBUG: [ORDER_PROCESS] Order {} is ALREADY PAID. Skipping.", order.getOrderCode());
            return;
        }

        log.info("DEBUG: [ORDER_PROCESS] Order status: {}, Customer Email: {}", order.getPaymentStatus(), order.getCustomer() != null ? order.getCustomer().getEmail() : "NULL");

        try {
            order.setPaymentStatus(PaymentStatus.PAID);
            order.setOrderStatus(OrderStatus.CONFIRMED);
            order.setPaidAt(LocalDateTime.now());
            if (order.getTickets() == null) order.setTickets(new ArrayList<>());
            
            for (OrderItem item : order.getItems()) {
                TicketType tt = item.getTicketType();
                if (tt.getRemainingQuantity() < item.getQuantity()) {
                    throw new AppException(ErrorCode.TICKET_NOT_ENOUGH);
                }
                tt.setRemainingQuantity(tt.getRemainingQuantity() - item.getQuantity());
                ticketTypeRepository.save(tt);

                for (int i = 0; i < item.getQuantity(); i++) {
                    String ticketCode = "TKT-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
                    Ticket ticket = Ticket.builder()
                            .order(order)
                            .ticketType(tt)
                            .ticketCode(ticketCode)
                            .qrCode("https://api.qrserver.com/v1/create-qr-code/?size=150x150&data=" + ticketCode)
                            .status(TicketStatus.VALID)
                            .build();
                    ticketRepository.save(ticket);
                    order.getTickets().add(ticket);
                }
            }
            orderRepository.save(order);

            // Fetch order with Customer to avoid LazyInitializationException
            Order fullOrder = orderRepository.findByIdWithCustomer(order.getId()).orElse(order);
            
            // Đảm bảo nạp Tickets (do đang trong Transaction nên có thể gọi trực tiếp)
            if (fullOrder.getTickets() != null) fullOrder.getTickets().size(); 
            
            if (fullOrder.getCustomer().getEmail() != null) {
                try {
                    byte[] pdfBytes = pdfService.generateOrderInvoice(fullOrder);
                    emailService.sendOrderConfirmationWithInvoice(fullOrder.getCustomer().getEmail(), fullOrder, pdfBytes);
                } catch (Exception e) {
                    log.error("Lỗi khi tạo PDF hoặc gửi mail: {}", e.getMessage());
                    e.printStackTrace();
                }
            }
        } catch (AppException ae) {
            throw ae;
        } catch (Exception e) {
            e.printStackTrace();
            throw new AppException(ErrorCode.UNCATEGORIZED_EXCEPTION);
        }
    }

    public byte[] getOrderInvoice(String orderId) {
        User user = getCurrentUser();
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new AppException(ErrorCode.ORDER_NOT_FOUND));
        
        if (!order.getCustomer().getId().equals(user.getId())) {
            throw new AppException(ErrorCode.UNCATEGORIZED_EXCEPTION);
        }
        return pdfService.generateOrderInvoice(order);
    }

    public Page<OrderResponse> getMyOrders(PageRequest pageRequest) {
        User user = getCurrentUser();
        Page<Order> orderPage = orderRepository.findByCustomerId(user.getId(), pageRequest);
        return orderPage.map(order -> {
            OrderResponse response = orderMapper.toOrderResponse(order);
            // Nếu là PENDING và dùng PAYOS, ta tạo link mới để khách có thể tiếp tục thanh toán
            if (order.getPaymentStatus() == PaymentStatus.PENDING && order.getPaymentMethod() == PaymentMethod.PAYOS) {
                try {
                    response.setPaymentUrl(paymentService.createPayOSPaymentLink(order, "web"));
                } catch (Exception e) {
                    log.error("Không thể tạo lại link thanh toán cho đơn hàng {}: {}", order.getOrderCode(), e.getMessage());
                }
            }
            return response;
        });
    }

    @Transactional
    public void cancelAndRestoreCart(Long orderCode) {
        User user = getCurrentUser();
        Order order = orderRepository.findByOrderCode(orderCode)
                .orElseThrow(() -> new AppException(ErrorCode.ORDER_NOT_FOUND));

        if (!order.getCustomer().getId().equals(user.getId())) {
            throw new AppException(ErrorCode.UNCATEGORIZED_EXCEPTION);
        }

        if (order.getOrderStatus() == OrderStatus.CANCELLED) {
            return;
        }

        if (order.getPaymentStatus() != PaymentStatus.PENDING) {
            return;
        }

        Cart cart = cartRepository.findByCustomerId(user.getId())
                .orElseGet(() -> {
                    Cart newCart = new Cart();
                    newCart.setCustomer(user);
                    newCart.setItems(new ArrayList<>());
                    return cartRepository.save(newCart);
                });

        if (cart.getItems() == null) {
            cart.setItems(new ArrayList<>());
        }

        for (OrderItem orderItem : order.getItems()) {
            CartItem existingItem = null;
            for (CartItem ci : cart.getItems()) {
                if (ci.getTicketType().getId().equals(orderItem.getTicketType().getId())) {
                    existingItem = ci;
                    break;
                }
            }

            if (existingItem != null) {
                int newQuantity = existingItem.getQuantity() + orderItem.getQuantity();
                existingItem.setQuantity(newQuantity);
                BigDecimal unitPrice = existingItem.getUnitPrice();
                existingItem.setSubtotal(unitPrice.multiply(new BigDecimal(newQuantity)));
            } else {
                BigDecimal unitPrice = orderItem.getUnitPrice();
                CartItem newItem = new CartItem();
                newItem.setCart(cart);
                newItem.setTicketType(orderItem.getTicketType());
                newItem.setQuantity(orderItem.getQuantity());
                newItem.setUnitPrice(unitPrice);
                newItem.setSubtotal(unitPrice.multiply(new BigDecimal(orderItem.getQuantity())));
                cart.getItems().add(newItem);
            }
        }

        cartRepository.save(cart);
        order.setOrderStatus(OrderStatus.CANCELLED);
        orderRepository.save(order);
        System.out.println("DEBUG: Order #" + orderCode + " cancelled and cart restored.");
    }
}

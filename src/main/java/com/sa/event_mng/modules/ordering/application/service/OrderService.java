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

/**
 * Dịch vụ xử lý đơn hàng (Order) và luồng thanh toán.
 * OrderService chuyển giỏ hàng thành đơn hàng, xử lý thanh toán, tạo vé, gửi email và quản lý trạng thái đơn.
 */
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

    /**
     * Lấy thông tin user hiện tại từ SecurityContext.
     * Được gọi trong mỗi service method để xác định user thực hiện hành động.
     * @return User object của người dùng hiện tại
     */
    private User getCurrentUser() {
        // Lấy username từ SecurityContextHolder
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        
        // Tìm User: SELECT * FROM users WHERE username = ?
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_EXISTED));
    }

    /**
     * Thanh toán toàn bộ giỏ hàng hiện tại của user.
     * @param paymentMethod - phương thức thanh toán (MOMO, PAYOS)
     * @param voucherCode - mã giảm giá nếu có (optional)
     * @param platform - nền tảng (web, mobile)
     * @return OrderResponse chứa thông tin đơn hàng mới tạo
     */
    @Transactional
    public OrderResponse checkout(PaymentMethod paymentMethod, String voucherCode, String platform) {
        // Lấy user hiện tại từ Spring Security
        User user = getCurrentUser();
        
        // Tìm giỏ hàng của user bằng SQL: SELECT * FROM carts WHERE customer_id = ?
        Cart cart = cartRepository.findByCustomerId(user.getId())
                .orElseThrow(() -> new AppException(ErrorCode.CART_EMPTY));

        // Kiểm tra giỏ hàng có sản phẩm hay không
        if (cart.getItems().isEmpty()) {
            throw new AppException(ErrorCode.CART_EMPTY);
        }

        // Tạo đơn hàng từ toàn bộ giỏ hàng
        return createOrderFromItems(user, cart.getItems(), paymentMethod, cart, voucherCode, platform);
    }

    /**
     * Thanh toán chỉ các item được chọn trong giỏ hàng.
     * @param itemIds - danh sách ID của CartItem cần thanh toán
     * @param paymentMethod - phương thức thanh toán
     * @param voucherCode - mã giảm giá
     * @param platform - nền tảng
     * @return OrderResponse của đơn hàng mới
     */
    @Transactional
    public OrderResponse checkoutSelected(List<Long> itemIds, PaymentMethod paymentMethod, String voucherCode, String platform) {
        // Lấy user hiện tại
        User user = getCurrentUser();
        
        // Lấy giỏ hàng
        Cart cart = cartRepository.findByCustomerId(user.getId())
                .orElseThrow(() -> new AppException(ErrorCode.CART_EMPTY));

        // Lọc ra các item cần thanh toán từ danh sách ID
        List<CartItem> selectedItems = new ArrayList<>();
        for (CartItem item : cart.getItems()) {
            if (itemIds.contains(item.getId())) {
                selectedItems.add(item);
            }
        }

        // Nếu không có item nào được chọn thì báo lỗi
        if (selectedItems.isEmpty()) {
            throw new AppException(ErrorCode.CART_EMPTY);
        }

        // Tạo đơn hàng từ các mục được chọn
        return createOrderFromItems(user, selectedItems, paymentMethod, cart, voucherCode, platform);
    }

    /**
     * Tạo đơn hàng từ danh sách CartItem.
     * Phương thức này xử lý: kiểm tra điều kiện, tính tiền, tạo đơn, và xử lý thanh toán.
     */
    private OrderResponse createOrderFromItems(User user, List<CartItem> items, PaymentMethod paymentMethod, Cart cart, String voucherCode, String platform) {
        // ========== BƯỚC 1: TÍNH TOÁN GIÁ TRỊ ĐƠN HÀNG ==========
        BigDecimal subTotal = BigDecimal.ZERO;
        for (CartItem item : items) {
            // Cộng dồn subtotal từ từng item
            subTotal = subTotal.add(item.getSubtotal());
            
            // Kiểm tra sự kiện có đang mở bán không
            Event event = item.getTicketType().getEvent();
            if (event.getStatus() != EventStatus.OPENING) {
                throw new AppException(ErrorCode.EVENT_NOT_OPENING);
            }
            
            // Kiểm tra tồn kho vé
            if (item.getTicketType().getRemainingQuantity() < item.getQuantity()) {
                throw new AppException(ErrorCode.TICKET_NOT_ENOUGH);
            }
        }

        // ========== BƯỚC 2: TÍNH GIẢM GIÁ VÀ PHÂN CHIA TIỀN ==========
        // Nếu có mã giảm giá, tính lượng giảm
        BigDecimal discountAmount = BigDecimal.ZERO;
        if (voucherCode != null && !voucherCode.isBlank()) {
            // Nhóm tổng tiền theo sự kiện để kiểm tra điều kiện giảm giá
            Map<Long, Double> eventAmounts = items.stream()
                    .collect(Collectors.groupingBy(
                            item -> item.getTicketType().getEvent().getId(),
                            Collectors.summingDouble(item -> item.getSubtotal().doubleValue())
                    ));
            // Gọi service để tính lượng giảm
            Double discount = voucherService.calculateDiscount(voucherCode, eventAmounts);
            discountAmount = BigDecimal.valueOf(discount);
        }

        // Tính tổng tiền sau giảm
        BigDecimal totalAmount = subTotal.subtract(discountAmount);
        
        // Phí nền tảng = 25% tổng tiền
        BigDecimal platformFeeRate = new BigDecimal("0.25");
        BigDecimal serviceFee = totalAmount.multiply(platformFeeRate);
        
        // Tiền tổ chức nhận = tổng tiền - phí nền tảng
        BigDecimal organizerAmount = totalAmount.subtract(serviceFee);

        // ========== BƯỚC 3: TẠO ĐỐI TƯỢNG ORDER ==========
        // Tạo Order mới với trạng thái PENDING (chưa thanh toán)
        Order order = Order.builder()
                .customer(user)
                .orderCode(System.currentTimeMillis() % 1000000000L)  // Tạo mã đơn duy nhất
                .organizerAmount(organizerAmount)
                .serviceFee(serviceFee)
                .platformFeeRate(platformFeeRate.floatValue())
                .totalAmount(totalAmount)
                .discountAmount(discountAmount)
                .voucherCode(voucherCode)
                .paymentMethod(paymentMethod)
                .paymentStatus(PaymentStatus.PENDING)  // Trạng thái thanh toán = PENDING
                .orderStatus(OrderStatus.PENDING)  // Trạng thái đơn = PENDING
                .orderDate(LocalDateTime.now())
                .build();

        // ========== BƯỚC 4: TẠO ORDER ITEMS ==========
        // Chuyển từng CartItem sang OrderItem
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

        // ========== BƯỚC 5: LƯU ORDER VÀO DATABASE ==========
        // INSERT INTO orders (...) và INSERT INTO order_items (...)
        Order savedOrder = orderRepository.save(order);
        OrderResponse response = orderMapper.toOrderResponse(savedOrder);

        // ========== BƯỚC 6: XỬ LÝ THANH TOÁN DỰA TRÊN PHƯƠNG THỨC ==========
        // MoMo: giả lập hoàn tất ngay (trong thực tế có thêm bước xác thực từ MoMo)
        if (paymentMethod == PaymentMethod.MOMO) {
            this.completePayment(savedOrder.getId());  // Gọi hoàn tất thanh toán
            response.setPaymentUrl("/payment/success?orderCode=" + savedOrder.getOrderCode());
        }

        // PayOS: tạo link thanh toán để khách truy cập
        if (paymentMethod == PaymentMethod.PAYOS) {
            try {
                String paymentUrl = paymentService.createPayOSPaymentLink(savedOrder, platform);
                response.setPaymentUrl(paymentUrl);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        // ========== BƯỚC 7: ÁP DỤNG MÃ GIẢM GIÁ ==========
        // Nếu có mã, giảm lượt sử dụng của mã
        if (voucherCode != null && !voucherCode.isBlank()) {
            voucherService.applyVoucher(voucherCode);
        }

        // ========== BƯỚC 8: XÓA CÁC ITEM ĐÃ THANH TOÁN KHỎI GIỎ ==========
        cart.getItems().removeAll(items);  // Xóa item khỏi giỏ
        cartRepository.save(cart);  // Lưu giỏ cập nhật

        return response;
    }

    /**
     * Hoàn tất thanh toán của một đơn hàng dựa trên OrderId.
     * @param orderId - ID của Order
     */
    @Transactional
    public void completePayment(String orderId) {
        // Tìm Order theo ID: SELECT * FROM orders WHERE id = ?
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new AppException(ErrorCode.ORDER_NOT_FOUND));
        
        // Gọi xử lý hoàn tất
        processCompletion(order);
    }

    /**
     * Hoàn tất thanh toán dựa trên OrderCode (mã đơn hàng).
     * Được gọi từ webhook PayOS hoặc redirect từ PayOS.
     * @param orderCode - mã đơn hàng duy nhất
     */
    @Transactional
    public void completePaymentByOrderCode(Long orderCode) {
        // Tìm Order theo orderCode: SELECT * FROM orders WHERE order_code = ?
        Order order = orderRepository.findByOrderCode(orderCode)
                .orElseThrow(() -> new AppException(ErrorCode.ORDER_NOT_FOUND));
        
        // Gọi xử lý hoàn tất
        processCompletion(order);
    }

    /**
     * Hủy thanh toán của một đơn hàng.
     * Được gọi khi khách hủy thanh toán trên PayOS hoặc khi thanh toán thất bại.
     * Các item trong đơn hàng sẽ được chuyển về lại giỏ hàng.
     * @param orderCode - mã đơn hàng
     */
    @Transactional
    public void cancelPaymentByOrderCode(Long orderCode) {
        // Tìm Order
        Order order = orderRepository.findByOrderCode(orderCode)
                .orElseThrow(() -> new AppException(ErrorCode.ORDER_NOT_FOUND));

        // Nếu đơn đã thanh toán rồi thì không hủy được
        if (order.getPaymentStatus() == PaymentStatus.PAID) {
            return;
        }

        // Nếu đơn đã hủy trước đó rồi thì bỏ qua
        if (order.getPaymentStatus() == PaymentStatus.FAILED && order.getOrderStatus() == OrderStatus.CANCELLED) {
            return;
        }

        // Lấy customer
        User customer = order.getCustomer();
        
        // Lấy giỏ hàng của customer, nếu chưa có thì tạo mới
        Cart cart = cartRepository.findByCustomerId(customer.getId())
                .orElseGet(() -> {
                    Cart newCart = new Cart();
                    newCart.setCustomer(customer);
                    newCart.setItems(new ArrayList<>());
                    return cartRepository.save(newCart);
                });

        // Khởi tạo items list nếu null
        if (cart.getItems() == null) {
            cart.setItems(new ArrayList<>());
        }

        // ========== CHUYỂN ITEMS TỪ ĐƠN VỀ GIỎ ==========
        for (OrderItem orderItem : order.getItems()) {
            CartItem existingItem = null;
            
            // Kiểm tra xem item này đã tồn tại trong giỏ hay chưa
            for (CartItem cartItem : cart.getItems()) {
                if (cartItem.getTicketType().getId().equals(orderItem.getTicketType().getId())) {
                    existingItem = cartItem;
                    break;
                }
            }

            if (existingItem != null) {
                // Nếu tồn tại rồi thì cộng dồn số lượng
                int newQuantity = existingItem.getQuantity() + orderItem.getQuantity();
                existingItem.setQuantity(newQuantity);
                existingItem.setSubtotal(existingItem.getUnitPrice().multiply(new BigDecimal(newQuantity)));
            } else {
                // Nếu chưa tồn tại thì tạo CartItem mới
                CartItem newItem = new CartItem();
                newItem.setCart(cart);
                newItem.setTicketType(orderItem.getTicketType());
                newItem.setQuantity(orderItem.getQuantity());
                newItem.setUnitPrice(orderItem.getUnitPrice());
                newItem.setSubtotal(orderItem.getUnitPrice().multiply(new BigDecimal(orderItem.getQuantity())));
                cart.getItems().add(newItem);
            }
        }

        // Lưu giỏ cập nhật
        cartRepository.save(cart);
        
        // Cập nhật trạng thái đơn hàng
        order.setPaymentStatus(PaymentStatus.FAILED);
        order.setOrderStatus(OrderStatus.CANCELLED);
        orderRepository.save(order);
    }

    /**
     * Xử lý hoàn tất thanh toán.
     * Các bước chính:
     * 1. Cập nhật trạng thái Order thành PAID
     * 2. Giảm tồn kho vé từ TicketType
     * 3. Tạo các Ticket cho từng vé được mua
     * 4. Gửi email xác nhận kèm hóa đơn PDF
     * @param order - đối tượng Order cần hoàn tất
     */
    private void processCompletion(Order order) {
        log.info("DEBUG: [ORDER_PROCESS] Starting completion for OrderID: {}, OrderCode: {}", order.getId(), order.getOrderCode());
        
        // ========== KIỂM TRA: Tránh xử lý lặp ==========
        if (order.getPaymentStatus() == PaymentStatus.PAID) {
            log.warn("DEBUG: [ORDER_PROCESS] Order {} is ALREADY PAID. Skipping.", order.getOrderCode());
            return;
        }

        log.info("DEBUG: [ORDER_PROCESS] Order status: {}, Customer Email: {}", order.getPaymentStatus(), order.getCustomer() != null ? order.getCustomer().getEmail() : "NULL");

        try {
            // ========== BƯỚC 1: CẬP NHẬT TRẠNG THÁI ORDER ==========
            order.setPaymentStatus(PaymentStatus.PAID);      // Đánh dấu đã thanh toán
            order.setOrderStatus(OrderStatus.CONFIRMED);     // Đơn hàng được xác nhận
            order.setPaidAt(LocalDateTime.now());             // Ghi lại thời gian thanh toán
            
            // Khởi tạo danh sách vé nếu null
            if (order.getTickets() == null) order.setTickets(new ArrayList<>());
            
            // ========== BƯỚC 2: GIẢM TỒNKHO VÀ TẠO VÉ ==========
            for (OrderItem item : order.getItems()) {
                TicketType tt = item.getTicketType();
                
                // Kiểm tra lại tồn kho (phòng trường hợp có người khác mua cùng lúc)
                if (tt.getRemainingQuantity() < item.getQuantity()) {
                    throw new AppException(ErrorCode.TICKET_NOT_ENOUGH);
                }
                
                // Giảm tồn kho: UPDATE ticket_types SET remaining_quantity = ? WHERE id = ?
                tt.setRemainingQuantity(tt.getRemainingQuantity() - item.getQuantity());
                ticketTypeRepository.save(tt);

                // ========== TẠO CÁC VÉ RIÊNG LẺ ==========
                // Ví dụ: khách mua 2 vé loại A thì tạo 2 Ticket riêng biệt
                for (int i = 0; i < item.getQuantity(); i++) {
                    // Tạo mã vé duy nhất: TKT-XXXXXXXX (8 ký tự random)
                    String ticketCode = "TKT-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
                    
                    // Tạo QR code chứa mã vé (sẽ dùng để check-in)
                    Ticket ticket = Ticket.builder()
                            .order(order)
                            .ticketType(tt)
                            .ticketCode(ticketCode)
                            .qrCode("https://api.qrserver.com/v1/create-qr-code/?size=150x150&data=" + ticketCode)
                            .status(TicketStatus.VALID)  // Vé mới tạo có trạng thái VALID
                            .build();
                    
                    // Lưu vé: INSERT INTO tickets (...)
                    ticketRepository.save(ticket);
                    
                    // Thêm vé vào danh sách của order
                    order.getTickets().add(ticket);
                }
            }
            
            // Lưu order cùng với tickets: UPDATE orders + các vé đã được save
            orderRepository.save(order);

            // ========== BƯỚC 3: LẤY ORDER VỚI CUSTOMER ĐẦY ĐỦ ==========
            // Tránh LazyInitializationException khi truy cập customer.email ngoài transaction
            Order fullOrder = orderRepository.findByIdWithCustomer(order.getId()).orElse(order);
            
            // Force load danh sách vé
            if (fullOrder.getTickets() != null) fullOrder.getTickets().size(); 
            
            // ========== BƯỚC 4: GỬI EMAIL XÁC NHẬN ==========
            if (fullOrder.getCustomer().getEmail() != null) {
                try {
                    // Tạo file PDF hóa đơn từ thông tin order
                    byte[] pdfBytes = pdfService.generateOrderInvoice(fullOrder);
                    
                    // Gửi email kèm PDF đến khách
                    emailService.sendOrderConfirmationWithInvoice(fullOrder.getCustomer().getEmail(), fullOrder, pdfBytes);
                } catch (Exception e) {
                    // Nếu gửi email thất bại thì chỉ log, không làm fail toàn bộ process
                    log.error("Lỗi khi tạo PDF hoặc gửi mail: {}", e.getMessage());
                    e.printStackTrace();
                }
            }
        } catch (AppException ae) {
            // Nếu là AppException thì throw lại
            throw ae;
        } catch (Exception e) {
            // Lỗi chung chung thì wrapper thành AppException
            e.printStackTrace();
            throw new AppException(ErrorCode.UNCATEGORIZED_EXCEPTION);
        }
    }

    /**
     * Lấy file PDF hóa đơn của một đơn hàng.
     * Chỉ chủ sở hữu đơn hàng mới được phép tải.
     * @param orderId - ID của Order
     * @return byte array của file PDF
     */
    public byte[] getOrderInvoice(String orderId) {
        // Lấy user hiện tại
        User user = getCurrentUser();
        
        // Tìm Order: SELECT * FROM orders WHERE id = ?
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new AppException(ErrorCode.ORDER_NOT_FOUND));
        
        // Kiểm tra quyền: chỉ người sở hữu mới lấy được
        if (!order.getCustomer().getId().equals(user.getId())) {
            throw new AppException(ErrorCode.UNCATEGORIZED_EXCEPTION);
        }
        
        // Tạo và trả về PDF
        return pdfService.generateOrderInvoice(order);
    }

    /**
     * Lấy danh sách đơn hàng của user (có phân trang).
     * Sắp xếp theo thời gian tạo giảm dần (mới nhất trước).
     * @param pageRequest - thông tin phân trang (page, size, sort)
     * @return Page chứa danh sách OrderResponse
     */
    public Page<OrderResponse> getMyOrders(PageRequest pageRequest) {
        // Lấy user hiện tại
        User user = getCurrentUser();
        
        // Tìm Orders của user: SELECT * FROM orders WHERE customer_id = ? ORDER BY created_at DESC LIMIT ? OFFSET ?
        Page<Order> orderPage = orderRepository.findByCustomerId(user.getId(), pageRequest);
        
        // Chuyển từng Order sang OrderResponse
        return orderPage.map(order -> {
            OrderResponse response = orderMapper.toOrderResponse(order);
            
            // Nếu đơn chưa thanh toán và dùng PAYOS thì tạo lại link thanh toán
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

    /**
     * Hủy đơn hàng và chuyển các item trở lại giỏ hàng.
     * Chỉ được hủy nếu đơn chưa thanh toán.
     * @param orderCode - mã đơn hàng
     */
    @Transactional
    public void cancelAndRestoreCart(Long orderCode) {
        // Lấy user hiện tại
        User user = getCurrentUser();
        
        // Tìm Order: SELECT * FROM orders WHERE order_code = ?
        Order order = orderRepository.findByOrderCode(orderCode)
                .orElseThrow(() -> new AppException(ErrorCode.ORDER_NOT_FOUND));

        // Kiểm tra quyền: chỉ chủ sở hữu mới được hủy
        if (!order.getCustomer().getId().equals(user.getId())) {
            throw new AppException(ErrorCode.UNCATEGORIZED_EXCEPTION);
        }

        // Nếu đã hủy rồi thì bỏ qua
        if (order.getOrderStatus() == OrderStatus.CANCELLED) {
            return;
        }

        // Chỉ hủy được nếu chưa thanh toán
        if (order.getPaymentStatus() != PaymentStatus.PENDING) {
            return;
        }

        // Lấy giỏ hàng hiện tại hoặc tạo mới
        Cart cart = cartRepository.findByCustomerId(user.getId())
                .orElseGet(() -> {
                    Cart newCart = new Cart();
                    newCart.setCustomer(user);
                    newCart.setItems(new ArrayList<>());
                    return cartRepository.save(newCart);
                });

        // Khởi tạo items list nếu null
        if (cart.getItems() == null) {
            cart.setItems(new ArrayList<>());
        }

        // ========== CHUYỂN ITEMS TỪ ĐƠN VỀ GIỎ ==========
        for (OrderItem orderItem : order.getItems()) {
            CartItem existingItem = null;
            
            // Kiểm tra item này đã có trong giỏ hay chưa
            for (CartItem ci : cart.getItems()) {
                if (ci.getTicketType().getId().equals(orderItem.getTicketType().getId())) {
                    existingItem = ci;
                    break;
                }
            }

            if (existingItem != null) {
                // Nếu có rồi thì cộng dồn số lượng
                int newQuantity = existingItem.getQuantity() + orderItem.getQuantity();
                existingItem.setQuantity(newQuantity);
                BigDecimal unitPrice = existingItem.getUnitPrice();
                existingItem.setSubtotal(unitPrice.multiply(new BigDecimal(newQuantity)));
            } else {
                // Nếu chưa có thì tạo mới
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

        // Lưu giỏ cập nhật
        cartRepository.save(cart);
        
        // Cập nhật trạng thái đơn hàng
        order.setOrderStatus(OrderStatus.CANCELLED);
        orderRepository.save(order);
        
        System.out.println("DEBUG: Order #" + orderCode + " cancelled and cart restored.");
    }
}

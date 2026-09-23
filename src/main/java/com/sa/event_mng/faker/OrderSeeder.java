package com.sa.event_mng.faker;

import com.sa.event_mng.model.enums.OrderStatus;
import com.sa.event_mng.model.enums.PaymentMethod;
import com.sa.event_mng.model.enums.PaymentStatus;
import com.sa.event_mng.modules.event.domain.repository.EventRepository;
import com.sa.event_mng.modules.identity.domain.model.User;
import com.sa.event_mng.modules.identity.domain.repository.UserRepository;
import com.sa.event_mng.modules.ordering.domain.model.Order;
import com.sa.event_mng.modules.ordering.domain.repository.OrderRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Random;

@Component
@RequiredArgsConstructor
public class OrderSeeder {

    private final OrderRepository orderRepository;
    private final UserRepository userRepository;
    private final EventRepository eventRepository;

    private final Random random = new Random();

    public void seed() {
        // Khong co guard nay thi moi lan khoi dong lai se sinh them 1-3 don hang
        // cho tung khach hang.
        if (orderRepository.count() > 0) return;

        List<User> customers = userRepository.findByRoles_Name("CUSTOMER");
        List<com.sa.event_mng.modules.event.domain.model.Event> events = eventRepository.findAll();
        if (customers.isEmpty() || events.isEmpty()) return;

        for (User customer : customers) {
            int numberOfOrders = random.nextInt(3) + 1;
            for (int i = 0; i < numberOfOrders; i++) {
                Order order = new Order();
                order.setCustomer(customer);
                order.setOrderStatus(OrderStatus.CONFIRMED);
                order.setPaymentMethod(random.nextBoolean() ? PaymentMethod.VNPAY : PaymentMethod.MOMO);
                order.setPaymentStatus(PaymentStatus.PAID);
                BigDecimal totalAmount = BigDecimal.valueOf(100_000 + random.nextInt(4_900_001));
                BigDecimal serviceFee = totalAmount.multiply(BigDecimal.valueOf(0.25));
                order.setTotalAmount(totalAmount);
                order.setServiceFee(serviceFee);
                order.setOrganizerAmount(totalAmount.subtract(serviceFee));
                order.setPlatformFeeRate(0.05f);
                LocalDateTime start = LocalDateTime.of(2025, 1, 1, 0, 0);
                long totalDays = java.time.temporal.ChronoUnit.DAYS.between(start, LocalDateTime.now());
                long randomDays = (long) (random.nextDouble() * totalDays);
                LocalDateTime paidAt = start.plusDays(randomDays);
                order.setOrderDate(paidAt.minusHours(random.nextInt(24)));
                order.setPaidAt(paidAt);
                orderRepository.save(order);
            }
        }
    }
}

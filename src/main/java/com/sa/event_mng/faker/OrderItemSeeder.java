package com.sa.event_mng.faker;

import com.sa.event_mng.modules.event.domain.model.TicketType;
import com.sa.event_mng.modules.event.domain.repository.EventRepository;
import com.sa.event_mng.modules.event.domain.repository.TicketTypeRepository;
import com.sa.event_mng.modules.ordering.domain.model.Order;
import com.sa.event_mng.modules.ordering.domain.model.OrderItem;
import com.sa.event_mng.modules.ordering.domain.repository.OrderItemRepository;
import com.sa.event_mng.modules.ordering.domain.repository.OrderRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.List;
import java.util.Random;

@Component
@RequiredArgsConstructor
public class OrderItemSeeder {

    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;
    private final EventRepository eventRepository;
    private final TicketTypeRepository ticketTypeRepository;

    private final Random random = new Random();

    public void seed() {
        // Khong co guard nay thi moi lan khoi dong lai se them item vao TAT CA
        // don hang cu va tru tiep ton kho ve.
        if (orderItemRepository.count() > 0) return;

        List<Order> orders = orderRepository.findAll();
        List<com.sa.event_mng.modules.event.domain.model.Event> events = eventRepository.findAll();
        if (orders.isEmpty() || events.isEmpty()) return;

        for (Order order : orders) {
            com.sa.event_mng.modules.event.domain.model.Event event = events.get(random.nextInt(events.size()));
            List<TicketType> types = ticketTypeRepository.findByEventId(event.getId());
            if (types.isEmpty()) continue;

            int itemCount = random.nextInt(3) + 1;
            for (int j = 0; j < itemCount; j++) {
                TicketType tt = types.get(random.nextInt(types.size()));
                if (tt.getRemainingQuantity() == null || tt.getRemainingQuantity() <= 0) continue;

                int qty = Math.min(random.nextInt(3) + 1, tt.getRemainingQuantity());
                BigDecimal unit = tt.getPrice();

                OrderItem item = new OrderItem();
                item.setOrder(order);
                item.setTicketType(tt);
                item.setQuantity(qty);
                item.setUnitPrice(unit);
                item.setSubtotal(unit.multiply(BigDecimal.valueOf(qty)));
                orderItemRepository.save(item);
            }
        }
    }
}

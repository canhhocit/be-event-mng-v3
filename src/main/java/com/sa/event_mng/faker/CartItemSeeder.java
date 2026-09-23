package com.sa.event_mng.faker;

import com.sa.event_mng.modules.event.domain.model.TicketType;
import com.sa.event_mng.modules.event.domain.repository.TicketTypeRepository;
import com.sa.event_mng.modules.ordering.domain.model.Cart;
import com.sa.event_mng.modules.ordering.domain.model.CartItem;
import com.sa.event_mng.modules.ordering.domain.repository.CartItemRepository;
import com.sa.event_mng.modules.ordering.domain.repository.CartRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.List;
import java.util.Random;

@Component
@RequiredArgsConstructor
public class CartItemSeeder {

    private final CartRepository cartRepository;
    private final CartItemRepository cartItemRepository;
    private final TicketTypeRepository ticketTypeRepository;

    private final Random random = new Random();

    public void seed() {
        // Khong co guard nay thi moi lan khoi dong lai se them mot lot cart item
        // moi va tru tiep ton kho ve.
        if (cartItemRepository.count() > 0) return;

        List<Cart> carts = cartRepository.findAll();
        List<TicketType> ticketTypes = ticketTypeRepository.findAll();

        if (carts.isEmpty() || ticketTypes.isEmpty()) return;

        for (Cart cart : carts) {
            int numberOfItems = random.nextInt(3) + 1;
            for (int i = 0; i < numberOfItems; i++) {
                TicketType tt = ticketTypes.get(random.nextInt(ticketTypes.size()));
                if (tt.getRemainingQuantity() == null || tt.getRemainingQuantity() <= 0) continue;

                int qty = Math.min(random.nextInt(5) + 1, tt.getRemainingQuantity());
                BigDecimal unit = tt.getPrice();

                CartItem item = CartItem.builder()
                        .cart(cart)
                        .ticketType(tt)
                        .quantity(qty)
                        .unitPrice(unit)
                        .subtotal(unit.multiply(BigDecimal.valueOf(qty)))
                        .build();

                cartItemRepository.save(item);
            }
        }
    }
}

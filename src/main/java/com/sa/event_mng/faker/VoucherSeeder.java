package com.sa.event_mng.faker;

import com.sa.event_mng.modules.event.domain.model.Event;
import com.sa.event_mng.modules.event.domain.repository.EventRepository;
import com.sa.event_mng.modules.identity.domain.model.User;
import com.sa.event_mng.modules.identity.domain.repository.UserRepository;
import com.sa.event_mng.modules.marketing.domain.model.Voucher;
import com.sa.event_mng.modules.marketing.domain.repository.VoucherRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Random;

@Component
@RequiredArgsConstructor
public class VoucherSeeder {

    private final VoucherRepository voucherRepository;
    private final UserRepository userRepository;
    private final EventRepository eventRepository;

    private final Random random = new Random();

    public void seed() {
        if (voucherRepository.count() > 0) return;

        List<User> admins = userRepository.findByRoles_Name("ADMIN");
        List<User> organizers = userRepository.findByRoles_Name("ORGANIZER");
        List<Event> events = eventRepository.findAll();

        if (admins.isEmpty() && organizers.isEmpty()) return;

        // 10 global vouchers by ADMIN (PERCENTAGE)
        for (int i = 1; i <= 10; i++) {
            User creator = admins.isEmpty() ? organizers.get(0) : admins.get(random.nextInt(admins.size()));
            voucherRepository.save(Voucher.builder()
                    .code("GLOBAL" + String.format("%02d", i))
                    .discountType("PERCENTAGE")
                    .amount(BigDecimal.valueOf(5 + i * 2L))       // 7%, 9%, ... 25%
                    .maxDiscount(BigDecimal.valueOf(100_000 + i * 50_000L))
                    .minOrderAmount(BigDecimal.valueOf(200_000))
                    .quantity(100)
                    .startDate(LocalDateTime.now().minusDays(5))
                    .endDate(LocalDateTime.now().plusDays(30 + i))
                    .event(null)
                    .creator(creator)
                    .build());
        }

        // 10 fixed-amount vouchers by ADMIN
        for (int i = 1; i <= 10; i++) {
            User creator = admins.isEmpty() ? organizers.get(0) : admins.get(random.nextInt(admins.size()));
            voucherRepository.save(Voucher.builder()
                    .code("FLAT" + String.format("%02d", i))
                    .discountType("AMOUNT")
                    .amount(BigDecimal.valueOf(i * 20_000L))       // 20k, 40k, ... 200k
                    .maxDiscount(null)
                    .minOrderAmount(BigDecimal.valueOf(i * 50_000L))
                    .quantity(50)
                    .startDate(LocalDateTime.now().minusDays(3))
                    .endDate(LocalDateTime.now().plusDays(15))
                    .event(null)
                    .creator(creator)
                    .build());
        }

        // Event-specific vouchers by ORGANIZER
        if (!organizers.isEmpty() && !events.isEmpty()) {
            for (int i = 0; i < Math.min(10, events.size()); i++) {
                Event event = events.get(i);
                User organizer = organizers.get(random.nextInt(organizers.size()));
                voucherRepository.save(Voucher.builder()
                        .code("EVT" + event.getId() + "OFF")
                        .discountType("PERCENTAGE")
                        .amount(BigDecimal.valueOf(10 + random.nextInt(21)))  // 10-30%
                        .maxDiscount(BigDecimal.valueOf(150_000))
                        .minOrderAmount(BigDecimal.valueOf(100_000))
                        .quantity(30)
                        .startDate(LocalDateTime.now().minusDays(1))
                        .endDate(LocalDateTime.now().plusDays(20))
                        .event(event)
                        .creator(organizer)
                        .build());
            }
        }
    }
}

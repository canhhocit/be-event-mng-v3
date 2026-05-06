//package com.sa.event_mng.faker;
//
//import org.springframework.boot.CommandLineRunner;
//import org.springframework.context.annotation.Bean;
//import org.springframework.context.annotation.Configuration;
//import org.springframework.core.annotation.Order;
//
//@Configuration
//public class DataFaker {
//
//    @Bean
//    @Order(2)
//    CommandLineRunner seedDatabase(
//            CategorySeeder categorySeeder,
//            UserSeeder userSeeder,
//            EventSeeder eventSeeder,
//            TicketTypeSeeder ticketTypeSeeder,
//            CartSeeder cartSeeder,
//            CartItemSeeder cartItemSeeder,
//            OrderSeeder orderSeeder,
//            OrderItemSeeder orderItemSeeder,
//            TicketSeeder ticketSeeder,
//            BlogTagSeeder blogTagSeeder,
//            BlogPostSeeder blogPostSeeder,
//            VoucherSeeder voucherSeeder
//    ) {
//        return args -> {
//            System.out.println("=== START SEED DATABASE ===");
//
//            seedAndLog("CategorySeeder", categorySeeder::seed);
//            seedAndLog("UserSeeder", userSeeder::seed);
//            seedAndLog("EventSeeder", eventSeeder::seed);
//            seedAndLog("TicketTypeSeeder", ticketTypeSeeder::seed);
//            seedAndLog("CartSeeder", cartSeeder::seed);
//            seedAndLog("CartItemSeeder", cartItemSeeder::seed);
//            seedAndLog("OrderSeeder", orderSeeder::seed);
//            seedAndLog("OrderItemSeeder", orderItemSeeder::seed);
//            seedAndLog("TicketSeeder", ticketSeeder::seed);
//            seedAndLog("BlogTagSeeder", blogTagSeeder::seed);
//            seedAndLog("BlogPostSeeder", blogPostSeeder::seed);
//            seedAndLog("VoucherSeeder", voucherSeeder::seed);
//
//            System.out.println("=== END SEED DATABASE ===");
//        };
//    }
//
//    private void seedAndLog(String seedName, Runnable seedAction) {
//        seedAction.run();
//        System.out.println("=== " + seedName + " SEED SUCCESS ===");
//    }
//}

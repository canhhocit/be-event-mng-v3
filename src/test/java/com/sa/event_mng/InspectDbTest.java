package com.sa.event_mng;

import org.junit.jupiter.api.Test;
import java.sql.*;

public class InspectDbTest {

    @Test
    void fixUserPasswordsInNeonDb() throws Exception {
        String url = "jdbc:postgresql://ep-orange-art-b54e7538-pooler.c-7.us-east-2.aws.neon.tech/neondb?sslmode=require";
        String user = "neondb_owner";
        String pass = "npg_IhY0WEfZQVp9";

        String validHashFor123456 = "$2a$10$Yh/F0RRuLqSg6HnsmSAPB.OI/Y5fHzpiQt1nCEpjQqCkBQr3TuhIu";

        try (Connection conn = DriverManager.getConnection(url, user, pass)) {
            System.out.println("=== UPDATING NEON DB PASSWORDS DIRECTLY ===");
            String sql = "UPDATE users SET password = ?, enabled = true WHERE username IN ('admin', 'saigon_concerts', 'techfest_vn', 'vn_marathon', 'hoanganh', 'thuylinh', 'quangminh')";
            try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
                pstmt.setString(1, validHashFor123456);
                int updatedCount = pstmt.executeUpdate();
                System.out.println("Updated user rows: " + updatedCount);
            }

            System.out.println("\n=== VERIFYING USERS AFTER UPDATE ===");
            try (Statement stmt = conn.createStatement();
                 ResultSet rs = stmt.executeQuery("SELECT id, username, password, enabled FROM users WHERE username IN ('admin', 'hoanganh')")) {
                while (rs.next()) {
                    System.out.printf("ID: %d | Username: %s | PasswordHash: %s | Enabled: %b%n",
                            rs.getInt("id"),
                            rs.getString("username"),
                            rs.getString("password"),
                            rs.getBoolean("enabled"));
                }
            }
        }
    }
}

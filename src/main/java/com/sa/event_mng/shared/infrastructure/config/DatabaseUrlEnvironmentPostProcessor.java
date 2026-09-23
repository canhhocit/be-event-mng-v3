package com.sa.event_mng.shared.infrastructure.config;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.env.EnvironmentPostProcessor;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MapPropertySource;

import java.util.HashMap;
import java.util.Map;

public class DatabaseUrlEnvironmentPostProcessor implements EnvironmentPostProcessor {

    @Override
    public void postProcessEnvironment(ConfigurableEnvironment environment, SpringApplication application) {
        String dbUrl = environment.getProperty("DATABASE_URL");
        if (dbUrl == null || dbUrl.isBlank()) {
            dbUrl = environment.getProperty("POSTGRES_URL");
        }

        if (dbUrl != null && !dbUrl.isBlank()) {
            String fixedUrl = dbUrl.trim();
            if (fixedUrl.startsWith("postgresql://")) {
                fixedUrl = "jdbc:" + fixedUrl;
            } else if (!fixedUrl.startsWith("jdbc:postgresql://")) {
                fixedUrl = "jdbc:postgresql://" + fixedUrl;
            }

            Map<String, Object> map = new HashMap<>();
            map.put("spring.datasource.url", fixedUrl);
            environment.getPropertySources().addFirst(new MapPropertySource("fixedDatabaseUrlSource", map));
        }
    }
}

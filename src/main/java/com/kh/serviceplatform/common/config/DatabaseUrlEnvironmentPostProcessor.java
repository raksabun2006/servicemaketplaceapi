package com.kh.serviceplatform.common.config;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.env.EnvironmentPostProcessor;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MapPropertySource;

import java.net.URI;
import java.util.HashMap;
import java.util.Map;

/**
 * Automatically parses Railway / Heroku style DATABASE_URL or DATABASE_PUBLIC_URL
 * into Spring Boot datasource properties (spring.datasource.url, username, password).
 */
@Order(Ordered.HIGHEST_PRECEDENCE)
public class DatabaseUrlEnvironmentPostProcessor implements EnvironmentPostProcessor {

    private static final String DATABASE_URL = "DATABASE_URL";
    private static final String DATABASE_PUBLIC_URL = "DATABASE_PUBLIC_URL";

    @Override
    public void postProcessEnvironment(ConfigurableEnvironment environment, SpringApplication application) {
        String dbUrl = environment.getProperty(DATABASE_URL);
        if (dbUrl == null || dbUrl.isBlank()) {
            dbUrl = environment.getProperty(DATABASE_PUBLIC_URL);
        }

        if (dbUrl == null || dbUrl.isBlank()) {
            return;
        }

        try {
            String trimmedUrl = dbUrl.trim();
            // Handle postgres:// or postgresql://
            if (trimmedUrl.startsWith("postgres://")) {
                trimmedUrl = "postgresql://" + trimmedUrl.substring("postgres://".length());
            }

            if (!trimmedUrl.startsWith("postgresql://")) {
                return;
            }

            URI uri = URI.create(trimmedUrl);
            String host = uri.getHost();
            int port = uri.getPort() == -1 ? 5432 : uri.getPort();
            String path = uri.getPath();
            String dbName = (path != null && path.length() > 1) ? path.substring(1) : "";
            if (dbName.contains("?")) {
                dbName = dbName.substring(0, dbName.indexOf('?'));
            }

            Map<String, Object> targetProps = new HashMap<>();
            targetProps.put("spring.datasource.url", "jdbc:postgresql://" + host + ":" + port + "/" + dbName);

            String userInfo = uri.getUserInfo();
            if (userInfo != null && !userInfo.isBlank()) {
                String[] parts = userInfo.split(":", 2);
                targetProps.put("spring.datasource.username", parts[0]);
                if (parts.length > 1) {
                    targetProps.put("spring.datasource.password", parts[1]);
                }
            }

            environment.getPropertySources().addFirst(new MapPropertySource("railwayDatabaseUrlConfig", targetProps));
            System.out.println("Successfully configured Spring datasource from DATABASE_URL (" + host + ":" + port + "/" + dbName + ")");
        } catch (Exception ex) {
            System.err.println("Failed to parse DATABASE_URL: " + ex.getMessage());
        }
    }
}

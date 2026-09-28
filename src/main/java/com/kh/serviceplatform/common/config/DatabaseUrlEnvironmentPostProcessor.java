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
 * Automatically parses Railway / Heroku style DATABASE_URL / REDIS_URL
 * into Spring Boot properties.
 */
@Order(Ordered.HIGHEST_PRECEDENCE)
public class DatabaseUrlEnvironmentPostProcessor implements EnvironmentPostProcessor {

    private static final String DATABASE_URL = "DATABASE_URL";
    private static final String DATABASE_PUBLIC_URL = "DATABASE_PUBLIC_URL";

    private static final String REDIS_URL = "REDIS_URL";
    private static final String REDIS_PUBLIC_URL = "REDIS_PUBLIC_URL";

    @Override
    public void postProcessEnvironment(ConfigurableEnvironment environment, SpringApplication application) {
        processDatabaseUrl(environment);
        processRedisUrl(environment);
    }

    private void processDatabaseUrl(ConfigurableEnvironment environment) {
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

            String query = uri.getQuery();
            String jdbcUrl = "jdbc:postgresql://" + host + ":" + port + "/" + dbName;
            if (query != null && !query.isBlank()) {
                jdbcUrl += "?" + query;
            }

            Map<String, Object> targetProps = new HashMap<>();
            targetProps.put("spring.datasource.url", jdbcUrl);

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

    private void processRedisUrl(ConfigurableEnvironment environment) {
        String redisUrl = environment.getProperty(REDIS_URL);
        if (redisUrl == null || redisUrl.isBlank()) {
            redisUrl = environment.getProperty(REDIS_PUBLIC_URL);
        }

        if (redisUrl == null || redisUrl.isBlank()) {
            return;
        }

        try {
            String trimmedUrl = redisUrl.trim();
            if (!trimmedUrl.startsWith("redis://") && !trimmedUrl.startsWith("rediss://")) {
                return;
            }

            URI uri = URI.create(trimmedUrl);
            String host = uri.getHost();
            int port = uri.getPort() == -1 ? 6379 : uri.getPort();
            boolean isSsl = trimmedUrl.startsWith("rediss://");

            Map<String, Object> targetProps = new HashMap<>();
            targetProps.put("spring.data.redis.url", trimmedUrl);
            targetProps.put("spring.data.redis.host", host);
            targetProps.put("spring.data.redis.port", port);
            targetProps.put("spring.data.redis.ssl.enabled", isSsl);

            String userInfo = uri.getUserInfo();
            if (userInfo != null && !userInfo.isBlank()) {
                String[] parts = userInfo.split(":", 2);
                if (parts.length > 1) {
                    targetProps.put("spring.data.redis.username", parts[0]);
                    targetProps.put("spring.data.redis.password", parts[1]);
                } else {
                    targetProps.put("spring.data.redis.password", parts[0]);
                }
            }

            environment.getPropertySources().addFirst(new MapPropertySource("railwayRedisUrlConfig", targetProps));
            System.out.println("Successfully configured Spring Redis from REDIS_URL (" + host + ":" + port + ", ssl=" + isSsl + ")");
        } catch (Exception ex) {
            System.err.println("Failed to parse REDIS_URL: " + ex.getMessage());
        }
    }
}

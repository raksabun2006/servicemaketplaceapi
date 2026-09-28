# Khmer Service Marketplace Backend API

Production-ready backend API for the Khmer Service Marketplace platform built with Spring Boot 3, Java 21, Spring Data JPA / Hibernate, PostgreSQL, Flyway, and Redis.

---

## Table of Contents
1. [Overview](#overview)
2. [Redis Caching Architecture](#redis-caching-architecture)
3. [Local Development with Docker](#local-development-with-docker)
4. [Environment Variables](#environment-variables)
5. [Cache Names & TTL Configurations](#cache-names--ttl-configurations)
6. [Redis CLI Commands & Cache Verification](#redis-cli-commands--cache-verification)
7. [Fault Tolerance & Fallback Behavior](#fault-tolerance--fallback-behavior)
8. [Railway Deployment Guide](#railway-deployment-guide)
9. [Spring Boot Actuator Evaluation](#spring-boot-actuator-evaluation)

---

## Overview

The platform uses PostgreSQL as the primary source of truth and Redis (Lettuce client) as a high-performance secondary cache layer for read-heavy public endpoints:
- Category listings
- Provider profiles
- Public service marketplace listings
- Open service requests
- Geolocation and availability provider searches

---

## Redis Caching Architecture

- **Cache Provider**: Spring Cache abstraction with Redis (`Lettuce` connection factory).
- **Serialization**:
  - Keys: `StringRedisSerializer` (human-readable cache prefixes and composite parameters).
  - Values: `GenericJackson2JsonRedisSerializer` configured with `JavaTimeModule` (ISO-8601 timestamps), custom Spring Data `Page` / `PageImpl` deserializer, and restricted type handling.
  - Null Values: `disableCachingNullValues()` prevents caching `null` entries.
- **Cache Isolation & Security**:
  - Sensitive user data (passwords, JWTs, refresh tokens, private chat messages, payment details) are never cached.
  - Private user operations (`/my` requests, customer dashboards) bypass Redis to prevent data leakage across users.

---

## Local Development with Docker

### Starting PostgreSQL and Redis Containers

Run Docker Compose to spin up PostgreSQL and Redis:

```bash
docker compose up -d
```

Containers started:
- `service-marketplace-postgres`: PostgreSQL database on port `5432`.
- `service-marketplace-redis`: Redis 7 on port `6379` with AOF persistence.

---

## Environment Variables

### Local Development (`application.yaml` defaults)
| Variable | Default Value | Description |
|---|---|---|
| `SPRING_CACHE_TYPE` | `redis` | Cache provider (`redis` for local/prod, `simple` for unit tests) |
| `REDIS_HOST` | `localhost` | Redis server hostname |
| `REDIS_PORT` | `6379` | Redis server port |
| `REDIS_USERNAME` | *(empty)* | Optional Redis ACL username |
| `REDIS_PASSWORD` | *(empty)* | Redis password (if authentication enabled) |
| `REDIS_SSL` | `false` | Enable TLS/SSL connection to Redis |
| `REDIS_URL` | *(optional)* | Full Redis URL (e.g., `redis://default:pwd@host:port`) |

### Production / Railway Deployment
Railway provides either `REDIS_URL` or `REDIS_PUBLIC_URL` (format: `redis://user:password@host:port` or `rediss://...` for TLS).
The backend includes `DatabaseUrlEnvironmentPostProcessor` which automatically parses `REDIS_URL` and `REDIS_PUBLIC_URL` into `spring.data.redis.*` properties with zero manual configuration.

---

## Cache Names & TTL Configurations

Configured in `application.yaml` under `app.cache.*`:

| Cache Name | Constant | Default TTL | Description & Invalidation Strategy |
|---|---|---|---|
| `serviceCategories` | `CacheNames.SERVICE_CATEGORIES` | **1 hour** (3600s) | Read-heavy, rarely changing. Evicted on category create, update, delete, or toggle. |
| `providerProfiles` | `CacheNames.PROVIDER_PROFILES` | **15 minutes** (900s) | Provider profile by ID. Evicted when provider updates profile, availability, or is approved/rejected by admin. |
| `providerSearch` | `CacheNames.PROVIDER_SEARCH` | **2 minutes** (120s) | Nearby and available provider searches with composite filter keys. Evicted on provider profile modifications. |
| `publicServices` | `CacheNames.PUBLIC_SERVICES` | **5 minutes** (300s) | Public services and filter listings. Evicted when services are added, updated, or removed. |
| `serviceRequests` | `CacheNames.SERVICE_REQUESTS` | **5 minutes** (300s) | Public open requests. Evicted when requests are created, updated, cancelled, or offers accepted. |

---

## Redis CLI Commands & Cache Verification

### 1. Verify Redis Connection (Ping)
```bash
docker exec -it service-marketplace-redis redis-cli ping
# Expected Output: PONG
```

### 2. Inspect Cached Keys
```bash
docker exec -it service-marketplace-redis redis-cli
127.0.0.1:6379> KEYS *
# Example output:
# 1) "serviceCategories::all"
# 2) "providerProfiles::f8efb0ce-115e-4655-bb8b-c1b59269dab9"
# 3) "providerSearch::nearby:11.5564:104.9282:10.0:0:20:UNSORTED"
```

### 3. Inspect Cached Values & TTL
```bash
# Check remaining TTL in seconds (-1 = no expiry, -2 = does not exist)
127.0.0.1:6379> TTL "serviceCategories::all"
(integer) 3540

# Read cached JSON payload
127.0.0.1:6379> GET "serviceCategories::all"
```

### 4. Flush Cache Manually
```bash
# Flush current database cache
docker exec -it service-marketplace-redis redis-cli FLUSHDB

# Or from outside the container:
docker exec -it service-marketplace-redis redis-cli -h localhost -p 6379 FLUSHDB
```

---

## Fault Tolerance & Fallback Behavior

The application implements `CustomCacheErrorHandler` registered via `CachingConfigurer`:
- **Redis Outages**: If Redis crashes, experiences network timeouts, or becomes unreachable:
  - Cache `GET`, `PUT`, `EVICT`, and `CLEAR` exceptions are caught and logged at `WARN` level.
  - Zero runtime exceptions are thrown to the client.
  - Requests gracefully fall back to querying PostgreSQL directly.
- **High Availability**: Redis acts strictly as a cache. PostgreSQL remains the single source of truth.

---

## Railway Deployment Guide

1. **Add Redis Plugin**: In your Railway project, click **+ New** -> **Database** -> **Redis**.
2. **Link Environment Variables**:
   - Railway automatically injects `REDIS_URL` into the application container.
   - Alternatively, you can explicitly set `REDIS_HOST`, `REDIS_PORT`, `REDIS_PASSWORD`, and `REDIS_SSL=true`.
3. **Internal vs. Public Networking**:
   - For services in the same Railway project, use the private `REDIS_URL` for low latency and zero egress cost.
4. **Troubleshooting**:
   - If Redis connection times out on Railway, verify that `REDIS_SSL=true` is set if using `rediss://`.
   - Check application logs for `DatabaseUrlEnvironmentPostProcessor: Parsed REDIS_URL/REDIS_PUBLIC_URL successfully`.

---

## Spring Boot Actuator Evaluation

- **Current Status**: Spring Boot Actuator is **NOT installed** in this project to maintain a lightweight dependency footprint.
- **Recommendation for Production**:
  - Adding `org.springframework.boot:spring-boot-starter-actuator` is **recommended** when moving to high-scale production.
  - **Benefits**:
    - Exposes `/actuator/health` which automatically monitors Redis and PostgreSQL connection health.
    - Exposes `/actuator/metrics` including `cache.gets`, `cache.puts`, `cache.evictions`, and `cache.hit.ratio` for Prometheus/Grafana monitoring.
  - **Configuration Needed When Added**:
    ```yaml
    management:
      endpoints:
        web:
          exposure:
            include: health, info, metrics
      endpoint:
        health:
          show-details: when-authorized
    ```

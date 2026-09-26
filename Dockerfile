# ============================================================
# Stage 1: Build the Spring Boot application using Java 25 JDK
# ============================================================
FROM eclipse-temurin:25-jdk-noble AS builder

WORKDIR /build

# Copy Gradle wrapper and configuration files first for Docker layer caching
COPY gradlew ./
COPY gradle ./gradle
COPY build.gradle settings.gradle ./

# Fix potential Windows CRLF issues and ensure gradlew is executable
RUN sed -i 's/\r$//' ./gradlew && chmod +x ./gradlew

# Pre-download Gradle and project dependencies
RUN ./gradlew dependencies --no-daemon || true

# Copy application source code
COPY src ./src

# Build the Spring Boot executable jar (skipping tests during Docker build)
RUN ./gradlew bootJar -x test --no-daemon

# Locate and normalize the built jar file
RUN cp build/libs/*.jar app.jar

# ============================================================
# Stage 2: Minimal runtime container using Java 25 JRE
# ============================================================
FROM eclipse-temurin:25-jre-noble AS runner

WORKDIR /app

# Install curl (used for healthchecks)
RUN apt-get update && apt-get install -y --no-install-recommends curl && rm -rf /var/lib/apt/lists/*

# Create directory for file uploads
RUN mkdir -p /uploads && chmod 777 /uploads

# Copy built application and entrypoint script
COPY --from=builder /build/app.jar /app/app.jar
COPY entrypoint.sh /app/entrypoint.sh

# Fix CRLF line endings on entrypoint script and grant execution permissions
RUN sed -i 's/\r$//' /app/entrypoint.sh && chmod +x /app/entrypoint.sh

# Container environment defaults
ENV PORT=8080
ENV JAVA_OPTS="-XX:+UseG1GC -XX:MaxRAMPercentage=75.0 -Djava.security.egd=file:/dev/./urandom"
ENV FILE_STORAGE_PATH=/uploads

EXPOSE 8080

HEALTHCHECK --interval=30s --timeout=5s --start-period=40s --retries=3 \
  CMD curl -f http://localhost:${PORT:-8080}/actuator/health || exit 1

ENTRYPOINT ["/app/entrypoint.sh"]

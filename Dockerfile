# ============================================================
# Stage 1: Build Stage (Maven + Eclipse Temurin JDK 21)
# ============================================================
FROM maven:3.9-eclipse-temurin-21 AS builder
WORKDIR /build

# Pre-cache Maven dependencies
COPY pom.xml .
RUN mvn dependency:go-offline -B

# Copy application source code and package executable JAR
COPY src ./src
RUN mvn clean package -DskipTests -B

# ============================================================
# Stage 2: Runtime Stage (Eclipse Temurin JRE 21 - Slim & Secure)
# ============================================================
FROM eclipse-temurin:21-jre AS runtime
WORKDIR /app

# Install curl for HEALTHCHECK instruction
RUN apt-get update && \
    apt-get install -y --no-install-recommends curl && \
    rm -rf /var/lib/apt/lists/*

# Create non-root system group and user
RUN groupadd -r appgroup && useradd -r -u 1001 -g appgroup appuser

# Copy JAR from builder stage with proper non-root ownership
COPY --from=builder --chown=appuser:appgroup /build/target/esi-project-0.0.1-SNAPSHOT.jar app.jar

# Switch to non-root user
USER appuser

# Expose default application port
EXPOSE 8085

# Support configurable Spring active profile and port
ENV SPRING_PROFILES_ACTIVE=dev \
    SERVER_PORT=8085

# Container Healthcheck verifying /api/env endpoint
HEALTHCHECK --interval=30s --timeout=5s --start-period=20s --retries=3 \
  CMD curl -f http://localhost:8085/api/env || exit 1

# Execute application
ENTRYPOINT ["java", "-Djava.security.egd=file:/dev/./urandom", "-jar", "app.jar"]

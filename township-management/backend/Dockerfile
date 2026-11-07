# ==============================================================================
# Multi-stage Dockerfile for Township Management System Backend (Spring Boot 3 / 4)
# ==============================================================================

# Stage 1: Build stage
FROM eclipse-temurin:17-jdk-alpine AS builder
WORKDIR /workspace/app

# Copy Maven wrapper & pom.xml for dependency caching
COPY .mvn/ .mvn/
COPY mvnw pom.xml ./
RUN chmod +x mvnw && ./mvnw dependency:go-offline -B

# Copy application sources & build jar
COPY src/ src/
RUN ./mvnw clean package -DskipTests -B

# Stage 2: Minimal hardened runtime image
FROM eclipse-temurin:17-jre-alpine AS runtime
WORKDIR /opt/township-backend

# Security: Create non-root system user and group
RUN addgroup -S township && adduser -S township -G township

# Create uploads directory and grant ownership
RUN mkdir -p /opt/township-backend/uploads/complaints /opt/township-backend/uploads/visitors && \
    chown -R township:township /opt/township-backend

# Copy executable jar from builder stage
COPY --from=builder --chown=township:township /workspace/app/target/township-backend-0.0.1-SNAPSHOT.jar app.jar

USER township

# Expose default HTTP server port
EXPOSE 8080

# Health check
HEALTHCHECK --interval=30s --timeout=5s --start-period=60s --retries=3 \
  CMD wget --no-verbose --tries=1 --spider http://localhost:8080/api/health || exit 1

# Launch JVM with container-aware memory allocation
ENTRYPOINT ["java", \
  "-XX:+UseContainerSupport", \
  "-XX:MaxRAMPercentage=75.0", \
  "-Djava.security.egd=file:/dev/./urandom", \
  "-jar", "app.jar"]


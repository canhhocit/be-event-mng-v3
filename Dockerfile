# Stage 1: Build JAR with Maven dependency caching
FROM maven:3.9.6-eclipse-temurin-17-alpine AS builder
WORKDIR /app

# Cache dependencies
COPY pom.xml .
RUN mvn dependency:go-offline -B

# Copy source code and build production package
COPY src ./src
RUN mvn package -DskipTests -B

# Stage 2: Optimized Lightweight Runtime Environment
FROM eclipse-temurin:17-jre-alpine
WORKDIR /app

# Non-root user for security best practices
RUN addgroup -S appgroup && adduser -S appuser -G appgroup
USER appuser

# Copy JAR from builder stage
COPY --from=builder /app/target/*.jar app.jar

# Expose HTTP Port
EXPOSE 8080

# Production JVM Flags for Cloud Containers (Render Free Tier 512MB RAM optimization)
# -XX:MaxRAMPercentage=75.0 ensures JVM stays within container limit (avoiding OOM killer)
ENTRYPOINT ["sh", "-c", "java -XX:+UseG1GC -XX:MaxRAMPercentage=75.0 -XX:InitialRAMPercentage=40.0 -XX:+ExitOnOutOfMemoryError -jar app.jar --server.port=${PORT:-8080}"]

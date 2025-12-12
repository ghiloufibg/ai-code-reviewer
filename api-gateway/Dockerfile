# Multi-stage Dockerfile for ai-code-reviewer api-gateway
FROM maven:3.9-eclipse-temurin-21 AS builder

WORKDIR /app

# Copy all module pom.xml files first (required for parent pom resolution)
COPY pom.xml .
COPY core/pom.xml core/pom.xml
COPY api-gateway/pom.xml api-gateway/pom.xml
COPY security-analysis-api/pom.xml security-analysis-api/pom.xml
COPY llm-worker/pom.xml llm-worker/pom.xml

# Download dependencies (may fail for some, but caches what it can)
RUN mvn dependency:go-offline -pl api-gateway -am -B || true

# Copy source code for modules we need to build
COPY core/ core/
COPY api-gateway/ api-gateway/

# Build the application
RUN mvn clean package -pl api-gateway -am -DskipTests -B

# Runtime stage - smaller image
FROM eclipse-temurin:21-jre-alpine

WORKDIR /app

# Copy the built jar from builder stage
COPY --from=builder /app/api-gateway/target/*.jar app.jar

# Create logs directory and non-root user
RUN mkdir -p /app/logs && \
    addgroup -S appgroup && \
    adduser -S appuser -G appgroup && \
    chown -R appuser:appgroup /app

USER appuser

# Expose application port and debug port
EXPOSE 8080 5005

# JVM options for development (with remote debugging)
ENV JAVA_OPTS="-Xmx1024m -Xms512m -XX:+UseContainerSupport -XX:MaxRAMPercentage=75.0 -agentlib:jdwp=transport=dt_socket,server=y,suspend=n,address=*:5005"

# Run the application
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]

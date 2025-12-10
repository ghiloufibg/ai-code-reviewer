FROM maven:3.9.6-eclipse-temurin-21-alpine AS builder

WORKDIR /build

# Copy all module pom.xml files first (required for parent pom resolution)
COPY pom.xml .
COPY core/pom.xml core/pom.xml
COPY api-gateway/pom.xml api-gateway/pom.xml
COPY security-analysis-api/pom.xml security-analysis-api/pom.xml
COPY llm-worker/pom.xml llm-worker/pom.xml

# Download dependencies (may fail for some, but caches what it can)
RUN mvn dependency:go-offline -pl security-analysis-api -am -B || true

# Copy source code for modules we need to build
COPY core/ core/
COPY security-analysis-api/ security-analysis-api/

RUN mvn clean package -pl security-analysis-api -am -DskipTests -B

FROM eclipse-temurin:21-jre-alpine

RUN apk add --no-cache \
    python3 \
    py3-pip \
    && pip3 install --no-cache-dir --break-system-packages semgrep==1.143.1

WORKDIR /app

COPY --from=builder /build/security-analysis-api/target/*.jar app.jar

RUN addgroup -S appgroup && adduser -S appuser -G appgroup
USER appuser

EXPOSE 8080

HEALTHCHECK --interval=30s --timeout=3s --start-period=40s --retries=3 \
  CMD wget --no-verbose --tries=1 --spider http://localhost:8080/actuator/health || exit 1

ENTRYPOINT ["java", "-Xmx512m", "-XX:+UseZGC", "-jar", "app.jar"]

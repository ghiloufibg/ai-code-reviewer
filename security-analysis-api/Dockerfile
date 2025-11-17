FROM maven:3.9.6-eclipse-temurin-21-alpine AS builder

WORKDIR /build

COPY pom.xml .
COPY core/ core/
COPY api-gateway/ api-gateway/
COPY security-analysis-api/ security-analysis-api/

RUN mvn clean package -pl security-analysis-api -am -DskipTests

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

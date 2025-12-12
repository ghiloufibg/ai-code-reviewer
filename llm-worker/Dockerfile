FROM maven:3.9-eclipse-temurin-21 AS builder

WORKDIR /app

COPY pom.xml .
COPY core/pom.xml core/pom.xml
COPY api-gateway/pom.xml api-gateway/pom.xml
COPY security-analysis-api/pom.xml security-analysis-api/pom.xml
COPY llm-worker/pom.xml llm-worker/pom.xml

RUN mvn dependency:go-offline -pl llm-worker -am -B || true

COPY core/ core/
COPY llm-worker/ llm-worker/

RUN mvn clean package -pl llm-worker -am -DskipTests -B

FROM eclipse-temurin:21-jre-alpine

RUN apk add --no-cache wget

WORKDIR /app

COPY --from=builder /app/llm-worker/target/*.jar app.jar

# Create logs directory and non-root user
RUN mkdir -p /app/logs && \
    addgroup -S appgroup && \
    adduser -S appuser -G appgroup && \
    chown -R appuser:appgroup /app

USER appuser

EXPOSE 8080

ENV JAVA_OPTS="-Xmx512m -Xms256m -XX:+UseContainerSupport -XX:MaxRAMPercentage=75.0 -XX:+UseZGC"

ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]

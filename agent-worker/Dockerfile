# syntax=docker/dockerfile:1

# ============================================================================
# AI Code Reviewer - Agent Worker
# Production-optimized multi-stage Dockerfile for agentic code review
# ============================================================================

ARG JAVA_VERSION=21

# ----------------------------------------------------------------------------
# Stage 1: Dependency resolution (cached layer)
# ----------------------------------------------------------------------------
FROM maven:3.9-eclipse-temurin-${JAVA_VERSION} AS deps
WORKDIR /build

COPY pom.xml .
COPY core/pom.xml core/pom.xml
COPY api-gateway/pom.xml api-gateway/pom.xml
COPY llm-worker/pom.xml llm-worker/pom.xml
COPY agent-worker/pom.xml agent-worker/pom.xml

RUN --mount=type=cache,target=/root/.m2 \
    mvn dependency:go-offline -pl agent-worker -am -B -q || true

# ----------------------------------------------------------------------------
# Stage 2: Build application
# ----------------------------------------------------------------------------
FROM deps AS build
COPY core/ core/
COPY agent-worker/ agent-worker/
RUN --mount=type=cache,target=/root/.m2 \
    mvn package -pl agent-worker -am -DskipTests -B -q && \
    mv agent-worker/target/*-exec.jar agent-worker/target/app.jar

# ----------------------------------------------------------------------------
# Stage 3: Extract layered JAR
# ----------------------------------------------------------------------------
FROM build AS extract
RUN java -Djarmode=layertools -jar agent-worker/target/app.jar extract \
    --destination agent-worker/target/extracted

# ----------------------------------------------------------------------------
# Stage 4: Production runtime
# ----------------------------------------------------------------------------
FROM eclipse-temurin:${JAVA_VERSION}-jre-alpine AS final

# OCI Labels
LABEL org.opencontainers.image.title="AI Code Reviewer - Agent Worker" \
      org.opencontainers.image.description="Agentic worker for autonomous code review with repository cloning" \
      org.opencontainers.image.vendor="Ghiloufi" \
      org.opencontainers.image.version="v1.0.0-mvp" \
      org.opencontainers.image.source="https://github.com/ghiloufi/ai-code-reviewer"

# Install git for repository cloning operations
RUN apk add --no-cache git

# Security: Non-root user with Docker group access
ARG UID=10001
ARG GID=998
RUN addgroup -g ${GID} docker && \
    adduser \
    --disabled-password \
    --gecos "" \
    --home "/nonexistent" \
    --shell "/sbin/nologin" \
    --no-create-home \
    --uid "${UID}" \
    --ingroup docker \
    appuser

WORKDIR /app

# Create workspace directory for cloned repositories
RUN mkdir -p /workspace && chown appuser:docker /workspace

# Copy layers in order: stable -> volatile
COPY --from=extract --chown=appuser:docker /build/agent-worker/target/extracted/dependencies/ ./
COPY --from=extract --chown=appuser:docker /build/agent-worker/target/extracted/spring-boot-loader/ ./
COPY --from=extract --chown=appuser:docker /build/agent-worker/target/extracted/snapshot-dependencies/ ./
COPY --from=extract --chown=appuser:docker /build/agent-worker/target/extracted/application/ ./

USER appuser

# Production JVM settings (ZGC for low-latency, larger heap for analysis)
ENV JAVA_OPTS="-XX:+UseContainerSupport \
    -XX:MaxRAMPercentage=75.0 \
    -XX:+UseZGC \
    -XX:+ExitOnOutOfMemoryError \
    -Djava.security.egd=file:/dev/./urandom"

EXPOSE 8080

HEALTHCHECK --interval=30s --timeout=5s --start-period=60s --retries=3 \
    CMD wget --no-verbose --tries=1 --spider http://localhost:8080/actuator/health || exit 1

ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS org.springframework.boot.loader.launch.JarLauncher"]

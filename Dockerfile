# Simplified single-stage Dockerfile for ai-code-reviewer (no caching)
FROM maven:3.9-eclipse-temurin-21

WORKDIR /app

# Copy all project files
COPY pom.xml .
COPY core/ core/
COPY api-gateway/ api-gateway/

# Build the application (no caching, always fresh build)
RUN mvn clean package -DskipTests -B

# Copy the uber jar to app directory
RUN cp api-gateway/target/*.jar app.jar

# Create logs directory
RUN mkdir -p /app/logs

# Expose application port and debug port
EXPOSE 8080 5005

# JVM options for development (with remote debugging)
ENV JAVA_OPTS="-Xmx1024m -Xms512m -XX:+UseContainerSupport -XX:MaxRAMPercentage=75.0 -agentlib:jdwp=transport=dt_socket,server=y,suspend=n,address=*:5005"

# Run the application
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]

# Simplified single-stage Dockerfile for ai-code-reviewer (no caching)
FROM maven:3.9-eclipse-temurin-21

WORKDIR /app

# Copy all project files
COPY pom.xml .
COPY core/ core/
COPY api-gateway/ api-gateway/

# Create placeholder for security-analysis-api module referenced in parent POM
RUN mkdir -p security-analysis-api && \
    echo '<?xml version="1.0" encoding="UTF-8"?><project xmlns="http://maven.apache.org/POM/4.0.0" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd"><modelVersion>4.0.0</modelVersion><parent><groupId>com.ghiloufi.aicode</groupId><artifactId>ai-code-reviewer</artifactId><version>0.2.0</version></parent><artifactId>security-analysis-api</artifactId><version>0.0.2-SNAPSHOT</version><name>Security Analysis API (Placeholder)</name></project>' > security-analysis-api/pom.xml

# Build the application (no caching, always fresh build)
# Use -pl to build only api-gateway module and its dependencies
RUN mvn clean package -pl api-gateway -am -DskipTests -B

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

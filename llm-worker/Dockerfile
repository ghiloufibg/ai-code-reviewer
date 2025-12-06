FROM maven:3.9-eclipse-temurin-21

WORKDIR /app

COPY pom.xml .
COPY core/ core/
COPY llm-worker/ llm-worker/

RUN mvn clean package -pl llm-worker -am -DskipTests -B

RUN cp llm-worker/target/*.jar app.jar

RUN mkdir -p /app/logs

EXPOSE 8080

ENV JAVA_OPTS="-Xmx512m -Xms256m -XX:+UseContainerSupport -XX:MaxRAMPercentage=75.0 -XX:+UseZGC"

ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]

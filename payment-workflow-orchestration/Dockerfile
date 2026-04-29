# Shared Dockerfile for all services in this multi-module Maven project.
# Build the JARs first: mvn clean package -DskipTests
# Then: docker-compose up --build
#
# ARG JAR_FILE is set per-service in docker-compose.yml

ARG JAR_FILE
FROM eclipse-temurin:21-jre-alpine

# Non-root user — principle of least privilege
RUN addgroup -S payment && adduser -S payment -G payment

WORKDIR /app

COPY ${JAR_FILE} app.jar

USER payment

# Container-aware JVM: respects Docker memory limits instead of using host RAM
ENTRYPOINT ["java", \
  "-XX:+UseContainerSupport", \
  "-XX:MaxRAMPercentage=75.0", \
  "-Djava.security.egd=file:/dev/./urandom", \
  "-jar", "app.jar"]

FROM eclipse-temurin:21-jre-alpine

RUN addgroup -S payment && adduser -S payment -G payment

WORKDIR /app

COPY target/validation-service-1.0.0-SNAPSHOT.jar app.jar

USER payment

EXPOSE 8081

ENTRYPOINT ["java", \
  "-XX:+UseContainerSupport", \
  "-XX:MaxRAMPercentage=75.0", \
  "-Djava.security.egd=file:/dev/./urandom", \
  "-jar", "app.jar"]

FROM eclipse-temurin:21-jre-alpine

RUN addgroup -S payment && adduser -S payment -G payment

WORKDIR /app

COPY target/provider-integration-1.0.0-SNAPSHOT.jar app.jar

USER payment

EXPOSE 8086

ENTRYPOINT ["java", \
  "-XX:+UseContainerSupport", \
  "-XX:MaxRAMPercentage=75.0", \
  "-Djava.security.egd=file:/dev/./urandom", \
  "-jar", "app.jar"]

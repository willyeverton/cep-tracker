FROM openjdk:11-jre-slim

LABEL maintainer="Stefanini Challenge"

WORKDIR /app

COPY target/cep-tracker-*.jar app.jar

EXPOSE 8080

HEALTHCHECK --interval=30s --timeout=10s --start-period=60s --retries=3 \
    CMD curl -f http://localhost:8080/actuator/health || exit 1

ENTRYPOINT ["java", "-jar", "app.jar"]
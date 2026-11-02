# Image du microservice ai-service (le jar est construit par Maven avant : mvn package)
FROM eclipse-temurin:17-jre-alpine

LABEL org.opencontainers.image.title="voyages-affaires/ai-service"       org.opencontainers.image.source="https://github.com/nesrrine/voyage"

# Utilisateur sans privileges
RUN addgroup -S app && adduser -S app -G app
WORKDIR /app
COPY target/ai-service-1.0.0.jar app.jar
RUN mkdir -p /app/data && chown -R app:app /app
USER app

# La JVM s'adapte a la memoire allouee au conteneur (limites Docker / Kubernetes)
ENV JAVA_OPTS="-XX:MaxRAMPercentage=75 -XX:+ExitOnOutOfMemoryError -Djava.security.egd=file:/dev/./urandom"

EXPOSE 8084
HEALTHCHECK --interval=15s --timeout=3s --start-period=60s --retries=5   CMD wget -qO- http://localhost:8084/actuator/health/liveness || exit 1

ENTRYPOINT ["sh", "-c", "exec java $JAVA_OPTS -jar /app/app.jar"]

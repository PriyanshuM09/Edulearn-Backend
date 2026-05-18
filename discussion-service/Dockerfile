FROM eclipse-temurin:17-jre-alpine
WORKDIR /app
COPY discussion-service/target/discussion-service-0.0.1-SNAPSHOT.jar app.jar
EXPOSE 8088
HEALTHCHECK --interval=30s --timeout=10s --start-period=60s --retries=3 \
  CMD wget --quiet --tries=1 --spider http://localhost:8088/actuator/health || exit 1
ENTRYPOINT ["java", "-XX:MaxRAMPercentage=75.0", "-jar", "app.jar"]

# Stage 1: Build the Maven project
FROM maven:3.9.6-eclipse-temurin-17-alpine AS build
WORKDIR /app

# Copy parent POM
COPY pom.xml .

# Copy the service code
COPY progress-service/pom.xml progress-service/
COPY progress-service/src progress-service/src

# Build the service
WORKDIR /app/progress-service
RUN mvn clean package -DskipTests

# Stage 2: Runtime image
FROM eclipse-temurin:17-jre-alpine
WORKDIR /app
COPY --from=build /app/progress-service/target/progress-service-0.0.1-SNAPSHOT.jar app.jar
EXPOSE 8087
HEALTHCHECK --interval=30s --timeout=10s --start-period=60s --retries=3 \
  CMD wget --quiet --tries=1 --spider http://localhost:8087/actuator/health || exit 1
ENTRYPOINT ["java", "-XX:MaxRAMPercentage=75.0", "-jar", "app.jar"]

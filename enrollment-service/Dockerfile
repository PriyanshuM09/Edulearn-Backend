# Stage 1: Build the Maven project
FROM maven:3.9.6-eclipse-temurin-17-alpine AS build
WORKDIR /app

# Copy parent POM
COPY pom.xml .

# Copy the service code
COPY enrollment-service/pom.xml enrollment-service/
COPY enrollment-service/src enrollment-service/src

# Build the service
WORKDIR /app/enrollment-service
RUN mvn clean package -DskipTests

# Stage 2: Runtime image
FROM eclipse-temurin:17-jre-alpine
WORKDIR /app
COPY --from=build /app/enrollment-service/target/enrollment-service-1.0.0.jar app.jar
EXPOSE 8084
HEALTHCHECK --interval=30s --timeout=10s --start-period=60s --retries=3 \
  CMD wget --quiet --tries=1 --spider http://localhost:8084/actuator/health || exit 1
ENTRYPOINT ["java", "-XX:MaxRAMPercentage=75.0", "-jar", "app.jar"]

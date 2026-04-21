# Stage 1: Build the application
FROM gradle:9.4-jdk21 AS builder
WORKDIR /app
COPY gradlew gradlew.bat settings.gradle build.gradle gradle.properties ./
COPY gradle ./gradle
RUN ./gradlew dependencies --no-daemon
COPY src ./src
# Tests are run separately in CI before this image is built; skip here to avoid
# requiring a live database during the Docker build step.
RUN ./gradlew bootJar --no-daemon -x test

# Stage 2: Runtime image
FROM eclipse-temurin:21-jre
WORKDIR /app
COPY --from=builder /app/build/libs/mugs-*.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]

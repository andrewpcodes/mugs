# syntax=docker/dockerfile:1

FROM eclipse-temurin:21-jdk-jammy AS build
WORKDIR /workspace

COPY gradlew build.gradle settings.gradle gradle.properties ./
COPY gradle ./gradle
RUN chmod +x ./gradlew

COPY src ./src
RUN ./gradlew --no-daemon clean bootJar

FROM eclipse-temurin:21-jre-jammy
WORKDIR /app

ENV JAVA_TOOL_OPTIONS=""
ENV TZ=UTC
ENV SPRING_DATASOURCE_URL=jdbc:sqlite:/data/mugs.db

RUN useradd --create-home --uid 10001 appuser \
    && mkdir -p /data \
    && chown -R appuser:appuser /app /data

COPY --from=build /workspace/build/libs/*-SNAPSHOT.jar /app/app.jar

EXPOSE 8080
VOLUME ["/data"]

USER appuser
ENTRYPOINT ["java", "-jar", "/app/app.jar"]


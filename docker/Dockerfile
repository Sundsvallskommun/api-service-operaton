# syntax=docker/dockerfile:1
# ---- build stage: compile the bootable fat jar from source ----
# Required for source-based builders (Dokploy/CI) that clone the repo without a pre-built target/.
FROM maven:3.9-eclipse-temurin-25 AS build
WORKDIR /build
COPY . .
# Reactor build; tests skipped (operaton-app integration tests need testcontainers, which are
# unavailable inside an image build). The root build places the repackaged fat jar in /build/target/.
RUN --mount=type=cache,target=/root/.m2 mvn -B -ntp clean package -Dmaven.test.skip=true

# ---- runtime stage ----
FROM eclipse-temurin:25-jre
WORKDIR /app

RUN useradd -u 10001 -r -s /usr/sbin/nologin appuser

COPY --from=build /build/target/operaton-app-*.jar /app/app.jar

RUN chown -R appuser:appuser /app
USER appuser

ENV JAVA_TOOL_OPTIONS="-XX:MaxRAMPercentage=75 -XX:InitialRAMPercentage=25 -XX:+ExitOnOutOfMemoryError"

EXPOSE 8080
ENTRYPOINT ["java","-jar","/app/app.jar"]

FROM eclipse-temurin:21-jdk-jammy@sha256:b2711c65970c1227197c29c46e669bfdbb137524e9da0e664703a140ec25a040 AS build
WORKDIR /workspace
# El SHA-256 del Maven Wrapper corresponde al ZIP.
RUN apt-get update \
    && apt-get install -y --no-install-recommends unzip \
    && rm -rf /var/lib/apt/lists/*
COPY .mvn/wrapper/maven-wrapper.properties .mvn/wrapper/maven-wrapper.properties
COPY mvnw pom.xml ./
RUN sh ./mvnw -B -ntp dependency:go-offline
COPY src/main/ src/main/
# Las pruebas se ejecutan antes de empaquetar, con MySQL aislado (ver la guía).
RUN sh ./mvnw -B -ntp compile jar:jar spring-boot:repackage \
    && mv target/library-*.jar /workspace/app.jar

FROM eclipse-temurin:21-jre-jammy@sha256:a8bff88f760e87e0c8844f8461f807c203fd5e8f78c951c843024647e965eaf5
WORKDIR /app
COPY --from=build --chown=10001:10001 /workspace/app.jar /app/app.jar
COPY docker/entrypoint.sh /app/entrypoint.sh
USER 10001:10001
ENV JAVA_TOOL_OPTIONS="-XX:MaxRAMPercentage=50"
EXPOSE 8080
ENTRYPOINT ["/bin/sh", "/app/entrypoint.sh"]

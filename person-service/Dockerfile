FROM gradle:jdk24-corretto-al2023 AS build

ARG NEXUS_URL=http://host.docker.internal:8800/repository/maven-snapshots
ARG NEXUS_USERNAME=admin
ARG NEXUS_PASSWORD=admin

ENV NEXUS_URL=${NEXUS_URL}
ENV NEXUS_USERNAME=${NEXUS_USERNAME}
ENV NEXUS_PASSWORD=${NEXUS_PASSWORD}

WORKDIR /home/gradle/project

COPY build.gradle.kts .
COPY settings.gradle.kts .
COPY src ./src
COPY openapi ./openapi

RUN gradle clean build --no-daemon -x test
RUN gradle publish --info

FROM eclipse-temurin:24-jre

RUN apt-get update && apt-get install -y curl && rm -rf /var/lib/apt/lists/*

WORKDIR /app

COPY --from=build /home/gradle/project/build/libs/*.jar /app/app.jar

EXPOSE 8082
CMD ["java", "-jar", "/app/app.jar"]


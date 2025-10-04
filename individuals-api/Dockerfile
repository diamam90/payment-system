FROM gradle:jdk24-corretto-al2023 AS build

WORKDIR /app

COPY . /app

RUN gradle build --no-daemon

FROM eclipse-temurin:24-alpine

EXPOSE 8081

WORKDIR /app

COPY --from=build /app/build/libs/*.jar /app/app.jar

CMD ["java", "-jar", "/app/app.jar"]


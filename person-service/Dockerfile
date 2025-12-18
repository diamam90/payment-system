FROM gradle:jdk24-corretto AS build

WORKDIR /source

COPY . .
RUN gradle clean build -x test

FROM eclipse-temurin:24-alpine
WORKDIR /app

COPY --from=build /source/build/libs/person-service-1.0.0-SNAPSHOT.jar /app/app.jar
EXPOSE 8082

CMD ["java", "-jar", "/app/app.jar"]


FROM gradle:jdk24-corretto-al2023 AS build
ARG VERSION
RUN echo "Building person-service version $VERSION"
WORKDIR /source
COPY . .
RUN gradle clean build --no-daemon -x test

FROM eclipse-temurin:24-alpine
ARG VERSION
EXPOSE 8082
WORKDIR /app
COPY --from=build /source/build/libs/person-service-${VERSION}.jar /app/app.jar
CMD ["java", "-jar", "/app/app.jar"]


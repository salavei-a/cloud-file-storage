FROM gradle:jdk21 AS build
WORKDIR /app
COPY build.gradle settings.gradle ./
COPY src ./src
RUN gradle --no-daemon build -x test

FROM eclipse-temurin:21-jre
COPY --from=build /app/build/libs/*.jar app.jar
ENTRYPOINT ["java","-jar","/app.jar", "--spring.profiles.active=prod"]
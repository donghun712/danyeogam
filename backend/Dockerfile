FROM eclipse-temurin:17-jdk AS build

WORKDIR /workspace
COPY gradlew gradle.properties settings.gradle build.gradle ./
COPY gradle ./gradle
COPY src ./src
RUN bash ./gradlew --no-daemon bootJar -x test

FROM eclipse-temurin:17-jre

WORKDIR /app
COPY --from=build /workspace/build/libs/danyeogam-backend-0.0.1-SNAPSHOT.jar /app/app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "/app/app.jar"]

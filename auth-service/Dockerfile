# Build from the repo root: docker build -f auth-service/Dockerfile .

FROM eclipse-temurin:21-jdk-alpine AS build
WORKDIR /workspace

COPY . .

RUN chmod +x gradlew && ./gradlew :auth-service:bootJar --no-daemon

RUN java -Djarmode=tools -jar auth-service/build/libs/*-SNAPSHOT.jar         extract --layers --destination extracted  && mv extracted/application/*.jar extracted/application/app.jar

FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

RUN addgroup -S app && adduser -S app -G app

COPY --from=build /workspace/extracted/dependencies/ ./
COPY --from=build /workspace/extracted/snapshot-dependencies/ ./
COPY --from=build /workspace/extracted/application/ ./

USER app
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]

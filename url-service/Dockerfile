# Build from the repo root: docker build -f url-service/Dockerfile .

FROM eclipse-temurin:21-jdk-alpine AS build
WORKDIR /workspace

COPY . .

RUN chmod +x gradlew && ./gradlew :url-service:bootJar --no-daemon

RUN java -Djarmode=tools -jar url-service/build/libs/*-SNAPSHOT.jar extract --layers --destination extracted  && mv extracted/application/*.jar extracted/application/app.jar

FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

RUN addgroup -S app && adduser -S app -G app

COPY --from=build /workspace/extracted/dependencies/ ./
COPY --from=build /workspace/extracted/snapshot-dependencies/ ./
COPY --from=build /workspace/extracted/application/ ./

USER app
EXPOSE 8081
ENTRYPOINT ["java", "-jar", "app.jar"]

FROM eclipse-temurin:17-jdk-alpine AS builder
WORKDIR /app
COPY pom.xml .
COPY src ./src
RUN ./mvnw dependency:go-offline -B
RUN ./mvnw package -DskipTests -B

FROM eclipse-temurin:17-jre-alpine
RUN apk add --no-cache bash curl
WORKDIR /app
COPY --from=builder /app/target/1a_java-kubeautomation-1.0.0-SNAPSHOT.jar app.jar
COPY src/main/resources/applic*.yaml /app/config/
COPY entrypoint.sh /app/entrypoint.sh
RUN chmod +x /app/entrypoint.sh
RUN addgroup -g 1000 -S chaos && adduser -u 1000 -S chaos -G chaos
USER chaos
ENTRYPOINT ["/app/entrypoint.sh"]
CMD ["--config", "/app/config/application.yaml"]

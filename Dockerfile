# 1. 빌드 단계
FROM eclipse-temurin:17-jdk AS builder
WORKDIR /app
COPY . .
RUN chmod +x gradlew && ./gradlew bootJar -x test --no-daemon
RUN cp "$(find build/libs -maxdepth 1 -name '*.jar' ! -name '*-plain.jar' | head -n 1)" /app/app.jar

# 2. 실행 단계
FROM eclipse-temurin:17-jre
WORKDIR /app
COPY --from=builder /app/app.jar app.jar

EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
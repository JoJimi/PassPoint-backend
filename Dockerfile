# 빌드 스테이지
FROM eclipse-temurin:21-jdk AS build
WORKDIR /app

COPY gradlew gradlew.bat settings.gradle.kts build.gradle.kts ./
COPY gradle gradle
RUN chmod +x gradlew && ./gradlew dependencies --no-daemon || true

COPY src src
RUN ./gradlew bootJar --no-daemon -x test

# 런타임 스테이지 (슬림 JRE, 빌드 도구 미포함)
FROM eclipse-temurin:21-jre AS runtime
WORKDIR /app

COPY --from=build /app/build/libs/*.jar app.jar

EXPOSE 8080
# 환경변수(SPRING_PROFILES_ACTIVE, FIREBASE_CREDENTIALS_JSON 등)는 컨테이너 실행 시 주입한다.
# 비밀값은 SSM에서 로드해 -e로 전달하며, 이미지 자체에는 포함하지 않는다.
ENTRYPOINT ["java", "-jar", "app.jar"]

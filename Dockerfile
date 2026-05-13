FROM eclipse-temurin:21-jdk-jammy AS build
WORKDIR /app
COPY gradlew gradlew.bat settings.gradle build.gradle ./
COPY gradle/ gradle/
RUN chmod +x gradlew && ./gradlew dependencies --no-daemon -q || true
COPY src/ src/
RUN ./gradlew bootJar --no-daemon -x test

FROM eclipse-temurin:21-jre-jammy
WORKDIR /app
RUN groupadd --system --gid 1001 kronos && \
    useradd --system --uid 1001 --gid kronos --no-create-home kronos
COPY --from=build /app/build/libs/kronos-0.0.1-SNAPSHOT.jar ./app.jar
RUN chown kronos:kronos ./app.jar
USER kronos
ENV SPRING_PROFILES_ACTIVE=prod
EXPOSE 8080
HEALTHCHECK --interval=30s --timeout=5s --start-period=60s --retries=3 \
    CMD wget -qO- http://localhost:8080/actuator/health || exit 1
CMD ["java", "-jar", "app.jar"]

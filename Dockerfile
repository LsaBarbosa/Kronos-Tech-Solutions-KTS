FROM gradle:jdk21 AS build
WORKDIR /app
COPY . .
RUN gradle bootJar

# Estágio de execução
FROM eclipse-temurin:21-jre-jammy
WORKDIR /app
COPY --from=build /app/build/libs/kronos-0.0.1-SNAPSHOT.jar ./app.jar
ENV SPRING_PROFILES_ACTIVE=prod
EXPOSE 8080
CMD ["java", "-jar", "app.jar"]

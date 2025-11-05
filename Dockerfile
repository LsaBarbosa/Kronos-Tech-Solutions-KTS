FROM gradle:jdk21 AS build
WORKDIR /app
COPY . .
RUN gradle bootJar

# Estágio de execução
FROM openjdk:21-jdk-slim
WORKDIR /app
COPY --from=build /app/build/libs/kronos-0.0.1-SNAPSHOT.jar ./app.jar
EXPOSE 8080
CMD ["java", "-jar", "app.jar"]
# Estágio 1: Build da aplicação com o Gradle
FROM gradle:8.5.0-jdk21-jammy AS build
WORKDIR /home/gradle/src
COPY --chown=gradle:gradle . .
RUN gradle bootJar --no-daemon

# Estágio 2: Criação da imagem final
FROM gcr.io/distroless/cc-debian12
WORKDIR /app

# Copia o JAR da aplicação do estágio de build
COPY --from=build /home/gradle/src/build/libs/*.jar ./application.jar

# Expõe a porta que a aplicação vai usar
EXPOSE 8080

# Comando para iniciar a aplicação
ENTRYPOINT ["/app/application.jar"]
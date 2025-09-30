# Estágio 1: Build - Usando a imagem oficial do GraalVM com o Native Image Toolset pré-instalado
FROM ghcr.io/graalvm/native-image:21 AS builder
WORKDIR /app
COPY . .

# Instalar utilitários essenciais (mantemos, pois findutils é necessário para o Gradle)
# Assumindo que esta imagem base também é RHEL/Oracle Linux (microdnf)
RUN microdnf update && microdnf install -y findutils

# Comando para construir a Native Image usando o Gradle Plugin
# O native-image tool está no PATH desta imagem
RUN chmod +x ./gradlew && ./gradlew clean nativeCompile

# Estágio 2: Execução - Usando Temurin JDK 21 (Alpine/Musl) para um binário pequeno
FROM eclipse-temurin:21-jre-alpine AS runtime
WORKDIR /app

# Copia o executável nativo do estágio de build
COPY --from=builder /app/build/native/nativeCompile/kronos .
COPY --from=builder /app/build/resources/main/application.yml .
EXPOSE 8080

# Comando de execução da Native Image (binário)
ENTRYPOINT ["./kronos"]
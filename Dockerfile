# Estágio de build
# Usamos 'gradle:jdk21' para o build.
FROM gradle:jdk21 AS build
WORKDIR /app
# Copia o build.gradle, settings.gradle e gradlew para aproveitar o cache do Docker nas dependências
COPY build.gradle settings.gradle gradlew /app/
COPY gradle /app/gradle
# Copia a aplicação
COPY . .
# Gera o JAR executável. Este passo é cacheado.
RUN gradle bootJar

# Estágio de execução
# Usamos a imagem Alpine JRE para menor tamanho e inicialização mais rápida.
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

# Argumento para pegar o nome correto do JAR.
# O nome do arquivo no build.gradle é 'kronos-0.0.1-SNAPSHOT.jar'
ARG JAR_FILE=build/libs/kronos-0.0.1-SNAPSHOT.jar

# Copia o JAR do estágio de build para o estágio de execução
COPY --from=build /app/${JAR_FILE} ./app.jar

# Define o usuário 'nonroot' para melhor segurança (opcional, mas boa prática)
USER nonroot

EXPOSE 8080
# Comando de inicialização.
CMD ["java", "-jar", "app.jar"]
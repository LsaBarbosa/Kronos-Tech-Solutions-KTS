FROM gradle:8.14.2-jdk21 AS build
WORKDIR /app

# Copia primeiro apenas arquivos de build para maximizar cache de dependências
COPY gradlew gradlew.bat build.gradle settings.gradle ./
COPY gradle ./gradle
RUN chmod +x ./gradlew
RUN ./gradlew --no-daemon dependencies

# Copia código da aplicação somente após resolver dependências
COPY src ./src
RUN ./gradlew --no-daemon bootJar -x test

# Estágio de execução
FROM eclipse-temurin:21-jre-jammy
WORKDIR /app

# Usuário sem privilégios para reduzir impacto em caso de comprometimento
RUN addgroup --system kronos && adduser --system --ingroup kronos kronos

COPY --from=build /app/build/libs/*.jar ./app.jar
RUN chown -R kronos:kronos /app

USER kronos
EXPOSE 8080
ENTRYPOINT ["java", "-XX:MaxRAMPercentage=75.0", "-XX:+UseG1GC", "-jar", "app.jar"]
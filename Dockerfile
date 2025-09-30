# Este Dockerfile apenas descreve a imagem final.
# O build será feito pelo Gradle.
FROM gcr.io/distroless/cc-debian12
WORKDIR /app
COPY . .
EXPOSE 8080
ENTRYPOINT ["./kronos"]
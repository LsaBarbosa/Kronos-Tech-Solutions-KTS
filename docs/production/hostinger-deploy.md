# Production Deployment — Hostinger VPS

## Objetivo

Descrever um guia seguro e genérico de deploy em produção do back-end Kronos em VPS, sem expor segredos operacionais.

## Arquitetura de produção

Referência mínima recomendada:

- aplicação Spring Boot em container ou processo Java dedicado;
- PostgreSQL gerenciado ou isolado;
- Nginx como proxy reverso;
- HTTPS obrigatório;
- armazenamento de documentos e biometria fora do repositório;
- observabilidade básica com health checks e logs.

## Pré-requisitos

- VPS Linux atualizada;
- Docker e/ou runtime Java 21 instalados;
- acesso administrativo controlado;
- domínio configurado, por exemplo `<APP_DOMAIN>`;
- banco disponível em `<DB_HOST>` para `<DB_NAME>`;
- segredos injetados por ambiente, nunca commitados.

## Variáveis de ambiente

Exemplos seguros de placeholders:

```bash
SPRING_PROFILES_ACTIVE=prod
SPRING_DATASOURCE_URL=jdbc:postgresql://<DB_HOST>:5432/<DB_NAME>
SPRING_DATASOURCE_USERNAME=<DB_USERNAME>
SPRING_DATASOURCE_PASSWORD=<DB_PASSWORD>
JWT_SECRET=<JWT_SECRET>
JWT_EXPIRATION=900000
AUTH_COOKIE_SECURE=true
AUTH_COOKIE_SAME_SITE=Lax
FRONTEND_ALLOWED_ORIGINS=https://<APP_DOMAIN>
AWS_REGION=<AWS_REGION>
AWS_ACCESS_KEY_ID=<AWS_ACCESS_KEY_ID>
AWS_SECRET_ACCESS_KEY=<AWS_SECRET_ACCESS_KEY>
AWS_S3_BUCKET_NAME=<AWS_S3_BUCKET_NAME>
AWS_REKOGNITION_COLLECTION_ID=<AWS_REKOGNITION_COLLECTION_ID>
```

`.env`, certificados, chaves e secrets não devem ser commitados.

## Banco de dados

- Aplicar migrations com Flyway no boot ou pipeline controlado.
- Validar conectividade antes do deploy.
- Ter rotina de backup para `<DB_NAME>`.
- Não executar mudanças destrutivas sem snapshot ou backup restaurável.

## Build da aplicação

Exemplo genérico:

```bash
./gradlew clean build
docker build -t kronos:prod .
```

Se o deploy for sem container:

```bash
./gradlew bootJar
```

## Execução como serviço

Exemplo conceitual usando `systemd`:

```ini
[Unit]
Description=Kronos Backend
After=network.target

[Service]
User=kronos
WorkingDirectory=/opt/kronos
EnvironmentFile=/opt/kronos/.env
ExecStart=/usr/bin/java -jar /opt/kronos/app.jar
Restart=always

[Install]
WantedBy=multi-user.target
```

Alternativamente, executar via Docker Compose ou orquestração equivalente.

## Nginx / proxy reverso

Exemplo seguro com placeholders:

```nginx
server {
    listen 80;
    server_name <APP_DOMAIN>;
    return 301 https://$host$request_uri;
}

server {
    listen 443 ssl http2;
    server_name <APP_DOMAIN>;

    ssl_certificate /etc/letsencrypt/live/<APP_DOMAIN>/fullchain.pem;
    ssl_certificate_key /etc/letsencrypt/live/<APP_DOMAIN>/privkey.pem;

    location / {
        proxy_pass http://127.0.0.1:8080;
        proxy_set_header Host $host;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto https;
    }
}
```

Antes de recarregar:

```bash
nginx -t
sudo systemctl reload nginx
```

## HTTPS

- HTTPS deve ser obrigatório em produção.
- Cookies de autenticação e CSRF sensíveis devem permanecer `Secure`.
- Certificados privados não devem ser armazenados no repositório.

## Health checks

Validar após deploy:

```bash
curl -fsS https://<APP_DOMAIN>/actuator/health
curl -fsS https://<APP_DOMAIN>/actuator/health/liveness
curl -fsS https://<APP_DOMAIN>/actuator/health/readiness
```

## Logs

- Centralizar logs da aplicação e do proxy.
- Evitar exposição desnecessária de PII, segredos, tokens ou payloads sensíveis.
- Revisar falhas de autenticação, incidentes e retenção.

## Backup

- Validar backup do banco e, quando aplicável, do storage documental.
- Testar restauração periodicamente.
- Registrar frequência, retenção e responsável operacional fora do repositório.

## Rollback

Ter um procedimento simples e testável:

- voltar para a imagem/tag anterior;
- restaurar artefato `.jar` anterior;
- restaurar backup se houver migração incompatível;
- revalidar health checks e logs após rollback.

Exemplo conceitual:

```bash
docker ps
docker image ls
docker stop kronos-app
docker run --name kronos-app --env-file /opt/kronos/.env <PREVIOUS_IMAGE_TAG>
```

## Segurança operacional

- Não expor Swagger publicamente em produção sem decisão explícita.
- Restringir origens CORS.
- Não reutilizar segredos entre ambientes.
- Limitar acesso SSH e privilégios administrativos.
- Revisar exposição de endpoints Actuator.
- Nunca commitar `.env`, certificados ou arquivos de chave.

## Checklist pós-deploy

- [ ] Build e artefato corretos foram publicados.
- [ ] Variáveis de ambiente foram carregadas sem segredos no repositório.
- [ ] `nginx -t` foi executado antes do reload.
- [ ] HTTPS está ativo.
- [ ] `/actuator/health` responde como esperado.
- [ ] Logs iniciais não mostram falhas críticas.
- [ ] Estratégia de rollback está disponível.

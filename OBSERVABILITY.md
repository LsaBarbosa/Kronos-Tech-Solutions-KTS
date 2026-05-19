# Observability KRONOS

## Visao geral

Esta implementacao adiciona uma camada de observabilidade ao back-end KRONOS sem alterar contratos funcionais existentes. O escopo cobre:

- Spring Boot Actuator em porta segregada
- endpoint externo seguro `GET /observability/status`
- metricas tecnicas e de negocio com Micrometer/Prometheus
- logs estruturados com correlation ID
- tracing opcional com OpenTelemetry/Tempo
- stack Docker para Prometheus, Grafana, Loki, Promtail e Tempo
- dashboards base
- regras de alerta Prometheus

Os fluxos funcionais existentes continuam sob JWT, CSRF e regras atuais de role.

## Arquitetura

- API principal: `:8080`
- management/Actuator: `127.0.0.1:8081`
- endpoint externo seguro: `GET /observability/status`
- Prometheus coleta `http://host.docker.internal:8081/actuator/prometheus`
- Promtail coleta `KRONOS_LOG_DIR=/var/log/kronos`
- Loki indexa logs estruturados
- Grafana le de Prometheus, Loki e Tempo
- Tempo recebe OTLP HTTP/GRPC quando tracing estiver habilitado

## Profiles

Arquivo novo:

- `src/main/resources/application-observability.yml`

Ativar em conjunto com o profile funcional:

```bash
./gradlew bootRun --args='--spring.profiles.active=local,observability'
```

ou em producao:

```bash
SPRING_PROFILES_ACTIVE=prod,observability ./gradlew bootRun
```

Principais propriedades:

- `MANAGEMENT_SERVER_PORT=8081`
- `MANAGEMENT_SERVER_ADDRESS=127.0.0.1`
- `TRACING_ENABLED=true`
- `TRACING_SAMPLING_PROBABILITY=0.1`
- `OTEL_EXPORTER_OTLP_ENDPOINT=http://localhost:4318/v1/traces`
- `LOG_FILE_NAME=./logs/kronos-backend.log`

## Endpoints

### Externo

- `GET /observability/status`

Retorna apenas:

- `application`
- `status`
- `environment`
- `timestamp`

Sem JWT, sem cookies de sessao, sem detalhes internos do Actuator.

### Internos

Disponiveis apenas na porta de management:

- `/actuator/health`
- `/actuator/info`
- `/actuator/metrics`
- `/actuator/prometheus`

Nao habilitados:

- `env`
- `beans`
- `configprops`
- `mappings`
- `threaddump`
- `heapdump`

## Seguranca

- `SecurityConfig` libera publicamente apenas `GET /observability/status`
- `/actuator/**` nao fica publico na API principal
- `/users/own-profile`, `/records/checkin`, `/documents`, `/legal/afd` seguem protegidos
- logs nao devem conter:
  - senha
  - JWT
  - `Authorization`
  - `faceImageBase64`
  - CPF/CNPJ
  - email
  - latitude/longitude
  - `storagePath`
  - nome de arquivo

## Correlation ID

Filtro:

- `src/main/java/com/kts/kronos/observability/adapter/in/web/CorrelationIdFilter.java`

Regra:

- reutiliza `X-Correlation-Id` se vier na request
- gera UUID se nao vier
- devolve o header na response
- escreve `correlationId` no MDC
- limpa MDC ao final

Padrao de log do profile `observability`:

```text
correlation_id=%X{correlationId:-na}
```

## Metricas

### Tecnicas

- `http_server_requests_seconds_*`
- `jvm_memory_used_bytes`
- `jvm_threads_live_threads`
- `jvm_gc_pause_seconds_count`
- `process_cpu_usage`
- `system_cpu_usage`
- `hikaricp_connections_*`

### Negocio

Autenticacao:

- `kronos_auth_login_success_total`
- `kronos_auth_login_failure_total`
- `kronos_auth_face_login_success_total`
- `kronos_auth_face_login_failure_total`
- `kronos_password_recovery_request_total`
- `kronos_password_recovery_email_sent_total`
- `kronos_password_recovery_failure_total`
- `kronos_password_reset_success_total`
- `kronos_password_reset_failure_total`

Ponto:

- `kronos_time_record_checkin_success_total`
- `kronos_time_record_checkout_success_total`
- `kronos_time_record_implicit_break_total`
- `kronos_time_record_day_off_converted_total`
- `kronos_time_record_absence_converted_total`
- `kronos_time_record_failure_total`
- `kronos_time_record_duration_seconds`

Documentos:

- `kronos_document_upload_success_total`
- `kronos_document_upload_failure_total`
- `kronos_document_download_success_total`
- `kronos_document_download_failure_total`
- `kronos_document_delete_success_total`
- `kronos_document_delete_failure_total`

Legal/Fiscal:

- `kronos_legal_afd_generation_success_total`
- `kronos_legal_afd_generation_failure_total`
- `kronos_legal_aej_generation_success_total`
- `kronos_legal_aej_generation_failure_total`
- `kronos_legal_point_mirror_generation_success_total`
- `kronos_legal_point_mirror_generation_failure_total`
- `kronos_legal_technical_certificate_success_total`
- `kronos_legal_technical_certificate_failure_total`
- `kronos_legal_generation_duration_seconds`

Schedulers:

- `kronos_scheduler_execution_success_total`
- `kronos_scheduler_execution_failure_total`
- `kronos_scheduler_execution_duration_seconds`
- `kronos_scheduler_records_processed_total`

NTP:

- `kronos_ntp_drift_seconds`

### Labels permitidas

- `operation`
- `result`
- `reason`
- `scheduler`
- `document_type`
- `legal_document_type`
- `action`

IDs de negocio, CPF/CNPJ, email, username, token, JWT, latitude, longitude e dados de arquivo nao devem virar label.

## Logs estruturados

Formato padrao:

```text
event=<event_name> result=<success|failure> reason=<reason>
```

Eventos cobertos:

- `auth_login`
- `auth_face_login`
- `password_recovery`
- `password_reset`
- `time_record_register`
- `document_upload`
- `document_download`
- `document_delete`
- `legal_afd_generation`
- `legal_aej_generation`
- `legal_point_mirror_generation`
- `legal_technical_certificate_generation`
- `scheduler_execution`
- `ntp_check`

## Tracing

Dependencias:

- `micrometer-tracing-bridge-otel`
- `opentelemetry-exporter-otlp`

Spans manuais adicionados em fluxos criticos:

- login facial
- validacao NTP
- validacao facial
- validacao geolocalizacao
- registro de ponto
- geracao AFD
- geracao AEJ
- assinatura digital
- upload de documento

Desligar tracing:

```bash
TRACING_ENABLED=false
```

## Stack Docker

Arquivos em `infra/observability/`:

- `docker-compose.yml`
- `prometheus/prometheus.yml`
- `prometheus/alerts.yml`
- `promtail/promtail.yml`
- `loki/loki-config.yml`
- `tempo/tempo.yml`
- `grafana/provisioning/datasources/datasources.yml`
- `grafana/provisioning/dashboards/dashboards.yml`
- `grafana/dashboards/*.json`
- `.env.example`
- `nginx/observability.kronostechsolutions.conf.example`

Subir:

```bash
cd infra/observability
cp .env.example .env
docker compose up -d
```

Regras da stack:

- Grafana publica apenas `127.0.0.1:3000`
- Prometheus sem porta publica
- Loki sem porta publica
- Tempo sem porta publica
- volumes persistentes
- `host.docker.internal` mapeado por `host-gateway`

## Prometheus

Target principal:

- `host.docker.internal:8081`

Validacao:

```bash
curl http://127.0.0.1:8081/actuator/health
curl http://127.0.0.1:8081/actuator/prometheus
docker compose -f infra/observability/docker-compose.yml exec prometheus wget -qO- http://localhost:9090/api/v1/targets
```

## Grafana

Datasources provisionados:

- Prometheus
- Loki
- Tempo

Dashboards provisionados:

- `kronos-api-dashboard.json`
- `kronos-jvm-dashboard.json`
- `kronos-hikari-dashboard.json`
- `kronos-business-dashboard.json`
- `kronos-logs-dashboard.json`

## Loki e Promtail

Promtail le:

```text
/var/log/kronos/*.log
```

Labels:

- `job=kronos-backend`
- `application=kronos-backend`
- `environment=${APP_ENV}`

## Nginx, SSL e Basic Auth

Arquivo exemplo:

- `infra/observability/nginx/observability.kronostechsolutions.conf.example`

Passos:

1. Criar `.htpasswd-observability`
2. Publicar Grafana via `127.0.0.1:3000`
3. Emitir certificado com Certbot
4. Forcar redirect HTTP -> HTTPS

Comandos tipicos:

```bash
sudo apt-get install -y apache2-utils
sudo htpasswd -c /etc/nginx/.htpasswd-observability <usuario>
sudo certbot --nginx -d observability.kronostechsolutions.com
```

## Firewall

Portas publicas permitidas:

- `22`
- `80`
- `443`

Portas que devem permanecer fechadas externamente:

- `3000`
- `8081`
- `9090`
- `3100`
- `3200`
- `5432`
- `6379`
- `5672`

Validacao:

```bash
sudo ufw status numbered
ss -tulpn | grep -E ':3000|:8081|:9090|:3100|:3200|:5432|:6379|:5672'
```

## Alertas

Regras prontas:

- `KronosBackendDown`
- `KronosBackendHttp5xx`
- `KronosBackendLatencyP95High`
- `KronosTimeRecordFailureSpike`
- `KronosSchedulerFailure`
- `KronosNtpDriftHigh`

Arquivo:

- `infra/observability/prometheus/alerts.yml`

## Testes e validacao

Baseline/regressao:

```bash
./gradlew clean test --no-daemon
```

Validacoes de seguranca:

```bash
curl -i http://localhost:8080/observability/status
curl -i http://localhost:8080/users/own-profile
curl -i http://localhost:8080/documents
curl -i http://localhost:8080/legal/afd
curl -i http://localhost:8080/actuator/prometheus
```

Esperado:

- `/observability/status` -> `200`
- endpoints protegidos -> `401` ou `403`
- `/actuator/prometheus` na porta principal -> nao exposto para uso publico

## Troubleshooting

### `/observability/status` retorna 404

- validar se a aplicacao subiu com o codigo novo
- validar `SecurityConfig`
- validar se o controller foi registrado

### Prometheus target DOWN

- validar `MANAGEMENT_SERVER_PORT`
- validar `MANAGEMENT_SERVER_ADDRESS`
- validar `host.docker.internal`
- validar que a API subiu com `prod,observability` ou `local,observability`

### Logs nao chegam no Loki

- validar `KRONOS_LOG_DIR`
- validar permissao de leitura do volume
- validar `docker compose logs promtail`

### Traces nao aparecem

- validar `TRACING_ENABLED=true`
- validar `OTEL_EXPORTER_OTLP_ENDPOINT`
- validar container `tempo`

## Checklist de deploy

- [ ] `./gradlew clean test --no-daemon` passou
- [ ] app sobe com `local,observability`
- [ ] app sobe com `prod,observability`
- [ ] `GET /observability/status` responde `200`
- [ ] `/actuator/prometheus` responde na porta `8081`
- [ ] endpoints protegidos continuam exigindo autenticacao/CSRF
- [ ] target `kronos-backend` aparece `UP` no Prometheus
- [ ] Grafana responde em `127.0.0.1:3000`
- [ ] Nginx/HTTPS publicados
- [ ] Basic Auth ativo no subdominio
- [ ] UFW sem expor portas internas
- [ ] dashboards provisionados
- [ ] alertas carregados
- [ ] logs sem dados sensiveis

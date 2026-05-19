# Backlog de Observabilidade Completa — KRONOS

## 1. Objetivo

Implementar uma camada completa de observabilidade no back-end KRONOS, cobrindo:

- métricas técnicas
- métricas de negócio
- logs estruturados
- correlation ID
- health check externo seguro
- Prometheus
- Grafana
- Loki
- Promtail
- tracing opcional com OpenTelemetry/Tempo
- dashboards
- alertas
- documentação operacional
- testes automatizados de segurança e regressão

A implementação deve preservar integralmente os fluxos existentes do sistema, principalmente:

- autenticação JWT
- login por senha
- login facial
- recuperação de senha
- registro de ponto
- documentos
- geração de AFD
- geração de AEJ
- espelho de ponto
- atestado técnico
- schedulers
- regras de autorização por role

---

## 2. Restrições obrigatórias

É proibido:

- alterar payloads de endpoints existentes
- alterar contratos de API existentes
- alterar regras de autenticação JWT
- alterar regras de autorização por role
- abrir endpoints protegidos acidentalmente
- expor `/actuator/**` publicamente
- expor Prometheus publicamente
- expor Loki publicamente
- expor porta interna do Actuator publicamente
- expor PostgreSQL, Redis ou RabbitMQ publicamente
- logar senha
- logar JWT
- logar header `Authorization`
- logar `faceImageBase64`
- logar latitude/longitude
- logar CPF
- logar CNPJ
- logar e-mail
- logar storage path
- logar chave S3/bucket
- usar IDs de negócio como label Prometheus
- usar `employeeId`, `companyId`, `userId`, `documentId`, `timeRecordId`, `cpf`, `cnpj`, `email`, `username` como label de métrica
- fazer refatoração estrutural grande fora do escopo

---

## 3. Épico 0 — Preparação e segurança

### OBS-0001 — Criar branch isolada de observabilidade

**Objetivo:**  
Garantir que o trabalho fique isolado da branch principal.

**Tarefas:**

```bash
git checkout main
git pull
git checkout -b feature/observability
```

**Critérios de aceite:**

- Branch criada.
- Projeto compila antes das alterações.
- Testes existentes rodam antes das alterações.
- Nenhuma regra de negócio alterada.

---

### OBS-0002 — Auditar configuração de segurança atual

**Objetivo:**  
Mapear como o Spring Security está configurado antes de adicionar endpoints novos.

**Tarefas:**

- Localizar:
  - `SecurityConfig`
  - filtros JWT
  - configuração de CORS
  - configuração de CSRF, se existir
  - lista atual de endpoints públicos
  - lista atual de endpoints protegidos
- Confirmar que os seguintes endpoints continuam protegidos:
  - `/users/own-profile`
  - `/records/checkin`
  - `/documents`
  - `/legal/afd`
  - `/legal/aej`
  - `/legal/espelho-ponto`
- Planejar liberação explícita apenas para:
  - `GET /observability/status`

**Critérios de aceite:**

- Mapeamento documentado no PR.
- Nenhum endpoint protegido fica público.
- `/actuator/**` não é liberado publicamente.

---

### OBS-0003 — Rodar baseline de testes

**Objetivo:**  
Registrar o estado dos testes antes da implementação.

**Tarefas:**

```bash
mvn clean test
```

**Critérios de aceite:**

- Se passar, seguir implementação.
- Se falhar, documentar falhas existentes antes de alterar código.
- Não misturar falhas pré-existentes com a implementação de observabilidade.

---

## 4. Épico 1 — Dependências e configuração base

### OBS-0101 — Adicionar Spring Boot Actuator

**Objetivo:**  
Habilitar endpoints técnicos de saúde, métricas e informações.

**Tarefas:**

Adicionar no `pom.xml`:

```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-actuator</artifactId>
</dependency>
```

Rodar:

```bash
mvn clean test
```

**Critérios de aceite:**

- Aplicação compila.
- `/actuator/health` funciona localmente.
- Nenhum endpoint funcional foi alterado.

---

### OBS-0102 — Adicionar Micrometer Prometheus

**Objetivo:**  
Permitir coleta de métricas pelo Prometheus.

**Tarefas:**

Adicionar no `pom.xml`:

```xml
<dependency>
    <groupId>io.micrometer</groupId>
    <artifactId>micrometer-registry-prometheus</artifactId>
</dependency>
```

**Critérios de aceite:**

- `/actuator/prometheus` disponível localmente.
- Métricas HTTP aparecem.
- Métricas JVM aparecem.
- Métricas Hikari aparecem quando banco está configurado.

---

### OBS-0103 — Criar profile `application-observability.yml`

**Objetivo:**  
Separar configuração de observabilidade.

**Arquivo:**

```text
src/main/resources/application-observability.yml
```

**Conteúdo sugerido:**

```yaml
management:
  server:
    port: ${MANAGEMENT_SERVER_PORT:8081}

  endpoints:
    web:
      base-path: /actuator
      exposure:
        include:
          - health
          - info
          - metrics
          - prometheus

  endpoint:
    health:
      show-details: never
      probes:
        enabled: true

  health:
    db:
      enabled: true

  metrics:
    tags:
      application: ${spring.application.name:kronos-backend}
      environment: ${APP_ENV:local}

  prometheus:
    metrics:
      export:
        enabled: true

  tracing:
    sampling:
      probability: ${TRACING_SAMPLING_PROBABILITY:0.1}
```

**Critérios de aceite:**

- Aplicação sobe com `local,observability`.
- Aplicação sobe com `prod,observability`.
- `show-details` não vaza detalhes técnicos.
- Management port usa `8081` por padrão.
- A porta de management não é pública.

---

### OBS-0104 — Validar profile de observabilidade local

**Objetivo:**  
Garantir que o profile funciona isoladamente.

**Comando:**

```bash
mvn spring-boot:run -Dspring-boot.run.profiles=local,observability
```

**Validações:**

```bash
curl http://localhost:8081/actuator/health
curl http://localhost:8081/actuator/prometheus
```

**Critérios de aceite:**

- Health retorna `UP`.
- Prometheus retorna métricas.
- API principal continua respondendo na porta padrão.

---

## 5. Épico 2 — Endpoint externo seguro

### OBS-0201 — Criar pacote de observabilidade

**Objetivo:**  
Isolar a implementação de observabilidade.

**Estrutura sugerida:**

```text
src/main/java/.../observability/
  adapter/in/web/ObservabilityController.java
  application/ObservabilityStatusUseCase.java
  application/impl/ObservabilityStatusUseCaseImpl.java
  domain/ObservabilityStatus.java
```

**Critérios de aceite:**

- Pacote isolado.
- Sem dependência direta de entidades de negócio.
- Sem acesso a CPF, CNPJ, e-mail, usuário, token ou documentos.

---

### OBS-0202 — Criar response sanitizado de status

**Objetivo:**  
Retornar apenas dados seguros.

**Classe:**

```java
public record ObservabilityStatus(
        String application,
        String status,
        String environment,
        OffsetDateTime timestamp
) {
}
```

**Não incluir:**

- stack trace
- usuário
- CPF
- CNPJ
- e-mail
- token
- JWT
- URL do banco
- IP interno
- storage path
- credenciais
- detalhes do Actuator

**Critérios de aceite:**

- JSON não contém dados sensíveis.
- Teste valida ausência de campos sensíveis.

---

### OBS-0203 — Criar `GET /observability/status`

**Objetivo:**  
Permitir checagem externa simples da aplicação.

**Controller sugerido:**

```java
@RestController
@RequiredArgsConstructor
public class ObservabilityController {

    private final ObservabilityStatusUseCase useCase;

    @GetMapping("/observability/status")
    public ObservabilityStatus status() {
        return useCase.getStatus();
    }
}
```

**Critérios de aceite:**

- Endpoint retorna `200`.
- Endpoint não exige JWT.
- Endpoint não retorna detalhes internos.
- Endpoint não altera sessão nem cookies.

---

### OBS-0204 — Liberar apenas `/observability/status` no SecurityConfig

**Objetivo:**  
Evitar abertura acidental de endpoints protegidos.

**Tarefas:**

- Adicionar permissão explícita para:
  - `GET /observability/status`
- Não liberar:
  - `/actuator/**`
  - `/records/**`
  - `/documents/**`
  - `/legal/**`
  - `/users/**`
  - `/employee/**`

**Critérios de aceite:**

- `/observability/status` responde sem token.
- `/records/checkin` continua protegido.
- `/documents` continua protegido.
- `/legal/afd` continua protegido.
- `/users/own-profile` continua protegido.

---

### OBS-0205 — Criar testes de segurança do endpoint externo

**Objetivo:**  
Garantir que a observabilidade não quebrou segurança.

**Testes com MockMvc:**

- `GET /observability/status` sem token → `200`
- `GET /users/own-profile` sem token → `401` ou `403`
- `POST /records/checkin` sem token → `401` ou `403`
- `GET /documents` sem token → `401` ou `403`
- `GET /legal/afd` sem token → `401` ou `403`

**Critérios de aceite:**

- Todos os testes passam.
- Nenhum endpoint sensível ficou público.

---

## 6. Épico 3 — Métricas técnicas padrão

### OBS-0301 — Validar métricas HTTP

**Objetivo:**  
Garantir visibilidade sobre requisições.

**Métricas esperadas:**

```text
http_server_requests_seconds_count
http_server_requests_seconds_sum
http_server_requests_seconds_max
```

**Critérios de aceite:**

- Métricas aparecem após chamar endpoints.
- Labels incluem:
  - `method`
  - `uri`
  - `status`
  - `exception`
  - `application`
  - `environment`

---

### OBS-0302 — Validar métricas JVM

**Objetivo:**  
Monitorar saúde da JVM.

**Métricas esperadas:**

```text
jvm_memory_used_bytes
jvm_threads_live_threads
jvm_gc_pause_seconds_count
process_cpu_usage
system_cpu_usage
```

**Critérios de aceite:**

- Métricas aparecem em `/actuator/prometheus`.
- Métricas são consultáveis no Prometheus.

---

### OBS-0303 — Validar métricas Hikari/PostgreSQL

**Objetivo:**  
Monitorar pool de conexões.

**Métricas esperadas:**

```text
hikaricp_connections_active
hikaricp_connections_idle
hikaricp_connections_pending
hikaricp_connections_timeout_total
```

**Critérios de aceite:**

- Métricas aparecem.
- Não há exposição de connection string.
- Painel de banco pode usar essas métricas.

---

## 7. Épico 4 — Métricas de negócio

### OBS-0401 — Criar componente `KronosMetrics`

**Objetivo:**  
Centralizar métricas customizadas.

**Classe sugerida:**

```text
src/main/java/.../observability/application/KronosMetrics.java
```

**Responsabilidades:**

- Registrar counters.
- Registrar timers.
- Registrar gauges.
- Fornecer métodos seguros para incremento.
- Evitar labels proibidas.

**Critérios de aceite:**

- Componente usa `MeterRegistry`.
- Nomes padronizados.
- Nenhuma métrica usa ID de entidade como label.

---

### OBS-0402 — Instrumentar login por senha

**Métricas:**

```text
kronos_auth_login_success_total
kronos_auth_login_failure_total
```

**Tarefas:**

- Incrementar sucesso após login válido.
- Incrementar falha após autenticação inválida.
- Não logar senha.
- Não logar JWT.
- Não alterar payload do login.

**Critérios de aceite:**

- Login continua funcionando.
- Métrica de sucesso incrementa.
- Métrica de falha incrementa.
- Testes passam.

---

### OBS-0403 — Instrumentar login facial

**Métricas:**

```text
kronos_auth_face_login_success_total
kronos_auth_face_login_failure_total
```

**Razões permitidas:**

```text
invalid_image
face_not_recognized
inactive_user
user_not_found
unknown
```

**Critérios de aceite:**

- Login facial continua funcionando.
- Base64 da imagem nunca é logado.
- Employee ID não vira label.
- Falhas incrementam métrica.

---

### OBS-0404 — Instrumentar recuperação e reset de senha

**Métricas:**

```text
kronos_password_recovery_request_total
kronos_password_recovery_email_sent_total
kronos_password_recovery_failure_total
kronos_password_reset_success_total
kronos_password_reset_failure_total
```

**Regras:**

- Preservar anti-enumeração.
- Não diferenciar externamente CPF/e-mail existente ou inexistente.
- Não logar CPF.
- Não logar e-mail.
- Não logar token de reset.

**Critérios de aceite:**

- Endpoint mantém retorno atual.
- Testes existentes passam.
- Métricas não vazam dados.

---

### OBS-0405 — Instrumentar registro de ponto

**Métricas:**

```text
kronos_time_record_checkin_success_total
kronos_time_record_checkout_success_total
kronos_time_record_implicit_break_total
kronos_time_record_day_off_converted_total
kronos_time_record_absence_converted_total
kronos_time_record_failure_total
kronos_time_record_duration_seconds
```

**Razões permitidas para falha:**

```text
ntp
face
geolocation
status
unknown
```

**Critérios de aceite:**

- Check-in incrementa sucesso.
- Check-out incrementa sucesso.
- Falha NTP incrementa `reason=ntp`.
- Falha facial incrementa `reason=face`.
- Falha geolocalização incrementa `reason=geolocation`.
- Latitude/longitude não aparecem em log.
- `faceImageBase64` não aparece em log.
- Nenhuma regra de ponto é alterada.

---

### OBS-0406 — Instrumentar documentos

**Métricas:**

```text
kronos_document_upload_success_total
kronos_document_upload_failure_total
kronos_document_download_success_total
kronos_document_download_failure_total
kronos_document_delete_success_total
kronos_document_delete_failure_total
```

**Labels permitidas:**

```text
operation
document_type
result
reason
```

**Critérios de aceite:**

- Upload incrementa sucesso/falha.
- Download incrementa sucesso/falha.
- Delete incrementa sucesso/falha.
- `storagePath` não aparece em log.
- Nome do arquivo não vira label.

---

### OBS-0407 — Instrumentar legal/fiscal

**Métricas:**

```text
kronos_legal_afd_generation_success_total
kronos_legal_afd_generation_failure_total
kronos_legal_aej_generation_success_total
kronos_legal_aej_generation_failure_total
kronos_legal_point_mirror_generation_success_total
kronos_legal_point_mirror_generation_failure_total
kronos_legal_technical_certificate_success_total
kronos_legal_technical_certificate_failure_total
kronos_legal_generation_duration_seconds
```

**Labels permitidas:**

```text
legal_document_type=afd|aej|point_mirror|technical_certificate
result=success|failure
reason=digital_signature|generation|unknown
```

**Critérios de aceite:**

- AFD incrementa métrica.
- AEJ incrementa métrica.
- Espelho incrementa métrica.
- Atestado incrementa métrica.
- Falha de assinatura digital é observável.
- Conteúdo dos documentos não é logado.

---

### OBS-0408 — Instrumentar schedulers

**Métricas:**

```text
kronos_scheduler_execution_success_total
kronos_scheduler_execution_failure_total
kronos_scheduler_execution_duration_seconds
kronos_scheduler_records_processed_total
```

**Schedulers:**

```text
day_off
weekly_swap
password_token_cleanup
message_cleanup
approval_cleanup
time_sync
```

**Critérios de aceite:**

- Cada scheduler registra sucesso.
- Cada scheduler registra falha.
- Duração é medida.
- Quantidade processada é medida quando possível.
- Crons não são alterados.

---

### OBS-0409 — Instrumentar drift NTP

**Métrica:**

```text
kronos_ntp_drift_seconds
```

**Critérios de aceite:**

- Gauge registra desvio de horário.
- Falha de consulta NTP gera log seguro.
- Não altera regra atual de bloqueio por horário divergente.

---

## 8. Épico 5 — Logs estruturados e correlation ID

### OBS-0501 — Criar filtro `CorrelationIdFilter`

**Objetivo:**  
Rastrear requests.

**Header:**

```text
X-Correlation-Id
```

**Comportamento:**

- Se request tiver header, reutilizar.
- Se não tiver, gerar UUID.
- Adicionar no MDC.
- Retornar no response.
- Limpar MDC ao final.

**Critérios de aceite:**

- Response sempre tem `X-Correlation-Id`.
- Logs carregam correlation ID.
- MDC é limpo no `finally`.
- Testes com MockMvc passam.

---

### OBS-0502 — Padronizar eventos de log

**Formato recomendado:**

```text
event=<event_name> result=<success|failure> reason=<reason>
```

**Eventos mínimos:**

```text
auth_login
auth_face_login
password_recovery
password_reset
time_record_register
document_upload
document_download
document_delete
legal_afd_generation
legal_aej_generation
legal_point_mirror_generation
legal_technical_certificate_generation
scheduler_execution
ntp_check
```

**Critérios de aceite:**

- Logs relevantes possuem `event`.
- Logs de erro possuem `reason`.
- Logs possuem correlation ID via MDC.

---

### OBS-0503 — Sanitizar logs sensíveis

**Campos proibidos:**

```text
password
newPassword
confirmPassword
token
Authorization
JWT
faceImageBase64
cpf
cnpj
email
latitude
longitude
storagePath
s3Key
fileName
```

**Critérios de aceite:**

- Busca textual não encontra logs com objetos inteiros sensíveis.
- Não existe `ex.printStackTrace()`.
- Testes validam ausência de campos sensíveis em logs críticos.

---

## 9. Épico 6 — Tracing distribuído

### OBS-0601 — Adicionar Micrometer Tracing/OpenTelemetry

**Objetivo:**  
Permitir traces.

**Dependências sugeridas, se compatíveis:**

```xml
<dependency>
    <groupId>io.micrometer</groupId>
    <artifactId>micrometer-tracing-bridge-otel</artifactId>
</dependency>

<dependency>
    <groupId>io.opentelemetry</groupId>
    <artifactId>opentelemetry-exporter-otlp</artifactId>
</dependency>
```

**Critérios de aceite:**

- Projeto compila.
- Falha no collector não derruba aplicação.
- Tracing pode ser desligado por configuração.

---

### OBS-0602 — Configurar exportação OTLP opcional

**Configuração:**

```yaml
management:
  tracing:
    sampling:
      probability: ${TRACING_SAMPLING_PROBABILITY:0.1}

  otlp:
    tracing:
      endpoint: ${OTEL_EXPORTER_OTLP_ENDPOINT:http://localhost:4318/v1/traces}
```

**Critérios de aceite:**

- Em produção sampling inicial não é 100%.
- Endpoint configurável via variável.
- Aplicação sobe mesmo sem collector.

---

### OBS-0603 — Criar spans manuais em fluxos críticos

**Fluxos:**

- login facial
- validação NTP
- validação facial
- validação geolocalização
- registro de ponto
- geração AFD
- geração AEJ
- assinatura digital
- upload de documento

**Critérios de aceite:**

- Spans não contêm dados sensíveis.
- Spans possuem nomes estáveis.
- Exceções são marcadas no span.

---

## 10. Épico 7 — Infraestrutura Docker

### OBS-0701 — Criar estrutura `infra/observability`

**Estrutura:**

```text
infra/observability/
  docker-compose.yml
  prometheus/prometheus.yml
  promtail/promtail.yml
  grafana/provisioning/datasources/
  grafana/provisioning/dashboards/
  grafana/dashboards/
```

**Critérios de aceite:**

- Estrutura versionada.
- Sem senhas hardcoded.
- `.env.example` criado.

---

### OBS-0702 — Criar Docker Compose

**Serviços:**

- Prometheus
- Grafana
- Loki
- Promtail
- Tempo, opcional

**Regras:**

- Grafana somente em `127.0.0.1:3000:3000`.
- Prometheus sem porta pública.
- Loki sem porta pública.
- Volumes persistentes.

**Critérios de aceite:**

- `docker compose up -d` sobe stack.
- Containers reiniciam com `unless-stopped`.
- Nenhum serviço interno fica público.

---

### OBS-0703 — Configurar Prometheus

**Arquivo:**

```text
infra/observability/prometheus/prometheus.yml
```

**Configuração:**

```yaml
global:
  scrape_interval: 15s
  evaluation_interval: 15s

scrape_configs:
  - job_name: "kronos-backend"
    metrics_path: "/actuator/prometheus"
    static_configs:
      - targets:
          - "host.docker.internal:8081"
```

**Critérios de aceite:**

- Target `kronos-backend` aparece UP.
- Métricas aparecem no Prometheus.

---

### OBS-0704 — Configurar Promtail

**Arquivo:**

```text
infra/observability/promtail/promtail.yml
```

**Objetivo:**  
Coletar logs em:

```text
/var/log/kronos/*.log
```

**Critérios de aceite:**

- Logs chegam no Loki.
- Labels:
  - `application=kronos-backend`
  - `environment=production`
  - `job=kronos-backend`

---

### OBS-0705 — Provisionar datasources Grafana

**Datasources:**

- Prometheus
- Loki
- Tempo, se existir

**Critérios de aceite:**

- Grafana sobe com datasources prontos.
- Sem configuração manual obrigatória.

---

## 11. Épico 8 — Nginx, SSL e firewall

### OBS-0801 — Configurar Nginx para Grafana

**Subdomínio sugerido:**

```text
observability.kronostechsolutions.com
```

**Proxy:**

```text
http://127.0.0.1:3000
```

**Critérios de aceite:**

- Grafana acessível via HTTPS.
- Porta 3000 não pública.
- Headers `X-Forwarded-*` configurados.

---

### OBS-0802 — Adicionar Basic Auth

**Objetivo:**  
Criar camada extra de segurança antes do Grafana.

**Critérios de aceite:**

- Acesso exige Basic Auth.
- Grafana também exige login.
- Senha não versionada.

---

### OBS-0803 — Configurar SSL com Certbot

**Critérios de aceite:**

- Certificado válido.
- Renovação automática ativa.
- HTTP redireciona para HTTPS.

---

### OBS-0804 — Revisar firewall

**Portas públicas permitidas:**

```text
22
80
443
```

**Portas proibidas publicamente:**

```text
3000
8081
9090
3100
3200
5432
6379
5672
```

**Critérios de aceite:**

- `ufw status` mostra apenas portas necessárias.
- Serviços internos acessíveis apenas local/rede Docker.

---

## 12. Épico 9 — Dashboards

### OBS-0901 — Dashboard API

**Painéis:**

- requests por minuto
- erros 4xx
- erros 5xx
- latência média
- latência p95
- endpoints lentos
- endpoints com mais erro

**Critérios de aceite:**

- Dashboard exportado como JSON.
- Usa labels `application` e `environment`.

---

### OBS-0902 — Dashboard JVM

**Painéis:**

- heap usado
- non-heap usado
- threads
- GC
- CPU
- uptime

**Critérios de aceite:**

- Dashboard exportado.
- Métricas carregam corretamente.

---

### OBS-0903 — Dashboard PostgreSQL/Hikari

**Painéis:**

- conexões ativas
- conexões ociosas
- conexões pendentes
- timeouts
- uso máximo do pool

**Critérios de aceite:**

- Dashboard ajuda a diagnosticar saturação de banco.
- Usa métricas Hikari.

---

### OBS-0904 — Dashboard negócio KRONOS

**Painéis:**

- logins com sucesso/falha
- login facial
- check-ins
- check-outs
- falhas de ponto
- falhas por NTP
- falhas por face
- falhas por geolocalização
- uploads/downloads de documentos
- geração AFD
- geração AEJ
- geração espelho de ponto
- geração atestado técnico
- schedulers
- drift NTP

**Critérios de aceite:**

- Dashboard operacional permite entender saúde do negócio.
- Nenhum painel depende de dados pessoais.

---

### OBS-0905 — Dashboard logs

**Painéis:**

- erros recentes
- logs por event
- logs por correlation ID
- falhas de schedulers
- falhas legal/fiscal
- falhas de autenticação

**Critérios de aceite:**

- Logs consultáveis no Grafana.
- Correlation ID funciona.

---

## 13. Épico 10 — Alertas

### OBS-1001 — Alerta aplicação down

```promql
up{job="kronos-backend"} == 0
```

**Severidade:** crítica.

---

### OBS-1002 — Alerta erro 5xx

```promql
sum(rate(http_server_requests_seconds_count{status=~"5.."}[5m])) > 0
```

**Severidade:** alta.

---

### OBS-1003 — Alerta latência alta

```promql
histogram_quantile(
  0.95,
  sum(rate(http_server_requests_seconds_bucket[5m])) by (le, uri)
) > 2
```

**Severidade:** média/alta.

---

### OBS-1004 — Alerta falha de registro de ponto

```promql
increase(kronos_time_record_failure_total[10m]) > 5
```

**Severidade:** crítica.

---

### OBS-1005 — Alerta falha de scheduler

```promql
increase(kronos_scheduler_execution_failure_total[30m]) > 0
```

**Severidade:** alta.

---

### OBS-1006 — Alerta drift NTP

```promql
abs(kronos_ntp_drift_seconds) > 5
```

**Severidade:** crítica.

---

## 14. Épico 11 — Testes

### OBS-1101 — Testes de segurança

**Obrigatório:**

- `/observability/status` público.
- `/users/own-profile` protegido.
- `/records/checkin` protegido.
- `/documents` protegido.
- `/legal/afd` protegido.
- `/actuator/prometheus` não público na API principal.

---

### OBS-1102 — Testes de métricas customizadas

**Cobrir:**

- login sucesso/falha
- login facial sucesso/falha
- ponto sucesso/falha
- documentos sucesso/falha
- legal/fiscal sucesso/falha
- schedulers sucesso/falha

---

### OBS-1103 — Testes de correlation ID

**Cobrir:**

- request sem header gera correlation ID.
- request com header preserva correlation ID.
- response retorna header.
- MDC é limpo.

---

### OBS-1104 — Testes de sanitização

**Validar que logs não contêm:**

- senha
- JWT
- Authorization
- faceImageBase64
- latitude
- longitude
- CPF
- CNPJ
- e-mail
- storagePath

---

### OBS-1105 — Testes do endpoint externo

**Cobrir:**

- status retorna JSON.
- status não contém campos sensíveis.
- status retorna ambiente.
- status retorna timestamp.

---

## 15. Épico 12 — Documentação

### OBS-1201 — Criar `OBSERVABILITY.md`

**Conteúdo:**

- visão geral
- arquitetura
- profiles
- endpoints
- métricas
- logs
- tracing
- Docker Compose
- Prometheus
- Grafana
- Loki
- Promtail
- Nginx
- SSL
- firewall
- dashboards
- alertas
- troubleshooting

---

### OBS-1202 — Criar checklist de deploy

**Checklist:**

- build passou
- testes passaram
- app sobe com `prod,observability`
- `/observability/status` externo OK
- `/actuator/prometheus` interno OK
- Prometheus target UP
- Grafana HTTPS OK
- Basic Auth OK
- UFW OK
- dashboards importados
- alertas configurados
- logs sem dados sensíveis

---

## 16. Ordem recomendada de execução

```text
1. OBS-0001 até OBS-0003
2. OBS-0101 até OBS-0104
3. OBS-0201 até OBS-0205
4. OBS-0301 até OBS-0303
5. OBS-0401
6. OBS-0402 até OBS-0409
7. OBS-0501 até OBS-0503
8. OBS-0601 até OBS-0603
9. OBS-1101 até OBS-1105
10. OBS-0701 até OBS-0705
11. OBS-0801 até OBS-0804
12. OBS-0901 até OBS-0905
13. OBS-1001 até OBS-1006
14. OBS-1201 até OBS-1202
```

---

## 17. Definição de pronto

A observabilidade estará concluída quando:

- `mvn clean test` passar
- aplicação subir com `local,observability`
- aplicação subir com `prod,observability`
- `/observability/status` funcionar
- `/actuator/prometheus` funcionar internamente
- endpoints protegidos continuarem protegidos
- Prometheus coletar métricas
- Grafana exibir dashboards
- Loki receber logs
- correlation ID funcionar
- logs não vazarem dados sensíveis
- métricas de negócio aparecerem
- schedulers forem observáveis
- alertas mínimos estiverem definidos
- documentação operacional existir

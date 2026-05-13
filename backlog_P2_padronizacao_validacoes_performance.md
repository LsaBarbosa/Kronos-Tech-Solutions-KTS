# Backlog de Correções — Kronos `PROD_HOSTINGER`

> Origem: auditoria de produção da branch `PROD_HOSTINGER`.
> Objetivo: organizar correções e melhorias por prioridade para preparar a branch para produção.

# Prioridade P2 — Padronização REST, Validações, Upload, Observabilidade e Performance

## Objetivo

Elevar a qualidade do contrato HTTP, reduzir bugs previsíveis, melhorar validação de entrada, reforçar upload/download, alinhar observabilidade e corrigir gargalos de performance.

## Critério para encerrar P2

A prioridade P2 só deve ser considerada concluída quando:

- status codes REST estiverem padronizados;
- responses de sucesso forem consistentes;
- DTOs críticos tiverem Bean Validation;
- CPF/CNPJ forem validados corretamente;
- relatórios validarem datas e referência;
- upload tiver política segura;
- Prometheus/Actuator estiver alinhado;
- scanner de dependências existir no CI;
- queries críticas de ponto e AEJ não carregarem histórico completo em memória.

---

# KRN-P2-001 — Padronizar status codes REST

## Tipo

Contrato API

## Severidade

Média

## Problema

Vários endpoints `POST`, `PATCH` e `DELETE` retornam `200` vazio.

## Ajustes obrigatórios

| Endpoint | Atual | Novo |
|---|---:|---:|
| `POST /companies` | 200 | 201 |
| `POST /users` | 200 | 201 |
| `POST /documents/{employeeId}/upload` | 200 | 201 |
| `PATCH /companies/{cnpj}` | 200 | 204 |
| `PATCH /users/{id}` | 200 | 204 |
| `DELETE /users/{id}` | 200 | 204 |
| `DELETE /companies/{cnpj}` | 200 | 204 |
| `DELETE /documents/{id}` | 200 | 204 |
| `PATCH /time-records/{id}` | 200 | 204 |
| `PATCH /time-records/vacation/{id}/approve` | 200 | 204 |

## Critérios de aceite

- Criação retorna `201`.
- Atualização sem body retorna `204`.
- Delete/inativação retorna `204`.
- Erro de validação retorna `400`.
- Duplicidade retorna `409`.
- Sem autenticação retorna `401`.
- Sem permissão retorna `403`.

## Testes obrigatórios

- Testes de controller para cada status.
- Testes de contrato para responses vazios.

---

# KRN-P2-002 — Padronizar responses de sucesso

## Tipo

Contrato API

## Severidade

Média

## Problema

Há mistura de DTOs, `void`, `Boolean` cru, `Long` cru e responses inconsistentes.

## Ajustes recomendados

| Atual | Novo |
|---|---|
| `Boolean` cru em `/terms/status` | `TermsStatusResponse` |
| `Long` cru em `/time-records/time-off` | `TimeOffRequestResponse` |
| `void` com 200 | `204 No Content` |
| `LoginResponse(token)` | Cookie + DTO sem token |

## DTOs sugeridos

```java
public record TermsStatusResponse(boolean accepted) {}
```

```java
public record TimeOffRequestResponse(Long firstTimeRecordId) {}
```

```java
public record CreatedResourceResponse<T>(T id) {}
```

## Critérios de aceite

- Nenhum endpoint retorna valor primitivo cru.
- Nenhum `void` retorna `200`.
- Responses seguem padrão previsível.
- Front-end consegue consumir sem heurísticas.

---

# KRN-P2-003 — Adicionar Bean Validation nos DTOs críticos

## Tipo

Validação

## Severidade

Alta / Média

## Problema

DTOs críticos não possuem validação suficiente e alguns controllers não usam `@Valid`.

## DTOs prioritários

| DTO | Validações |
|---|---|
| `LoginRequest` | `@NotBlank username`, `@NotBlank password` |
| `ResetPasswordRequest` | token e senha obrigatórios |
| `RecoverPasswordRequest` | CPF e e-mail obrigatórios |
| `CreateCompanyRequest` | CNPJ, nome, e-mail, CEP |
| `CreateEmployeeRequest` | CPF, nome, e-mail, cargo |
| `ListReportRequest` | `reference` com pattern `HH:mm` |
| Upload requests | arquivo obrigatório/tipo válido |

## Exemplo

```java
public record LoginRequest(
        @NotBlank String username,
        @NotBlank String password
) {}
```

Controller:

```java
public ResponseEntity<Void> login(@Valid @RequestBody LoginRequest request)
```

## Critérios de aceite

- Payload nulo não gera `500`.
- Campos obrigatórios retornam `400`.
- Erros aparecem em `validationErrors`.
- Senhas inválidas retornam `400`.
- Datas inválidas retornam `400`.

---

# KRN-P2-004 — Validar CPF/CNPJ com dígito verificador

## Tipo

Validação / Domínio

## Severidade

Média

## Problema

CNPJ é validado apenas por regex de 14 dígitos.

## Implementação esperada

Criar anotações customizadas:

```java
@ValidCnpj
private String cnpj;
```

```java
@ValidCpf
private String cpf;
```

## Critérios de aceite

- CNPJ com 14 números inválidos retorna `400`.
- CPF inválido retorna `400`.
- CPF/CNPJ são normalizados removendo máscara.
- Duplicidade retorna `409`.

## Testes obrigatórios

- CPF válido.
- CPF inválido.
- CNPJ válido.
- CNPJ inválido.
- CNPJ com máscara.
- CNPJ sem máscara.

---

# KRN-P2-005 — Validar relatórios e intervalos de datas

## Tipo

Validação / Ponto

## Severidade

Média

## Problema

`reference` de relatório pode gerar erro interno por `split` direto.

## Arquivos envolvidos

| Arquivo | Ação |
|---|---|
| `ListReportRequest.java` | Adicionar `@Pattern` |
| `TimeRecordService.java` | Validar antes de processar |
| `RestExceptionHandler.java` | Retornar erro 400 |

## Regras

- `reference` deve ser `HH:mm`.
- `startDate <= endDate`.
- Intervalo máximo deve ser definido.
- Datas vazias devem retornar `400`.

## Critérios de aceite

- `reference=abc` retorna `400`.
- `reference=25:99` retorna `400`.
- Intervalo invertido retorna `400`.
- Intervalo excessivo retorna `400` ou `422`, conforme padrão definido.

---

# KRN-P2-006 — Política de antivírus ou bloqueio formal de arquivos

## Tipo

Segurança / Upload

## Severidade

Média

## Problema

Upload valida tipo, mas antivírus está desabilitado por default.

## Caminhos possíveis

### Opção A — Integrar ClamAV

- Validar arquivo antes de salvar.
- Rejeitar arquivo infectado.
- Registrar evento de segurança.

### Opção B — Política restritiva temporária

Enquanto não houver antivírus:

- permitir somente PDF/JPG/PNG;
- limitar tamanho;
- validar magic bytes;
- impedir DOC/DOCX;
- registrar decisão formal.

## Critérios de aceite

- Arquivo `.exe` renomeado para `.pdf` é rejeitado.
- MIME real é validado.
- Tamanho máximo é respeitado.
- Nome do arquivo é sanitizado.
- Download não vaza `storagePath`.

## Testes obrigatórios

- Upload PDF válido.
- Upload MIME falso.
- Upload acima do limite.
- Filename com path traversal.
- Download com `Content-Disposition` seguro.

---

# KRN-P2-007 — Corrigir Prometheus/Actuator

## Tipo

Observabilidade

## Severidade

Média

## Problema

Prometheus aponta para `/actuator/prometheus`, mas actuator expõe apenas `health`.

## Opções

### Opção A — Expor Prometheus

```yaml
management:
  endpoints:
    web:
      exposure:
        include: health,prometheus
```

### Opção B — Remover scrape Prometheus

Caso não seja usado, remover configuração de scrape.

## Critérios de aceite

- Configuração e infraestrutura estão alinhadas.
- `/actuator/health` funciona.
- `/actuator/prometheus` funciona ou não é mais referenciado.
- Endpoint sensível não fica público indevidamente.

---

# KRN-P2-008 — Adicionar scanner de dependências no CI

## Tipo

Segurança / CI

## Severidade

Média

## Problema

Não há scanner CVE automatizado.

## Implementação sugerida

Adicionar um dos seguintes:

- OWASP Dependency Check.
- Trivy.
- Snyk.
- GitHub Dependabot.
- GitLab Dependency Scanning.

## Critérios de aceite

- Pipeline falha em vulnerabilidade crítica.
- Relatório é gerado no CI.
- Dependências vulneráveis são listadas.
- Imagem Docker é escaneada.

---

# KRN-P2-009 — Hardening do Dockerfile

## Tipo

Deploy / Segurança

## Severidade

Baixa / Média

## Problema

Dockerfile usa Gradle global e roda como root.

## Implementação esperada

- Usar `./gradlew`.
- Criar usuário não-root.
- Definir `HEALTHCHECK`.
- Usar imagem runtime menor.
- Não copiar arquivos desnecessários.

## Exemplo

```dockerfile
RUN addgroup --system kronos && adduser --system --ingroup kronos kronos
USER kronos
```

## Critérios de aceite

- Container roda sem root.
- Build usa wrapper.
- Healthcheck funciona.
- Imagem final não contém cache de build desnecessário.

---

# KRN-P2-010 — Otimizar relatórios de ponto

## Tipo

Performance / Banco

## Severidade

Média / Alta

## Problema

`TimeRecordService` carrega todos os registros do colaborador e filtra em memória.

## Arquivos envolvidos

| Arquivo | Ação |
|---|---|
| `TimeRecordService.java` | Remover filtro em memória |
| `TimeRecordRepository.java` | Criar queries por intervalo |
| Migration | Adicionar índice, se necessário |

## Query esperada

```java
List<TimeRecordEntity> findByEmployeeIdAndStartWorkBetween(
    UUID employeeId,
    LocalDateTime start,
    LocalDateTime end
);
```

Com filtros:

- `employeeId`
- `startWork between`
- `status`
- `active`

## Critérios de aceite

- Relatório mensal não carrega histórico completo.
- Filtro de data ocorre no banco.
- Filtro de status ocorre no banco.
- Endpoint mantém mesmo contrato de resposta.
- Índices suportam a query.

---

# KRN-P2-011 — Otimizar validação de overlap

## Tipo

Performance / Regra de negócio

## Severidade

Alta / Média

## Problema

A validação de sobreposição usa histórico inteiro em memória.

## Implementação esperada

Criar query específica para detectar conflito:

```sql
where employee_id = :employeeId
and time_record_id <> :currentId
and start_work < :newEnd
and end_work > :newStart
```

## Critérios de aceite

- Validação consulta somente registros potencialmente conflitantes.
- Registros do próprio ajuste são ignorados.
- Casos de borda são testados.

---

# KRN-P2-012 — Otimizar AEJ

## Tipo

Performance / Legal

## Severidade

Média

## Problema

AEJ é gerado e assinado em memória para muitos registros.

## Implementação esperada

- Buscar registros por empresa e intervalo.
- Evitar histórico completo por colaborador.
- Definir limite máximo de período.
- Avaliar stream para geração textual.
- Medir tamanho antes da assinatura.

## Critérios de aceite

- AEJ de período grande tem limite explícito.
- Consulta filtra por intervalo no banco.
- Empresas grandes não travam facilmente a aplicação.
- Erro de limite retorna mensagem clara.

---

# Checklist final P2

- [ ] Status codes REST padronizados.
- [ ] Responses de sucesso padronizados.
- [ ] DTOs críticos com Bean Validation.
- [ ] CPF/CNPJ validados com dígito.
- [ ] Relatórios validam datas e referência.
- [ ] Upload tem política segura.
- [ ] Actuator/Prometheus alinhados.
- [ ] Scanner CVE no CI.
- [ ] Dockerfile com hardening.
- [ ] Relatórios de ponto otimizados.
- [ ] Overlap otimizado.
- [ ] AEJ otimizado.

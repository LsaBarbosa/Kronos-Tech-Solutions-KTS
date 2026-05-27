# Endpoints Públicos de Privacidade LGPD

## Objetivo

Os endpoints públicos de privacidade existem para garantir transparência e acesso a informações sobre o tratamento de dados pessoais, conforme exigências da Lei Geral de Proteção de Dados (LGPD). Esses endpoints são acessíveis **sem autenticação** e retornam apenas conteúdo minimizado e institucional, sem exposição de dados pessoais reais.

## Rotas Públicas

### GET /public/privacy/processing-catalog

Retorna o catálogo público de atividades de tratamento de dados realizadas pela plataforma Kronos.

**Autenticação:** Não requerida  
**Resposta:** HTTP 200 OK

**Estrutura da Resposta:**

```json
{
  "version": "2026.05.1",
  "effectiveDate": "2026-05-27",
  "activities": [
    {
      "code": "COMPANY_REGISTRATION",
      "title": "Cadastro de Empresa",
      "description": "Operação de cadastro e administração da conta empresarial...",
      "dataCategories": ["Dados cadastrais da empresa", "Dados de contato corporativo"],
      "purposes": ["Criação e administração da conta", "Execução do contrato"],
      "legalBases": ["Execução de contrato", "Legítimo interesse"],
      "retentionPolicy": "Enquanto a conta estiver ativa...",
      "dataSubjectRights": ["Acesso aos dados", "Correção de dados"]
    }
  ]
}
```

**Atividades Incluídas:**

1. **COMPANY_REGISTRATION** - Cadastro de Empresa
2. **EMPLOYEE_REGISTRATION** - Cadastro de Colaborador
3. **TIME_RECORD** - Registro Eletrônico de Ponto
4. **FACIAL_BIOMETRY** - Biometria Facial
5. **DOCUMENTS** - Documentos Enviados ou Gerados
6. **LGPD_REQUESTS** - Solicitações LGPD

---

### GET /public/privacy/policy

Retorna a política de privacidade pública da plataforma Kronos.

**Autenticação:** Não requerida  
**Resposta:** HTTP 200 OK

**Estrutura da Resposta:**

```json
{
  "version": "2026.05.1",
  "effectiveDate": "2026-05-27",
  "title": "Política de Privacidade e Proteção de Dados - Kronos",
  "sections": [
    {
      "title": "Quem Opera a Plataforma",
      "content": "A plataforma Kronos é operada por..."
    },
    {
      "title": "Dados Que Podem Ser Tratados",
      "content": "A plataforma processa dados de empresas..."
    }
  ]
}
```

**Seções Incluídas:**

1. Quem Opera a Plataforma
2. Dados Que Podem Ser Tratados
3. Finalidades do Tratamento
4. Bases Legais
5. Uso de Biometria Facial
6. Uso de Geolocalização
7. Compartilhamento com Operadores e Infraestrutura
8. Retenção e Descarte
9. Direitos do Titular
10. Canal de Contato
11. Segurança da Informação
12. Alterações desta Política

---

### GET /public/privacy/biometric-term

Retorna o termo público de biometria facial da plataforma Kronos.

**Autenticação:** Não requerida  
**Resposta:** HTTP 200 OK

**Estrutura da Resposta:**

```json
{
  "version": "2026.05.1",
  "effectiveDate": "2026-05-27",
  "title": "Termo de Consentimento e Uso de Biometria Facial",
  "sections": [
    {
      "title": "O Que é Biometria Facial no Kronos",
      "content": "Biometria facial é um método de autenticação..."
    }
  ]
}
```

**Seções Incluídas:**

1. O Que é Biometria Facial no Kronos
2. Para Que a Biometria é Usada
3. Quais Dados São Tratados
4. Base Legal e Necessidade de Consentimento
5. Consequências da Recusa ou Revogação
6. Revogação do Consentimento
7. Retenção de Dados Biométricos
8. Segurança de Dados Biométricos
9. Canal para Dúvidas e Solicitações

---

## Diferenças entre Catálogo Público e Fluxos Autenticados

| Aspecto | Catálogo Público | Fluxos Autenticados |
|---------|------------------|-------------------|
| Autenticação | Não requerida | JWT obrigatório |
| Conteúdo | Minimizado, institucional | Detalhado, dados de usuário/empresa |
| Acesso | Qualquer pessoa | Usuários autenticados |
| Dados Pessoais | Nenhum | Dados reais do usuário/colaborador |
| Propósito | Transparência legal | Gestão operacional |
| Endpoints | `/public/privacy/**` | `/lgpd/**`, `/terms/**` |

---

## Garantias de Minimização

Os endpoints públicos **nunca** retornam:

- ✗ `employeeId`, `userId`, `companyId`, `documentId`
- ✗ `storagePath`, `bucket`, `s3`, `faceS3ObjectKey`
- ✗ CPF, e-mail pessoal, senha
- ✗ Nomes de tabelas (`tb_*`)
- ✗ Nomes de roles internas (`ROLE_*`)
- ✗ Detalhes de infraestrutura
- ✗ Logs ou stack traces

**Verificação Automatizada:**

```bash
curl http://localhost:8080/public/privacy/processing-catalog | \
  grep -iE 'employeeId|userId|companyId|storagePath|tb_|ROLE_' && echo "FALHA" || echo "SUCESSO"
```

---

## Versionamento

Cada resposta pública inclui:

```json
{
  "version": "2026.05.1",
  "effectiveDate": "2026-05-27"
}
```

- **version**: Versão da política/termo (formato `YYYY.MM.numero`)
- **effectiveDate**: Data de vigência (formato ISO 8601)

---

## Exemplos de Uso

### Consultar Catálogo de Tratamento

```bash
curl -X GET http://localhost:8080/public/privacy/processing-catalog
```

### Consultar Política de Privacidade

```bash
curl -X GET http://localhost:8080/public/privacy/policy
```

### Consultar Termo de Biometria

```bash
curl -X GET http://localhost:8080/public/privacy/biometric-term
```

### Verificar Ausência de Autenticação

```bash
# Sem token, retorna 200 OK
curl -i http://localhost:8080/public/privacy/processing-catalog

# Resultado esperado:
# HTTP/1.1 200 OK
# Content-Type: application/json
```

---

## Testes Executados

### Testes de Acesso Público

- ✓ GET /public/privacy/processing-catalog sem JWT retorna 200
- ✓ GET /public/privacy/policy sem JWT retorna 200
- ✓ GET /public/privacy/biometric-term sem JWT retorna 200

### Testes de Estrutura

- ✓ Respostas públicas possuem `version` e `effectiveDate`
- ✓ Processing catalog possui lista `activities` não vazia
- ✓ Cada atividade possui categorias, finalidades, bases legais e retenção
- ✓ Policy possui lista `sections` não vazia
- ✓ Biometric term possui lista `sections` não vazia

### Testes de Segurança

- ✓ Responses públicas não contêm `employeeId`
- ✓ Responses públicas não contêm `userId`
- ✓ Responses públicas não contêm `companyId`
- ✓ Responses públicas não contêm `storagePath`, `bucket`, `s3`
- ✓ Responses públicas não contêm `tb_`, `ROLE_`
- ✓ GET /terms/status sem JWT continua retornando 401 (protegido)
- ✓ POST /terms/accept-biometric sem JWT continua retornando 401 (protegido)

### Testes de Regressão

- ✓ Endpoints autenticados de LGPD continuam funcionando
- ✓ Endpoints de aceite biométrico continuam protegidos
- ✓ Fluxos existentes não foram quebrados

---

## Migração e Compatibilidade

### Endpoints Mantidos

O endpoint autenticado original continua funcionando:

```http
GET /lgpd/processing-catalog
Authorization: Bearer {token}
```

Retorna a mesma estrutura, mas exige autenticação.

### Endpoints Novos

Os novos endpoints públicos coexistem:

```http
GET /public/privacy/processing-catalog
```

Retorna conteúdo minimizado sem autenticação.

---

## Segurança na Spring

A configuração em `SecurityConfig.java` expõe explicitamente:

```java
auth.requestMatchers(
    org.springframework.http.HttpMethod.GET,
    "/public/privacy/**"
).permitAll();
```

Essa regra é adicionada **antes** da regra `anyRequest().authenticated()`, garantindo que:

- ✓ Rotas `/public/privacy/**` são acessíveis sem JWT
- ✓ Todas as outras rotas requerem autenticação
- ✓ Endpoints protegidos (como `/terms/status`) permanecem bloqueados

---

## Observações Operacionais

1. **Cache**: Os endpoints retornam conteúdo estático, podendo ser cacheados no cliente ou CDN.

2. **CORS**: O CORS está configurado para permitir requisições de qualquer origem autenticada.

3. **Rate Limiting**: Não há rate limiting específico para essas rotas; está sujeito à política geral da plataforma.

4. **Monitoramento**: Requisições aos endpoints públicos são registradas normalmente.

5. **Alterações Futuras**: Mudanças na política ou termo devem atualizar `version` e `effectiveDate`.

---

## Dúvidas e Conformidade

Para dúvidas sobre conformidade LGPD relacionadas aos endpoints públicos, consulte:

- Documentação técnica: `/docs/lgpd/`
- Spec de origem: `LGPD-PUBLIC-PRIVACY-001`
- Security Config: `config/SecurityConfig.java`
- Controller: `adapter/in/web/http/PublicPrivacyController.java`
- Service: `application/service/PublicPrivacyService.java`

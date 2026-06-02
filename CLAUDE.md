# 🕐 Kronos - Guia Técnico do Projeto

## Visão Geral

**Kronos** é uma plataforma SaaS de gestão integrada de:
- Jornada e ponto eletrônico (REP)
- Gestão de colaboradores
- Gestão de empresas e usuários
- Documentos legais e fiscais
- Relatórios e análises de RH/Fiscal

**Status:** Produção | **Branch padrão:** main | **Versão:** Java 21, Spring Boot 3.x

---

## 🏗️ Stack Técnica

### Backend
- **Linguagem:** Java 21
- **Framework:** Spring Boot 3.x
- **Segurança:** Spring Security + JWT
- **Banco:** PostgreSQL 13+
- **Migrations:** Flyway
- **Build:** Gradle 8.x
- **Integrações:** Storage (S3/GCS), Biometria Facial, Email, NTP, Assinatura Digital

### Frontend (Padrão)
- **Framework:** React 18+ (TypeScript)
- **HTTP:** Axios com interceptores
- **Auth:** AuthContext (JWT/Bearer)
- **Padrão API:** `api.ts` com normalização e tratamento de erros
- **Build:** Vite

---

## 🏛️ Arquitetura

### Padrão: Arquitetura Hexagonal (Portas e Adaptadores)

```
src/main/java/com/kts/kronos/
├── domain/              # Entidades, Value Objects, Casos de Uso
├── application/         # Serviços de Aplicação, DTOs
├── adapter/
│   ├── in/
│   │   ├── web/http/    # Controllers, REST endpoints
│   │   └── event/       # Event listeners
│   └── out/
│       ├── persistence/ # Repositories, JPA entities
│       ├── external/    # Integrações (Email, Storage, etc.)
│       └── messaging/   # Eventos de saída
└── config/              # Configurações Spring
```

### Regras Obrigatórias de Arquitetura

1. **Controllers** devem ser finos:
   - Apenas mapear HTTP request → DTO
   - Chamar case de uso (service/application)
   - Mapear response → HTTP response
   - ❌ Não colocar regra de negócio em controller

2. **Domain** é o coração:
   - Entidades com invariantes
   - Value Objects imutáveis
   - Exceções de domínio
   - ✅ Lógica de negócio aqui

3. **Application Services**:
   - Orquestração de casos de uso
   - Transações
   - DTOs entrada/saída
   - Logging e rastreabilidade

4. **Adapters**:
   - Isolam frameworks externos
   - Convertem entre formato interno e externo
   - Tratam exceções técnicas

---

## 🔒 Regras de Segurança

### Autenticação e Autorização
- **JWT obrigatório** em todas as rotas protegidas
- **Roles baseados em tenant** (multi-tenant)
- **Token expiration:** 1 hora (access token)
- **Refresh token:** 30 dias
- ✅ Usar `@PreAuthorize`, `@PostAuthorize`
- ✅ Validar tenant em cada requisição

### Dados Sensíveis
- ❌ Nunca logar dados pessoais (CPF, email real, telefone completo)
- ✅ Usar mascaramento em logs e resposta de erro
- ✅ Criptografar em repouso (campos sensíveis no BD)
- ✅ TLS 1.2+ em transmissão

### SQL Injection e Injection
- ✅ Usar JPA Named Queries ou Criteria API
- ✅ Nunca concatenar strings em SQL
- ❌ Não usar `@Query` com string direta
- ✅ Parametrizar sempre

### CORS e CSRF
- ✅ Configurar CORS explicitamente por origem
- ✅ CSRF token em formulários
- ❌ Não usar `allowCredentials=true` com `*`

---

## 📋 Regras LGPD (Lei Geral de Proteção de Dados)

### Conformidade Obrigatória

1. **Coleta Mínima:**
   - Coletar apenas dados necessários ao caso de uso
   - Documentar base legal (consentimento, contrato, obrigação legal)
   - ❌ Não coletar "para o futuro"

2. **Consentimento:**
   - Explícito e informado
   - Armazenar prova de consentimento com timestamp
   - Permitir revogação a qualquer momento
   - Registrar consentimento por campo sensível

3. **Direitos do Titular:**
   - ✅ Acesso: endpoint `/data/me` exportando todos os dados
   - ✅ Retificação: permitir edição
   - ✅ Exclusão: right-to-be-forgotten com soft-delete ou anonymização
   - ✅ Portabilidade: exportar em formato padrão (JSON/CSV)

4. **Dados Sensíveis (requerem proteção extra):**
   - CPF/CNPJ (identificação)
   - Biometria facial (dados biométricos)
   - Geolocalização (em tempo real)
   - Documentos (RG, CNH, foto, etc.)
   - Registros de ponto (dados de localização + horário)
   - Histórico de férias/abonos (relacionado a saúde)

5. **Retenção:**
   - Definir período máximo de retenção por tipo de dado
   - Deletar automaticamente após expiração
   - Documented no BD com `deleted_at` (soft-delete)

6. **Segurança:**
   - Criptografia de campos sensíveis
   - Isolamento de dados por tenant
   - Logs de acesso a dados sensíveis
   - Mascaramento em logs de erro

7. **Documentação:**
   - Manter registro de processamento (ROPA: Record of Processing Activity)
   - Justificativa de cada campo sensível

---

## ⏱️ Regras para Ponto Eletrônico (REP)

### Coleta de Dados
- **Timestamp:** data/hora com precisão NTP
- **Geolocalização:** com consentimento explícito
- **Biometria:** validação facial com comparação de score
- **Foto:** capturada no momento (não armazenar histórico completo)

### Validações Obrigatórias
- ✅ Horário permitido (não antes de 04:00, não depois de 23:59)
- ✅ Intervalo mínimo entre pontos (não permitir 2 marcas em < 1 min)
- ✅ Limite de marcas por dia (máx 4: entrada, saída, retorno, saída final)
- ✅ Validação de geofence (colaborador dentro da empresa?)

### Mascaramento
- Logar apenas `employee_id` + timestamp (não facial score, não coordenadas exatas)
- Retornar ao app apenas `status: success/error` sem detalhes biométricos

### Retenção
- Manter dados de ponto por **mínimo 12 meses** (trabalhista)
- Deletar fotos/biometria após **6 meses**
- Manter apenas histórico resumido (entrada, saída, horas trabalhadas)

---

## 🗄️ Regras para Banco de Dados

### Estrutura e Migrations

1. **Flyway obrigatório:**
   - Versionamento: `V1__initial_schema.sql`, `V2__add_column.sql`
   - Idempotência: scripts devem rodar múltiplas vezes sem erro
   - Never drop production tables
   - Usar `IF EXISTS`, `IF NOT EXISTS`

2. **Schema padrão:**
   ```sql
   CREATE TABLE companies (
     id UUID PRIMARY KEY,
     tenant_id UUID NOT NULL,
     name VARCHAR(255) NOT NULL,
     created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
     created_by UUID,
     updated_at TIMESTAMP,
     updated_by UUID,
     deleted_at TIMESTAMP,  -- soft-delete para LGPD
     UNIQUE(tenant_id, name)
   );
   ```

3. **Convenções:**
   - PK: `id` (UUID)
   - Auditoria: `created_at`, `created_by`, `updated_at`, `updated_by`
   - Soft-delete: `deleted_at` (nunca físico)
   - Tenant isolation: `tenant_id` em todas as tabelas multi-tenant
   - Índices em foreign keys e campos filtrados

4. **Dados Sensíveis:**
   - Criptografar em repouso (CPF, biometria, documentos)
   - Usar `pgcrypto` ou integração externa
   - ❌ Nunca armazenar em plaintext

### Operações Perigosas
- ❌ DELETE direto (usar soft-delete com `deleted_at`)
- ❌ DROP TABLE sem aprovação
- ❌ TRUNCATE em produção
- ✅ SEMPRE usar migrations versionadas

---

## ✅ Comandos de Validação

### Build e Testes
```bash
# Build limpo
./gradlew clean bootJar -x test

# Todos os testes
./gradlew test

# Testes unitários apenas
./gradlew test --tests '*Test'

# Testes de integração
./gradlew test --tests '*IT'

# Com relatório detalhado
./gradlew test --info

# Verificar cobertura
./gradlew jacocoTestReport
```

### Validação de Código
```bash
# Lint (checkstyle)
./gradlew checkstyleMain

# Dependências vulneráveis
./gradlew dependencyCheck

# Build de produção
./gradlew clean bootJar -x test

# Verificar propriedades (sem secrets)
grep -E '\$\{[A-Z_]+\}' src/main/resources/application.yml
```

### Git
```bash
# Status
git status

# Diff da branch
git diff origin/main..HEAD

# Log com contexto
git log --oneline origin/main..HEAD

# Verificar secrets acidentalmente commitados
git diff origin/main -- | grep -E '\$\{|password|token|key|secret'
```

### Banco (em desenvolvimento)
```bash
# Conectar (localmente)
psql -h localhost -U kronos -d kronos

# Ver schema
\dt

# Ver migrations aplicadas
SELECT * FROM flyway_schema_history ORDER BY installed_rank DESC;
```

---

## 📝 Formato Esperado de Respostas do Claude

### Para Alterações de Código

1. **Contexto:**
   - Problema/requisito
   - Arquivos envolvidos (caminho exato)
   - Risco (alto/médio/baixo)

2. **Plano:**
   - Passo-a-passo
   - Alternativas consideradas
   - Trade-offs

3. **Implementação:**
   - Código com mudanças
   - Comentários apenas onde "WHY" não é óbvio
   - Seguir padrões do projeto

4. **Testes:**
   - Testes novos ou modificados
   - Cobertura esperada
   - Como rodar: `./gradlew test --tests 'ComNomeDoTeste'`

5. **Validação:**
   - `git diff` limpo
   - `./gradlew test` passando
   - Nenhum `TODO` ou `FIXME` acidental

### Para Análise de Erros/Logs

1. **Sintoma:** O que está acontecendo
2. **Evidência:** Stacktrace, logs relevantes (mascarados)
3. **Causa Provável:** Hipótese baseada em evidências
4. **Como Confirmar:** Comandos para validar
5. **Correção Segura:** Fix com teste
6. **Rollback Plan:** Como desfazer se necessário

---

## 🚀 Próximos Passos

1. Criar `.claude/settings.json` com permissões granulares
2. Configurar skills customizadas (backend-review, lgpd-review, debug-prod, deploy-check)
3. Adicionar proteções no `.gitignore` para secrets locais
4. Criar template `CLAUDE.local.example.md` para devs

---

## 📚 Referências Rápidas

- **LGPD:** Lei nº 13.709/2018
- **Conformidade:** Auditado em 2026-05-25 com aprovação técnica 100%
- **Testes:** 1654+ testes passando (P0+P1+P2 LGPD completo)
- **Repositório:** GitHub - Kronos Tech Solutions

---

**Última atualização:** 2026-06-02
**Mantenedor:** Kronos DevOps Team

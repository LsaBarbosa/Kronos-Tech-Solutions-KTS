# Kronos API

Plataforma corporativa para **gestão de ponto eletrônico, colaboradores, documentos e conformidade legal (Portaria 671)**, com foco em segurança, rastreabilidade e governança operacional.

## 📌 Visão Geral

A **Kronos API** é um backend Java/Spring Boot voltado a operações de RH e compliance, cobrindo:

- Autenticação com JWT e login facial.
- Gestão de empresas, usuários e colaboradores.
- Registro de jornada, aprovações e solicitações de ajustes/abonos.
- Geração de artefatos fiscais e legais (AFD, AEJ, espelho de ponto, atestado técnico).
- Upload e gestão de documentos com integração em armazenamento externo.
- Monitoramento com Actuator + Prometheus.

## 🏗️ Arquitetura e Stack

### Arquitetura

O projeto segue uma organização baseada em **ports and adapters (hexagonal)**:

- `adapter/in`: entrada HTTP (controllers, DTOs, exceptions web).
- `application`: casos de uso e serviços de aplicação.
- `domain`: modelos e regras centrais.
- `adapter/out`: persistência, segurança, notificações e integrações externas.

### Tecnologias principais

- **Java 21**
- **Spring Boot 3.5.6** (Web, Security, Validation, Data JPA, Actuator, Mail)
- **PostgreSQL**
- **JWT (jjwt 0.12.7)**
- **AWS SDK v2 (S3 e Rekognition)**
- **OpenAPI/Swagger (springdoc)**
- **Micrometer + Prometheus**
- **Gradle + JaCoCo**
- **Docker (build multi-stage)**

## ✅ Capacidades de Negócio

- **Autenticação e Segurança**
  - Login com credenciais.
  - Login facial com validação biométrica.
  - Recuperação e redefinição de senha.
  - Proteção de endpoints com papéis (`CTO`, `MANAGER`, `PARTNER`).

- **Gestão Organizacional**
  - Cadastro e manutenção de empresas.
  - Cadastro e manutenção de usuários e colaboradores.
  - Validações de unicidade (CNPJ, CPF, username).

- **Jornada e Solicitações**
  - Registro de check-in/out.
  - Correções e aprovações de ponto.
  - Solicitações de férias e abonos.
  - Relatórios operacionais.

- **Conformidade Legal**
  - Emissão de AFD e AEJ.
  - Geração de espelho de ponto em PDF.
  - Geração de atestado técnico assinado.
  - Gestão de aceite de termos biométricos (LGPD).

- **Comunicação Interna**
  - Mural corporativo com envio, listagem e exclusão de mensagens.

## 🔐 Segurança e Conformidade

- Autenticação baseada em **JWT** com secret obrigatório via variável de ambiente.
- Política de senha configurável (`SECURITY_PASSWORD_POLICY_REGEX`).
- Endpoints protegidos por contexto de autorização Spring Security.
- Suporte a aceite de termo de biometria com persistência documental.
- Logs e rastreabilidade via camadas de auditoria e monitoramento.

## 🌐 APIs e Documentação

Após iniciar a aplicação, acesse:

- **Swagger UI**: `http://localhost:8080/swagger-ui/index.html`
- **OpenAPI JSON**: `http://localhost:8080/v3/api-docs`
- **Healthcheck**: `http://localhost:8080/actuator/health`
- **Métricas Prometheus**: `http://localhost:8080/actuator/prometheus`

### Principais domínios de endpoint

- `/auth` – autenticação e recuperação de senha
- `/companies` – gestão de empresas
- `/users` – gestão de usuários
- `/employee` – gestão de colaboradores
- `/records` – ponto, aprovações e solicitações
- `/documents` – documentos e uploads
- `/messages` – mural corporativo
- `/legal` – arquivos fiscais e relatórios legais
- `/terms` – aceite e status de termos

## ⚙️ Pré-requisitos

- Java 21
- Gradle Wrapper (`./gradlew`)
- PostgreSQL acessível
- Credenciais AWS (S3/Rekognition), quando aplicável

## 🚀 Execução Local

### 1) Clonar o repositório

```bash
git clone <url-do-repo>
cd Kronos-Tech-Solutions-KTS
```

### 2) Configurar variáveis de ambiente

Exemplo mínimo para inicialização:

```bash
export PORT=8080
export DB_HOST=localhost
export DB_PORT=5432
export DB_NAME=kronos
export DB_USER=postgres
export DB_PASSWORD=postgres

export JWT_SECRET="troque-por-um-segredo-forte"

export FRONTEND_BASE_URL_PLATAFORM="http://localhost:3000"
export FRONTEND_BASE_URL_RECORD="http://localhost:3001"
export FRONTEND_BASE_URL_LOCAL="http://localhost:5173"
export FRONTEND_BASE_URL_LOCAL_2="http://localhost:5174"

export MAIL_HOST="smtp.exemplo.com"
export MAIL_PORT=587
export MAIL_USERNAME="usuario"
export MAIL_PASSWORD="senha"

export AWS_ACCESS_KEY_ID="..."
export AWS_SECRET_ACCESS_KEY="..."
export AWS_REGION="us-east-1"
export AWS_S3_BUCKET_NAME="bucket-imagens"
export AWS_REKOGNITION_COLLECTION_ID="kronos_render"

export SECRET_TERM="salt-termo-biometria"
```

### 3) Rodar a aplicação

```bash
./gradlew bootRun
```

## 🧪 Qualidade e Testes

Executar suíte completa com cobertura:

```bash
./gradlew test
```

O pipeline local já gera:

- `jacocoTestReport` (XML e HTML)
- `jacocoTestCoverageVerification` com limite padrão de **70%** de cobertura de linhas

> É possível ajustar o mínimo com `-PminimumCoverage=<valor>`, por exemplo:
>
> ```bash
> ./gradlew test -PminimumCoverage=0.80
> ```

## 📦 Build e Containerização

### Build da aplicação

```bash
./gradlew clean bootJar
```

### Build da imagem Docker

```bash
docker build -t kronos-api:local .
```

### Executar container

```bash
docker run --rm -p 8080:8080 --env-file .env kronos-api:local
```

## 📈 Observabilidade

- Actuator habilitado com exposição de `health`, `info`, `metrics` e `prometheus`.
- Arquivo `prometheus.yml` pronto para scrape em `/actuator/prometheus`.
- Health probes de liveness/readiness configuradas para ambientes orquestrados.

## ☁️ Deploy

O repositório inclui `render.yaml` para deploy no Render com:

- Serviço web em Docker.
- Healthcheck em `/actuator/health`.
- Volume persistente para documentos (`/mnt/data/documents`).
- Variáveis de ambiente e segredos previstos em manifesto.

## 🤝 Governança de Contribuição

Sugestão de fluxo corporativo:

1. Criar branch por feature/hotfix.
2. Implementar com testes automatizados.
3. Garantir `./gradlew test` com sucesso.
4. Abrir PR com descrição de impacto funcional, risco e plano de rollback.
5. Exigir revisão técnica antes do merge.

## 📄 Licença

Defina a estratégia de licenciamento conforme a política da organização (privado/proprietário ou OSS).

---

Se desejar, posso evoluir este README para incluir:

- matriz de responsabilidades (RACI),
- política de versionamento (SemVer + release notes),
- SLO/SLA operacionais,
- e um runbook de incidentes (on-call).

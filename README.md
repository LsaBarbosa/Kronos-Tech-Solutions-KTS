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


## 🍪 Integração Frontend com sessão HttpOnly

Com as mudanças recentes, o backend está preparado para autenticação por cookie HttpOnly (sem depender de JWT em storage do navegador). Para o frontend funcionar corretamente, siga este checklist.

### 1) Chamar API sempre com credenciais

Use `credentials: "include"` em **todas** as chamadas que dependem de sessão:

```ts
await fetch(`${API_URL}/auth/login`, {
  method: "POST",
  headers: { "Content-Type": "application/json" },
  credentials: "include",
  body: JSON.stringify({ username, password })
});
```

> Se usar Axios, configure `withCredentials: true` no client global.

### 2) Não usar JWT no frontend

- Não gravar token em `localStorage`/`sessionStorage`.
- Não montar header `Authorization: Bearer ...` para o fluxo principal web.
- O cookie de sessão é enviado automaticamente pelo navegador.

### 3) Login e logout no novo contrato

- `POST /auth/login` e `POST /auth/login-face`:
  - backend seta cookie no `Set-Cookie`;
  - body retorna `token: null` por segurança.
- `POST /auth/logout`:
  - backend invalida cookie;
  - frontend apenas limpa estado visual local e redireciona se necessário.

### 4) Inicialização de sessão no app

Ao abrir/recarregar o app, valide sessão com endpoint autenticado (ex.: `GET /employee/own-profile`) e derive o estado da UI a partir do resultado:

- `200`: sessão válida.
- `401`: sem sessão/expirada → pedir autenticação novamente.

### 5) CORS e ambiente

Para cookies funcionarem entre front e API:

- backend com `allowCredentials(true)` e origem explícita (não `*`);
- frontend deve usar a mesma origem permitida na configuração;
- em produção, habilitar HTTPS e `AUTH_COOKIE_SECURE=true`.

### 6) SameSite e domínio do cookie

Defina no backend conforme topologia:

- `AUTH_COOKIE_SAME_SITE=Lax` (preferencial quando possível).
- `AUTH_COOKIE_SAME_SITE=None` somente se cross-site real, sempre com `AUTH_COOKIE_SECURE=true`.
- `AUTH_COOKIE_DOMAIN` e `AUTH_COOKIE_PATH` consistentes com domínio/rotas usadas pelo frontend.

### 7) Fluxo recomendado de check-in (uma ação)

Para o fluxo de “Registrar ponto” em uma única ação:

1. Capturar imagem facial.
2. Chamar `POST /auth/login-face` com a imagem (`credentials: "include"`).
3. Na sequência imediata, chamar `POST /records/checkin` com a **mesma imagem** e geolocalização (`credentials: "include"`).
4. Opcional: chamar `POST /auth/logout` ao final, se o modelo de negócio exigir sessão curtíssima para check-in.

### 8) Segurança recomendada no frontend

- CSP restritiva para reduzir superfície de XSS.
- Nunca logar payloads com dados sensíveis (imagem base64/claims).
- Tratar `401/403` de forma centralizada em interceptor.
- Evitar fallback para token em JS.

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
export FRONTEND_BASE_URL_LOCAL_2="http://127.0.0.1:5173"

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

## 🛠️ Checklist operacional de deploy (cookie + CORS)

Use este checklist em **homolog/prod** antes de liberar frontend + API:

1. **Validar combinação de cookies no ambiente**
   - Confirmar variáveis `AUTH_COOKIE_SECURE`, `AUTH_COOKIE_SAME_SITE`, `AUTH_COOKIE_DOMAIN`, `AUTH_COOKIE_PATH` no ambiente de deploy e respectivos defaults em `application.yml`.
   - Garantir:
     - `AUTH_COOKIE_SECURE=true` em homolog/prod;
     - `AUTH_COOKIE_SAME_SITE` somente `Lax`, `Strict` ou `None`;
     - se `AUTH_COOKIE_SAME_SITE=None`, então `AUTH_COOKIE_SECURE=true`;
     - `AUTH_COOKIE_PATH` iniciando com `/`;
     - `AUTH_COOKIE_DOMAIN` compatível com o(s) host(s) de frontend configurados.

2. **Confirmar CORS com origins reais do frontend**
   - Revisar `frontend.base-url-*` em variáveis de ambiente (`FRONTEND_BASE_URL_PLATAFORM`, `FRONTEND_BASE_URL_RECORD`, etc.).
   - Validar que `SecurityConfig#corsConfigurationSource` está recebendo as origens reais (sem path, query ou fragment).
   - Em homolog/prod, garantir origins HTTPS e sem hosts locais (`localhost`, `127.0.0.1`).

3. **Executar smoke test manual de login no browser**
   - Abrir DevTools (Network + Application/Storage).
   - Fazer `POST /auth/login` com `credentials: "include"`.
   - Validar no response header a presença de `Set-Cookie`.
   - Validar que o cookie foi persistido no domínio esperado.
   - Executar chamada autenticada (ex.: `GET /employee/own-profile`) e confirmar envio automático do cookie no request.

4. **Registrar evidências do deploy**
   - Salvar screenshot do DevTools (response de login e request autenticado) ou copiar os headers validados.
   - Anotar no changelog/issue de release o resultado do checklist para auditoria e prevenção de regressão.

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

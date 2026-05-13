# Backlog de Correções — Kronos `PROD_HOSTINGER`

> Origem: auditoria de produção da branch `PROD_HOSTINGER`.
> Objetivo: organizar correções e melhorias por prioridade para preparar a branch para produção.

# Prioridade P0 — Bloqueantes de Produção

## Objetivo

Remover impedimentos reais de produção relacionados a:

- autenticação e sessão
- cookie HttpOnly
- logout
- CSRF
- retenção legal
- integridade de dados trabalhistas/fiscais
- configuração crítica de certificado digital

## Critério para encerrar P0

A prioridade P0 só deve ser considerada concluída quando:

- o JWT não for mais retornado no body das respostas;
- o login usar cookie `HttpOnly`;
- o logout expirar corretamente o cookie;
- a estratégia de CSRF estiver definida e testada;
- dados legais/fiscais não forem apagados fisicamente;
- exclusão de usuário e empresa for substituída por inativação/soft delete;
- certificado digital de produção não usar placeholder;
- testes de autenticação, sessão, logout e retenção estiverem verdes.

---

# KRN-P0-001 — Implementar autenticação via cookie HttpOnly

## Tipo

Segurança / Autenticação

## Severidade

Bloqueante

## Problema

O login retorna o JWT no corpo da resposta por meio de `LoginResponse(token)` e a aplicação depende de `Authorization: Bearer`.

Esse modelo contradiz o padrão obrigatório definido para produção, onde o token deve ficar protegido em cookie `HttpOnly`.

## Arquivos envolvidos

| Arquivo | Ação |
|---|---|
| `AuthController.java` | Alterar login para emitir cookie |
| `LoginResponse.java` | Remover/deprecar exposição direta do token |
| `JwtAuthenticationFilter.java` | Passar a ler token do cookie |
| `JwtAuthenticatedUser.java` | Remover dependência exclusiva de header Bearer |
| `SecurityConfig.java` | Ajustar política de cookies, CORS e CSRF |

## Implementação esperada

Criar uma camada centralizada para emissão de cookie de autenticação.

Exemplo conceitual:

```java
ResponseCookie cookie = ResponseCookie.from("KRONOS_ACCESS_TOKEN", token)
        .httpOnly(true)
        .secure(true)
        .sameSite("Lax")
        .path("/")
        .maxAge(Duration.ofHours(8))
        .build();
```

Resposta esperada para login, caso não seja necessário retornar dados do usuário:

```http
HTTP/1.1 204 No Content
Set-Cookie: KRONOS_ACCESS_TOKEN=...; HttpOnly; Secure; SameSite=Lax; Path=/; Max-Age=28800
```

Resposta alternativa, caso o front precise de dados mínimos da sessão:

```json
{
  "authenticated": true,
  "user": {
    "userId": "...",
    "employeeId": "...",
    "role": "MANAGER",
    "termsAccepted": true
  }
}
```

Nunca retornar o JWT no body.

## Critérios de aceite

- `POST /auth/login` não retorna token no body.
- `POST /auth/login-face` não retorna token no body.
- O header `Set-Cookie` contém `HttpOnly`.
- O cookie contém `Secure` em produção.
- O cookie contém `SameSite=Lax` ou `SameSite=Strict`.
- O filtro de autenticação lê o token a partir do cookie.
- Requisição protegida sem cookie válido retorna `401`.
- Requisição com cookie expirado retorna `401`.
- O front-end não precisa mais armazenar token em `localStorage` ou `sessionStorage`.

## Testes obrigatórios

- Login com senha emite cookie `HttpOnly`.
- Login facial emite cookie `HttpOnly`.
- Cookie ausente retorna `401`.
- Cookie inválido retorna `401`.
- Cookie expirado retorna `401`.
- Token não aparece no JSON de resposta.

---

# KRN-P0-002 — Corrigir logout para expirar cookie

## Tipo

Segurança / Sessão

## Severidade

Bloqueante

## Problema

O logout depende de Bearer token e não invalida cookie.

## Arquivos envolvidos

| Arquivo | Ação |
|---|---|
| `AuthController.java` | Alterar logout |
| `SecurityConfig.java` | Garantir política correta da rota |
| Utilitário de cookie | Reutilizar nome, path e domain |

## Implementação esperada

O logout deve responder com o mesmo cookie usado no login, porém expirado:

```java
ResponseCookie expiredCookie = ResponseCookie.from("KRONOS_ACCESS_TOKEN", "")
        .httpOnly(true)
        .secure(true)
        .sameSite("Lax")
        .path("/")
        .maxAge(0)
        .build();
```

Resposta esperada:

```http
HTTP/1.1 204 No Content
Set-Cookie: KRONOS_ACCESS_TOKEN=; HttpOnly; Secure; SameSite=Lax; Path=/; Max-Age=0
```

## Critérios de aceite

- `POST /auth/logout` retorna `204`.
- Não exige `Authorization: Bearer`.
- Remove o cookie usando o mesmo `Path`.
- Não lança erro com header ausente.
- Após logout, endpoint protegido retorna `401`.

## Testes obrigatórios

- Logout com cookie válido.
- Logout sem cookie.
- Logout expira cookie.
- Acesso protegido após logout falha.

---

# KRN-P0-003 — Atualizar aceite/revogação de termos para não retornar JWT no body

## Tipo

Segurança / Contrato

## Severidade

Bloqueante

## Problema

`TermsController` retorna novo JWT no body após aceite ou revogação de termo biométrico.

## Arquivos envolvidos

| Arquivo | Ação |
|---|---|
| `TermsController.java` | Trocar retorno por cookie ou DTO sem token |
| `LoginResponse.java` | Remover dependência |
| `JwtUtils.java` | Continuar gerando token, mas entregar via cookie |

## Implementação esperada

Ao aceitar ou revogar termos, se for necessário atualizar a claim `terms_accepted`, emitir novo cookie.

Resposta sugerida:

```http
HTTP/1.1 204 No Content
Set-Cookie: KRONOS_ACCESS_TOKEN=novo_token; HttpOnly; Secure; SameSite=Lax; Path=/
```

Resposta alternativa:

```json
{
  "termsAccepted": true
}
```

Sem retornar token.

## Critérios de aceite

- `POST /terms/accept-biometric` não retorna JWT no body.
- `POST /terms/revoke-biometric` não retorna JWT no body.
- Novo token, quando necessário, é enviado por cookie.
- Claims atualizadas continuam funcionando.

## Testes obrigatórios

- Aceite de termo atualiza cookie.
- Revogação de termo atualiza cookie.
- Nenhum response contém campo `token`.

---

# KRN-P0-004 — Definir estratégia CSRF compatível com cookie

## Tipo

Segurança / CSRF

## Severidade

Alta / Bloqueante funcional para autenticação via cookie

## Problema

A aplicação está com CSRF desabilitado. Ao migrar autenticação para cookie, endpoints mutáveis podem ficar expostos a CSRF.

## Arquivos envolvidos

| Arquivo | Ação |
|---|---|
| `SecurityConfig.java` | Habilitar CSRF ou justificar exceções |
| `AuthController.java` | Expor token CSRF se usar double-submit |
| Front-end | Enviar header CSRF em mutações |

## Estratégia recomendada

- Cookie de sessão: `HttpOnly`, `Secure`, `SameSite=Lax`.
- CSRF token separado, não `HttpOnly`, para o front ler e reenviar em header.
- Header esperado: `X-CSRF-TOKEN`.

## Critérios de aceite

- Endpoints `POST`, `PUT`, `PATCH` e `DELETE` exigem CSRF quando autenticados por cookie.
- Endpoints públicos como login, recover e reset têm exceção controlada.
- Erro de CSRF retorna `403` padronizado.
- Front-end consegue obter e enviar CSRF token.

## Testes obrigatórios

- Mutação sem CSRF retorna `403`.
- Mutação com CSRF válido funciona.
- Login não quebra.
- Logout não quebra.

---

# KRN-P0-005 — Remover exclusão física em `DELETE /users/{id}`

## Tipo

Integridade legal / Banco / Domínio

## Severidade

Bloqueante

## Problema

`UserService.deleteUser` remove fisicamente documentos, registros de ponto, usuário e colaborador.

Isso pode apagar evidências legais, registros trabalhistas, documentos fiscais e histórico necessário para auditoria.

## Arquivos envolvidos

| Arquivo | Ação |
|---|---|
| `UserService.java` | Trocar delete físico por inativação |
| `EmployeeEntity.java` | Garantir flags de inativação |
| `UserEntity.java` | Garantir flags de inativação |
| `DocumentEntity.java` | Preservar documentos |
| `TimeRecordEntity.java` | Preservar ponto |
| Migration nova | Adicionar campos se necessário |

## Implementação esperada

Substituir exclusão física por inativação:

- `user.isActive = false`
- `employee.isActive = false`
- `deletedAt`
- `deletedBy`
- `deactivationReason`

Não apagar:

- `tb_time_records`
- `tb_document`
- `tb_afd_entry`
- registros fiscais
- documentos legais
- logs de auditoria

## Critérios de aceite

- `DELETE /users/{id}` não remove registro físico de ponto.
- `DELETE /users/{id}` não remove documentos.
- Usuário inativo não consegue autenticar.
- Colaborador inativo não aparece em listagens padrão de ativos.
- Histórico continua disponível para relatórios legais.
- Operação retorna `204`.

## Testes obrigatórios

- Inativar usuário preserva `time_records`.
- Inativar usuário preserva documentos.
- Usuário inativo não loga.
- Relatórios antigos continuam acessíveis por gestor autorizado.

---

# KRN-P0-006 — Bloquear exclusão física de empresa

## Tipo

Integridade legal / Multi-tenant

## Severidade

Alta / Bloqueante para produção segura

## Problema

`DELETE /companies/{cnpj}` remove empresa diretamente, com risco de perda de contexto legal/fiscal.

## Arquivos envolvidos

| Arquivo | Ação |
|---|---|
| `CompanyService.java` | Trocar delete por inativação |
| `CompanyController.java` | Ajustar status code |
| `CompanyEntity.java` | Garantir campos de inativação |
| Migration nova | Criar campos se necessário |

## Implementação esperada

Transformar exclusão em inativação:

```java
company.setActive(false);
company.setDeletedAt(now);
company.setDeletedBy(userId);
```

Ou remover o endpoint de delete e manter apenas toggle/inativação.

## Critérios de aceite

- Empresa não é apagada fisicamente.
- Colaboradores e usuários vinculados são inativados ou bloqueados conforme regra.
- Dados legais permanecem preservados.
- Retorno `204`.
- Tentativa de excluir empresa com histórico legal não remove dados.

## Testes obrigatórios

- Inativar empresa preserva colaboradores.
- Inativar empresa preserva ponto, documentos e AFD.
- Usuários da empresa inativa não autenticam.
- CTO ainda consegue consultar histórico.

---

# KRN-P0-007 — Corrigir certificado digital em produção

## Tipo

Configuração / Legal

## Severidade

Bloqueante

## Problema

O certificado digital usa placeholders como:

- `caminho/para/certificado.pfx`
- `sua_senha_secreta`

Isso pode fazer a geração legal/fiscal falhar em produção.

## Arquivos envolvidos

| Arquivo | Ação |
|---|---|
| `application.yml` | Remover defaults inseguros |
| `application-prod.yml` | Criar config produtiva |
| Serviço de assinatura digital | Validar ausência de config no startup |

## Implementação esperada

Usar variáveis obrigatórias:

```yaml
kronos:
  digital-certificate:
    path: ${DIGITAL_CERTIFICATE_PATH}
    password: ${DIGITAL_CERTIFICATE_PASSWORD}
```

Sem default.

## Critérios de aceite

- Aplicação falha no startup se certificado obrigatório estiver ausente em produção.
- Senha não aparece em log.
- Caminho não fica hardcoded.
- Ambiente local pode usar profile específico de dev/test.

## Testes obrigatórios

- Startup falha sem variável obrigatória em profile `prod`.
- Startup funciona com variável válida.
- Logs não imprimem senha.

---

# Checklist final P0

- [ ] Login usa cookie `HttpOnly`.
- [ ] Login facial usa cookie `HttpOnly`.
- [ ] Token não aparece no body.
- [ ] Logout expira cookie.
- [ ] CSRF definido e testado.
- [ ] Terms não retorna JWT no body.
- [ ] Usuário não é apagado fisicamente.
- [ ] Empresa não é apagada fisicamente.
- [ ] Certificado digital não usa placeholder.
- [ ] Testes P0 implementados.

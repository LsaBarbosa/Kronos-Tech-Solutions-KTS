## Objetivo
Desdobrar a revisão de segurança documentada na issue #213 em um plano técnico executável, com etapas, tarefas de backend, critérios de aceite e ordem sugerida de implementação.

> Issue de referência: #213

---

## Estratégia de execução
A implementação deve seguir esta ordem:
1. Remoção de exposição imediata de segredos e redução de risco operacional.
2. Endurecimento de autenticação e controle de acesso.
3. Proteção contra abuso e brute-force.
4. Hardening de borda HTTP e observabilidade.
5. Pipeline de segurança e validação contínua.

---

# Etapa 1 — Segredos, configuração sensível e baseline operacional

## 1.1 Remover qualquer material sensível de arquivos versionados
### Tarefas
- Remover do `application.yml` qualquer senha fixa, placeholder sensível ou caminho acoplado a material criptográfico.
- Substituir por variáveis obrigatórias de ambiente ou secret manager.
- Garantir falha explícita de inicialização se segredo crítico estiver ausente.

### Backend
- Criar classe de propriedades fortemente tipada para certificado/crypto.
- Validar no startup que `path` e `password` não venham de fallback inseguro.
- Impedir defaults inseguros para ambientes não locais.

### Critérios de aceite
- Nenhum segredo sensível real ou placeholder enganoso em arquivo versionado.
- Aplicação falha ao subir se segredo crítico não estiver configurado.

---

## 1.2 Endurecer `.gitignore`
### Tarefas
- Adicionar padrões para arquivos de segredo e certificados.
- Revisar se existe necessidade de ignorar arquivos locais adicionais de dev.

### Sugestão
```gitignore
.env
.env.*
*.pfx
*.p12
*.jks
*.pem
*.key
*.crt
secrets.*
```

### Critérios de aceite
- Arquivos sensíveis comuns não podem ser adicionados por engano ao repositório.

---

## 1.3 Ajustar logging para produção
### Tarefas
- Remover `org.springframework.security=DEBUG` de profile padrão de produção.
- Remover `mail.smtp.debug=true` de profile padrão de produção.
- Criar profile local se necessário para debug assistido.

### Backend
- Separar `application-local.yml` ou equivalente para diagnósticos.
- Mascarar dados sensíveis em logs de autenticação e integração.

### Critérios de aceite
- Produção não sobe com logging sensível em DEBUG.
- Logs não expõem detalhe útil de autenticação ou SMTP.

---

# Etapa 2 — JWT, autenticação e autorização

## 2.1 Endurecer o modelo de token JWT
### Tarefas
- Adicionar claims/padrões de segurança: `iss`, `aud`, `jti`.
- Avaliar inclusão de `nbf`.
- Segregar segredo JWT por ambiente.
- Definir rotação controlada de segredo.

### Backend
- Ajustar `JwtUtils` para emissão e validação dessas claims.
- Garantir rejeição de token com issuer/audience inválidos.
- Criar testes unitários e de integração para cenários válidos/inválidos.

### Critérios de aceite
- Tokens sem `iss`/`aud` válidos são rejeitados.
- Existe estratégia documentada de rotação de segredo.

---

## 2.2 Implementar revogação/versionamento de token
### Tarefas
- Definir abordagem: `tokenVersion` por usuário ou blacklist por `jti`.
- Revogar tokens em eventos críticos:
    - reset de senha
    - desativação de usuário
    - alteração de aceite sensível
    - possível logout global

### Backend
- Adicionar campo `tokenVersion` no usuário ou armazenamento de blacklist com TTL.
- Alterar validação do filtro JWT para considerar revogação.
- Criar serviço de revogação centralizado.

### Critérios de aceite
- Token antigo deixa de funcionar após evento crítico.
- Existem testes cobrindo reset de senha e usuário desativado.

---

## 2.3 Remover dependência exclusiva da claim `terms_accepted`
### Tarefas
- Parar de tratar a claim do JWT como fonte única de verdade para autorização.
- Definir consulta server-side para estado de aceite.
- Avaliar cache de curta duração apenas se necessário.

### Backend
- Refatorar `TermsValidationFilter` para validar estado atual do usuário em fonte confiável.
- Se houver cache, invalidar em mudança de aceite.
- Manter resposta de bloqueio padronizada.

### Critérios de aceite
- Mudança de aceite reflete imediatamente ou dentro de janela controlada e documentada.
- Token antigo não mantém acesso indevido por informação stale.

---

## 2.4 Padronizar respostas de autenticação
### Tarefas
- Evitar propagar mensagens específicas de falha para o cliente.
- Padronizar payload 401/403.

### Backend
- Ajustar `DelegatedAuthenticationEntryPoint`.
- Garantir que exceções como usuário inativo não vazem detalhe sensível externamente.
- Manter detalhe apenas para log/auditoria interna.

### Critérios de aceite
- Respostas externas de autenticação usam mensagem genérica.
- Estado interno continua auditável sem vazar informação ao cliente.

---

# Etapa 3 — Proteção contra abuso e brute-force

## 3.1 Proteger `/auth/login`
### Tarefas
- Implementar rate limiting por IP.
- Implementar rate limiting por usuário/login.
- Adotar backoff progressivo ou bloqueio temporário.
- Registrar tentativas suspeitas.

### Backend
- Criar componente/interceptor/filtro de rate limit.
- Persistir ou cachear contadores em Redis ou estrutura equivalente.
- Retornar resposta controlada para abuso.

### Critérios de aceite
- Tentativas excessivas são bloqueadas temporariamente.
- Há telemetria para identificar abuso.

---

## 3.2 Proteger `/auth/recover-password` e `/auth/reset-password`
### Tarefas
- Aplicar rate limit específico.
- Impedir enumeração de usuário.
- Usar respostas idempotentes e genéricas.
- Avaliar CAPTCHA adaptativo em excesso de tentativas.

### Backend
- Padronizar resposta do recover/reset independentemente da existência do usuário.
- Registrar abuso por IP/email/username.

### Critérios de aceite
- Fluxo não permite enumeração óbvia de usuários.
- Requisições abusivas são limitadas.

---

## 3.3 Revisar login facial e endpoints biométricos
### Tarefas
- Confirmar aplicação efetiva dos limites configurados.
- Garantir bloqueio consistente por janela.
- Auditar mensagens de erro para não expor detalhes de biometria.

### Backend
- Mapear onde os limites do `application.yml` são consumidos.
- Criar testes de taxa/abuso para endpoints biométricos.

### Critérios de aceite
- Limites configurados estão ativos em runtime.
- Há testes cobrindo excedente de tentativas.

---

# Etapa 4 — Hardening HTTP e superfície exposta

## 4.1 Revisar CORS
### Tarefas
- Restringir `allowedHeaders` ao conjunto mínimo necessário.
- Confirmar se `allowCredentials(true)` é indispensável.
- Formalizar lista de origens por ambiente.

### Backend
- Refatorar `CorsConfigurationSource` para configuração por profile.
- Validar se origem nula, vazia ou inesperada é rejeitada.

### Critérios de aceite
- Headers permitidos são mínimos.
- Apenas origens autorizadas funcionam.

---

## 4.2 Revisar CSRF conforme canal de autenticação
### Tarefas
- Confirmar que a API usa exclusivamente bearer token em header.
- Caso exista ou venha a existir cookie/sessão, revisar estratégia de CSRF.

### Backend
- Documentar decisão de segurança no código/configuração.
- Adicionar teste de regressão se houver canais web com cookie.

### Critérios de aceite
- Há decisão explícita e documentada sobre CSRF.

---

## 4.3 Adicionar security headers explícitos
### Tarefas
- Definir `Content-Security-Policy` conforme frontend.
- Adicionar `Referrer-Policy`.
- Adicionar `Permissions-Policy`.
- Revisar política de frame/embedding.

### Backend
- Configurar headers em `SecurityConfig`.
- Validar compatibilidade com frontend e documentação.

### Critérios de aceite
- Headers de hardening aparecem nas respostas esperadas.

---

## 4.4 Revisar Actuator e `HttpExchangeRepository`
### Tarefas
- Confirmar necessidade real do `InMemoryHttpExchangeRepository`.
- Se não for necessário, remover.
- Se for necessário, garantir que endpoints correlatos não sejam expostos.

### Backend
- Revisar perfis e exposição do Actuator.
- Criar teste/configuração que garanta exposição mínima.

### Critérios de aceite
- Não há risco de exposição acidental de exchanges HTTP.

---

# Etapa 5 — Pipeline, qualidade e validação contínua

## 5.1 Adicionar varredura de segredos e SAST
### Tarefas
- Habilitar secret scanning no repositório/plataforma usada.
- Adicionar análise estática de segurança no pipeline.
- Avaliar CodeQL, Semgrep ou equivalente.

### Critérios de aceite
- Pipeline falha ou alerta em presença de segredos e achados relevantes.

---

## 5.2 Adicionar testes automatizados de segurança
### Tarefas
- Testes de autenticação inválida.
- Testes de token expirado/revogado.
- Testes de CORS.
- Testes de brute-force/rate limit.
- Testes de aceite de termos atualizado.

### Critérios de aceite
- Cenários críticos de segurança possuem cobertura automatizada.

---

## 5.3 Definir checklist de release seguro
### Tarefas
- Verificação de logging de produção.
- Verificação de secrets obrigatórios.
- Verificação de endpoints públicos.
- Verificação de headers de segurança.
- Verificação de pipeline verde com SAST/secret scanning.

### Critérios de aceite
- Release não é promovido sem passar no checklist mínimo.

---

# Sugestão de quebra em issues filhas
1. Remover segredos/configuração sensível versionada.
2. Endurecer `.gitignore` e adicionar secret scanning.
3. Ajustar logging de segurança para produção.
4. Endurecer emissão/validação de JWT.
5. Implementar revogação/versionamento de token.
6. Refatorar validação de aceite de termos server-side.
7. Padronizar respostas 401/403.
8. Implementar rate limiting para login e recover/reset.
9. Revisar CORS/CSRF.
10. Adicionar security headers.
11. Revisar Actuator/HttpExchangeRepository.
12. Adicionar testes automatizados de segurança.
13. Adicionar SAST no pipeline.

---

# Definição de pronto sugerida
Uma entrega dessa frente só pode ser considerada pronta quando:
- riscos críticos e altos da issue #213 estiverem tratados ou formalmente aceitos;
- houver testes cobrindo os fluxos de segurança alterados;
- pipeline possuir ao menos varredura de segredos e análise estática básica;
- configuração de produção estiver sem defaults inseguros e sem logging sensível.

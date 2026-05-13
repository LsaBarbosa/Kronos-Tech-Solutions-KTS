# Backlog de Correções — Kronos `PROD_HOSTINGER`

> Origem: auditoria de produção da branch `PROD_HOSTINGER`.
> Objetivo: organizar correções e melhorias por prioridade para preparar a branch para produção.

# Prioridade P3 — Dívida Técnica, Limpeza e Qualidade

## Objetivo

Resolver itens de menor risco imediato, mas importantes para manter o projeto saudável, previsível e compatível com versões futuras.

## Critério para encerrar P3

A prioridade P3 só deve ser considerada concluída quando:

- warnings simples forem limpos;
- build estiver mais preparado para Gradle/JDK futuros;
- testes não emitirem alertas críticos;
- a base estiver mais limpa para manutenção.

---

# KRN-P3-001 — Limpar imports duplicados

## Tipo

Código

## Severidade

Baixa

## Problema

Há imports duplicados em provider relacionado a token de recuperação de senha.

## Arquivo envolvido

| Arquivo | Ação |
|---|---|
| `PasswordResetTokenProviderImpl.java` | Remover imports duplicados |

## Critérios de aceite

- Imports duplicados removidos.
- IDE não mostra warnings simples.
- Build continua verde.
- Testes continuam passando.

## Testes obrigatórios

- `./gradlew clean test`
- `./gradlew clean build`

---

# KRN-P3-002 — Corrigir warnings de compatibilidade com Gradle 9

## Tipo

Build

## Severidade

Baixa

## Problema

O build apresenta warnings de recursos deprecated que podem afetar compatibilidade futura com Gradle 9.

## Arquivos envolvidos

| Arquivo | Ação |
|---|---|
| `build.gradle` | Atualizar configurações deprecadas |
| `settings.gradle` | Validar sintaxe |
| Gradle Wrapper | Atualizar se necessário |

## Critérios de aceite

- Build sem warnings de depreciação relevantes.
- Plugins atualizados.
- `./gradlew clean build` continua verde.
- Compatibilidade futura documentada.

## Testes obrigatórios

- `./gradlew clean test`
- `./gradlew clean build`
- `./gradlew dependencies`

---

# KRN-P3-003 — Ajustar warning de dynamic agent do Mockito

## Tipo

Testes

## Severidade

Baixa

## Problema

Testes apresentam warning relacionado ao dynamic agent do Mockito.

Esse aviso pode se tornar problema em versões futuras do JDK.

## Arquivos envolvidos

| Arquivo | Ação |
|---|---|
| `build.gradle` | Ajustar configuração de testes |
| Configuração JVM de testes | Adicionar agente Mockito se necessário |

## Critérios de aceite

- Warning de dynamic agent removido ou documentado.
- Testes seguem compatíveis com JDK futuro.
- Não há regressão nos testes existentes.

## Testes obrigatórios

- `./gradlew clean test`
- Validar saída do terminal sem warning crítico.

---

# KRN-P3-004 — Documentar decisões de segurança e produção

## Tipo

Documentação técnica

## Severidade

Baixa

## Problema

Algumas decisões precisam ficar explícitas para evitar regressões futuras, principalmente:

- uso obrigatório de cookie HttpOnly;
- política de CSRF;
- retenção legal;
- exclusão lógica;
- liveness facial;
- uso de migrations;
- configuração produtiva.

## Arquivos sugeridos

| Arquivo | Conteúdo |
|---|---|
| `docs/security/session-policy.md` | Política de sessão e cookies |
| `docs/security/csrf-policy.md` | Estratégia CSRF |
| `docs/production/hostinger-deploy.md` | Deploy produtivo |
| `docs/database/migrations.md` | Política de migrations |
| `docs/legal/data-retention.md` | Retenção legal e soft delete |

## Critérios de aceite

- Decisões críticas documentadas.
- Onboarding de novo dev fica mais simples.
- Auditoria futura consegue validar intenção técnica.
- Documentação não contém secrets.

---

# KRN-P3-005 — Criar checklist de pré-produção

## Tipo

Processo / Qualidade

## Severidade

Baixa

## Objetivo

Criar um checklist objetivo para evitar que branches futuras cheguem à produção com os mesmos problemas.

## Checklist sugerido

- [ ] Build passa.
- [ ] Testes passam.
- [ ] Migrations aplicam em banco limpo.
- [ ] JWT não aparece em response body.
- [ ] Cookie possui `HttpOnly`.
- [ ] Cookie possui `Secure` em produção.
- [ ] Cookie possui `SameSite`.
- [ ] Logout expira cookie.
- [ ] CSRF validado.
- [ ] Nenhum delete físico em dados legais.
- [ ] Profile `prod` ativo.
- [ ] Swagger desabilitado em produção.
- [ ] Logs sem DEBUG sensível.
- [ ] Scanner CVE executado.
- [ ] Actuator restrito.
- [ ] Upload validado.
- [ ] CORS restrito ao domínio real.
- [ ] Smoke test em produção executado.

## Critérios de aceite

- Checklist criado em Markdown.
- Checklist referenciado no README ou pipeline.
- Checklist usado antes de novo deploy.

---

# Checklist final P3

- [ ] Imports duplicados removidos.
- [ ] Warnings Gradle revisados.
- [ ] Mockito agent warning tratado.
- [ ] Decisões de segurança documentadas.
- [ ] Checklist de pré-produção criado.

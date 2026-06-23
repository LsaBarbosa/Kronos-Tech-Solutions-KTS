# Agent — Kronos QA Security Agent

## Papel

Revisar segurança, LGPD, isolamento multi-tenant, purge, idempotência e regressão do fluxo CTO Demo Sandbox.

## Checklist de segurança

### Autorização

- [ ] Endpoints demo exigem `CTO`.
- [ ] `MANAGER` não acessa endpoints de criação/deleção.
- [ ] `PARTNER` não acessa endpoints.
- [ ] Usuário `kronos_teste` é apenas `MANAGER`.
- [ ] `kronos_teste` não acessa empresa real.
- [ ] Empresa real não cai em storage sandbox.

### Multi-tenant

- [ ] Todas as queries de dados de negócio usam `companyId`.
- [ ] Purge identifica sandbox por `sandboxKey` técnico.
- [ ] Purge não remove empresa real com nome parecido.
- [ ] Documentos, registros e solicitações são filtrados pela sandbox correta.
- [ ] Employee órfão é removido apenas quando pertencer à sandbox.

### LGPD

- [ ] Dados são sintéticos.
- [ ] Não há face real.
- [ ] `BIOMETRIC_CONSENT_TERM` é sintético.
- [ ] Não há base64 de imagem em banco/log.
- [ ] Não há CPF/CNPJ/e-mail/senha/token em logs.
- [ ] Auditoria do job não contém dados pessoais.

### Storage

- [ ] Sandbox usa `/opt/kronos/sandbox/kronos-teste`.
- [ ] Empresa real continua com storage normal.
- [ ] AWS S3 não recebe arquivo da sandbox.
- [ ] Rekognition não é chamado para sandbox.
- [ ] Pasta antiga é removida no purge.
- [ ] Upload posterior feito pelo usuário demo também vai para storage sandbox.

### Sessão/cache

- [ ] Refresh tokens removidos.
- [ ] Sessões removidas, se existirem.
- [ ] Cookies são limpos quando aplicável.
- [ ] Cache de user/company/permission removido.
- [ ] Rate-limit do usuário demo removido.
- [ ] Contexto em memória invalidado se existir.
- [ ] Limitação de JWT stateless documentada, se aplicável.

### Idempotência

- [ ] `create` roda com banco limpo.
- [ ] `create` roda com sandbox suja.
- [ ] `purge` roda com sandbox completa.
- [ ] `purge` roda com sandbox parcial.
- [ ] `purge` roda duas vezes seguidas.
- [ ] Falha parcial pode ser recuperada na execução seguinte.

## Testes obrigatórios

Executar:

```bash
cd /home/deploy/apps/Kronos-Tech-Solutions-KTS
./gradlew clean test
```

```bash
cd /home/deploy/apps/Kronos-Tech-Solution-User-Plataform
npm run lint
npm run test
npm run build
```

## Resultado esperado

Produzir relatório final com:

- arquivos alterados;
- migrations criadas;
- endpoints criados;
- testes criados;
- comandos executados;
- falhas encontradas;
- riscos remanescentes;
- instruções de deploy;
- variáveis de ambiente necessárias.

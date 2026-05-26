# Pre-Production Checklist — Kronos

## Build e testes

- [ ] `./gradlew clean build` executa com sucesso.
- [ ] Testes unitários executam com sucesso.
- [ ] Testes de integração críticos executam com sucesso.

## Segurança

- [ ] `JWT_SECRET` definido por variável de ambiente.
- [ ] Cookies seguros em produção.
- [ ] CSRF ativo para métodos de escrita.
- [ ] Swagger público desativado em produção.
- [ ] Actuator expõe apenas endpoints necessários.

## LGPD

- [ ] Política de privacidade publicada.
- [ ] Termo de uso publicado.
- [ ] Termo biométrico versionado.
- [ ] Revogação biométrica testada.
- [ ] Exportação LGPD testada.
- [ ] Retenção em `DRY_RUN` validada.
- [ ] Retenção em `APPLY` somente com autorização.

## Uploads e documentos

- [ ] Limite de upload validado.
- [ ] MIME real validado.
- [ ] Antivírus habilitado em produção ou risco formalmente aceito.
- [ ] Bucket ou document storage com acesso restrito.

## Produção

- [ ] HTTPS ativo.
- [ ] Backup validado.
- [ ] Rollback documentado.
- [ ] Logs sem dados sensíveis desnecessários.

## Governança adicional

- [ ] Origens CORS de produção foram revisadas.
- [ ] Segredos, certificados e `.env` permanecem fora do repositório.
- [ ] O jurídico, controlador ou DPO revisaram pendências regulatórias antes do go-live.

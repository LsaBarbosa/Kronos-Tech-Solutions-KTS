# LGPD Production Checklist

## Código

- [ ] Back-end: `./gradlew clean test` passou
- [ ] Back-end: `./gradlew clean bootJar -x test` passou
- [ ] Front-end: `npm run test -- --run` passou
- [ ] Front-end: `npm run build` passou
- [ ] `git diff --check` passou nos dois repositórios
- [ ] Sem `storage/` versionado
- [ ] Sem segredos reais no repositório
- [ ] Histórico da branch não contém `storage/documents`, PDFs ou nomes sensíveis
- [ ] Sem logs com `storage_path`
- [ ] Sem logs com `faceImageBase64`
- [ ] AuditLog.details é sanitizado antes de persistir
- [ ] Exportação LGPD sanitiza `auditLogs.details`
- [ ] Anonimização restrita a `ADMINISTRATOR`
- [ ] Solicitações LGPD respeitam tenant
- [ ] Exportação LGPD respeita tenant
- [ ] Exportação LGPD minimiza geolocalização por padrão

## Ambiente

- [ ] `JWT_SECRET` forte e fora do Git
- [ ] `SECRET_TERM` forte e fora do Git
- [ ] AWS credentials fora do Git
- [ ] SMTP password fora do Git
- [ ] Swagger desabilitado em produção
- [ ] Actuator restrito
- [ ] `AUTH_COOKIE_SECURE=true`
- [ ] `AUTH_COOKIE_SAME_SITE` definido
- [ ] CORS restrito aos domínios aprovados
- [ ] Bucket S3 privado
- [ ] Rekognition configurado
- [ ] Segredos fora do git

## Jurídico e Operacional

- [ ] Política de privacidade publicada
- [ ] Termo biométrico validado juridicamente
- [ ] Canal LGPD publicado
- [ ] Encarregado ou contato responsável definido
- [ ] Processo de resposta a solicitações LGPD definido
- [ ] Processo de revogação biométrica validado
- [ ] Retenção validada juridicamente antes de ações destrutivas
- [ ] Retenção destrutiva automática permanece desabilitada até validação jurídica
- [ ] Processo de incidente definido
- [ ] Histórico da branch revisado para artefatos sensíveis

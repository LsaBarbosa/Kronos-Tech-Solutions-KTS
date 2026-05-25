# LGPD Production Release Checklist

**Data:** 2026-05-25  
**Versão:** 1.0  
**Status:** Pronto para validação de liberação

---

## ✅ Pré-Requisitos de Compilação

- [ ] Branch `feature/lgpd-compliance` validada e atualizada
- [ ] Nenhum uncommitted changes em código crítico
- [ ] Git history limpo e sem merge conflicts

---

## ✅ Compilação Back-end

- [ ] `./gradlew clean compileJava` sem erros
- [ ] `./gradlew clean compileTestJava` sem erros
- [ ] `./gradlew clean build` completa com sucesso
- [ ] Build time aceitável (< 3 minutos)
- [ ] Nenhuma dependência deprecated em alert

---

## ✅ Compilação Front-end

- [ ] `npm ci` completa sem erros
- [ ] `npm run build` completa em < 15s
- [ ] Bundle size dentro do esperado (262KB main, 620KB PDF)
- [ ] Nenhuma dependência deprecated em alert
- [ ] Build artifacts gerados em `dist/`

---

## ✅ Testes Back-end

### Testes Gerais
- [ ] `./gradlew test` completa com ≥97% passando (1455/1498+)
- [ ] Nenhuma falha em testes LGPD críticos:
  - [ ] BiometricConsentComplianceTest (8 testes)
  - [ ] DataRetentionComplianceTest (12+ testes)
  - [ ] MultiTenantComplianceTest (8 testes)
  - [ ] LgpdProcessingCatalogIntegrationTest
  - [ ] LgpdExportIntegrationTest
  - [ ] LgpdAdminRequestManagementIntegrationTest

### Testes de Segurança
- [ ] ProductionSecurityPropertiesValidatorTest passando
- [ ] ProductionSecurityPropertiesValidatorCorsTest passando
- [ ] SecurityConfigIntegrationTest passando
- [ ] Nenhuma falha de AuthenticationTest (crítico)

### Testes de Auditoria
- [ ] AuditLogRetentionProcessorTest passando
- [ ] LgpdRetentionAuditValidationTest passando
- [ ] Audit logging validado para ações LGPD

---

## ✅ Testes Front-end

### Testes Unitários
- [ ] `npm run test` completa com ≥99% passando (382/383+)
- [ ] Nenhuma falha em testes LGPD críticos:
  - [ ] BiometricConsentCard.test.tsx
  - [ ] TermsAcceptanceGate.test.tsx
  - [ ] ExportManifestDisplay.test.tsx
  - [ ] ExportConfirmationModal.test.tsx
  - [ ] AnonymizationResultSummary.test.tsx

### Testes E2E (Privacy Center)
- [ ] `npm run test:e2e` completa com 100% passando (9/9)
  - [ ] Should load Privacy Center with mocked APIs
  - [ ] Should call processing catalog endpoint
  - [ ] Should handle biometric consent data
  - [ ] Should handle LGPD requests list
  - [ ] Should handle empty processing catalog
  - [ ] Should handle server error (500)
  - [ ] Should handle unauthorized (401)
  - [ ] Should handle network timeout

---

## ✅ Segurança - Dependências

### npm Audit
- [ ] `npm audit` completa com 0 vulnerabilidades críticas
- [ ] Nenhuma high/critical vulnerability identificada
- [ ] Warnings (deprecated) documentados
- [ ] Plano de atualização futuro para mediums

### Dependências Java
- [ ] `./gradlew dependencyCheck` sem vulnerabilidades críticas (se disponível)
- [ ] Todas as bibliotecas OWASP-validadas

---

## ✅ Segurança - Configuração

### CORS (Cross-Origin Resource Sharing)
- [ ] CORS não usa wildcard `*`
- [ ] Apenas HTTPS permitido em produção
- [ ] Origins específicas configuradas (não http://)
- [ ] Credentials permitido apenas para mesma origem
- [ ] Preflight requests validadas
- [ ] Teste: POST request de origem diferente é rejeitado

### Cookies
- [ ] Cookie `Secure` flag ativo em produção (HTTPS obrigatório)
- [ ] Cookie `HttpOnly` flag ativo (sem acesso JavaScript)
- [ ] Cookie `SameSite` configurado (Strict ou Lax)
- [ ] Sessão com timeout definido (15 minutos máximo)
- [ ] Teste: Acesso direto a cookies via JavaScript é bloqueado

### JWT (JSON Web Tokens)
- [ ] JWT_SECRET forte (≥32 caracteres, alphanumêrico + símbolos)
- [ ] JWT_SECRET **NÃO** em código fonte (variável de ambiente)
- [ ] Token expiration configurado (≤1 hora)
- [ ] Refresh token com expiration maior (≤7 dias)
- [ ] Validação de assinatura ativa
- [ ] Teste: Token inválido é rejeitado

### Actuator (Spring Boot Admin)
- [ ] Actuator endpoints não expostos publicamente
- [ ] `/actuator` requer autenticação
- [ ] Apenas endpoints essenciais habilitados:
  - [ ] `/actuator/health` (público, info básica)
  - [ ] `/actuator/metrics` (autenticado)
  - Todos os outros desabilitados

### Swagger/OpenAPI
- [ ] Swagger **DESLIGADO** em produção
- [ ] Nenhuma documentação de API em `/swagger-ui`
- [ ] Nenhuma exposição de endpoints em `/api-docs`
- [ ] Teste: GET `/swagger-ui.html` retorna 404

---

## ✅ Segurança - AWS/S3/Rekognition

### AWS Credentials
- [ ] AWS_ACCESS_KEY_ID presente e válido
- [ ] AWS_SECRET_ACCESS_KEY presente e válido
- [ ] AWS_REGION configurado (ex: us-east-1)
- [ ] Credentials **NÃO** em código fonte
- [ ] IAM policy restritivo (apenas S3, Rekognition necessários)

### S3 Buckets
- [ ] `bucket-name` (documentos) existe e acessível
- [ ] `bucket-name-docs` (payroll) existe e acessível
- [ ] Bucket versioning ativo (para audit)
- [ ] Bucket public access bloqueado
- [ ] Servidor-side encryption (AES-256) ativo
- [ ] Teste: Upload de arquivo bem-sucedido
- [ ] Teste: Download de arquivo bem-sucedido

### Rekognition
- [ ] Collection ID configurado e existe
- [ ] Permissões de IndexFaces, SearchFacesByImage ativas
- [ ] Liveness check será desabilitado em produção (decisão documentada)
- [ ] Teste: Indexação de face bem-sucedida
- [ ] Teste: Busca de face bem-sucedida

### Antivírus / File Scanning
- [ ] Arquivo de teste (.eicar) é rejeitado no upload
- [ ] FileScanningProvider implementado e ativo
- [ ] Logs de rejeição registrados em auditoria
- [ ] Teste: Upload de arquivo malicioso é bloqueado

---

## ✅ LGPD Específico

### Privacy Center
- [ ] Front-end `/privacy-center` carrega com sucesso
- [ ] Catálogo de processamento exibe corretamente
- [ ] Biometric consent card mostra status correto
- [ ] Formulário de solicitação LGPD funcional:
  - [ ] Acesso (ACCESS) funciona
  - [ ] Retificação (RECTIFICATION) funciona
  - [ ] Exclusão (DELETION) funciona
  - [ ] Portabilidade (PORTABILITY) funciona
- [ ] Histórico de consentimento exibe corretamente
- [ ] Mensagens de erro tratadas adequadamente

### Exportação de Dados
- [ ] Endpoint `/lgpd/employees/{id}/export` retorna JSON válido
- [ ] Manifest contém: exportId, exportedAt, targetEmployeeId, sections
- [ ] Geolocalização precisa **NÃO** incluída (apenas autorizado)
- [ ] Dados sensíveis (CPF, salário) inclusos quando autorizado
- [ ] Arquivo de download funcional
- [ ] Teste: Usuário não-autorizado não pode exportar outro

### Solicitações LGPD
- [ ] Endpoint `/lgpd/requests` lista solicitações do titular
- [ ] Criação de solicitação funciona (POST)
- [ ] Mudança de status funciona (OPEN → IN_PROGRESS → COMPLETED)
- [ ] Notificações enviadas ao titular
- [ ] Manager/CTO podem listar todas as solicitações (com filtros)
- [ ] Status de solicitação visível ao titular em tempo real

### Auditoria LGPD
- [ ] AuditLog registra todas as ações LGPD:
  - [ ] LGPD_REQUEST_CREATED
  - [ ] LGPD_REQUEST_ASSIGNED
  - [ ] LGPD_EXPORT_INITIATED
  - [ ] LGPD_RETENTION_DRY_RUN
  - [ ] LGPD_RETENTION_APPLY
- [ ] Details de audit **NÃO** contêm: CPF, email, token, senha, mensagem
- [ ] IP address e User-Agent minimizados (não em details)
- [ ] Teste: Query de audit logs sem PII bem-sucedida

### Consentimento Biométrico
- [ ] Solicitação de consentimento aparece antes de usar biometria
- [ ] Revogação de consentimento funciona e bloqueia uso
- [ ] Template biométrico deletado imediatamente após revogação
- [ ] Histórico de consentimento preservado indefinidamente
- [ ] Teste: Usuário sem consentimento não pode usar biometria

### Retenção de Dados
- [ ] Dry-run calcula elegibilidade real (não zero)
- [ ] Dry-run **NÃO** altera dados
- [ ] Apply está **controlado por flag** `kronos.lgpd.retention.allow-apply`
- [ ] Apply bloqueado com motivo quando flag desabilitado
- [ ] Apply executado corretamente quando flag habilitado:
  - [ ] Mensagens internas soft-deleted
  - [ ] Audit logs minimizados
  - [ ] Contagens agregadas retornadas (sem PII)
- [ ] Teste: Apply com flag desabilitado é rejeitado
- [ ] Teste: Apply com flag habilitado executa corretamente

---

## ✅ Documentação e Conformidade

### Política de Privacidade
- [ ] Política de privacidade redigida e revisada por jurídico
- [ ] Menciona: consentimento, direitos do titular, retenção, segurança
- [ ] Disponível em `/privacy-policy` ou similar
- [ ] Versão e data de atualização clara
- [ ] Teste: Acesso público à política bem-sucedido

### Termo de Uso
- [ ] Termo de uso redigido e revisado por jurídico
- [ ] Menciona: LGPD, biometria, auditoria, responsabilidades
- [ ] Disponível em `/terms` ou similar
- [ ] Versão e data de atualização clara
- [ ] Teste: Acesso público ao termo bem-sucedido

### Documentação Técnica
- [ ] Relatório final técnico LGPD finalizado (lgpd-final-technical-status.md)
- [ ] Checklist de aceite LGPD finalizado (lgpd-final-acceptance-checklist.md)
- [ ] Documentação de produção (lgpd-production-env-checklist.md)
- [ ] Documentação de variáveis de ambiente completa
- [ ] Diagrama de fluxo LGPD atualizado

### Parecer Jurídico
- [ ] Parecer jurídico formal emitido
- [ ] Validação de bases legais (especialmente preservação de evidência)
- [ ] Conformidade com Lei nº 13.709/2018 (LGPD) confirmada
- [ ] Sem recomendações bloqueantes
- [ ] Documento assinado e datado

---

## ✅ Deployment e Infraestrutura

### Variáveis de Ambiente
- [ ] JWT_SECRET configurado (forte, único)
- [ ] FRONTEND_ALLOWED_ORIGINS configurado (HTTPS, sem wildcard)
- [ ] DATABASE_URL validada
- [ ] AWS_ACCESS_KEY_ID e AWS_SECRET_ACCESS_KEY configurados
- [ ] AWS_REGION configurado
- [ ] SMTP/Email configurado (se necessário)
- [ ] Todas as variáveis documentadas em `.env.example`

### Database
- [ ] Migrations executadas com sucesso (V1-V24)
- [ ] Schema validado e sem erros
- [ ] Indices criados (performance)
- [ ] Backups configurados e testados
- [ ] Retention policy aplicada (se aplicável)
- [ ] Teste: Query de dados LGPD bem-sucedida

### Deployment
- [ ] Container/VM provisionado com recursos adequados
- [ ] Certificado SSL/TLS válido e configurado
- [ ] HTTPS obrigatório (redirecionamento de HTTP)
- [ ] Load balancer configurado (se aplicável)
- [ ] Monitoramento de logs ativo
- [ ] Alertas configurados (CPU, memory, errors)

### Rollback Plan
- [ ] Versão anterior testada e pronta
- [ ] Plano de rollback documentado (passo a passo)
- [ ] Backup de database anterior disponível
- [ ] Tempo de RTO/RPO definido (ex: 15 min / zero data loss)
- [ ] Time de on-call notificado do plano
- [ ] Teste de rollback simulado completado

---

## ✅ Validação Pré-Go-Live (24h antes)

- [ ] Smoke tests em produção staging executados com sucesso
- [ ] Load testing realizado (throughput aceitável)
- [ ] Teste de failover/HA validado
- [ ] Monitoramento ativo em staging
- [ ] Time de suporte notificado
- [ ] Plano de comunicação (usuários, stakeholders) preparado

---

## ✅ Go-Live

- [ ] Checklist 100% marcado
- [ ] Parecer jurídico aprovado
- [ ] Produto owner aprova liberação
- [ ] Security team aprova liberação
- [ ] Deployment executado durante janela de manutenção
- [ ] Validação pós-deploy bem-sucedida
- [ ] Notificação de usuários enviada
- [ ] Monitoramento intensificado (24h)

---

## ✅ Pós-Go-Live (Primeiras 24h)

- [ ] Monitoramento de logs contínuo (sem erros críticos)
- [ ] Monitoramento de performance (latência aceitável)
- [ ] Zero relatórios críticos de usuários
- [ ] Auditoria de login/acesso normal
- [ ] Exportações LGPD funcionando
- [ ] Solicitações LGPD processando

---

## Pendências Finais (Antes de Liberar)

| Item | Responsável | Status | Data |
|------|-------------|--------|------|
| Parecer jurídico LGPD | Jurídico | ⚠️ Pendente | --- |
| Aprovação de security | Security | ⚠️ Pendente | --- |
| Aprovação de produto | Product Owner | ⚠️ Pendente | --- |
| Testes de penetração (opcional) | Security | ⚠️ Recomendado | --- |
| Validação em staging (24h antes) | QA | ⚠️ Pendente | --- |
| Plano de comunicação | Marketing | ⚠️ Pendente | --- |

---

## Notas de Conformidade

### ✅ O que foi validado
- Implementação técnica LGPD completa
- Testes 97%+ back-end, 99.7% front-end
- E2E Privacy Center 100%
- Zero vulnerabilidades críticas npm
- Configuração segura de CORS, cookies, JWT

### ⚠️ O que depende de revisão jurídica
- Bases legais (especialmente preservação de evidência)
- Política de privacidade
- Termo de uso
- Conformidade plena com LGPD

### ⚠️ O que depende de produção real
- Variáveis de ambiente (JWT_SECRET, credentials)
- Certificado SSL válido
- Configuração de backup/DR
- Teste de failover
- Monitoramento em tempo real

---

**Versão:** 1.0  
**Última Atualização:** 2026-05-25  
**Próxima Revisão:** Após go-live (7 dias)

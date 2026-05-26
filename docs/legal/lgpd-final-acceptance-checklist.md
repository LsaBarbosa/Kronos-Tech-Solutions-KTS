# LGPD — Checklist de Aceitação Final

**Data:** 2026-05-25  
**Versão:** 1.0  
**Status:** ⚠️ PENDENTE VALIDAÇÃO (Gate para Liberação em Produção)

Este checklist consolida todos os critérios de aceitação técnicos, jurídicos, operacionais e de segurança necessários antes da liberação em produção.

---

## 1. Critérios de Aceitação Jurídicos (CRÍTICO)

### 1.1 Parecer Jurídico Formal

- [ ] **Parecer jurídico assinado obtido**
  - Status: ⏳ PENDENTE
  - Responsável: Head of Legal
  - Bloqueador: ✅ SIM
  - Detalhes:
    - [ ] Base legal REGULAR_EXERCISE_OF_RIGHTS validada para preservação de evidência
    - [ ] Prazo de retenção 2555 dias (~7 anos) é apropriado sob LGPD
    - [ ] Transferência internacional (AWS/Rekognition) é permissível
    - [ ] Minimização (não deleção) de dados é conforme LGPD
    - [ ] Preservação de logs de consentimento para ~7 anos é conformante
    - [ ] Direitos de titulares (acesso, revogação, exportação) foram considerados
    - [ ] Parecer menciona data de validade (recomendado: 12 meses)

---

### 1.2 Documentação Jurídica Atualizada

- [ ] **Política de Privacidade publicada**
  - Status: ⏳ PENDENTE
  - Responsável: Legal + Marketing
  - Bloqueador: ✅ SIM
  - Conteúdo obrigatório:
    - [ ] Seção "Consentimento Biométrico" explicando:
      - O que é coletado (rosto, template biométrico)
      - Como é usado (autenticação, validação de identidade)
      - Quem acessa (interno, AWS, Rekognition)
      - Quanto tempo é retido (2555 dias com ressalva)
    - [ ] Seção "Preservação de Evidência" mencionando:
      - Registros de consentimento podem ser preservados conforme lei
      - Justificativa legal (REGULAR_EXERCISE_OF_RIGHTS ou legal obligation)
      - Período de preservação (~7 anos)
      - Como exercer direitos apesar da preservação
    - [ ] Seção "Transferência Internacional" mencionando:
      - AWS S3 (Brasil ou exterior)
      - Amazon Rekognition (exterior)
      - Standard Contractual Clauses (SCC) ou adequacy decision
    - [ ] Seção "Seus Direitos" incluindo:
      - Direito de acesso (como solicitar exportação)
      - Direito de retificação (contato para correção)
      - Direito de revogação (link para Privacy Center)
      - Direito de exclusão (respeitando períodos de retenção legal)
      - Direito de reclamação (contato ANPD)
    - [ ] Data de última revisão e próxima revisão planejada
    - [ ] Assinatura de Head of Legal ou DPO

- [ ] **Termo de Consentimento Biométrico atualizado**
  - Status: ⏳ PENDENTE
  - Responsável: Legal
  - Bloqueador: ✅ SIM
  - Conteúdo obrigatório:
    - [ ] Explica que evidência será preservada mesmo pós-revogação
    - [ ] Descreve período de retenção (2555 dias / ~7 anos)
    - [ ] Menciona minimização de dados (não deleção)
    - [ ] Oferece mecanismo claro de revogação (botão no Privacy Center)
    - [ ] Inclui data de versão (formato: YYYYMMDD)
    - [ ] Inclui hash SHA256 do documento (para prova de entrega)
    - [ ] Separa consentimento de biometria de outros consentimentos
    - [ ] Pode ser aceito/revogado independentemente
    - [ ] Tradução em português claro (não jurídico técnico)
    - [ ] QR code ou link para mais informações

- [ ] **Termos de Uso atualizados (se aplicável)**
  - Status: ⏳ PENDENTE
  - Responsável: Legal
  - Bloqueador: ⚠️ RECOMENDADO
  - Conteúdo:
    - [ ] Referência a Política de Privacidade (link)
    - [ ] Confirmação de conformidade com LGPD
    - [ ] Menção a direitos de titulares

---

### 1.3 Avaliação de Impacto à Proteção de Dados (DPIA)

- [ ] **DPIA foi conduzido**
  - Status: ⏳ PENDENTE
  - Responsável: DPO
  - Bloqueador: ✅ SIM (se risco elevado)
  - Documentação:
    - [ ] Descrição do processamento
    - [ ] Justificativa da necessidade
    - [ ] Avaliação de riscos
    - [ ] Medidas de mitigação
    - [ ] Conclusão sobre risco residual
    - [ ] Data de conclusão e assinatura DPO

- [ ] **Se risco elevado: Parecer da ANPD obtido**
  - Status: ⏳ PENDENTE
  - Responsável: Legal
  - Bloqueador: ✅ SIM (se aplicável)
  - Detalhes:
    - [ ] Solicitação foi enviada (art. 38, §5º LGPD)
    - [ ] ANPD respondeu dentro de 30 dias
    - [ ] Parecer foi incorporado ao projeto
    - [ ] Se ANPD alertou sobre riscos, foram mitigados

---

### 1.4 Registro de Atividades de Tratamento

- [ ] **Registro documenta processamento LGPD**
  - Status: ⏳ PENDENTE
  - Responsável: DPO
  - Bloqueador: ✅ SIM
  - Registro obrigatório inclui:
    - [ ] Identificação do controlador (Kronos Tech Solutions)
    - [ ] Nome da atividade: "Preservação de evidência de consentimento biométrico"
    - [ ] Finalidade: Cumprimento de obrigações legais e validação de direitos
    - [ ] Base legal: REGULAR_EXERCISE_OF_RIGHTS (art. 7º, VII LGPD)
    - [ ] Categorias de dados: Biometria facial, templates, consentimento, timestamps
    - [ ] Categorias de pessoas: Usuários autenticados da plataforma
    - [ ] Categorias de destinatários:
      - [ ] AWS (S3, CloudTrail, CloudWatch)
      - [ ] Amazon Rekognition
      - [ ] Auditores internos
      - [ ] Consultores jurídicos conforme necessário
    - [ ] Período de retenção: 2555 dias (~7 anos)
    - [ ] Medidas de segurança:
      - [ ] Criptografia em trânsito (HTTPS/TLS)
      - [ ] Criptografia em repouso (AES-256, KMS)
      - [ ] Sanitização de logs
      - [ ] Acesso restrito (CTO role)
      - [ ] Auditoria de acessos
    - [ ] Transferência internacional: SIM (AWS, Rekognition)
    - [ ] Responsável pelo registro: Nome e contato DPO

---

## 2. Critérios de Aceitação de Conformidade com Terceiros

### 2.1 Data Processing Agreement (DPA) com AWS

- [ ] **DPA assinado está em vigor**
  - Status: ⏳ PENDENTE
  - Responsável: Procurement + Legal
  - Bloqueador: ✅ SIM
  - DPA deve incluir:
    - [ ] Menção explícita a "dados biométricos" como categoria de dados
    - [ ] Confirmação de conformidade com LGPD
    - [ ] Período de retenção mínimo: 2555 dias
    - [ ] Direito de auditoria (AWS deve permitir auditorias LGPD)
    - [ ] Cláusula de transferência internacional:
      - [ ] Standard Contractual Clauses (SCC) foram assinadas, OU
      - [ ] Adequacy decision da ANPD cobre Brasil → AWS, OU
      - [ ] Binding Corporate Rules (BCR) foram aprovadas
    - [ ] Mecanismo de deleção ao fim do contrato
    - [ ] Notificação de breach em < 72h
    - [ ] Cópia de políticas de segurança

---

### 2.2 Conformidade com Amazon Rekognition

- [ ] **Contrato/Termos com Rekognition vigentes**
  - Status: ⏳ PENDENTE
  - Responsável: Procurement + Legal
  - Bloqueador: ✅ SIM
  - Termos devem confirmar:
    - [ ] "Dados não serão usados para treinamento de modelos"
    - [ ] Conformidade com LGPD
    - [ ] Período mínimo de retenção de 2555 dias
    - [ ] Cláusulas de segurança:
      - [ ] Criptografia de dados em trânsito
      - [ ] Criptografia de dados em repouso
      - [ ] Controle de acesso (quem pode acessar)
      - [ ] Auditoria de acessos
    - [ ] Transferência internacional coberta por SCC ou adequacy decision
    - [ ] Notificação de breach

---

### 2.3 Conformidade com Legislação Estrangeira

**Aplica-se se usuários fora Brasil:**

- [ ] **GDPR (Regulamento UE)**
  - Status: 🔍 AVALIAR
  - [ ] Se processamento na UE: Adequacy decision ou SCC
  - [ ] Se usuários na UE: Privacy Shield ou SCC

- [ ] **CCPA (California)**
  - Status: 🔍 AVALIAR
  - [ ] Se usuários na CA: Validar conformidade

- [ ] **PDPA (Singapura)**
  - Status: 🔍 AVALIAR
  - [ ] Se usuários em SG: Validar conformidade

- [ ] **Outras jurisdições conforme necessário**
  - Status: 🔍 AVALIAR

---

## 3. Critérios de Aceitação Técnicos

### 3.1 Backend (Java Spring Boot)

- [ ] **Autenticação e Autorização**
  - [ ] Endpoints /lgpd/* requerem JWT válido (401 sem token)
  - [ ] Endpoints /lgpd/admin/* requerem role CTO (403 sem role)
  - [ ] Testes de segurança passam (auth, authz, CORS, headers)
  - [ ] Rate limiting está ativo (429 após limite)

- [ ] **Data Processing Catalog**
  - [ ] GET /lgpd/data-processing-catalog funciona (200)
  - [ ] Retorna array de processamentos
  - [ ] LEGAL_CONSENT_EVIDENCE está com base legal REGULAR_EXERCISE_OF_RIGHTS
  - [ ] Descrição pública não afirma conformidade absoluta
  - [ ] Endpoint é público (sem autenticação)

- [ ] **Biometric Consent Management**
  - [ ] POST /lgpd/biometric/consent funciona (201)
  - [ ] Registro é criado com status "ACCEPTED"
  - [ ] Timestamp é registrado
  - [ ] DELETE /lgpd/biometric/consent funciona (200)
  - [ ] Status muda para "REVOKED"
  - [ ] Dados biométricos são removidos de S3
  - [ ] Evidência é minimizada (não deletada)

- [ ] **Data Export (Direitos do Titular)**
  - [ ] POST /lgpd/user/data-export funciona (202 - async)
  - [ ] GET /lgpd/user/data-export/{id} retorna status
  - [ ] Export contém todos os dados do usuário
  - [ ] Export é entregue em JSON ou CSV
  - [ ] Limite de 30 dias é respeitado
  - [ ] Apenas o próprio usuário consegue acessar seu export

- [ ] **Retention Policy Execution**
  - [ ] GET /lgpd/admin/retention/dry-run funciona (200)
  - [ ] Retorna contagens: scanned, affected, skipped
  - [ ] Não altera dados (é dry-run)
  - [ ] POST /lgpd/admin/retention/apply funciona (202 - async)
  - [ ] Dados são minimizados após execução
  - [ ] Audit log registra execução
  - [ ] Apenas CTO consegue executar (403 para outros)

- [ ] **Logging & Auditoria**
  - [ ] Logs não contêm CPF inteiro (xxx.xxx.xxx-xx)
  - [ ] Logs não contêm email inteiro
  - [ ] Logs não contêm biometric templates
  - [ ] Logs não contêm JWT tokens
  - [ ] Logs não contêm senhas
  - [ ] SensitiveDataMasker está ativo
  - [ ] Audit log registra: user, action, timestamp, result

- [ ] **Segurança**
  - [ ] HTTPS está obrigatório (redirect de HTTP)
  - [ ] Certificado é válido (não auto-assinado)
  - [ ] TLS 1.2+
  - [ ] Headers de segurança estão presentes:
    - [ ] Strict-Transport-Security
    - [ ] X-Content-Type-Options
    - [ ] X-Frame-Options
    - [ ] Content-Security-Policy
  - [ ] CORS whitelist não é "*"
  - [ ] JWT_SECRET tem >= 32 caracteres
  - [ ] JWT_SECRET não é hardcoded
  - [ ] Não há secrets em código-fonte

---

### 3.2 Frontend (React/TypeScript)

- [ ] **Biometric Consent UI**
  - [ ] Privacy Center carrega (GET /privacy)
  - [ ] BiometricConsentCard renderiza
  - [ ] Status carrega corretamente (loading → active/revoked)
  - [ ] Botão "Aceitar e Cadastrar Biometria" funciona
  - [ ] Botão "Revogar Consentimento" funciona (se ativo)
  - [ ] Dialog de confirmação aparece (não 1-click)
  - [ ] Aviso menciona "pode ser preservado conforme lei"
  - [ ] Erro é tratado gracefully

- [ ] **Data Processing Catalog UI**
  - [ ] DataProcessingCatalogCard carrega (GET /privacy)
  - [ ] Catalog é carregado (GET /lgpd/data-processing-catalog)
  - [ ] Itens são renderizados
  - [ ] Não há vazamento de dados ao local storage
  - [ ] Erro é tratado (API fail, auth error, etc.)
  - [ ] Menção de "pode ser preservado" está presente

- [ ] **Segurança Frontend**
  - [ ] Nenhum JS inline em componentes LGPD
  - [ ] Tokens não estão em localStorage (usar HttpOnly cookies)
  - [ ] Nenhum XSS potencial (React automaticamente escapa)
  - [ ] Nenhum PII em data attributes HTML
  - [ ] CSP header é respeitado
  - [ ] CORS preflight funciona

- [ ] **Build & Deployment**
  - [ ] npm build sem erros
  - [ ] npm test: todos testes passam (21+ LGPD tests)
  - [ ] Não há warnings de segurança (npm audit)
  - [ ] Production build está otimizado (tree-shaken, minified)

---

### 3.3 Database

- [ ] **Tabelas LGPD existem**
  - [ ] lgpd_biometric_consent
  - [ ] lgpd_data_processing_catalog
  - [ ] retention_execution_log
  - [ ] Outras tabelas de auditoria

- [ ] **Dados iniciais estão inseridos**
  - [ ] DataProcessingCatalog tem >= 5 itens
  - [ ] RetentionPolicyCatalog tem >= 3 políticas
  - [ ] Enums estão populados

- [ ] **Backup funciona**
  - [ ] Database backup é automático
  - [ ] Restore foi testado (dry-run)
  - [ ] Retenção de backup >= 7 dias

- [ ] **Segurança**
  - [ ] Conexão usa SSL/TLS
  - [ ] Credenciais estão em variáveis de ambiente (não hardcoded)
  - [ ] Nenhum usuário com senha default
  - [ ] Apenas aplicação consegue acessar (network rules)

---

## 4. Critérios de Aceitação Operacionais

### 4.1 Configuração de Produção Validada

- [ ] **JWT & Session**
  - [ ] JWT_SECRET é seguro (validado)
  - [ ] JWT_EXPIRATION está configurado (recomendado: 1h)
  - [ ] REFRESH_TOKEN_EXPIRATION está configurado (recomendado: 7d)
  - [ ] Cookies são SECURE (HTTPS only)
  - [ ] Cookies são HttpOnly
  - [ ] Cookies têm SameSite=STRICT

- [ ] **S3 & Storage**
  - [ ] Bucket é privado (não público)
  - [ ] Versionamento está ativo
  - [ ] MFA delete está ativo
  - [ ] Encryption padrão: AES-256 ou KMS
  - [ ] ACL é mínimo (não "Everyone")

- [ ] **Logging Centralizado**
  - [ ] Logs são enviados para ELK/CloudWatch/similar
  - [ ] Retenção: 90 dias
  - [ ] Logs são imutáveis (append-only)
  - [ ] Acesso a logs é auditado
  - [ ] Nenhum PII em logs (amostras validadas)

- [ ] **Monitoramento**
  - [ ] Alerts para acesso suspeito estão configurados
  - [ ] Alerts para erros críticos estão configurados
  - [ ] Dashboards estão disponíveis (Grafana/Datadog)
  - [ ] Incident response plan existe

---

### 4.2 Segurança de Infraestrutura

- [ ] **Network**
  - [ ] HTTPS/TLS 1.2+ é obrigatório
  - [ ] WAF (Web Application Firewall) está ativo
  - [ ] Rate limiting está configurado
  - [ ] DDoS protection está ativo (CloudFlare/AWS Shield)

- [ ] **IAM & Access Control**
  - [ ] AWS IAM Role está configurado (não static credentials)
  - [ ] Princípio de menos privilégio aplicado
  - [ ] MFA está habilitado para acesso admin
  - [ ] Service account credentials são rotacionadas

- [ ] **Antivirus & Scanning**
  - [ ] Antivirus instalado e atualizado
  - [ ] Definições são atualizadas diariamente
  - [ ] Scanning é agendado (diário)

---

## 5. Critérios de Aceitação de Teste

### 5.1 Testes Técnicos

- [ ] **Backend Tests**
  - [ ] Testes de unidade: >= 95% passing
  - [ ] Testes de integração LGPD: >= 50 passing
  - [ ] Testes de segurança: vulnerabilidades críticas = 0
  - [ ] SAST (SonarQube): vulnerabilidades altas = 0

- [ ] **Frontend Tests**
  - [ ] Testes de componente: >= 20 passing
  - [ ] Testes de integração: >= 10 passing
  - [ ] Browser testing: Chrome, Firefox, Safari, Edge
  - [ ] Mobile testing: iOS Safari, Android Chrome

- [ ] **Integration Tests**
  - [ ] API + Frontend workflows
  - [ ] Database + API workflows
  - [ ] S3 + API workflows
  - [ ] Rekognition + API workflows (se integrado)

---

### 5.2 Testes de Conformidade LGPD

- [ ] **Smoke Tests Funcionais**
  - [ ] Login funciona
  - [ ] Privacy Center carrega
  - [ ] Usuário consegue aceitar consentimento
  - [ ] Aceitação é registrada com timestamp
  - [ ] Usuário consegue revogar consentimento
  - [ ] Revogação é registrada
  - [ ] Dados biométricos são deletados de S3
  - [ ] Evidência é preservada (logs)

- [ ] **Teste de Direitos do Titular**
  - [ ] Exportação de dados funciona
  - [ ] Export contém todos os dados
  - [ ] Export é entregue em JSON/CSV
  - [ ] Acesso de terceiros é bloqueado
  - [ ] Prazo de 30 dias é respeitado

- [ ] **Teste de Retenção**
  - [ ] Dry-run retorna contagens corretas
  - [ ] Apply minimiza dados conforme política
  - [ ] Dados são retidos pelo prazo configurado
  - [ ] Audit log registra execução

---

### 5.3 Teste de Penetração

- [ ] **Pentest foi executado**
  - Status: ⏳ PENDENTE
  - Responsável: Security + Pentest Team
  - Bloqueador: ✅ SIM
  - Resultado esperado: 0 críticas, 0 altas não-mitigadas

- [ ] **Vulnerabilidades foram resolvidas**
  - [ ] Críticas: 100% fixadas
  - [ ] Altas: 100% fixadas ou mitigadas com aprovação
  - [ ] Médias: Planejadas para próxima sprint
  - [ ] Baixas: Backlog

---

## 6. Critérios de Aceitação de Comunicação

### 6.1 Stakeholders Internos

- [ ] **Executivos notificados**
  - [ ] CEO/Board
  - [ ] VP Operations
  - [ ] Head of Product
  - [ ] Status: Aprovação para produção

- [ ] **Time Técnico Treinado**
  - [ ] DevOps: deploy, rollback, monitoramento
  - [ ] Backend: endpoints LGPD, fluxos de consentimento
  - [ ] Frontend: UI components, workflows
  - [ ] Database: backup, restore, compliance

- [ ] **Compliance & Legal Treinados**
  - [ ] DPO: procedures de resposta a incidente
  - [ ] Head of Legal: parecer jurídico distribuído
  - [ ] Compliance: regulatório checklist

---

### 6.2 Usuários Finais

- [ ] **Documentação de Privacy Center publicada**
  - [ ] FAQ sobre biometric consent
  - [ ] Guia de revogação
  - [ ] Política de privacidade atualizada
  - [ ] Termo de consentimento disponível

- [ ] **Suporte preparado**
  - [ ] Scripts de troubleshooting
  - [ ] FAQ respondidas
  - [ ] Escalação para DPO definida

---

## 7. Critérios de Aceitação de Conformidade Regulatória

### 7.1 LGPD Compliance

- [ ] **Conformidade técnica**
  - [ ] Consentimento informado (Termo de Consentimento Biométrico)
  - [ ] Direito de acesso (exportação de dados)
  - [ ] Direito de revogação (Privacy Center)
  - [ ] Direito de exclusão (respeitando retenção legal)
  - [ ] Transparência (Política de Privacidade + Catálogo Público)

- [ ] **Conformidade processual**
  - [ ] Parecer jurídico obtido
  - [ ] DPIA conduzido
  - [ ] DPO designado
  - [ ] Registro de atividades preenchido
  - [ ] Comunicação a ANPD (se breach)

---

## 8. Matriz de Risco Residual

| Risco | Probabilidade | Impacto | Mitigação | Status |
|-------|--------------|---------|-----------|--------|
| Parecer jurídico rejeita implementação | Baixa | Alto | Legal review prévia | 🟡 Em revisão |
| Vazamento de dados biométricos | Muito Baixa | Crítico | Pentest, encryption | 🔴 Pentest pendente |
| Falha de retenção automática | Baixa | Médio | Testes, monitoring | 🟢 Testado |
| Não-conformidade com GDPR | Baixa | Alto | Legal review GDPR | 🟡 Em revisão |
| Revogação falha em produção | Muito Baixa | Médio | E2E tests | 🟢 Testado |

---

## 9. Sign-Off e Aprovações Finais

Antes de liberar em produção, os seguintes sign-offs são obrigatórios:

| Papel | Nome | Data | Assinatura | Status |
|-------|------|------|-----------|--------|
| Head of Legal | | | | ⏳ PENDENTE |
| CTO/Arquitetura | | | | ⏳ PENDENTE |
| Head of Security | | | | ⏳ PENDENTE |
| DPO | | | | ⏳ PENDENTE |
| VP Operations | | | | ⏳ PENDENTE |

---

## 10. Go/No-Go Decision

### 10.1 GO Criteria (Tudo deve estar ✅)

- [ ] Parecer jurídico formal obtido e assinado
- [ ] Políticas públicas (Privacidade + Termo) atualizadas e publicadas
- [ ] DPIA concluído com risco aceitável
- [ ] DPO designado e informado
- [ ] DPA com AWS assinado
- [ ] Contrato com Rekognition vigente
- [ ] Registro de Atividades preenchido
- [ ] Configuração de produção validada
- [ ] Pentest concluído com 0 críticas
- [ ] Testes funcionais LGPD: 100% passing
- [ ] Backend + Frontend + Database integrados
- [ ] Backup + Rollback testados em staging
- [ ] Monitoramento ativo
- [ ] Logs centralizados
- [ ] Time treinado
- [ ] Runbook de operação pronto
- [ ] Runbook de rollback pronto
- [ ] Runbook de incident response pronto

### 10.2 NO-GO Criteria (Qualquer um bloqueia)

- ❌ Parecer jurídico não está obtido ou é negativo
- ❌ Vulnerabilidades críticas de segurança não corrigidas
- ❌ PII vaza em logs ou responses
- ❌ HTTPS não está ativo
- ❌ DPA ou contrato não estão assinados
- ❌ Testes LGPD falharam
- ❌ DPO não foi designado

---

## 11. Plano de Remediação

Se qualquer critério acima falhar, siga este plano:

### 11.1 Jurídico/Compliance (CRÍTICO)

Se parecer jurídico rejeita:
1. Reunir com Head of Legal e DPO
2. Discutir mudanças na implementação
3. Reviar timeline (novo parecer pode levar 2+ semanas)

Se DPIA indica risco elevado:
1. Solicitar parecer ANPD (art. 38, §5º)
2. Aguardar 30 dias pela resposta
3. Incorporar feedback ao projeto

### 11.2 Segurança (CRÍTICO)

Se pentest encontra crítica:
1. Parar tudo
2. Notificar Head of Security + CTO
3. Fix + re-teste em staging
4. Liberar apenas após aprovação final

Se teste falha:
1. Debug e identificar root cause
2. Fix código
3. Re-rodar teste
4. Não liberar sem 100% passing

### 11.3 Operacional (RECOMENDADO)

Se configuração não está validada:
1. Executar validation checklist
2. Corrigir misconfigurations
3. Re-validar
4. Documentar em remediation log

---

## 12. Próximas Fases

Após aprovação para produção:

### Fase 3: Deployment em Produção
- Deploy backend
- Deploy frontend
- Validação de health checks
- Smoke tests em produção
- Escalação para on-call

### Fase 4: Monitoramento Pós-Deploy
- Daily: revisar logs/alerts (1º mês)
- Weekly: revisar métricas (1º mês)
- Monthly: relatório de conformidade
- Quarterly: revisão de controles

---

## 13. Documentação Relacionada

- [LGPD Status Técnico Final](lgpd-final-technical-status.md)
- [LGPD Legal Review Checklist](lgpd-legal-review-checklist.md)
- [Production Release Checklist](../production/lgpd-production-release-checklist.md)
- [Config Validation (Staging/Prod)](../production/lgpd-staging-production-config-validation.md)
- [Rollback Runbook](../production/lgpd-rollback-validation-runbook.md)
- [Penetration Testing Scope](../security/lgpd-penetration-testing-scope.md)

---

**Documento de:** Legal + Compliance + Engineering  
**Última revisão:** 2026-05-25  
**Próxima revisão:** Após parecer jurídico + pentest  
**Status:** ⚠️ AGUARDANDO VALIDAÇÃO (Gate para Produção)

## Notas Finais

1. Este checklist é **BLOQUEADOR** para produção. Nenhuma liberação sem 100% de conformidade.
2. Cada item deve ter um proprietário designado e data de conclusão esperada.
3. Se qualquer item não puder ser atendido, escalar para CTO + Head of Legal imediatamente.
4. Risco jurídico é o maior bloqueador — parecer jurídico formal é **OBRIGATÓRIO**.
5. Risco de segurança é o segundo bloqueador — pentest com 0 críticas é **OBRIGATÓRIO**.

Para dúvidas, contactar: legal@kronos.com, dpo@kronos.com, ou cto@kronos.com

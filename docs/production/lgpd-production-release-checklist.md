# LGPD Production Release Checklist

**Data:** 2026-05-25  
**Versão:** 1.0  
**Status:** ⚠️ PRÉ-PRODUÇÃO (Awaiting Legal Review)

Este checklist deve ser completado antes de qualquer liberação em produção.

---

## 1. Validação Jurídica (CRÍTICO)

### 1.1 Parecer Jurídico Formal

- [ ] Parecer jurídico assinado confirmando:
  - Base legal REGULAR_EXERCISE_OF_RIGHTS é válida?
  - Prazo de 2555 dias (~7 anos) é apropriado?
  - Transferência internacional (AWS/Rekognition) é permissível?
  - Minimização (não deleção) é conforme LGPD?

**Responsável:** Jurídico Externo  
**Bloqueador:** ✅ SIM — Não liberar produção sem parecer

---

### 1.2 Policies Jurídicas Atualizadas

- [ ] Política de Privacidade atualizada
  - [ ] Seção sobre consentimento biométrico
  - [ ] Menção explícita a preservação de evidência
  - [ ] Prazo de retenção (2555 dias) declarado
  - [ ] Menção a terceiros (AWS, Rekognition)
  - [ ] Direitos do titular (acesso, exclusão, revogação)

- [ ] Termo de Consentimento Biométrico atualizado
  - [ ] Explica que evidência será preservada
  - [ ] Descreve período de retenção
  - [ ] Menciona minimização de dados
  - [ ] Oferece mecanismo claro de revogação
  - [ ] Inclui data de versão e hash

- [ ] Termos de Uso atualizados (se aplicável)
  - [ ] Referência a política de privacidade
  - [ ] Conformidade com LGPD

**Responsável:** Legal/Marketing  
**Bloqueador:** ✅ SIM — Não liberar produção sem atualizar

---

## 2. Conformidade com Terceiros

### 2.1 Data Processing Agreement (DPA)

**AWS S3:**
- [ ] DPA assinado incluindo:
  - [ ] Menção explícita a dados biométricos
  - [ ] Conformidade com LGPD
  - [ ] Período de retenção de dados
  - [ ] Direito de auditoria
  - [ ] Cláusula de transferência internacional (SCCs)
  - [ ] Mecanismo de deleção em fim de contrato

**Amazon Rekognition:**
- [ ] Contrato/DPA incluindo:
  - [ ] Confirmação: dados não serão usados para treinamento
  - [ ] Conformidade com LGPD
  - [ ] Período de retenção
  - [ ] Cláusulas de segurança (encryption, access control)
  - [ ] Transferência internacional (Standard Contractual Clauses)

**Outros Provedores (se aplicável):**
- [ ] (Listar e validar)

**Responsável:** Procurement/Legal  
**Bloqueador:** ✅ SIM — Não liberar produção sem DPA

---

### 2.2 Conformidade com Legislação Estrangeira

**Se processamento em múltiplos países:**
- [ ] Validar conformidade com GDPR (UE)
- [ ] Validar conformidade com CCPA (Califórnia)
- [ ] Validar conformidade com PIPEDA (Canadá)
- [ ] Validar conformidade com PDPA (Singapura)
- [ ] Outras legislações conforme localização dos dados

**Responsável:** Legal/Compliance  
**Bloqueador:** ⚠️ SIM (se aplicável)

---

## 3. Governança de Dados Pessoais

### 3.1 Encarregado de Proteção de Dados (DPO)

- [ ] DPO designado ou terceirizado
- [ ] DPO informado sobre:
  - [ ] Retenção de evidência de consentimento
  - [ ] Política de 2555 dias
  - [ ] Transferência para AWS/Rekognition
  - [ ] Procedimento de resposta a incidentes
- [ ] DPO confirmou conformidade

**Responsável:** RH/Legal  
**Bloqueador:** ✅ SIM — DPO deve estar designado

---

### 3.2 Avaliação de Impacto à Proteção de Dados (DPIA)

- [ ] DPIA conduzido para:
  - [ ] Consentimento biométrico
  - [ ] Retenção de evidência (2555 dias)
  - [ ] Transferência internacional
  - [ ] Acesso por terceiros

- [ ] DPIA documenta:
  - [ ] Descrição do processamento
  - [ ] Justificativa da necessidade
  - [ ] Avaliação de riscos
  - [ ] Medidas de mitigação
  - [ ] Conclusão: risco residual aceitável?

- [ ] Se risco elevado:
  - [ ] Parecer da ANPD solicitado (art. 38, §5º)
  - [ ] Parecer da ANPD obtido

**Responsável:** DPO/Compliance  
**Bloqueador:** ✅ SIM — Obrigatório se risco elevado

---

### 3.3 Registro de Atividades de Tratamento

- [ ] Registro documenta:
  - [ ] Identificação do controlador
  - [ ] Finalidade: "Preservação de evidência de consentimento"
  - [ ] Categorias de dados: Biometria, consentimento, timestamps
  - [ ] Categorias de destinatários: AWS, Rekognition, auditores
  - [ ] Período de retenção: 2555 dias
  - [ ] Medidas de segurança: Encryption, sanitização, auditoria
  - [ ] Transferência internacional: SIM (AWS, Rekognition)

**Responsável:** DPO/Compliance  
**Bloqueador:** ✅ SIM — Obrigatório por lei

---

## 4. Segurança Técnica

### 4.1 Configuração de Produção

**Autenticação e Sessão:**
- [ ] JWT_SECRET é seguro (min 32 caracteres, alfanumérico)
- [ ] JWT_SECRET não é hardcoded
- [ ] JWT_EXPIRATION está configurado (recomendado: 1h)
- [ ] REFRESH_TOKEN_EXPIRATION está configurado (recomendado: 7 dias)
- [ ] Cookies são SECURE (HTTPS only)
- [ ] Cookies são HttpOnly (não acessível via JavaScript)
- [ ] Cookies têm SameSite=STRICT

**Comunicação:**
- [ ] HTTPS/TLS 1.2+ é obrigatório
- [ ] Certificado é válido e não auto-assinado
- [ ] HSTS (HTTP Strict Transport Security) está ativo
- [ ] CORS whitelist é restritivo (não "*")
- [ ] CORS não expõe credentials sem necessidade

**Autorização:**
- [ ] Endpoints LGPD requerem autenticação
- [ ] Endpoints sensíveis requerem papel específico (CTO, Admin)
- [ ] Rate limiting está ativo (e.g., 100 req/min por IP)
- [ ] Rate limiting é mais restritivo para endpoints sensíveis

**Data Storage:**
- [ ] S3 bucket é privado (não público)
- [ ] S3 bucket tem versionamento ativado
- [ ] S3 bucket tem MFA delete ativado
- [ ] S3 bucket tem encryption padrão (AES-256)
- [ ] S3 bucket tem ACL mínimo (não "Everyone")
- [ ] Backup é automático e encriptado
- [ ] Replicação (se multi-region) é encriptada

**Logging:**
- [ ] Logs não contêm PII (validar amostras)
- [ ] Logs são enviados para sistema centralizado
- [ ] Retenção de logs está configurada (recomendado: 90 dias)
- [ ] Logs são imutáveis (append-only)
- [ ] Acesso a logs é auditado

**Monitoramento:**
- [ ] Alerts para acesso suspeito estão ativados
- [ ] Alerts para mudanças de configuração estão ativados
- [ ] Dashboards de segurança estão configurados
- [ ] Incident response plan existe e está testado

**Responsável:** DevOps/Security  
**Bloqueador:** ✅ SIM — Segurança é prerequisito

---

### 4.2 Teste de Segurança

- [ ] Teste de penetração realizado
  - [ ] Foco em LGPD (consentimento, retenção, exportação)
  - [ ] Certificado por terceiro independente
  - [ ] Vulnerabilidades críticas corrigidas
  - [ ] Vulnerabilidades altas: mitigação implementada

- [ ] Auditoria de segurança de código
  - [ ] Análise SAST (Static Application Security Testing)
  - [ ] Não há vulnerabilidades críticas
  - [ ] Não há vulnerabilidades altas não mitigadas

- [ ] Teste de vazamento de dados
  - [ ] Validar que PII não está em logs
  - [ ] Validar que PII não está em responses
  - [ ] Validar que PII não está em cache

**Responsável:** Security/QA  
**Bloqueador:** ✅ SIM — Segurança é obrigatória

---

## 5. Teste de Conformidade LGPD

### 5.1 Smoke Test — Fluxos LGPD

**Login e Consentimento:**
- [ ] Usuário pode fazer login
- [ ] Usuário pode ver Privacy Center
- [ ] Usuário pode aceitar consentimento biométrico
- [ ] Aceitação é registrada com timestamp

**Revogação:**
- [ ] Usuário pode revogar consentimento
- [ ] Revogação é registrada
- [ ] Dados biométricos são deletados de sistemas operacionais
- [ ] Evidência é minimizada (não deletada)

**Exportação de Dados:**
- [ ] Usuário pode solicitar exportação
- [ ] Exportação contém todos os dados processados
- [ ] Exportação não contém PII de terceiros
- [ ] Exportação é em formato estruturado (JSON/CSV)
- [ ] Exportação é entregue em prazo (30 dias)

**Retenção Técnica:**
- [ ] Dry-run de retenção retorna contagens corretas
- [ ] Apply de retenção executa sem erros
- [ ] Dados são minimizados (não deletados) conforme política
- [ ] Dados são retidos pelo prazo configurado

**Direitos do Titular:**
- [ ] Usuário pode acessar dados pessoais
- [ ] Usuário pode corrigir dados (se aplicável)
- [ ] Usuário pode solicitar exclusão (respeitando retenção)
- [ ] Usuário pode revogar consentimento
- [ ] Usuário pode exportar dados

**Responsável:** QA  
**Bloqueador:** ✅ SIM — Funcionalidade LGPD deve estar 100%

---

### 5.2 Teste de Resposta a Incidente

- [ ] Simulado: vazamento de dados de consentimento
  - [ ] Time é notificado em < 1 hora
  - [ ] Incidente é documentado
  - [ ] ANPD é notificada (conforme LGPD)
  - [ ] Titulares são notificados (se risco alto)
  - [ ] Causa raiz é identificada
  - [ ] Medidas corretivas são implementadas

**Responsável:** Security/Incident Response  
**Bloqueador:** ✅ SIM — Plano deve estar testado

---

## 6. Verificação Pré-Produção (Go/No-Go)

### GO Criteria (Tudo deve estar ✅)

- [ ] Parecer jurídico formal obtido
- [ ] Políticas públicas atualizadas
- [ ] DPA com terceiros assinado
- [ ] DPO designado e informado
- [ ] DPIA conduzido (se risco elevado, parecer ANPD obtido)
- [ ] Registro de Atividades preenchido
- [ ] Configuração de produção validada
- [ ] Teste de segurança passado
- [ ] Smoke test LGPD passado
- [ ] Plano de resposta a incidente testado
- [ ] Backup e rollback testados
- [ ] Monitoramento está ativo
- [ ] Logs estão centralizados
- [ ] Time de produção foi treinado
- [ ] Runbook de operação existe

### NO-GO Criteria (Qualquer um bloqueia)

- ❌ Parecer jurídico não está obtido
- ❌ Vulnerabilidades críticas de segurança não corrigidas
- ❌ PII está em logs ou responses
- ❌ HTTPS não está ativo
- ❌ DPA não está assinado
- ❌ Smoke test LGPD falhou

---

## 7. Rollback Plan

### Preparação

- [ ] Backup de produção é feito antes do deploy
- [ ] Backup foi testado (restore funciona)
- [ ] Versão anterior está pronta para rollback
- [ ] Database migration pode ser revertida

### Execução (se necessário)

- [ ] Rollback é executado em < 1 hora
- [ ] Dados retornam ao estado anterior
- [ ] Verificação: Sistem está operacional
- [ ] Comunicação: Usuários são notificados
- [ ] Root cause analysis é conduzido

**Responsável:** DevOps  
**Bloqueador:** ✅ SIM — Teste antes de produção

---

## 8. Comunicação e Treinamento

### 8.1 Time Interno

- [ ] DevOps: Treinado em deploy, rollback, monitoramento
- [ ] Support: Treinado em procedimentos LGPD
- [ ] Security: Escalação de incidentes definida
- [ ] Compliance: Ponto de contato designado

### 8.2 Usuários Finais

- [ ] Documentação de Privacy Center está disponível
- [ ] FAQ sobre consentimento está disponível
- [ ] Processo de revogação é claro
- [ ] Contato de suporte/DPO está comunicado

### 8.3 Stakeholders

- [ ] Executivos foram informados de status
- [ ] Parecer jurídico foi aprovado internamente
- [ ] Plano de produção foi aprovado

**Responsável:** Product/Marketing/Legal  
**Bloqueador:** ⚠️ RECOMENDADO

---

## 9. Monitoramento Pós-Produção (Primeiro Mês)

- [ ] Daily: Verificar logs de erro
- [ ] Daily: Verificar alerts de segurança
- [ ] Weekly: Revisar métricas de conformidade
- [ ] Weekly: Validar que retenção está funcionando
- [ ] Weekly: Validar que revogação está funcionando
- [ ] Monthly: Revisar incidentes (zero esperado)
- [ ] Monthly: Relatório de conformidade

---

## 10. Sign-Off

| Papel | Nome | Data | Assinatura |
|---|---|---|---|
| CTO/Arquitetura | | | |
| Head of Security | | | |
| Head of Legal | | | |
| DPO | | | |
| Head of Compliance | | | |
| VP Operations | | | |

---

**Documento de:** Release Engineering  
**Última revisão:** 2026-05-25  
**Próxima revisão:** Após parecer jurídico  
**Status:** ⚠️ AWAITING LEGAL REVIEW

## Notas

Este checklist é baseado em práticas recomendadas de LGPD e boas práticas de segurança. Não substitui parecer jurídico formal ou avaliação de compliance profissional.

Para qualquer dúvida, contactar: legal@kronos.com ou dpo@kronos.com

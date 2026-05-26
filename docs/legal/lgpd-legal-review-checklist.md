# LGPD Legal Review Checklist — Base Legal de Evidência de Consentimento

**Data:** 2026-05-25  
**Versão:** 1.0  
**Status:** ⚠️ Pendente de Parecer Jurídico Formal

---

## 1. Escopo de Revisão Jurídica

Este documento enumera os pontos que **requerem parecer jurídico formal** antes de produção.

O sistema Kronos implementa tecnicamente:
- ✅ Retenção de evidência de consentimento (2555 dias / ~7 anos)
- ✅ Minimização de dados após período (remove PII, preserva evidência)
- ✅ Base legal declarada: `REGULAR_EXERCISE_OF_RIGHTS` (art. 7º, VII, LGPD)
- ✅ Sistema bloqueado por padrão (`allow-apply=false`)

**O que ainda não tem parecer jurídico:**

Todos os pontos abaixo requerem parecer jurídico formal antes de go-live em produção.

---

## 2. Base Legal de Evidência — Questões Críticas

### 2.1 Validação de REGULAR_EXERCISE_OF_RIGHTS

**Pergunta jurídica:**
> A preservação de evidência de consentimento por 2555 dias (~7 anos) é válida sob a base legal "exercício regular de direitos" (art. 7º, VII, LGPD)?

**Contexto técnico:**
- Consentimento biométrico é fornecido pelo titular
- Evidência serve para validar direitos em caso de litígio/reclamação
- Após revogação do consentimento, evidência é preservada (não deletada)
- Minimização remove PII, mantém metadados de comprovação

**Decisão técnica adotada:**
- ✅ Mudança de `CONSENT` → `REGULAR_EXERCISE_OF_RIGHTS` (P1-BE-002)
- ✅ Texto público não afirma conformidade definitiva
- ✅ Sistema inclui ressalva: "(sujeito a parecer jurídico)"

**Risco residual:** ⚠️ CRÍTICO  
Se base legal não for sustentável, sistema estaria fora de conformidade LGPD.

---

### 2.2 Prazo de Retenção (2555 dias)

**Pergunta jurídica:**
> O prazo de ~7 anos para preservação de evidência de consentimento é:
> - Adequado (suficiente para validação/litígio)?
> - Excessivo (viola princípio de limitação)?

**Contexto técnico:**
- 2555 dias = ~7 anos
- Lei de prescrição civil: até 10 anos
- Lei de prescrição trabalhista: até 5 anos
- AWS/Rekognition pode reter biometria por prazo similar

**Decisão técnica adotada:**
- ✅ Prazo reflete período de prescrição
- ✅ RetentionPolicyCatalog documenta: "validação jurídica formal necessária"
- ✅ Sistema bloqueado, requer validação manual

**Risco residual:** ⚠️ CRÍTICO  
Prazo pode ser considerado excessivo ou insuficiente.

---

### 2.3 Revogação vs Preservação

**Pergunta jurídica:**
> Quando titular revoga consentimento biométrico:
> - Qual informação pode ser preservada?
> - Por quanto tempo?
> - Qual transparência é devida?

**Contexto técnico:**
- AcceptTermsService registra LegalConsent com status `ACTIVE`
- Titular pode revogar via Privacy Center
- LegalConsentRetentionProcessor.executeApply() minimiza dados revogados
- Minimização: remove PII, mantém metadados (timestamps, hash, versão)

**Decisão técnica adotada:**
- ✅ Minimização (não deleção) preserva direitos de titulares
- ✅ Texto público explica: "podem ser preservados para fins de conformidade jurídica"
- ✅ Não promete exclusão absoluta

**Risco residual:** ⚠️ CRÍTICO  
Políticas de privacidade devem explicar que evidência persiste.

---

## 3. Conformidade de Comunicação ao Titular

### 3.1 Política de Privacidade

**Item a verificar:**
- [ ] Política explica que consentimento biométrico gera evidência permanente?
- [ ] Política menciona preservação por ~7 anos mesmo após revogação?
- [ ] Política cita exercício de direitos como base legal?
- [ ] Política menciona terceiros (AWS, Rekognition) com acesso?

**Status:** ⚠️ PENDENTE

---

### 3.2 Termo de Consentimento Biométrico

**Item a verificar:**
- [ ] Termo menciona que evidência será preservada?
- [ ] Termo descreve período de retenção?
- [ ] Termo menciona propósito (conformidade jurídica + validação de direitos)?
- [ ] Termo fornece mecanismo claro de revogação?

**Status:** ⚠️ PENDENTE

---

### 3.3 Aviso de Revogação

**Item a verificar:**
- [ ] Quando titular revoga, aviso explica que evidência persiste?
- [ ] Aviso descreve dados que serão minimizados vs preservados?
- [ ] Aviso indica prazo de retenção?

**Status:** ⚠️ PENDENTE

---

## 4. Terceiros e DPA

### 4.1 AWS e Armazenamento S3

**Item a verificar:**
- [ ] Contrato com AWS menciona LGPD?
- [ ] Contrato cobre dados de consentimento?
- [ ] AWS está vinculado por Data Processing Agreement (DPA)?
- [ ] DPA define período de retenção?

**Status:** ⚠️ PENDENTE

---

### 4.2 Amazon Rekognition

**Item a verificar:**
- [ ] Contrato de Rekognition menciona que dados não serão usados para treinamento?
- [ ] Contrato cobre consentimento biométrico?
- [ ] Existe DPA ou aditivo?
- [ ] Dados podem ser transferidos internacionalmente?

**Status:** ⚠️ PENDENTE

---

### 4.3 Transferência Internacional

**Item a verificar:**
- [ ] AWS/Rekognition estão em qual país?
- [ ] Se EUA: existe Standard Contractual Clauses (SCC)?
- [ ] Se Europa: existe adequação?
- [ ] Existe consentimento informado para transferência?

**Status:** ⚠️ PENDENTE

---

## 5. Direitos do Titular

### 5.1 Direito de Acesso (art. 18, LGPD)

**Item a verificar:**
- [ ] Titular pode acessar registros de consentimento/evidência?
- [ ] Formato de exportação é legível?
- [ ] Dados sensíveis (PII) estão mascarados na exportação?

**Status:** ⚠️ PENDENTE

---

### 5.2 Direito de Retificação (art. 19, LGPD)

**Item a verificar:**
- [ ] Há mecanismo para corrigir consentimentos registrados erroneamente?
- [ ] Evidência de consentimento pode ser alterada?

**Status:** ⚠️ PENDENTE

---

### 5.3 Direito de Exclusão (art. 17, LGPD)

**Item a verificar:**
- [ ] Titular pode solicitar exclusão de consentimento revogado?
- [ ] Qual período mínimo deve ser preservado?
- [ ] Exclusão é permitida ou preservação é obrigatória?

**Status:** ⚠️ PENDENTE

---

### 5.4 Direito de Portabilidade (art. 20, LGPD)

**Item a verificar:**
- [ ] Registros de consentimento podem ser exportados?
- [ ] Formato é estruturado e interoperável?

**Status:** ⚠️ PENDENTE

---

## 6. Governança de Dados Pessoais

### 6.1 Encarregado de Proteção de Dados (DPO)

**Item a verificar:**
- [ ] DPO foi designado?
- [ ] DPO foi informado sobre armazenamento de evidência?
- [ ] DPO revisou política de retenção?

**Status:** ⚠️ PENDENTE

---

### 6.2 Avaliação de Impacto à Proteção de Dados (DPIA)

**Item a verificar:**
- [ ] DPIA foi conduzido para biometria + consentimento?
- [ ] DPIA documenta riscos de retenção prolongada?
- [ ] Medidas de mitigação foram definidas?

**Status:** ⚠️ PENDENTE

---

### 6.3 Registro de Atividades de Tratamento

**Item a verificar:**
- [ ] Registro documenta finalidade da retenção?
- [ ] Registro documenta categorias de dados?
- [ ] Registro documenta período de retenção?
- [ ] Registro documenta destinatários (AWS, Rekognition)?

**Status:** ⚠️ PENDENTE

---

## 7. Conformidade Regulatória Específica

### 7.1 Compatibilidade com Lei Trabalhista

**Questão:** Evidência de consentimento à biometria pode ser usada em processos trabalhistas?

**Contexto:**
- Lei 12.965/2014 (Marco Civil) cobre internet, não biometria
- Lei 13.709/2018 (LGPD) é principal regulador
- CLT/Lei Trabalhista pode ter limites adicionais

**Status:** ⚠️ PENDENTE parecer

---

### 7.2 Compatibilidade com Lei de Proteção à Privacidade

**Questão:** Estados/municípios podem ter leis adicionais?

**Contexto:**
- LGPD é lei federal (prevalece)
- Alguns estados têm leis adicionais
- Verificar aplicabilidade regional

**Status:** ⚠️ PENDENTE

---

## 8. Resposta a Incidentes

### 8.1 Vazamento de Dados

**Item a verificar:**
- [ ] Plano de resposta cobre dados de consentimento?
- [ ] Notificação obrigatória cobre evidência de consentimento?
- [ ] Prazo de notificação é cumprido (LGPD: sem demora, máximo prazo razoável)?

**Status:** ⚠️ PENDENTE

---

### 8.2 Requisição de Autoridades

**Item a verificar:**
- [ ] Procedimento para responder requisições (LGPD art. 28)?
- [ ] Sistema pode fornecer evidência sob ordem judicial?

**Status:** ⚠️ PENDENTE

---

## 9. Auditoria e Testes

### 9.1 Auditoria Técnica

**Realizado:**
- ✅ Diagnóstico de bases legais (P1-BE-001)
- ✅ Ajuste de base legal para REGULAR_EXERCISE_OF_RIGHTS (P1-BE-002)
- ✅ Ajuste de texto público (P1-BE-003)

**Pendente:**
- [ ] Auditoria jurídica independente
- [ ] Teste de conformidade com LGPD
- [ ] Revisão por órgão regulador (ANPD)

**Status:** ⚠️ PENDENTE

---

### 9.2 Testes de Conformidade

**Pendente:**
- [ ] Teste de revogação: evidência é minimizada corretamente?
- [ ] Teste de exportação: PII está mascarado?
- [ ] Teste de retenção: dados são retidos pelo prazo correto?
- [ ] Teste de deleção: sistema não deleta antes do prazo?

**Status:** ⚠️ PENDENTE

---

## 10. Declaração de Status

### Status Técnico
- ✅ **IMPLEMENTAÇÃO:** Técnicamente funcional
- ✅ **TESTES:** 100% dos testes de integração passando
- ✅ **COMPILAÇÃO:** Sem erros
- ✅ **SEGURANÇA:** PII sanitizado em logs e responses
- ✅ **BLOQUEIO:** Sistema bloqueado por padrão (`allow-apply=false`)

### Status Jurídico
- ⚠️ **PARECER JURÍDICO:** ❌ NÃO FINALIZADO
- ⚠️ **POLÍTICAS PÚBLICAS:** ❌ NÃO ATUALIZADAS
- ⚠️ **DPA:** ❌ NÃO VALIDADO
- ⚠️ **DPIA:** ❌ NÃO CONDUZIDO

### Conclusão
**Aprovado para validação em staging, CONDICIONADO a parecer jurídico formal.**

Não deve ser liberado para produção até que:
1. Parecer jurídico confirme base legal
2. Políticas públicas sejam atualizadas
3. DPA com terceiros seja formalizado
4. DPIA seja conduzido

---

## 11. Próximos Passos

| Responsável | Ação | Prazo | Status |
|---|---|---|---|
| Jurídico | Parecer sobre base legal REGULAR_EXERCISE_OF_RIGHTS | ❌ PENDENTE | ⚠️ CRÍTICO |
| Jurídico | Parecer sobre prazo (2555 dias) | ❌ PENDENTE | ⚠️ CRÍTICO |
| Marketing/Legal | Atualizar Política de Privacidade | ❌ PENDENTE | ⚠️ CRÍTICO |
| Legal | Atualizar Termo de Consentimento Biométrico | ❌ PENDENTE | ⚠️ CRÍTICO |
| Compliance | Validar DPA com AWS | ❌ PENDENTE | ⚠️ CRÍTICO |
| DPO | Conduzir DPIA para biometria | ❌ PENDENTE | ⚠️ CRÍTICO |
| DPO | Atualizar Registro de Atividades | ❌ PENDENTE | ⚠️ CRÍTICO |
| DevOps | Validar configuração de produção | ❌ PENDENTE | ⚠️ CRÍTICO |
| QA | Smoke test em staging | ❌ PENDENTE | ⚠️ CRÍTICO |

---

**Documento de: Arquitetura Técnica**  
**Revisado por: (Aguardando parecer jurídico)**  
**Aprovado por: (Aguardando parecer jurídico)**

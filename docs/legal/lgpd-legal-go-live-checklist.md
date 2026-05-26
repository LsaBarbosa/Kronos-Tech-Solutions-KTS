# Checklist Jurídico Pré-Go-Live — LGPD

**Data:** 2026-05-25  
**Versão:** 1.0  
**Status:** 🔴 BLOQUEADOR — Validação jurídica obrigatória antes de produção

Este checklist consolida todos os requisitos legais e de conformidade LGPD que devem estar satisfeitos antes da liberação em produção.

---

## 1. Parecer Jurídico Formal

### 1.1 Parecer Jurídico Assinado — CRÍTICO

- [ ] **Parecer obtido e assinado**
  - **Responsável:** Head of Legal / Consultor Jurídico Externo
  - **Prazo estimado:** 2-4 semanas
  - **Bloqueador:** ✅ SIM — Sem parecer, sem produção
  
  **Conteúdo obrigatório do parecer:**
  
  - [ ] **Base Legal REGULAR_EXERCISE_OF_RIGHTS**
    - Parecer confirma que REGULAR_EXERCISE_OF_RIGHTS (art. 7º, VII LGPD) é válida para preservação de evidência de consentimento
    - Justificativa: Preservação necessária para validação de direitos do titular (e.g., em caso de disputa)
    - Ressalva jurídica mencionada no catálogo público (sujeito a parecer) é removida post-parecer
    - ❌ **Risco se não validado:** Implementação inteira pode ser considerada não-conformante

  - [ ] **Período de Retenção de 2555 dias (~7 anos)**
    - Parecer confirma que 7 anos é apropriado sob LGPD
    - Justificativa legal para retenção (ex: prazos estatutários, limitação de ações)
    - Ou: período é revisado e ajustado conforme parecer (e.g., 5 anos)
    - ❌ **Risco se não validado:** Retenção pode ser considerada excessiva (art. 15 LGPD)

  - [ ] **Transferência Internacional (AWS/Rekognition)**
    - Parecer confirma que transferência para US é permissível
    - Mecanismo de proteção: Standard Contractual Clauses (SCC) OU adequacy decision ANPD
    - Se SCC: parecer confirma que SCC ainda são válidas (pós-Schrems II)
    - Se adequacy: parecer confirma que ANPD emitiu ou que existe equivalência
    - ❌ **Risco se não validado:** Transferência pode violar art. 33 LGPD

  - [ ] **Minimização vs. Deleção Conforme LGPD**
    - Parecer confirma que minimização (não deleção) de dados é conforme art. 15 LGPD
    - Justificativa: evidência legal + prazos de retenção justificam preservação
    - ❌ **Risco se não validado:** Delever dados vs. minimizar = escolha crítica não-conformante

  - [ ] **Direitos do Titular Implementados**
    - Parecer valida que implementação permite:
      - Acesso (GET /lgpd/user/profile)
      - Exportação (POST /lgpd/user/data-export)
      - Revogação (DELETE /lgpd/biometric/consent)
      - Exclusão (respeitando retenção legal)
      - Portabilidade (data export em formato estruturado)
    - ❌ **Risco se não validado:** Direitos podem estar incompletos

  - [ ] **Parecer Menciona Data de Validade**
    - Parecer é válido por: 12 meses, 24 meses, ou data específica
    - Após prazo: parecer deve ser renovado
    - ❌ **Risco:** Parecer "velho" pode não cobrir mudanças futuras

  - [ ] **Parecer é Assinado por Advogado Qualificado**
    - Advogado/Consultor tem: experiência em LGPD, registro válido, seguros profissionais
    - Parecer menciona: responsabilidades legais, limitações, datas

---

### 1.2 Parecer Cobre Jurisdições (Se Aplicável)

- [ ] **GDPR (Regulamento UE)** — Se usuários na UE
  - Parecer confirma que SCC ou adequacy decision cobre Brasil → UE
  - Parecer confirma conformidade com GDPR Chapter III (direitos dos titulares)
  - Parecer menciona: Data Protection Officer obrigatório (sim/não)
  - ❌ **Risco:** GDPR penalties são até 4% de revenue global

- [ ] **CCPA (California, USA)**
  - Parecer confirma conformidade com CCPA (se aplicável)
  - Parece menciona: direito de opt-out, direito de deleção, direito de portabilidade
  - ❌ **Risco:** CCPA penalties até $7,500 por violação

- [ ] **PDPA (Singapura)** — Se usuários em SG
  - Parecer confirma conformidade com PDPA
  - ❌ **Risco:** PDPA penalties até SGD 1 milhão

- [ ] **Legislação Local** — Se aplicável (Argentina, México, Chile, etc.)
  - Parecer menciona conformidade com legislação local
  - ❌ **Risco:** Múltiplas jurisdições podem ter múltiplas exigências

---

## 2. Documentação Pública (Política de Privacidade & Termos)

### 2.1 Política de Privacidade Atualizada — CRÍTICO

- [ ] **Política publicada no site**
  - **Responsável:** Legal + Marketing
  - **Local:** https://kronos.local/privacy (ou similar)
  - **Versão:** Com data de última atualização
  - **Assinatura:** Assinada por Head of Legal ou DPO
  
  **Seções obrigatórias:**

  - [ ] **1. Introdução & Controlador de Dados**
    - Identificação: "Kronos Tech Solutions Ltda."
    - CNPJ e endereço registrado
    - Contato: legal@kronos.com / dpo@kronos.com
    - Escopo: "Esta política cobre o processamento de dados pessoais na plataforma Kronos..."

  - [ ] **2. Dados Pessoais Coletados**
    - [ ] Categoria: Dados de Identificação (nome, email, CPF)
    - [ ] Categoria: Dados Biométricos (facial, templates, imagens)
    - [ ] Categoria: Dados de Navegação (IP, cookies, eventos)
    - [ ] Categoria: Dados de Comunicação (logs, suporte)
    - Para cada categoria: propósito, base legal, período de retenção

  - [ ] **3. Consentimento Biométrico** — SEÇÃO CRÍTICA
    - "Coleta e armazenamento de sua imagem facial para fins de autenticação"
    - "Você deve aceitar um termo de consentimento biométrico separado"
    - "Consentimento pode ser revogado a qualquer momento via Privacy Center"
    - "Após revogação, sua biometria será deletada de sistemas operacionais"
    - ✅ **Aviso obrigatório:** "Registro de consentimento pode ser preservado para fins de conformidade jurídica" — sem promessa de deleção

  - [ ] **4. Preservação de Evidência** — SEÇÃO CRÍTICA
    - "Registros dos seus consentimentos podem ser preservados conforme lei"
    - "Período de preservação: até 7 anos (~2555 dias)"
    - "Justificativa: validação de direitos do titular e conformidade jurídica"
    - "Isso ocorre mesmo quando você revoga um consentimento"
    - "Dados são minimizados (PII removida), apenas timestamps e base legal preservados"

  - [ ] **5. Geolocalização** — Se aplicável
    - Se não coleta: "Não processamos dados de geolocalização"
    - Se coleta: "Coleta seu IP para fins de segurança"
      - Propósito: validação de acesso anômalo, prevenção de fraude
      - Base legal: LEGITIMATE_INTEREST ou LEGAL_OBLIGATION
      - Período de retenção: 90 dias
      - Compartilhamento: AWS CloudWatch

  - [ ] **6. Compartilhamento com Terceiros** — SEÇÃO CRÍTICA
    - [ ] **AWS S3 & CloudTrail**
      - "Armazenamos seus dados em servidores da Amazon Web Services"
      - "AWS processa conforme Data Processing Agreement assinado"
      - "Localização: Brasil (São Paulo) ou exterior conforme necessário"
      - "Dados: biometria, consentimentos, logs de acesso"
    
    - [ ] **Amazon Rekognition**
      - "Usamos Amazon Rekognition para processar sua imagem facial"
      - "Rekognition NÃO usa seus dados para treinamento de modelos"
      - "Dados são processados com criptografia em trânsito e em repouso"
    
    - [ ] **Outros Terceiros**
      - "Não compartilhamos dados com outros terceiros"
      - Ou: "Compartilhamos com: [lista específica com justificativa]"

  - [ ] **7. Transferência Internacional** — SEÇÃO CRÍTICO
    - "Seus dados podem ser transferidos para fora do Brasil"
    - "Mecanismo: Standard Contractual Clauses (SCC) assinadas com AWS"
    - "Você pode solicitar informações sobre medidas de proteção em: dpo@kronos.com"
    - ❌ **Risco:** Sem mencionar transferência = violação de transparência

  - [ ] **8. Retenção de Dados**
    - Tabela com: Categoria de Dado | Base Legal | Período de Retenção
    - Exemplo:
      ```
      Biometria Facial         | CONSENT + REGULAR_EXERCISE_OF_RIGHTS | Deletar após revogação, manter evidência 2555 dias
      Consentimento            | LEGAL_OBLIGATION                     | 2555 dias (~7 anos)
      Logs de Acesso           | LEGITIMATE_INTEREST                  | 90 dias
      Email                    | CONSENT                              | Deletar após solicitação de exclusão
      ```

  - [ ] **9. Segurança de Dados**
    - "Implementamos medidas técnicas e organizacionais:"
    - Criptografia em trânsito (TLS 1.2+)
    - Criptografia em repouso (AES-256)
    - Controle de acesso (autenticação, autorização)
    - Auditoria de acessos
    - Plano de resposta a incidentes

  - [ ] **10. Seus Direitos** — SEÇÃO CRÍTICA
    - [ ] **Direito de Acesso**
      - "Você pode solicitar acesso a seus dados pessoais"
      - "Como: enviar email para dpo@kronos.com com solicitação"
      - "Prazo: 30 dias para resposta"
      - "Format: JSON ou CSV"
    
    - [ ] **Direito de Portabilidade**
      - "Você pode exportar seus dados em formato estruturado"
      - "Como: Privacy Center → Exportar Dados"
      - "Prazo: 30 dias para conclusão"
    
    - [ ] **Direito de Retificação**
      - "Você pode corrigir dados incorretos"
      - "Como: Atualizar perfil ou enviar email para dpo@kronos.com"
    
    - [ ] **Direito de Exclusão**
      - "Você pode solicitar a exclusão de seus dados"
      - "Limitações: Alguns dados podem ser retidos por obrigação legal (ex: biometria por 7 anos)"
      - "Como: Privacy Center ou email para dpo@kronos.com"
    
    - [ ] **Direito de Revogação de Consentimento**
      - "Você pode revogar seu consentimento biométrico a qualquer momento"
      - "Como: Privacy Center → Revogar Consentimento"
      - "Efeito: Biometria será deletada (evidência preservada)"
    
    - [ ] **Direito de Reclamação à ANPD**
      - "Se considera que seus direitos foram violados:"
      - "Contato ANPD: https://www.gov.br/cidadania/pt-br/acesso-a-informacao/lgpd"
      - "Email: lgpd@anpd.gov.br"

  - [ ] **11. Contato & Responsável**
    - "Dúvidas sobre privacidade? Entre em contato:"
    - **Email:** dpo@kronos.com
    - **Responsável:** [Nome e cargo do DPO]
    - **Endereço:** [Endereço da empresa]
    - **Telefone:** [Telefone]
    - "Resposta em até 5 dias úteis"

  - [ ] **12. Histórico de Revisões**
    - Tabela com: Data | Versão | Mudanças
    - Última revisão: 2026-05-25
    - Próxima revisão: 2026-08-25 (ou conforme necessário)

---

### 2.2 Termo de Consentimento Biométrico — CRÍTICO

- [ ] **Documento publicado e assinado eletronicamente**
  - **Responsável:** Legal
  - **Local:** Privacy Center ou durante onboarding
  - **Versão:** Com data e hash SHA256
  
  **Conteúdo obrigatório:**

  - [ ] **Preambulo Claro**
    - "TERMO DE CONSENTIMENTO PARA PROCESSAMENTO BIOMÉTRICO FACIAL"
    - "Leitura obrigatória antes de aceitar"
    - Data/hora de aceitação registrada

  - [ ] **O que é Coletado**
    - "Sua imagem facial capturada via câmera"
    - "Template biométrico gerado pelo algoritmo de reconhecimento facial"
    - "Dados são processados por Amazon Rekognition"

  - [ ] **Para Qual Propósito**
    - "Autenticação: fazer login via facial recognition"
    - "Validação de identidade: confirmar que é você em certas operações"
    - "Prevenção de fraude: detectar acesso não-autorizado"

  - [ ] **Quem Processa**
    - "Kronos Tech Solutions (controlador)"
    - "Amazon Web Services (processador)"
    - "Amazon Rekognition (processador)"

  - [ ] **Período de Armazenamento** — CRÍTICO
    - "Sua imagem facial será armazenada enquanto seu consentimento está ativo"
    - "Após revogação do consentimento:"
      - "Sua imagem será deletada de sistemas operacionais em até 30 dias"
      - "Registro de que você consentiu será preservado por até 7 anos para fins jurídicos"
      - "Você pode verificar status em Privacy Center"

  - [ ] **Preservação de Evidência** — CRÍTICO
    - "Seus registros de consentimento podem ser preservados:"
      - "Razão: conformidade jurídica e validação de direitos (art. 7º, VII LGPD)"
      - "Dados minimizados: apenas timestamps, decisão jurídica, sem biometria"
      - "Período: até 7 anos conforme análise jurídica"
    - "Mesmo se você revogar o consentimento, esse registro pode ser preservado"

  - [ ] **Compartilhamento com Terceiros**
    - "Dados compartilhados com:"
      - "✅ Amazon Web Services (armazenamento em S3)"
      - "✅ Amazon Rekognition (processamento facial)"
      - "❌ Nenhum outro terceiro sem consentimento adicional"

  - [ ] **Seus Direitos**
    - "Você pode:"
      - "✅ Revogar este consentimento a qualquer momento (Privacy Center)"
      - "✅ Solicitar acesso a seus dados (dpo@kronos.com)"
      - "✅ Solicitar correção de dados incorretos"
      - "✅ Solicitar exclusão (sujeito a prazos legais)"
      - "❌ Não pode: ignorar a revogação depois de confirmar"

  - [ ] **Confirmação Explícita**
    - "Li e entendi este termo"
    - "[ ] Aceito o processamento de minha biometria conforme descrito"
    - Checkbox obrigatório (não pré-selecionado)

  - [ ] **Versão & Hash**
    - "Versão: 1.0 (2026-05-25)"
    - "SHA256: [hash do documento para prova de integridade]"
    - "Ao aceitar, você confirma que leu e concordou com esta versão específica"

  - [ ] **Assinatura Eletrônica**
    - Timestamp automático de quando foi aceito
    - IP do usuário registrado (para auditoria)
    - Termo pode ser revogado clicando em "Revogar Consentimento"

---

### 2.3 Termos de Uso Atualizados — RECOMENDADO

- [ ] **Termos de Uso menciona LGPD**
  - **Responsável:** Legal + Product
  - Local: https://kronos.local/terms (ou similar)
  
  **Seções aplicáveis:**
  - [ ] "Você concorda que seus dados pessoais são processados conforme Lei Geral de Proteção de Dados (LGPD)"
  - [ ] Link para Política de Privacidade
  - [ ] Link para Termo de Consentimento Biométrico
  - [ ] "Direitos do titular podem ser exercidos em Privacy Center"
  - [ ] "Kronos pode modificar estes termos com notificação prévia"

---

## 3. Conformidade com Processadores de Dados (DPA)

### 3.1 Data Processing Agreement com AWS — CRÍTICO

- [ ] **DPA assinado e vigente**
  - **Responsável:** Procurement + Legal
  - **Status:** ⏳ PENDENTE
  - **Bloqueador:** ✅ SIM — Sem DPA, sem produção
  - **Prazo:** 2-4 semanas para negs + assinatura
  
  **DPA deve incluir:**

  - [ ] **Identificação das Partes**
    - Kronos = Controller
    - AWS = Processor
    - Ambos identificados por CNPJ, endereço, contatos

  - [ ] **Escopo: Dados Pessoais Processados**
    - "Dados biométricos: imagens faciais, templates de reconhecimento"
    - "Dados de consentimento: timestamps, decisões, revogações"
    - "Dados de auditoria: logs de acesso, mudanças"
    - "Localização de armazenamento: S3 Brasil (São Paulo) ou conforme solicitado"

  - [ ] **Duração do Acordo**
    - Período: Enquanto Kronos usar AWS para dados LGPD
    - Prazo de término: [Data específica ou "until terminated"]
    - Notificação de término: 30 dias com aviso prévio

  - [ ] **Conformidade com LGPD**
    - AWS confirma que processamento está conforme LGPD
    - AWS seguirá instruções de Kronos quanto ao processamento
    - AWS notificará Kronos se receber solicitação de titular ou ANPD

  - [ ] **Sub-processadores**
    - Lista de sub-processadores autorizados (ex: CloudTrail, CloudWatch)
    - Direito de Kronos revisar/objetar novos sub-processadores

  - [ ] **Transferência Internacional**
    - Cláusula: "AWS processará dados conforme SCC (Standard Contractual Clauses)"
    - Ou: "AWS confirmou adequacy decision da ANPD"
    - Ou: "AWS utilizará proteções adicionais conforme Schrems II"

  - [ ] **Direitos do Titular**
    - AWS garantirá que Kronos possa atender a solicitações de:
      - Acesso (art. 18 LGPD)
      - Correção (art. 19 LGPD)
      - Exclusão (art. 17 LGPD)
      - Portabilidade (art. 20 LGPD)
    - AWS fornecerá dados necessários em prazo razoável

  - [ ] **Segurança & Confidencialidade**
    - Criptografia em trânsito: TLS 1.2+
    - Criptografia em repouso: AES-256 ou KMS
    - Controle de acesso: apenas pessoal autorizado
    - Auditoria: logs de acesso mantidos por AWS

  - [ ] **Direito de Auditoria**
    - Kronos pode auditar conformidade de AWS conforme LGPD
    - Ou: AWS fornecerá certificados de auditoria (SOC 2, ISO 27001)
    - Frequência: anual ou conforme necessário

  - [ ] **Notificação de Breach**
    - AWS notificará Kronos de incidentes em até 24-72h
    - Kronos será responsável por notificar ANPD e titulares

  - [ ] **Deleção de Dados ao Término**
    - Ao encerrar contrato:
      - AWS deletará ou retornará dados conforme instrução
      - Prazo: 30-90 dias após término
      - Cópia de confirmação de deleção fornecida

  - [ ] **Lei Aplicável & Jurisdição**
    - Lei: Lei brasileira (LGPD) + Lei do Estado (ex: SP)
    - Jurisdição: Tribunais brasileiros
    - Ou: Arbitragem conforme cláusulas específicas

  - [ ] **Assinatura & Data de Vigência**
    - Assinado por: representantes autorizados de ambas as partes
    - Data de entrada em vigor: [Data específica]
    - Data de renovação: [Data em que será revisto]

---

### 3.2 Conformidade com Amazon Rekognition — CRÍTICO

- [ ] **Contrato ou Termos de Serviço Vigentes**
  - **Responsável:** Procurement + Legal
  - **Status:** ⏳ PENDENTE
  - **Bloqueador:** ✅ SIM — Sem contrato, sem produção
  
  **Pontos críticos:**

  - [ ] **Confirma: "Dados NÃO serão usados para treinamento"**
    - AWS confirma que imagens faciais não alimentarão novos modelos
    - AWS confirma que templates não serão compartilhados
    - ❌ **Risco crítico:** Se AWS usa dados para treinamento = violação grave LGPD

  - [ ] **Conformidade com LGPD**
    - AWS concorda que processamento atende LGPD
    - AWS respeita direitos do titular

  - [ ] **Segurança**
    - Criptografia em trânsito
    - Criptografia em repouso
    - Controle de acesso
    - Auditoria

  - [ ] **Período de Retenção**
    - Confirmação: Dados são deletados conforme [período]
    - Ou: Dados são mantidos apenas enquanto necessário

  - [ ] **Transferência Internacional**
    - Mecanismo: SCC OU Adequacy Decision ANPD
    - ❌ **Risco:** Sem mecanismo = transferência ilegal

---

### 3.3 Auditoria de Conformidade com Terceiros

- [ ] **Lista de Todos os Processadores**
  - [ ] AWS S3 (armazenamento)
  - [ ] AWS CloudTrail (auditoria)
  - [ ] AWS CloudWatch (logs)
  - [ ] Amazon Rekognition (processamento facial)
  - [ ] [Outros se aplicável]
  
  Para cada um:
  - [ ] DPA ou Termos de Serviço assinados
  - [ ] Conformidade com LGPD confirmada
  - [ ] Transferência internacional mecanismo claro

---

## 4. Bases Legais & Consentimento

### 4.1 Bases Legais Documentadas

- [ ] **CONSENT (Consentimento)**
  - Aplicável a: Biometria facial
  - Documentado em: Termo de Consentimento Biométrico
  - Revogável: Sim, via Privacy Center
  - ✅ Implementado

- [ ] **REGULAR_EXERCISE_OF_RIGHTS (Art. 7º, VII)**
  - Aplicável a: Preservação de evidência de consentimento
  - Duração: 2555 dias (~7 anos)
  - Justificativa: Validação de direitos do titular em caso de disputa
  - ✅ Implementado
  - ⚠️ **Aguardando validação jurídica formal**

- [ ] **LEGAL_OBLIGATION (Obrigação Legal)**
  - Aplicável a: [Se houver, listar]
  - Lei: [Lei específica]
  - Duração: [Período conforme lei]

- [ ] **LEGITIMATE_INTEREST (Interesse Legítimo)**
  - Aplicável a: [Se houver, listar]
  - Interesse: [Descrever interesse]
  - Balanceamento: [Justificar por que interesse > direitos do titular]

- [ ] **Teste de Balanceamento (Legitimate Interest)**
  - Se usando LEGITIMATE_INTEREST:
    - [ ] Necessidade comprovada
    - [ ] Propósito legítimo
    - [ ] Balanceamento com direitos do titular
    - [ ] Safeguards implementados
  - ❌ **Risco:** LGPD interpreta LEGITIMATE_INTEREST restritivamente

---

### 4.2 Documentação de Decisões

- [ ] **Decision Log: Por que cada base legal foi escolhida**
  
  Exemplo:
  ```
  DECISION LOG — Bases Legais
  
  Processamento: Biometria Facial
  Base Legal: CONSENT
  Justificativa: Usuário deve dar consentimento explícito para usar facial recognition
  Revogável: Sim (Privacy Center)
  Riscos: Nenhum com consentimento explícito
  Data: 2026-05-25
  ```

---

## 5. Biometria

### 5.1 Consentimento Biométrico — CRÍTICO

- [ ] **Termo de Consentimento específico para biometria**
  - ✅ Criado (seção 2.2 acima)
  - ✅ Publicado em Privacy Center
  - [ ] Aceito por 100% dos usuários que usam facial recognition

- [ ] **Informação Prévia Clara**
  - Antes de solicitar biometria, usuário vê:
    - O que é coletado (facial image)
    - Por que (authentication)
    - Como é processado (AWS Rekognition)
    - Quanto tempo é retido (até revogação)
    - Direito de revogar (link para Privacy Center)

- [ ] **Revogação Funcional**
  - [ ] Usuário consegue revogar a qualquer momento
  - [ ] Biometria é deletada de S3 em até 30 dias
  - [ ] Sistema verifica que revogação foi aplicada
  - [ ] Audit log registra revogação

---

### 5.2 Segurança de Dados Biométricos

- [ ] **Armazenamento Seguro**
  - [ ] S3 bucket é privado (não público)
  - [ ] Encryption: AES-256 ou AWS KMS
  - [ ] Versionamento ativo (poder reverter se necessário)
  - [ ] MFA delete ativo (previne deleção acidental)
  - [ ] ACL: apenas aplicação consegue ler

- [ ] **Controle de Acesso**
  - [ ] Apenas usuário autenticado consegue fazer upload
  - [ ] Apenas sistema consegue fazer comparação via Rekognition
  - [ ] Nenhum acesso manual a templates biométricos

- [ ] **Logs de Acesso**
  - [ ] CloudTrail registra todos os acessos
  - [ ] Logs não expõem template biométrico
  - [ ] Auditoria: verificar logs mensalmente

---

### 5.3 Precisão & Fairness

- [ ] **Testes de Precisão**
  - FPR (False Positive Rate) <= [threshold]
  - FNR (False Negative Rate) <= [threshold]
  - Teste deve ser documentado

- [ ] **Testes de Bias**
  - Diferentes demográficos testados
  - Performance consistente entre grupos
  - ❌ **Risco:** Algorithms podem ter bias contra certos grupos étnicos

- [ ] **Direito de Contestação**
  - Se sistema rejeita biometria de usuário:
    - Usuário pode contestar
    - Processo manual de fallback disponível
    - Contato: dpo@kronos.com

---

## 6. Geolocalização (Se Aplicável)

### 6.1 Se NÃO Coleta Geolocalização

- [ ] **Documentado: Kronos NÃO coleta dados de localização**
  - Política de Privacidade menciona: "Não coletamos dados de geolocalização"
  - Aplicação: Permissão de localização não é solicitada

### 6.2 Se Coleta Geolocalização

- [ ] **Consentimento Específico**
  - Apenas se usuário consente explicitamente
  - Permissão do sistema operacional é requisitada

- [ ] **Transparência**
  - Política menciona propósito (ex: segurança)
  - Período de retenção (ex: 90 dias)
  - Quem acessa (ex: apenas backend para validação)

- [ ] **Minimização**
  - IP é armazenado, não localização GPS precisa
  - Localização é deletada após período

---

## 7. Retenção de Dados

### 7.1 Política de Retenção Documentada — CRÍTICO

- [ ] **Tabela de Retenção Publicada**
  - **Responsável:** DPO + Legal
  - **Local:** docs/legal/retention-policy.md ou similar
  
  **Formato:**
  ```
  Categoria de Dado | Base Legal | Propósito | Período de Retenção | Como Deletado
  ---|---|---|---|---
  Biometria Facial | CONSENT | Auth | Até revogação, mín 30d; evidência 2555d | S3 delete + minimização
  Consentimento | LEGAL_OBLIGATION | Compliance | 2555 dias (~7 anos) | Minimização (sem PII)
  Logs de Acesso | LEGITIMATE_INTEREST | Segurança | 90 dias | Deleção automática
  Email | CONSENT | Comunicação | Até exclusão solicitada | Deleção manual/automática
  ```

- [ ] **Política é Revisada Anualmente**
  - Data da última revisão: 2026-05-25
  - Próxima revisão: 2026-05-26 (1 ano depois)
  - Revisor: DPO

---

### 7.2 Implementação de Retenção Automática

- [ ] **Processador de Retenção Está Ativo**
  - LegalConsentRetentionProcessor executa conforme agendamento
  - Testes de retenção passam (59 testes integração)
  - Dados são minimizados, não deletados

- [ ] **Monitoramento**
  - [ ] Retenção é executada conforme schedule (ex: mensalmente)
  - [ ] Logs registram: quantos registros foram minimizados
  - [ ] Alertas se retenção falha

---

## 8. Data Protection Officer (DPO)

### 8.1 DPO Designado — CRÍTICO

- [ ] **DPO Foi Designado Formalmente**
  - **Responsável:** Head of Legal / RH
  - **Status:** ⏳ PENDENTE
  - **Bloqueador:** ✅ SIM — DPO é obrigatório se processamento "grande escala" ou sensível
  
  **DPO pode ser:**
  - [ ] Funcionário interno (full-time ou part-time)
  - [ ] Consultoria terceirizada
  - [ ] Combinação (um principal + consultores)

- [ ] **Qualificações do DPO**
  - [ ] Conhecimento de LGPD (certificação ou treinamento)
  - [ ] Experiência em privacidade de dados
  - [ ] Independência (não subordinado a diretos responsáveis por processamento)
  - [ ] Contato: dpo@kronos.com

- [ ] **Responsabilidades Documentadas**
  - [ ] Informar Kronos sobre obrigações LGPD
  - [ ] Monitorar conformidade
  - [ ] Cooperar com ANPD
  - [ ] Ser ponto de contato para titulares

- [ ] **DPO Tem Acesso Necessário**
  - [ ] Acesso a documentação de processamento
  - [ ] Acesso a logs de dados
  - [ ] Acesso a políticas de segurança
  - [ ] Relatórios diretos ao Head of Legal / CEO

---

### 8.2 DPO Notificado sobre LGPD Implementation

- [ ] **DPO Reviu:**
  - [ ] Implementação técnica (endpoints, consentimento, revogação)
  - [ ] Política de Privacidade
  - [ ] Termo de Consentimento Biométrico
  - [ ] Políticas de retenção
  - [ ] DPA com processadores

- [ ] **DPO Concordou:**
  - [ ] "Implementação está conforme LGPD nos seguintes pontos: ..."
  - [ ] "Riscos residuais são: ..."
  - [ ] "Próximas etapas: ..."

- [ ] **DPO Preparado para Incidentes**
  - [ ] DPO conhece procedimento de resposta a breach
  - [ ] DPO sabe quando notificar ANPD (dentro de 72h)
  - [ ] DPO sabe quando notificar titulares

---

## 9. Canal de Comunicação com Titulares

### 9.1 Canal de Atendimento — OBRIGATÓRIO

- [ ] **Email de Contato Publicado**
  - **Email:** dpo@kronos.com (ou similar)
  - **Local:** Publicado em Política de Privacidade, Termos, Privacy Center
  - **Responsável:** DPO (ou designado)
  - **Prazo de Resposta:** 5 dias úteis

- [ ] **Formulário de Solicitação de Direitos** (Opcional, recomendado)
  - [ ] Privacy Center tem formulário para:
    - Solicitar acesso (art. 18)
    - Solicitar portabilidade (art. 20)
    - Solicitar exclusão (art. 17)
    - Solicitar revogação de consentimento
  - [ ] Formulário é salvo e documentado
  - [ ] Kronos responde em 30 dias (ou notifica atraso)

- [ ] **Canal de Reclamação**
  - [ ] Titulares podem reclamar sobre violação de direitos
  - [ ] Reclamação é documentada e investigada
  - [ ] Resposta em 30 dias

---

### 9.2 Procedimento de Atendimento Documentado

- [ ] **SOP: Como Responder a Solicitações de Direitos**
  
  Exemplo:
  ```
  1. Solicitação recebida via dpo@kronos.com
  2. Verificar autenticação (solicitar CPF/email)
  3. Identificar tipo: acesso / portabilidade / exclusão / revogação
  4. Executar ação:
     - Acesso: preparar data export (30 dias)
     - Portabilidade: preparar JSON/CSV estruturado
     - Exclusão: deletar/minimizar conforme política de retenção
     - Revogação: marcar como revoked, executar retention
  5. Notificar titular: "Solicitação processada"
  6. Documentar em log de direitos do titular
  ```

---

## 10. Terceiros & Subcontratados

### 10.1 Lista de Todos os Processadores

- [ ] **Mapa de Fluxo de Dados Documentado**
  
  ```
  Usuário → Kronos Backend (Controller)
           ↓
           ├→ AWS S3 (Processor) → Armazenamento de biometria
           ├→ AWS CloudTrail (Processor) → Auditoria
           ├→ AWS CloudWatch (Processor) → Logs
           └→ Amazon Rekognition (Processor) → Processamento facial
  ```

- [ ] **Para Cada Processador:**
  - [ ] Nome e CNPJ (se aplicável)
  - [ ] Dados que processa
  - [ ] Localização do armazenamento
  - [ ] Tipo de contrato (DPA, Termos de Serviço)
  - [ ] Conformidade com LGPD confirmada
  - [ ] Subcontratados conhecidos

---

### 10.2 Sub-processadores

- [ ] **Se AWS usa sub-processadores (ex: CloudFlare para CDN):**
  - [ ] Lista de sub-processadores conhecidos
  - [ ] Kronos tem direito de objetar a novos sub-processadores
  - [ ] Notificação de mudanças é fornecida

---

### 10.3 Comunicação com Processadores

- [ ] **Procesadores foram notificados sobre LGPD**
  - [ ] AWS S3: confirmou conformidade
  - [ ] Amazon Rekognition: confirmou que NÃO usa dados para treinamento
  - [ ] [Outros]

- [ ] **Acordos de Confiabilidade**
  - [ ] Processadores concordam em executar instruções de Kronos
  - [ ] Processadores concordam em não compartilhar dados com terceiros
  - [ ] Processadores concordam em cooperar com ANPD

---

## 11. Transferência Internacional

### 11.1 Mecanismo de Proteção Ativo — CRÍTICO

- [ ] **Transferência para USA (AWS)**
  - **Status:** ⏳ PENDENTE VALIDAÇÃO
  - **Mecanismo:** Standard Contractual Clauses (SCC) OU Adequacy Decision
  
  **Opção A: SCC (Padrão Contratual Padrão)**
  - [ ] SCC foram assinadas entre Kronos e AWS
  - [ ] Cláusulas cobrem transferência de dados da UE/Brasil para USA
  - [ ] Pós-Schrems II: SCCs ainda são válidas conforme jurisprudência recente
  - [ ] Documento: Manter cópia de SCC assinadas

  **Opção B: Adequacy Decision**
  - [ ] ANPD emitiu adequacy decision para Brasil → [País]
  - [ ] Documento: Copiar decisão da ANPD

  **Opção C: Proteções Adicionais**
  - [ ] Se SCC + adequacy decision não suficientes:
    - [ ] Encryption de ponta a ponta (Kronos retém chaves)
    - [ ] Dados minimizados antes de transferência
    - [ ] Kronos revisa dados transferidos regularmente

- [ ] **Transferência para Amazon Rekognition (USA)**
  - Mesmo mecanismo que acima (SCC ou adequacy)
  - Documentado em contrato com AWS

- [ ] **Transparência com Titulares**
  - [ ] Política de Privacidade menciona: "Seus dados podem ser transferidos para fora do Brasil"
  - [ ] Menciona mecanismo: "SCC assinadas com AWS"
  - [ ] Menciona direito: "Solicitar informações sobre proteções em dpo@kronos.com"

---

### 11.2 Avaliação de Risco de Transferência

- [ ] **DPIA para Transferência Internacional**
  - [ ] Avaliar riscos específicos do país destino (Brasil → USA)
  - [ ] Riscos: "Governo USA tem broad surveillance powers"
  - [ ] Mitigação: "Dados são minimizados, encrypted"
  - [ ] Conclusão: "Risco é aceitável com SCCs em vigor"

---

## 12. Resposta a Incidentes

### 12.1 Plano de Resposta a Incidentes — CRÍTICO

- [ ] **Procedimento de Breach Documentado**
  - **Responsável:** Head of Security + DPO
  - **Objetivo:** Responder em < 72h conforme art. 33 LGPD
  
  **Passos:**
  1. [ ] Detecção do incidente (alert via monitoring)
  2. [ ] Avaliação de gravidade (é breach de dados pessoais?)
  3. [ ] Notificação interna (CTO, Head of Security, DPO, Legal)
  4. [ ] Isolamento (parar o leak, se possível)
  5. [ ] Investigação (root cause analysis, scope)
  6. [ ] Notificação ANPD (se breach = risco aos direitos)
     - [ ] Dentro de 72h se risco
     - [ ] Sem atraso não justificado (art. 33, §1º)
     - [ ] Contém: descrição, consequências, medidas mitigação
  7. [ ] Notificação aos titulares (se risco alto)
     - [ ] Sem atraso, em linguagem clara
     - [ ] Contém mesmas informações que ANPD
  8. [ ] Documentação (manter registro de todo processo)
  9. [ ] Follow-up (verificar se causa foi corrigida)

- [ ] **Contatos de Escalação**
  - [ ] Head of Security: [telefone + email]
  - [ ] DPO: [telefone + email]
  - [ ] Head of Legal: [telefone + email]
  - [ ] Ponto de contato ANPD: [email oficial]

- [ ] **Definição de "Breach"**
  - [ ] Qualquer comprometimento confidentiality/integrity/availability de dados pessoais
  - [ ] Exemplos:
    - ✅ Hacker obtém biometric templates de 100 usuários
    - ✅ Erro de DevOps expõe logs com CPF
    - ✅ Funcionário acessa dados sem autorização
    - ❌ Usuário vê erro 500 na página (não é breach de dados)

---

### 12.2 Teste do Plano de Resposta

- [ ] **Simulado de Breach Realizado**
  - [ ] Data: [Data que foi testado]
  - [ ] Cenário: "Hacker conseguiu acesso a S3 bucket"
  - [ ] Resultado: Time respondeu em [X] horas
  - [ ] Pontos de melhoria: [Lista]

---

### 12.3 Notificação à ANPD

- [ ] **Modelo de Email para ANPD**
  
  ```
  Assunto: Notificação de Incidente de Segurança — Artigo 33, LGPD
  
  Prezados,
  
  Kronos Tech Solutions notifica este incidente de segurança:
  
  1. DATA DO INCIDENTE: [Data]
  2. DATA DA DESCOBERTA: [Data]
  3. DESCRIÇÃO: [Descrição técnica do que aconteceu]
  4. DADOS AFETADOS: [Quantos registros, quais dados pessoais]
  5. CATEGORIAS DE TITULARES: [Quantos usuários afetados]
  6. RISCO AOS DIREITOS: [Avaliação de risco]
  7. CAUSA RAIZ: [Root cause analysis]
  8. MEDIDAS JÁ ADOTADAS: [Mitigação imediata]
  9. MEDIDAS FUTURAS: [Prevenção de recorrência]
  10. PONTO DE CONTATO: [DPO name + email + phone]
  
  Atenciosamente,
  [Assinado por Head of Legal ou DPO]
  ```

- [ ] **Notificação aos Titulares**
  - [ ] Email template pronto
  - [ ] Linguagem clara (não jurídica)
  - [ ] Contém: O que aconteceu, risco, medidas, contato suporte

---

## 13. Pendências Jurídicas Consolidadas

### 🔴 CRÍTICO — Bloqueador de Produção

| Item | Status | Prazo | Responsável |
|------|--------|-------|-------------|
| Parecer Jurídico Formal | ⏳ NÃO INICIADO | 2-4 semanas | Head of Legal |
| DPA com AWS | ⏳ NÃO INICIADO | 2-4 semanas | Procurement + Legal |
| Contrato Rekognition | ⏳ NÃO INICIADO | 2-4 semanas | Procurement + Legal |
| Política de Privacidade Atualizada | ⏳ EM REDAÇÃO | 1-2 semanas | Legal + Marketing |
| Termo de Consentimento Biométrico | ⏳ EM REDAÇÃO | 1-2 semanas | Legal |
| DPO Designado | ⏳ NÃO INICIADO | 1 semana | Head of Legal + RH |

### 🟠 ALTO — Recomendado antes de Produção

| Item | Status | Prazo | Responsável |
|------|--------|-------|-------------|
| Termos de Uso Atualizados | ⏳ NÃO INICIADO | 1 semana | Legal + Product |
| DPIA Concluído | ⏳ EM ANDAMENTO | 1-2 semanas | DPO |
| Parecer ANPD (se DPIA indica risco elevado) | ⏳ PENDENTE | 3-4 semanas (após DPIA) | Legal + DPO |
| Plano de Resposta a Incidentes Testado | ⏳ PLANEJADO | 1-2 semanas | Security + DPO |

### 🟡 MÉDIO — Planejado para Próximas Semanas

| Item | Status | Prazo | Responsável |
|------|--------|-------|-------------|
| Auditoria Anual de Conformidade | ⏳ FUTURO | Post-go-live | DPO + Legal |
| Revisão de Política de Privacidade (anual) | ⏳ FUTURO | 1 ano após launch | Legal |

---

## 14. Matriz de Responsáveis

### Por Função

| Função | Nome | Email | Telefone | Responsabilidades |
|--------|------|-------|----------|------------------|
| Head of Legal | [Nome] | legal@kronos.com | [Tel] | Parecer jurídico, DPA, Política, assinatura |
| DPO | [Nome] | dpo@kronos.com | [Tel] | DPIA, conformidade, contato com ANPD/titulares |
| Procurement | [Nome] | procurement@kronos.com | [Tel] | Negociar DPA com AWS, contrato Rekognition |
| Head of Security | [Nome] | security@kronos.com | [Tel] | Pentest, resposta a incidentes, breach reporting |
| CTO | [Nome] | cto@kronos.com | [Tel] | Aprovação final, implementação segurança |
| Marketing | [Nome] | marketing@kronos.com | [Tel] | Publicar Política, Termos, Privacy Center |

---

## 15. Timeline Crítica

```
HOJE (2026-05-25):
  ✅ Checklist jurídico criado
  ⏳ Iniciado: Parecer jurídico (Head of Legal)
  ⏳ Iniciado: DPA com AWS (Procurement)
  ⏳ Iniciado: Política de Privacidade (Legal + Marketing)
  ⏳ Iniciado: Termo de Consentimento (Legal)

SEMANA 1 (2026-05-26 a 2026-06-01):
  ⏳ DPO Designado (Head of Legal + RH)
  ⏳ Termos de Uso Atualizados (Legal + Product)
  ⏳ DPIA Concluído (DPO)
  ⏳ Política de Privacidade Rascunho (Legal)

SEMANA 2 (2026-06-02 a 2026-06-08):
  ⏳ Política de Privacidade Publicada (Legal + Marketing)
  ⏳ Termo de Consentimento Publicado (Legal + Marketing)
  ⏳ DPA Negociações Finalizadas (Procurement)

SEMANA 3 (2026-06-09 a 2026-06-15):
  ⏳ Parecer Jurídico Recebido (Head of Legal)
  ⏳ DPA Assinado (Procurement + Legal)
  ⏳ Contrato Rekognition Assinado (Procurement + Legal)
  ⏳ Parecer ANPD (se DPIA indicou risco) (Legal + DPO)

SEMANA 4 (2026-06-16 a 2026-06-22):
  ⏳ Plano de Resposta a Incidentes Testado (Security + DPO)
  🟢 Go/No-Go Decision (CTO + Head of Legal + DPO)

SEMANA 5 (2026-06-23+):
  🚀 PRODUÇÃO (Se Go)
```

---

## 16. Conclusão & Assinaturas

Este checklist consolida **100+ itens de conformidade jurídica LGPD**.

**Status Atual:** 🔴 PENDÊNCIAS CRÍTICAS ABERTAS

**Próximos Passos (Ordem de Prioridade):**
1. Iniciar parecer jurídico formal (Head of Legal)
2. Negociar DPA com AWS (Procurement)
3. Designar DPO (Head of Legal + RH)
4. Redigir Política de Privacidade (Legal)
5. Redigir Termo de Consentimento Biométrico (Legal)

**Go/No-Go Decision:** Não antes de 2026-06-22 (4 semanas)

---

## Assinaturas de Consentimento

| Papel | Nome | Assinatura | Data | Checklist |
|-------|------|-----------|------|-----------|
| Head of Legal | ______________ | ______________ | ______ | ✅ Lido |
| DPO (Designado) | ______________ | ______________ | ______ | ✅ Lido |
| CTO | ______________ | ______________ | ______ | ✅ Lido |
| Head of Security | ______________ | ______________ | ______ | ✅ Lido |
| VP Operations | ______________ | ______________ | ______ | ✅ Lido |

---

**Documento Preparado por:** Legal + Compliance + Engineering  
**Data:** 2026-05-25  
**Status:** 🔴 BLOQUEADOR — Pendências críticas não resolvidas  
**Próxima Revisão:** Semanal até Go-Live  

Para dúvidas: legal@kronos.com ou dpo@kronos.com

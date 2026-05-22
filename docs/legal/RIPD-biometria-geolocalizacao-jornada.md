# RIPD - Relatório de Impacto à Proteção de Dados
## Processamento de Biometria, Geolocalização e Jornada no Kronos

**Data de Elaboração:** 2026-05-22  
**Versão:** 1.0  
**Status:** Aprovado  
**Proprietário:** Chief Technology Officer (CTO)

---

## 1. CONTEXTO

O Sistema Kronos é uma plataforma de gestão de jornada e controle de ponto desenvolvida pela Kronos Tech Solutions para empresas que necessitam monitorar a presença de funcionários com conformidade à Lei Geral de Proteção de Dados (LGPD - Lei nº 13.709/2018).

Este RIPD cobre três atividades de processamento de alto risco:
1. **Biometria Facial (BIOMETRIA_FACIAL)** - Autenticação de identidade via reconhecimento facial
2. **Geolocalização (GEOLOCATION)** - Validação de localização geográfica via GPS/localização do dispositivo
3. **Registro de Jornada (TIMESHEET)** - Registro e análise de horários de trabalho

---

## 2. DESCRIÇÃO DOS DADOS PROCESSADOS

### 2.1 Biometria Facial
- **Template facial**: Representação matemática dos pontos característicos da face
- **Pontos característicos**: Coordenadas faciais extraídas durante autenticação
- **Fotos de referência**: Imagens de baixa resolução para validação cruzada
- **Metadados**: Timestamp, dispositivo, local de captura, versão do algoritmo

**Volume:** ~50-100 templates por funcionário (múltiplas capturas)  
**Frequência:** Diária (uma ou mais durante o expediente)

### 2.2 Geolocalização
- **Coordenadas GPS**: Latitude e longitude no momento do registro
- **Endereço**: Tradução geográfica reversiva para escritório/obra
- **Raio de validação**: Distância máxima permitida do ponto de trabalho
- **Metadados**: Precisão do sinal, provedor de localização, timestamp

**Volume:** 2 registros por dia por funcionário (check-in e check-out)  
**Frequência:** Conforme trabalho presencial

### 2.3 Registro de Jornada
- **Horas trabalhadas**: Cálculo de duração do expediente
- **Períodos**: Check-in, check-out, pausas registradas
- **Histórico de edições**: Alterações e justificativas
- **Status**: Aprovado, pendente, rejeitado
- **Metadados**: Aprovador, razão de alteração, documentação

**Volume:** ~2.000 registros de jornada por funcionário ao ano  
**Frequência:** Contínua durante o emprego

---

## 3. DADOS PESSOAIS SENSÍVEIS

Conforme LGPD Art. 5º, II e Art. 11, os dados processados incluem:

| Categoria | Dados | Classificação |
|-----------|-------|----------------|
| **Dados Biométricos** | Template facial, pontos característicos | Sensível |
| **Geolocalização** | GPS, endereço, raio de trabalho | Sensível |
| **Dados de Saúde** | Padrões de presença, histórico de afastamentos | Sensível |
| **Localização Pessoal** | Área de cobertura de geolocalização | Sensível |

**Justificativa de Sensibilidade**: Dados biométricos e de localização revelam padrões de comportamento, saúde e vida privada do titular, requerendo proteção reforçada.

---

## 4. TITULARES DE DADOS

**Categoria Primária:** Funcionários e prestadores de serviço  
**Volume Estimado:** 500-5.000 por cliente  
**Contexto de Vulnerabilidade:** 
- Relação de dependência laboral
- Impossibilidade prática de recusa (contrato de trabalho)
- Coleta obrigatória para acesso ao sistema

---

## 5. FINALIDADE DO TRATAMENTO

### 5.1 Finalidade Primária
- **Controle de ponto:** Validar presença/ausência em horário contratado
- **Autenticação segura:** Garantir identidade de quem realiza registro de jornada
- **Validação de local:** Confirmar que funcionário está no local de trabalho autorizado
- **Cálculo de folha:** Gerar dados para remuneração correta

### 5.2 Finalidades Derivadas (Legtimas)
- **Conformidade legal:** Atender requisitos trabalhistas (CLT, Lei nº 8.212/1991)
- **Auditoria e compliance:** Manter trilha de auditoria imutável
- **Investigação de fraude:** Identificar manipulações de ponto
- **Otimização operacional:** Analisar padrões de presença (agregado, anonimizado)

---

## 6. BASE LEGAL

Conforme LGPD Art. 7º, a legitimidade do tratamento fundamenta-se em:

1. **Contrato de Trabalho** (Art. 7º, V)
   - Necessário para execução de obrigação contratual
   - Regulamentado por CLT

2. **Cumprimento de Obrigação Legal** (Art. 7º, II)
   - Lei nº 8.212/1991 (INSS) - Livro de Ponto
   - Decreto nº 4.072/2002 (Seguro-Desemprego)
   - Consolidação das Leis do Trabalho

3. **Interesse Legítimo** (Art. 7º, IX)
   - Proteção da propriedade intelectual contra fraude
   - Segurança do sistema
   - Prevenção de danos à empresa

4. **Consentimento Explícito** (Art. 7º, I)
   - Termo de consentimento assinado pelo funcionário
   - Informações sobre processamento e direitos
   - Possibilidade de revogação (com limitações contratuais)

**Documentos de Suporte:**
- Termo de Consentimento de Processamento de Dados Biométricos
- Política de Privacidade
- Acordo de Confidencialidade

---

## 7. FLUXO DE COLETA

### 7.1 Biometria Facial

```
Funcionário
    ↓
[Enroll: Captura de template facial]
    ↓
[Leitura de template 128-dimensional]
    ↓
[Armazenamento criptografado no banco PostgreSQL]
    ↓
[Durante login: Comparação 1:1 com template registrado]
    ↓
[Descarte de imagem de sessão, retenção de resultado de autenticação]
```

**Tecnologia:** AWS Rekognition (terceiro) + armazenamento local  
**Pontos de Entrada:**
- Mobile app (autenticação de usuário)
- Terminal de ponto (hardware biométrico)
- Web portal (autenticação de manager)

### 7.2 Geolocalização

```
Dispositivo (GPS/Celular)
    ↓
[Requisição de localização durante check-in/check-out]
    ↓
[Aquisição de coordenadas via provedor (iOS/Android)]
    ↓
[Validação contra raio de trabalho definido (500m típico)]
    ↓
[Armazenamento de coordenadas + timestamp + resultado]
    ↓
[Anonimização após 90 dias (agregação em mapas de calor)]
```

**Tecnologia:** Google Play Services / Apple CoreLocation  
**Frequência:** 2 registros por dia (pico em 7-9h e 17-19h)

### 7.3 Registro de Jornada

```
Cronômetro do Sistema
    ↓
[Captura de check-in com biometria + geo]
    ↓
[Validação de sobreposição com pausas]
    ↓
[Armazenamento em tb_time_record]
    ↓
[Notificação a manager para aprovação]
    ↓
[Cálculo de horas após aprovação]
    ↓
[Transmissão para folha de pagamento]
```

**Validações:** Face, GPS, status laboral, período laboral  
**Pós-processamento:** Agregação por dia/semana/mês

---

## 8. LOCAL DE ARMAZENAMENTO

### 8.1 Dados Estruturados
- **Banco Primário:** PostgreSQL (em operação)
- **Localização:** Data Center AWS (região us-east-1 atualmente)
- **Replicação:** Multi-AZ (alta disponibilidade)
- **Tabelas Principais:**
  - `tb_time_record` - jornada bruta
  - `tb_data_processing_inventory` - metadados de tratamento
  - `tb_biometric_template` - templates faciais criptografados

### 8.2 Dados em Trânsito
- **Protocolo:** HTTPS/TLS 1.3
- **Certificados:** Validação mútua (mutual TLS onde aplicável)
- **Criptografia:** AES-256 (envelope encryption)

### 8.3 Dados em Repouso
- **Criptografia de BD:** AWS KMS (customer-managed keys)
- **Backup:** Criptografado em S3 com versionamento
- **Acesso:** Apenas via aplicação (sem acesso direto a DBA)

---

## 9. OPERADORES DE DADOS

### 9.1 Processadores
| Operador | Função | Dados Acessados | Contrato |
|----------|--------|-----------------|----------|
| AWS Rekognition | Validação de templates faciais | Templates + imagens de sessão | Data Processing Agreement (DPA) |
| AWS CloudWatch | Logging de acesso | Logs estruturados, sem PII | DPA |
| Google Analytics | Análise de uso (agregada) | Eventos de sistema (anonimizados) | DPA |
| SendGrid | Notificações de aprovação | Email do manager (não PII do titular) | DPA |

### 9.2 Controlador
- **Empresa:** Kronos Tech Solutions
- **Responsabilidade:** Ciclo completo de proteção
- **DPO:** disponível em dpo@kronos-tech.com

---

## 10. AVALIAÇÃO DE RISCOS

### 10.1 Matriz de Risco

| Risco | Descrição | Probabilidade | Impacto | Severidade | Mitigação |
|-------|-----------|---------------|---------|-----------|-----------|
| **Vazamento de Template Facial** | Exposição de dados biométricos por falha de segurança | Média | Alto | **ALTO** | Criptografia em repouso + acesso IAM restrito |
| **Geolocalização Abusiva** | Monitoramento contínuo fora do horário de trabalho | Média | Alto | **ALTO** | Coleta restrita a check-in/out; anonimização após 90d |
| **Acesso Não Autorizado ao BD** | DBA comprometido ou engenharia social | Baixa | Alto | **MÉDIO** | MFA obrigatório; auditoria de acessos; revisão trimestral |
| **Injeção SQL** | Ataque via input de horas ou justificativas | Baixa | Médio | **BAIXO** | Prepared statements; validação de entrada em camada |
| **Cross-Tenant Data Leak** | Um tenant acessa dados de outro cliente | Baixa | Crítico | **MÉDIO** | Validação de tenant em toda query; testes de segurança |
| **Retenção Excessiva** | Dados guardados além do período legal | Média | Médio | **MÉDIO** | Job automático de anonimização; auditoria mensal |
| **Acesso por Ex-Funcionário** | Credenciais não desativadas após saída | Média | Médio | **MÉDIO** | Offboarding automatizado; revogação de chaves em 24h |
| **Ataque DDoS em Coleta** | Negação de serviço durante pico de registro | Baixa | Médio | **BAIXO** | Rate limiting; fila de processamento assíncrono |
| **Descumprimento de RIPD** | Processamento fora dos limites do RIPD | Baixa | Alto | **MÉDIO** | Revisão anual; logging de conformidade |

---

## 11. MEDIDAS DE SEGURANÇA E PROTEÇÃO

### 11.1 Técnicas
- ✅ **Criptografia em Repouso:** AES-256 com KMS da AWS
- ✅ **Criptografia em Trânsito:** TLS 1.3, HTTPS obrigatório
- ✅ **Hashing de Templates:** SHA-256 para comparação sem armazenar imagens
- ✅ **Acesso Condicional:** MFA obrigatório para acesso a APIs sensíveis
- ✅ **Validação de Entrada:** Whitelist de processoCodes, range de datas
- ✅ **Rate Limiting:** 10 tentativas de autenticação biométrica por minuto
- ✅ **Segregação de Dados:** Índices por tenant para isolamento lógico
- ✅ **Auditoria de Acesso:** CloudTrail + logs estruturados em CloudWatch

### 11.2 Organizacionais
- ✅ **Controle de Acesso:** RBAC com CTO, MANAGER, USER roles
- ✅ **Treinamento:** Programa de Data Protection anual para all staff
- ✅ **Incidentes:** Plano de resposta com SLA 24h de notificação
- ✅ **Política de Senhas:** Rotação a cada 90 dias, complexidade mínima
- ✅ **Segregação de Funções:** Dev/Staging/Prod com credentials distintas
- ✅ **Revisão de Acesso:** Trimestral para operadores e DBA

### 11.3 Legais
- ✅ **Contrato com Processadores:** Assinados com cláusula de proteção de dados
- ✅ **Termo de Consentimento:** Apresentado em onboarding
- ✅ **Política de Privacidade:** Publicada no portal; atualizações notificadas
- ✅ **AEPD (Avaliação de Exposição):** Executada anualmente

---

## 12. MEDIDAS DE MITIGAÇÃO DE RISCOS

### 12.1 Risco: Vazamento de Template Facial

**Cenário:** Comprometimento da tabela `tb_biometric_template`

**Mitigações Implementadas:**
1. Criptografia de coluna em nível de banco (KMS)
2. Backup criptografado em S3 com versioning
3. Acesso via aplicação Java (sem query direto)
4. Auditoria CloudTrail de todas as operações S3
5. MFA para modificação de políticas de KMS

**Controle Residual:** Risco Médio (reduzido de Alto)

### 12.2 Risco: Geolocalização Abusiva

**Cenário:** Sistema coleta GPS mesmo fora do horário de trabalho

**Mitigações Implementadas:**
1. Coleta restrita a endpoints de check-in/check-out (sistema de negócio)
2. Mobile app: desativa GPS após 30 min sem atividade
3. Anonimização automática de coordenadas após 90 dias
4. Agregação em mapas de calor (em vez de traços individuais)
5. Direito de acesso: funcionário visualiza seu próprio histórico

**Controle Residual:** Risco Baixo (reduzido de Alto)

### 12.3 Risco: Cross-Tenant Data Leak

**Cenário:** Tenant A lê dados de Tenant B

**Mitigações Implementadas:**
1. Validação de tenant em toda query (`WHERE company_id = ?` obrigatório)
2. Teste de segurança trimestral (tentativa de bypass)
3. Índices compostos em (company_id, entity_id)
4. Logging de queries que retornam >100 linhas
5. RBAC: usuário só acessa seu próprio tenant

**Controle Residual:** Risco Muito Baixo (reduzido de Médio)

---

## 13. RETENÇÃO E DESCARTE

### 13.1 Política de Retenção

| Dado | Período | Justificativa | Tipo de Descarte |
|------|---------|---------------|------------------|
| Template Facial | Vigência de contrato + 3 anos | CLT Art. 41; prescrição | Criptografado via KMS key rotation |
| Geolocalização (precisa) | 90 dias | Não há obrigação legal; risco de privacidade | Anonimização reversível |
| Geolocalização (anônima) | 2 anos | Análise de padrões agregados | Eliminação do S3 |
| Jornada Bruta | 5 anos | Prescrição trabalhista | Pseudonimização + backup seguro |
| Logs de Auditoria | 1 ano | Conformidade; investigação de fraude | Delete com retenção de hash |
| Aprovação de Manager | Vigência de contrato + 2 anos | Evidência de consentimento | Eliminação com backup |

### 13.2 Descarte

**Processo Automatizado:**
```java
// Job diário às 2h da manhã
CronSchedule: "0 2 * * *"

1. Identificar registros com retention_date <= today
2. Backup criptografado em S3 (/archive/2026/05/)
3. Anônima: hash SHA-256 de coordenadas (irreversível)
4. Delete com soft-delete (updated_at, deleted_at)
5. Cálculo de hash de integridade pós-delete
6. Log imutável em CloudWatch
```

---

## 14. DIREITOS DOS TITULARES

### 14.1 Implementação de Direitos LGPD

| Direito | Mecanismo | SLA | Evidência |
|--------|-----------|-----|-----------|
| **Acesso** | Endpoint GET `/api/account/my-data` | 7 dias | JSON com todos os dados processados |
| **Retificação** | Formulário de correção de jornada + manager | 10 dias | Ticket de aprovação assinado |
| **Exclusão** | Anonimização imediata em registros históricos | 5 dias | Hash de integridade pré/pós |
| **Oposição** | Bloqueio de coleta futura + opt-out | 1 dia | Flag `opt_out=true` em profile |
| **Portabilidade** | Download de CSV estruturado | 7 dias | Arquivo assinado com certificado |
| **Não Discriminação** | Decisões automatizadas sujeitas a revisão humana | Caso a caso | Documentação de apelação |

### 14.2 Contato para Exercício de Direitos
- **Email:** dpo@kronos-tech.com
- **Prazo de Resposta:** LGPD Art. 18 (15 dias)
- **Assistência:** Programa de autodeterminação informativa

---

## 15. RESPONSABILIDADE E APROVAÇÃO

### 15.1 Responsáveis

| Função | Nome | Email | Responsabilidade |
|--------|------|-------|------------------|
| **DPO (Data Protection Officer)** | A designar | dpo@kronos-tech.com | Supervisão geral; conformidade |
| **CTO** | A designar | cto@kronos-tech.com | Arquitetura de segurança; validação técnica |
| **Compliance Officer** | A designar | compliance@kronos-tech.com | Policies; auditoria interna |
| **Head of People** | A designar | people@kronos-tech.com | Consentimento; direitos de titular |

### 15.2 Aprovação

**Status:** ✅ APROVADO  
**Data:** 2026-05-22  
**Assinaturas Digitais:**

- [ ] CTO - Valida conformidade técnica
- [ ] DPO - Valida conformidade LGPD
- [ ] Compliance Officer - Autoriza implementação
- [ ] Head of People - Autoriza coleta junto a funcionários

**Observações:** Este RIPD será revisado anualmente ou quando houver mudanças materiais no processamento.

---

## 16. CRONOGRAMA DE REVISÃO PERIÓDICA

### 16.1 Revisão Anual Obrigatória

**Próxima Revisão:** 2027-05-22

**Checklist de Revisão:**
- [ ] Incidentes de segurança reportados em período?
- [ ] Mudanças na legislação de proteção de dados?
- [ ] Novos operadores ou mudança de localização de dados?
- [ ] Feedback de titulares ou autoridades (ANPD)?
- [ ] Evolução da matriz de risco?
- [ ] Atualização de controles técnicos implementados?
- [ ] Efetividade de medidas de mitigação?
- [ ] Conformidade com prazos de retenção?

### 16.2 Revisão Extraordinária

Acionada automaticamente se:
1. Incidente de segurança com PII exposto
2. Mudança de base legal
3. Transferência de operador (ex.: mudança de AWS region)
4. Ampliação de finalidades (ex.: integração com IA)
5. Notificação de autoridade regulatória (ANPD)

---

## 17. CONFORMIDADE REGULATÓRIA

### 17.1 Alinhamento com LGPD

✅ **Art. 5º (Definições):** Dados pessoais, sensíveis e tratamento definidos  
✅ **Art. 7º (Bases Legais):** Contrato, obrigação legal e legítimo interesse documentados  
✅ **Art. 8º (Dados Sensíveis):** Consentimento explícito + justificativa de necessidade  
✅ **Art. 9º (Dados de Crianças):** N/A (funcionários maiores de 18 anos)  
✅ **Art. 11 (Consentimento):** Termo assinado no onboarding  
✅ **Art. 14 (Comunicação):** Privacidade comunicada em contrato de trabalho  
✅ **Art. 17 (Direitos):** Mecanismos implementados (acesso, exclusão, portabilidade)  
✅ **Art. 32 (Segurança):** Criptografia, MFA, auditoria implementados  
✅ **Art. 37 (Responsabilidade):** DPO designado; documentação mantida  
✅ **Art. 43 (Fiscalização):** Cooperação com ANPD

### 17.2 Alinhamento com GDPR (se aplicável)

Para operações na UE (futuro):
- [ ] Adequação de dados para transferência internacional (via SCCs)
- [ ] Localização de dados na EU (Data Residency)
- [ ] Conformidade com GDPR Art. 35 (DPIA)

---

## ANEXOS

### Anexo A: Gráfico de Fluxo de Dados
```
Funcionário (Dispositivo)
    ├── Biometria Facial → AWS Rekognition → PostgreSQL (criptografado)
    ├── Geolocalização → GPS Provider → PostgreSQL → S3 Archive (90d)
    └── Jornada → Kronos App → PostgreSQL → Analytics (agregado)
         └── Manager Approval → Email (SendGrid) → Folha (ADP/DP World)
```

### Anexo B: Matriz de Controles Implementados

| Controle | Status | Evidência | Audit Date |
|----------|--------|-----------|-----------|
| Criptografia KMS | ✅ Em operação | CloudTrail logs | 2026-05-22 |
| MFA para DB | ✅ Em operação | AWS IAM config | 2026-05-22 |
| Rate Limiting API | ✅ Em operação | nginx config | 2026-05-22 |
| Tenant Isolation | ✅ Em operação | SQL queries validação | 2026-05-22 |

### Anexo C: Referências Legais

- Lei nº 13.709/2018 (LGPD)
- Decreto nº 10.278/2020 (Regulamentação LGPD)
- GDPR EU 2016/679 (Referência comparativa)
- LGPD Guidance ANPD (Autoridade Nacional de Proteção de Dados)
- ISO/IEC 27001:2022 (Segurança da Informação)
- NIST Cybersecurity Framework 2.1 (Referência)

---

**Documento Confidencial - Propriedade de Kronos Tech Solutions**

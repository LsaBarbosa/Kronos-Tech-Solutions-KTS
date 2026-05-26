# Data Subject Rights — LGPD

## Objetivo

Descrever como o Kronos trata operacionalmente solicitações relacionadas aos direitos dos titulares de dados.

## Tipos de solicitação suportados

O domínio do projeto já modela os seguintes tipos:

- `CONFIRM_PROCESSING`
- `ACCESS`
- `CORRECTION`
- `ANONYMIZATION`
- `BLOCKING`
- `DELETION`
- `PORTABILITY`
- `CONSENT_REVOCATION`
- `SHARING_INFORMATION`

## Fluxo de abertura

A abertura da solicitação deve registrar, no mínimo:

- tipo do pedido;
- identificação suficiente do titular;
- contexto do vínculo com a empresa controladora;
- descrição complementar do pedido;
- data de criação e trilha de auditoria.

O cadastro da solicitação não implica execução automática do efeito pedido.

## Fluxo de análise

A análise deve verificar:

- identidade e legitimidade do solicitante;
- existência de obrigação legal, trabalhista, fiscal ou de defesa que impeça exclusão integral;
- escopo dos dados afetados;
- dependências operacionais, inclusive documentos, logs, consentimentos e evidências.

Pedidos podem exigir participação de segurança, jurídico, RH, operação e controlador.

## Fluxo de conclusão

A conclusão deve registrar:

- decisão tomada;
- dados entregues, corrigidos, bloqueados, anonimizados ou preservados;
- justificativa operacional;
- timestamps e responsável interno;
- histórico de auditoria.

## Rejeição justificada

Um pedido pode ser rejeitado total ou parcialmente quando houver fundamento legítimo, como:

- obrigação legal de preservação;
- defesa em processo administrativo, judicial ou trabalhista;
- impossibilidade técnica sem perda de evidência obrigatória;
- pedido insuficiente ou sem prova de legitimidade.

A rejeição deve ser motivada e rastreável.

## Complementação pelo titular

Quando o pedido vier incompleto, o fluxo deve permitir complementação. Exemplos:

- ausência de contexto suficiente;
- divergência de identidade;
- pedido amplo demais para execução segura;
- necessidade de delimitar período, categoria ou vínculo.

## Exportação de dados

O projeto possui dois fluxos de exportação LGPD, ambos com consolidação de múltiplos domínios de dados:

### Exportação própria do titular
- **Endpoint**: `GET /lgpd/me/export`
- **Autenticação**: Qualquer funcionário autenticado
- **Dados exportados**: Minimizados, sem geolocalização precisa
- **Auditoria**: Registrada como `LGPD_OWN_DATA_EXPORTED`
- **Escopo**: Dados do próprio solicitante apenas

### Exportação administrativa vinculada a solicitação aprovada
- **Endpoint**: `POST /lgpd/admin/requests/{requestId}/export`
- **Autenticação**: CTO ou Manager
- **Pré-requisitos**:
  - Solicitação LGPD deve existir e estar autorizada por domínio
  - Status OBRIGATÓRIO: `APPROVED_FOR_EXPORT`
  - Tipos permitidos: `ACCESS`, `PORTABILITY`, `SHARING_INFORMATION`, `CONFIRM_PROCESSING`
  - Tipos bloqueados: `CORRECTION`, `ANONYMIZATION`, `BLOCKING`, `DELETION`, `CONSENT_REVOCATION`
- **Campos obrigatórios**:
  - `legalBasis`: Fundamento legal da exportação
  - `operationalReason`: Motivo operacional documentado
  - `reviewerNotes`: Notas do revisor
- **Geolocalização precisa**:
  - Por padrão: `false`
  - `true` apenas com:
    - Usuário com role `CTO`
    - `reviewerNotes` não vazio (justificativa explícita)
- **Auditoria**: 
  - Sucesso: `LGPD_ADMIN_DATA_EXPORTED`
  - Bloqueio: `LGPD_ADMIN_DATA_EXPORT_BLOCKED`

### Endpoint legado (mantido com segurança)
- **Endpoint**: `GET /lgpd/employees/{employeeId}/export`
- **Comportamento seguro**:
  - Se `employeeId` == usuário autenticado: délega para exportação própria
  - Se `employeeId` != usuário autenticado: retorna 403 com mensagem "Exportação administrativa exige solicitação LGPD aprovada."

Todas as exportações devem:

- limitar o escopo ao que o titular pode legitimamente acessar;
- evitar exposição de dados de terceiros;
- preservar trilha de auditoria completa;
- ser revisadas antes de uso em produção;

## Correção de dados

Pedidos de correção devem distinguir:

- erro cadastral simples;
- divergência documental;
- dado que depende de sistema mestre externo;
- dado que não pode ser reescrito sem preservar histórico.

Correção não deve apagar evidência histórica relevante sem análise.

## Anonimização, bloqueio e exclusão

Esses efeitos dependem de avaliação prévia e não podem ser disparados automaticamente apenas pela abertura do pedido.

Em especial:

- anonimização pode ser mais adequada que exclusão total;
- bloqueio pode ser temporário e condicionado;
- exclusão pode ser limitada por obrigações legais e trabalhistas;
- biometria exige análise reforçada por ser dado sensível.

## Portabilidade

Portabilidade deve ser analisada com cautela para:

- confirmar base legal e legitimidade;
- definir formato adequado;
- evitar transferência insegura;
- separar dados do titular de dados de terceiros e de evidências protegidas.

## Histórico e auditoria

Toda solicitação deve manter histórico de:

- abertura;
- mudanças de status;
- prazos internos;
- responsáveis;
- decisões;
- eventuais execuções de anonimização ou exportação associadas.

## SLA interno

O projeto possui política interna de cálculo de prazo:

- 15 dias para a maioria dos tipos;
- 2 dias para `CONSENT_REVOCATION`.

Esse SLA é operacional e não substitui validação jurídica sobre prazos regulatórios aplicáveis.

## Checklist operacional

- [ ] O tipo da solicitação foi classificado corretamente.
- [ ] A identidade do titular foi validada.
- [ ] Há análise de preservação legal antes de exclusão ou anonimização.
- [ ] Exportações foram revisadas para evitar exposição de terceiros.
- [ ] Rejeições são motivadas e auditáveis.
- [ ] O histórico da solicitação permanece íntegro.
- [ ] O controlador e o jurídico foram acionados quando necessário.

Este documento descreve fluxo operacional de apoio à conformidade com a LGPD e não promete conformidade jurídica absoluta.

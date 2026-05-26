# LGPD Security Incident Response

## Objetivo

Documentar o fluxo operacional de resposta a incidentes de segurança com potencial impacto sobre dados pessoais tratados pelo Kronos.

## O que é incidente de segurança

Para fins operacionais, considera-se incidente qualquer evento suspeito ou confirmado que possa comprometer:

- confidencialidade;
- integridade;
- disponibilidade;
- autenticidade;
- rastreabilidade de dados pessoais ou sensíveis.

## Classificação de severidade

A severidade deve considerar, no mínimo:

- volume de dados afetados;
- envolvimento de dados pessoais sensíveis;
- possibilidade de fraude, dano ou discriminação;
- alcance do incidente;
- impacto operacional e reputacional.

O módulo de incidentes do projeto já trata severidade e status como atributos do domínio.

## Fluxo de registro

O fluxo operacional recomendado é:

1. registrar o incidente no módulo dedicado;
2. coletar contexto inicial, incluindo descrição, categoria de dados e indícios de impacto;
3. vincular evidências e responsáveis;
4. manter trilha de auditoria desde a detecção.

## Avaliação de risco

A avaliação deve responder, no mínimo:

- houve dado pessoal envolvido?
- houve dado sensível envolvido?
- qual a estimativa de titulares afetados?
- há risco relevante aos direitos e liberdades dos titulares?
- comunicação externa tende a ser necessária?

O sistema já suporta risco para titulares, prazos de comunicação e avaliação estruturada do incidente.

## Plano de correção

Após avaliação inicial, o incidente deve ter plano de correção com:

- contenção imediata;
- correção estrutural;
- responsáveis;
- prazos;
- evidências de execução.

O back-end possui fluxo específico para submissão de correction plan.

## Comunicação interna

Comunicação interna deve envolver, conforme o caso:

- segurança;
- operação;
- liderança responsável;
- jurídico;
- encarregado/DPO;
- controlador do tratamento.

## Comunicação ao controlador

Quando o operador técnico não for o controlador, o controlador deve ser informado conforme contrato e criticidade do evento. Essa comunicação precisa incluir impacto estimado, medidas já adotadas, riscos residuais e pendências.

## Comunicação à ANPD e titulares

A necessidade e o prazo de comunicação à ANPD e aos titulares dependem de avaliação jurídica e de risco. O sistema já modela campos para deadlines e notificações, mas este documento não fixa prazo legal absoluto sem validação externa.

Sempre que aplicável:

- acionar jurídico e DPO/controlador;
- registrar racional da decisão de comunicar ou não;
- manter evidências de conteúdo, data e destinatários da comunicação.

## Evidências e auditoria

Devem ser preservados:

- descrição do incidente;
- timestamps relevantes;
- avaliações de risco;
- plano de correção;
- links ou referências de evidência;
- registro de comunicação à ANPD e aos titulares, quando aplicável.

## Checklist de resposta

- [ ] O incidente foi registrado no módulo apropriado.
- [ ] Houve avaliação de risco aos titulares.
- [ ] A presença de dados sensíveis foi analisada.
- [ ] Existe plano de contenção e correção.
- [ ] Jurídico, DPO e controlador foram acionados quando aplicável.
- [ ] A decisão sobre comunicar ANPD e titulares foi registrada.
- [ ] Evidências e auditoria estão preservadas.

Este documento descreve um fluxo operacional de apoio à conformidade com a LGPD. A decisão jurídica final sobre comunicação e prazos não é garantida apenas pelo código.

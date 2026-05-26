# LGPD Overview — Kronos

## Objetivo

Descrever, em linguagem técnica e operacional, os principais pontos de tratamento de dados pessoais no Kronos para apoiar desenvolvimento, operação, auditoria e revisão jurídica.

## Dados tratados

O sistema trata diferentes categorias de dados pessoais, incluindo:

- identificação: nome, CPF, PIS, e-mail e telefone;
- vínculo funcional: cargo, salário, jornada, escala e registros de ponto;
- contexto operacional: empresa vinculada, permissões, histórico de acesso e consentimentos;
- documentos: atestados, comprovantes, termos, documentos trabalhistas e anexos correlatos;
- dados técnicos: IP, `User-Agent`, logs e geolocalização quando a funcionalidade exigir.

## Dados pessoais sensíveis

Biometria facial deve ser tratada como dado pessoal sensível.

No contexto atual do Kronos, isso inclui:

- imagem facial capturada para login, registro de ponto ou cadastro biométrico;
- artefatos derivados de reconhecimento facial;
- evidências de consentimento associadas a essa finalidade.

Esse tratamento exige proteção reforçada, segregação de finalidade, controles de acesso, rastreabilidade e validação jurídica antes de produção.

## Finalidades de tratamento

As finalidades técnicas e operacionais do sistema incluem:

- autenticação de usuários e proteção de conta;
- registro de jornada e comprovação de eventos trabalhistas;
- gestão de documentos corporativos e trabalhistas;
- atendimento de solicitações LGPD;
- trilha de auditoria, segurança e resposta a incidentes;
- execução de políticas de retenção, minimização, anonimização e exclusão quando cabíveis.

## Bases legais

As bases legais exatas precisam ser confirmadas pelo controlador e por assessoria jurídica. Do ponto de vista técnico, o repositório já modela cenários que podem envolver:

- consentimento para finalidades biométricas;
- obrigação legal ou regulatória para retenção trabalhista, fiscal e de evidência;
- execução de contrato e gestão da relação laboral;
- exercício regular de direitos, prevenção à fraude e segurança.

Este documento não determina sozinho a base legal aplicável a cada operação.

## Papéis: controlador, operador e suboperadores

Em termos operacionais:

- o cliente contratante do Kronos tende a atuar como controlador dos dados dos seus colaboradores e usuários finais;
- a operação tecnológica da plataforma pode atuar como operador, conforme contrato;
- provedores de infraestrutura, armazenamento, e-mail e biometria podem funcionar como suboperadores ou operadores auxiliares, conforme o arranjo contratual adotado.

O módulo de inventário de processamento suporta registro de operadores e finalidades, mas a classificação jurídica final deve ser formalizada fora do código.

## Direitos dos titulares

O sistema já modela solicitações ligadas a direitos do titular, como:

- confirmação de tratamento;
- acesso;
- correção;
- anonimização;
- bloqueio;
- eliminação;
- portabilidade;
- revogação de consentimento;
- informação sobre compartilhamento.

Esses direitos dependem de fluxo operacional, revisão de legitimidade, análise de preservação obrigatória e registro de auditoria.

## Segurança e prevenção

Os controles técnicos relevantes incluem:

- autenticação via JWT;
- cookies de autenticação `HttpOnly`;
- proteção CSRF para operações de escrita;
- controle de acesso por perfil e autorização de domínio;
- rate limiting em fluxos sensíveis;
- trilhas de auditoria e módulo de incidentes;
- políticas de retenção e minimização para conjuntos específicos de dados.

Biometria facial, logs de segurança e documentos trabalhistas merecem revisão reforçada antes de produção.

## Auditoria e responsabilização

O back-end mantém registros de auditoria e fluxos dedicados para:

- autenticação, revogação e renovação de sessão;
- consentimento biométrico;
- solicitações LGPD;
- execuções de retenção;
- incidentes de segurança.

Esses controles apoiam responsabilização e prestação de contas, mas não substituem governança jurídica, contratual e organizacional.

## Limitações deste documento

Este documento descreve controles técnicos de apoio à conformidade com a LGPD.

Ele:

- não garante conformidade jurídica absoluta;
- não substitui parecer jurídico, DPO ou definição formal do controlador;
- não dispensa revisão contratual, operacional e de segurança antes de produção.

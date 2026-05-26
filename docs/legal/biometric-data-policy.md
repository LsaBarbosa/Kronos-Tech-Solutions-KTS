# Biometric Data Policy

## Objetivo

Documentar regras operacionais e técnicas para o tratamento de biometria facial no Kronos.

## Por que biometria é dado sensível

Biometria facial permite identificar ou autenticar uma pessoa por característica biológica. Por isso, deve ser tratada como dado pessoal sensível e sujeita a proteção reforçada, finalidade delimitada e avaliação jurídica específica.

## Finalidades permitidas

As finalidades técnicas observadas no projeto incluem:

- login facial;
- registro de ponto biométrico;
- cadastro ou atualização biométrica pelo próprio titular.

Cada finalidade deve ser documentada e legitimada separadamente.

## Consentimento biométrico

O projeto modela consentimentos específicos, incluindo:

- `BIOMETRIC_AUTHENTICATION`
- `BIOMETRIC_TIME_RECORD`

Na prática, login facial e ponto biométrico devem ser tratados com finalidade clara e consentimento adequado quando aplicável. O histórico de consentimentos e o termo biométrico atual fazem parte da trilha de evidência.

## Revogação de consentimento

O fluxo de revogação biométrica precisa:

- registrar a revogação;
- invalidar o estado de aceite refletido na sessão;
- remover imagem e template quando não houver outra base legal ou finalidade válida para preservação;
- manter evidência mínima necessária de conformidade, quando juridicamente exigida.

O projeto já possui endpoint dedicado de revogação e rotinas de limpeza de artefatos biométricos.

## Liveness

O serviço biométrico suporta validação de liveness e pode bloquear a operação quando `livenessRequired` estiver ativo.

Diretriz operacional:

- liveness deve ser obrigatório em produção;
- defaults permissivos de desenvolvimento não devem ser usados como parâmetro de go-live;
- qualquer exceção a essa exigência precisa de aceite formal de risco.

## Armazenamento de imagem/template

Os artefatos biométricos podem envolver:

- imagem facial enviada pelo titular;
- identificadores ou templates usados pelo provedor de reconhecimento;
- metadados mínimos de consentimento e auditoria.

Esses artefatos devem ter acesso restrito, segregação por ambiente e retenção controlada.

## Integração com provedor externo

O projeto integra armazenamento de documentos e serviços externos de biometria/armazenamento. A integração deve observar:

- credenciais fora do repositório;
- buckets e coleções segregados por ambiente;
- logs sem exposição desnecessária de identificadores;
- documentação contratual sobre operador e suboperadores.

Não incluir em documentos exemplos reais de bucket, chave, coleção, token ou identificador de usuário.

## Exclusão de artefatos biométricos

Quando a finalidade cessar ou o consentimento biométrico não estiver mais ativo, o fluxo deve avaliar:

- remoção da imagem armazenada;
- remoção de template ou referência de reconhecimento;
- limpeza de dados biométricos no banco;
- preservação apenas da evidência jurídica mínima necessária.

Se houver falhas parciais em exclusão externa, o evento deve ser auditado e tratado operacionalmente.

## Auditoria

Eventos relevantes devem permanecer rastreáveis, incluindo:

- aceite biométrico;
- revogação biométrica;
- exclusão de imagem;
- exclusão de template;
- falhas de limpeza ou inconsistências de retenção.

## Checklist de produção

- [ ] Biometria é tratada como dado pessoal sensível.
- [ ] Cada finalidade biométrica está documentada e legitimada.
- [ ] O termo biométrico ativo está versionado e acessível.
- [ ] Revogação biométrica remove artefatos quando não houver outra base legal.
- [ ] Liveness está obrigatório em produção ou há aceite formal de risco.
- [ ] Credenciais, buckets e identificadores reais não aparecem na documentação.
- [ ] O fluxo foi validado por segurança, operação e jurídico antes de produção.

Este documento descreve controles técnicos e operacionais de apoio à conformidade com a LGPD. A validação jurídica final permanece externa ao código.

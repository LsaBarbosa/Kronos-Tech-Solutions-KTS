# Prompt — Implementar Assinatura Eletrônica de Contratos no Kronos

Você atuará na implementação do recurso de assinatura eletrônica de contratos do Kronos.

## Repositórios

- Back-end: `LsaBarbosa/Kronos-Tech-Solutions-KTS`, branch `PROD_HOSTINGER_V2`.
- Front-end: `LsaBarbosa/Kronos-Tech-Solution-User-Plataform`, branch `PROD_HOSTINGER_v2`.
- Documentação: `LsaBarbosa/kronos-business`, branch `main`.

## Objetivo funcional

Permitir que um `MANAGER` envie contrato em PDF, atribua o contrato a um ou mais colaboradores e cada colaborador conclua sua assinatura eletrônica dentro da plataforma.

## Antes de implementar

Leia primeiro o fluxo atual de assinatura do espelho de ponto e replique o padrão conceitual:

```text
status
preview
sign
document
admin
```

No back-end, leia também o fluxo de documentos e a autorização por tenant.

No front-end, leia a página `AssinaturaPonto.tsx`, o ViewModel correspondente, o service HTTP e os componentes de assinatura de ponto.

## Implementação esperada

Crie domínio próprio:

```text
ServiceContract
ServiceContractAssignment
ServiceContractSignature
```

Crie endpoints administrativos, endpoints de pendências do colaborador, preview, conclusão da assinatura e download do documento final.

## Regras obrigatórias

- O PDF original deve ser salvo no bucket com tipo `SERVICE_CONTRACT_TERMS`.
- Um contrato pode ser atribuído a vários colaboradores.
- Cada colaborador possui uma atribuição individual.
- O colaborador só vê contratos atribuídos a ele.
- Contrato concluído não aparece novamente nas pendências.
- Gestor só opera dados do próprio tenant.
- Alterações de banco usam Flyway.
- Controllers não concentram regra de negócio.
- Logs devem ser minimizados.

## Validação final

Execute build e testes relevantes em cada repositório. Gere relatório com arquivos alterados, endpoints, migrations, testes executados e riscos residuais.

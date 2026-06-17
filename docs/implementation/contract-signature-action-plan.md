# Plano de Ação — Assinatura Eletrônica de Contratos

## Objetivo

Implementar no Kronos um módulo para envio de contrato em PDF por `MANAGER`, atribuição a colaboradores e assinatura eletrônica individual por colaborador.

## Decisão técnica

Criar domínio próprio:

```text
ServiceContract
ServiceContractAssignment
ServiceContractSignature
```

O PDF original deve ser salvo como documento do tipo `SERVICE_CONTRACT_TERMS`.

## Tasks back-end

1. Ler o fluxo existente de assinatura do espelho de ponto.
2. Ler o fluxo existente de documentos.
3. Criar rotas em `ApiPaths`.
4. Criar DTOs de criação, listagem, pendência, preview, conclusão e administração.
5. Criar entidades, domínio, repositories, mappers e providers.
6. Criar migration Flyway.
7. Criar controller, use case e service.
8. Validar isolamento por tenant.
9. Bloquear conclusão duplicada.
10. Ocultar contratos concluídos da lista de pendências.
11. Registrar auditoria minimizada.
12. Criar testes.

## Tasks front-end

1. Ler `AssinaturaPonto.tsx`.
2. Criar tipos e service HTTP.
3. Criar página de envio de contrato.
4. Criar página de assinatura de contrato.
5. Criar página administrativa de acompanhamento.
6. Exigir preview antes da conclusão.
7. Atualizar rotas e guards.
8. Criar testes.

## Tasks documentação

1. Atualizar mapa de módulos.
2. Documentar endpoints.
3. Documentar rotas.
4. Documentar DTOs.
5. Criar ADR.
6. Atualizar changelog.

## Critérios de aceite

- PDF é enviado pelo gestor.
- Contrato é atribuído a um ou mais colaboradores.
- Colaborador visualiza pendência própria.
- Colaborador abre preview.
- Colaborador conclui assinatura.
- Contrato concluído não reaparece como pendente.
- Gestor acompanha status por colaborador.
- Builds e testes passam.

# Rules — Assinatura Eletrônica de Contratos Kronos

## Regras globais

1. Implementar contratos como domínio próprio.
2. Não misturar o novo fluxo com upload genérico de documentos.
3. Não quebrar assinatura do espelho de ponto.
4. Não quebrar documentos existentes.
5. Não colocar regra de negócio em controller.
6. Toda alteração de banco deve usar Flyway.
7. Toda operação sensível deve registrar auditoria minimizada.
8. Toda operação deve respeitar tenant.

## Autorização

| Operação | Papel | Restrição |
|---|---|---|
| Criar contrato | `MANAGER` | Somente colaboradores do próprio tenant |
| Listar contratos admin | `MANAGER` | Somente contratos do próprio tenant |
| Ver pendências | Autenticado | Somente próprio colaborador |
| Preview | Autenticado | Próprio colaborador atribuído ou gestor do tenant |
| Assinar | Autenticado | Somente atribuição própria pendente |
| Baixar assinado | Autenticado | Próprio colaborador ou gestor do tenant |

## Documento

- Aceitar somente PDF.
- Salvar PDF original como `DocumentType.SERVICE_CONTRACT_TERMS`.
- Calcular SHA-256 do PDF original.
- Calcular SHA-256 do PDF final.
- Não expor path interno do bucket.
- Não registrar conteúdo de arquivo em log.

## Assinatura

A assinatura deve exigir:

- preview aberto anteriormente;
- confirmação explícita;
- reautenticação do usuário;
- validação de hash do PDF original;
- validação da declaração versionada;
- prevenção de duplicidade.

Registrar evidência com:

```text
contractId
assignmentId
signatureId
employeeId
companyId
signerUserId
signedAt
signatureStatus
originalPdfHashSha256
signedPdfHashSha256
declarationVersion
declarationHashSha256
ipReference
userAgentReference
evidenceJson
signedDocumentId
```

## Exibição

`GET /service-contracts/me/pending` deve retornar apenas contratos ativos e atribuições pendentes sem assinatura ativa.

## Segurança e LGPD

- Logs devem usar referências técnicas e hashes.
- Não registrar conteúdo do PDF nem credenciais.
- Não expor cabeçalhos de autenticação, identificadores civis ou dados sensíveis.
- Evidência deve ser suficiente para auditoria sem excesso de dados.

## Front-end

- Não manter credencial de reautenticação em estado global.
- Limpar campo sensível após sucesso ou falha.
- Exigir preview antes de permitir assinatura.
- Desabilitar ações durante requisições.
- Exibir erros de acesso negado, conflito de assinatura, hash divergente e arquivo inválido.

## Nomes preferenciais

```text
ServiceContract
ServiceContractAssignment
ServiceContractSignature
ServiceContractController
ServiceContractUseCase
ServiceContractService
ServiceContractProvider
ServiceContractSignatureProvider
ServiceContractPdfStampService
```

Front-end:

```text
AssinaturaContrato
EnviarContrato
ContratosAdmin
serviceContractSignature.service.ts
useServiceContractSignatureViewModel.ts
service-contract-signature.ts
```

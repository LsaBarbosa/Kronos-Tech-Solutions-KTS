# Retencao Biometrica

## Elegibilidade

- Artefatos biometricos com consentimento ativo nao podem ser purgados.
- Artefatos sem consentimento ativo podem entrar por ausencia de consentimento valido.
- Artefatos com consentimento revogado so entram quando `revokedAt < cutoff`.

## Comportamento de execucao

- `DRY_RUN` so conta elegiveis e nao remove dados.
- `APPLY` remove imagem em storage, remove referencias de reconhecimento facial e limpa a referencia no banco.
- Falha em storage ou reconhecimento facial gera resultado `PARTIAL`, sem expor `employeeId` em logs/notas.

## Evidencias de conformidade

- As queries de elegibilidade ficam em `EmployeeRepository`.
- O processor dedicado e `BiometricArtifactRetentionProcessor`.
- Os testes JPA e unitarios validam consentimento ativo, consentimento ausente, revogacao antiga e resiliencia a falha parcial.

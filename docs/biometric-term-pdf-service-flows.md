# Fluxos visuais — BiometricTermPdfService

Fluxo por método com imagem dedicada para cada operação do serviço.

## `generateConsentTerm`

![Fluxo biometric-term-pdf.generateConsentTerm](./flows/biometric-term-pdf/generateConsentTerm.svg)

- Receber dados de colaborador e empresa.
- Montar texto do termo com IP/User-Agent.
- Renderizar PDF de consentimento.
- Retornar bytes do termo.

# Fluxos visuais — TechnicalCertificateService

Fluxo por método com imagem dedicada para cada operação do serviço.

## `generateCertificate`

![Fluxo technical-certificate.generateCertificate](./flows/technical-certificate/generateCertificate.svg)

- Buscar empresa por ID.
- Delegar geração para TechnicalCertificatePdfService.
- Retornar bytes do certificado.

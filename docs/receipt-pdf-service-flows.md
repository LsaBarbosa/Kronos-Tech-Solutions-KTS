# Fluxos visuais — ReceiptPdfService

Fluxo por método com imagem dedicada para cada operação do serviço.

## `generateReceipt`

![Fluxo receipt-pdf.generateReceipt](./flows/receipt-pdf/generateReceipt.svg)

- Receber dados da empresa/colaborador/marcação.
- Montar template do comprovante.
- Renderizar PDF.
- Retornar bytes do comprovante.

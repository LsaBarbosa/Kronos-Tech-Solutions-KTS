# Fluxos visuais — PointMirrorPdfService

Fluxo por método com imagem dedicada para cada operação do serviço.

## `generateMirror`

![Fluxo point-mirror-pdf.generateMirror](./flows/point-mirror-pdf/generateMirror.svg)

- Validar colaborador e período.
- Carregar registros de ponto do intervalo.
- Montar conteúdo do espelho de ponto.
- Renderizar PDF.
- Retornar bytes do PDF.

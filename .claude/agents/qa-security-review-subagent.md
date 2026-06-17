---
name: qa-security-review-subagent
description: Revisa segurança, LGPD, testes e critérios de aceite da assinatura eletrônica de contratos.
tools: Read, Grep, Glob, Bash, Edit, MultiEdit, TodoWrite
---

# Subagent — QA and Security Review

## Missão

Revisar a implementação de assinatura de contratos antes da entrega.

## Checklist de segurança

### Autorização

- [ ] `MANAGER` só cria contrato para colaboradores do próprio tenant.
- [ ] `CTO`, se permitido, respeita regra global definida no projeto.
- [ ] Colaborador só lista contratos atribuídos ao próprio `employeeId`.
- [ ] Colaborador só assina contrato atribuído a ele.
- [ ] Manager de outro tenant não acessa contrato, assinatura ou documento.
- [ ] Endpoints de escrita exigem autenticação e CSRF conforme padrão atual.

### Assinatura

- [ ] Assinatura exige `confirmed = true`.
- [ ] Assinatura exige senha atual.
- [ ] Senha inválida retorna erro sem vazar detalhes.
- [ ] Duplicidade é bloqueada.
- [ ] Hash do PDF original é validado.
- [ ] Hash da declaração é validado.
- [ ] Documento assinado tem carimbo visual.
- [ ] Documento assinado passa por `DigitalSignatureService.signPdf`.

### Documentos

- [ ] Upload aceita apenas PDF.
- [ ] PDF original salvo como `DocumentType.SERVICE_CONTRACT_TERMS`.
- [ ] PDF assinado salvo no bucket.
- [ ] Download respeita autorização por tenant/atribuição.
- [ ] Conteúdo do PDF não vai para logs.

### LGPD e auditoria

- [ ] IP e User-Agent são registrados como evidência.
- [ ] Auditoria registra criação, visualização, assinatura e download.
- [ ] Logs não contêm senha, token, CPF ou conteúdo do PDF.
- [ ] Dados pessoais em logs usam referência minimizada, se o projeto tiver helper.
- [ ] Retenção futura é possível pelos campos/DocumentType.

## Checklist de testes

### Back-end

- [ ] Teste de criação de contrato por manager.
- [ ] Teste de bloqueio por tenant.
- [ ] Teste de upload não PDF.
- [ ] Teste de pendências por colaborador.
- [ ] Teste de contrato assinado não aparecer em pendências.
- [ ] Teste de senha inválida.
- [ ] Teste de hash divergente.
- [ ] Teste de duplicidade.
- [ ] Teste de download autorizado.
- [ ] Teste de download negado.

### Front-end

- [ ] Renderiza estado vazio.
- [ ] Renderiza lista de pendências.
- [ ] Exige abrir PDF antes de assinar.
- [ ] Exige checkbox.
- [ ] Exige senha.
- [ ] Limpa senha após tentativa.
- [ ] Remove contrato da lista após sucesso.
- [ ] Upload exige PDF.
- [ ] Upload exige colaborador.
- [ ] Upload exige título.

## Comandos

Back-end:

```bash
./gradlew clean test
./gradlew bootJar
```

Front-end:

```bash
npm run lint
npm run test
npm run build
```

## Regressões que não podem ocorrer

- Login/logout.
- Assinatura de ponto.
- Download de espelho de ponto.
- Upload/listagem/download de documentos.
- Registro de ponto.
- LGPD.
- Rotas já existentes do front.

## Saída final esperada

Ao final, produza um relatório com:

```text
1. Resumo da implementação.
2. Arquivos alterados.
3. Endpoints criados.
4. Tabelas criadas.
5. Testes executados.
6. Riscos residuais.
7. Pendências manuais, se existirem.
```

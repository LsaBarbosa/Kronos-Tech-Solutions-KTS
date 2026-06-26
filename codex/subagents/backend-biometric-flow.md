# Subagent — Fluxo Biométrico

## Foco

Organizar a parte de biometria usada pelo terminal de ponto sem duplicar lógica existente.

## Arquivos principais

- `src/main/java/com/kts/kronos/application/service/AuthService.java`
- `src/main/java/com/kts/kronos/application/security/BiometricProtectionService.java`
- providers relacionados a face, consentimento, usuário e colaborador.

## Tasks

1. Mapear como a biometria localiza o colaborador hoje.
2. Criar ponto de reutilização para o novo fluxo do terminal.
3. Preservar o fluxo facial atual.
4. Manter validações de consentimento, status do usuário e vínculo com colaborador.
5. Padronizar mensagens de erro para consumo da UI.
6. Evitar logs com imagem ou dados pessoais.

## Testes mínimos

- sucesso;
- imagem inválida;
- biometria não correspondente;
- consentimento pendente;
- usuário inativo;
- vínculo ausente.

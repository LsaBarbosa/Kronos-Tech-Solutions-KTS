# Database Migrations — Flyway

## Objetivo

Documentar a política de migrações de banco do Kronos usando Flyway de forma prática e segura.

## Convenção de nomes

A convenção recomendada é:

```text
V{numero}__descricao_curta.sql
```

Exemplo:

```text
V42__create_lgpd_request_tables.sql
```

## Ordem de execução

- O Flyway executa migrations em ordem de versão.
- Cada migration deve ser incremental.
- Não reutilizar número já empregado para nova mudança.
- A ordem deve ser previsível em todos os ambientes.

## Regras para criar migrations

- Não editar migration já aplicada em ambiente compartilhado.
- Criar nova migration para qualquer ajuste incremental.
- Manter scripts pequenos, legíveis e reversíveis quando possível.
- Descrever claramente o propósito no nome do arquivo.
- Validar impacto em índices, locks, tamanho de tabela e compatibilidade com dados existentes.

## Regras para alterar tabelas com dados sensíveis

Quando a mudança tocar dados pessoais, sensíveis ou evidências legais:

- documentar impacto no PR;
- revisar retenção, minimização e mascaramento;
- evitar `UPDATE` destrutivo sem estratégia de validação;
- revisar reflexos em exportação LGPD, auditoria e incidentes;
- alinhar com jurídico e operação quando houver risco regulatório.

## Rollback

Flyway não deve depender de edição retroativa de scripts já executados.

Estratégias preferidas:

- migration corretiva para seguir adiante;
- restore de backup quando a mudança for irreversível;
- feature flag ou rollout controlado quando aplicável.

Evitar `DROP` destrutivo sem plano de backup e reversão.

## Validação local

Comandos úteis:

```bash
./gradlew flywayInfo
./gradlew flywayValidate
./gradlew test --tests "*FlywayMigrationTest"
```

Executar essas validações antes de abrir PR para mudanças de esquema.

## Validação em produção

- aplicar somente artefatos revisados;
- validar backup antes da janela;
- acompanhar logs da aplicação e do banco;
- confirmar health checks após a execução;
- registrar rollback pronto caso a migration falhe.

## Boas práticas

- usar nomes objetivos;
- preferir alterações incrementais e compatíveis;
- não esconder transformação de dados complexa dentro de script pouco descrito;
- revisar compatibilidade entre PostgreSQL de produção e H2/testes quando houver diferença de função SQL;
- considerar volume, locks e tempo de execução.

## Checklist de PR

- [ ] O nome segue `V{numero}__descricao_curta.sql`.
- [ ] Nenhuma migration já aplicada foi editada.
- [ ] O impacto em dados pessoais ou sensíveis foi descrito.
- [ ] Há plano de rollback ou mitigação.
- [ ] A migration foi validada localmente.
- [ ] O PR informa riscos operacionais relevantes.

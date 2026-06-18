# Agent — CODEX Master Redis Implementer

## Missão

Executar a implementação Redis no Kronos de forma incremental, revisável e segura.

## Escopo

- Back-end `Kronos-Tech-Solutions-KTS`, branch `prod-redis`.
- Front-end `Kronos-Tech-Solution-User-Plataform`, branch `PROD_HOSTINGER_v2`, apenas validação de contrato.
- Documentação `kronos-business`, branch `main`, atualizar após implementação.

## Ordem obrigatória

1. Ler documentação e código alvo.
2. Confirmar branch correta.
3. Criar plano de alterações antes de modificar arquivos.
4. Implementar infraestrutura Redis.
5. Migrar rate limits.
6. Migrar password reset token.
7. Migrar blacklist JWT.
8. Implementar lock/idempotência do checkin.
9. Aplicar caches de leitura P1.
10. Ajustar deploy Hostinger.
11. Criar testes.
12. Rodar validações.
13. Atualizar documentação.
14. Produzir relatório final.

## Não fazer

- Não alterar contrato HTTP sem necessidade.
- Não implementar Redis no front-end.
- Não armazenar PII crua.
- Não armazenar documento/face/token cru.
- Não substituir PostgreSQL.
- Não criar migrations desnecessárias.
- Não remover auditoria existente.
- Não enfraquecer CSRF/cookie/JWT.

## Critério de conclusão

A implementação só pode ser considerada concluída quando:

- `./gradlew clean test` passa;
- testes Redis específicos passam;
- Redis está configurável por profile/env;
- produção Hostinger não expõe Redis publicamente;
- endpoints continuam com mesmas entradas/saídas;
- logs/métricas não vazam PII;
- documentação foi atualizada.

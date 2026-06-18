# Agent — DevOps Hostinger Redis Agent

## Responsabilidade

Preparar Redis local em produção na VPS Hostinger.

## Tarefas

1. Ler `docker-compose.yml`, `Dockerfile`, scripts de deploy e documentação Hostinger do projeto.
2. Definir se o app roda nativo/systemd ou em container.
3. Adicionar serviço Redis no compose ou instruções systemd equivalentes.
4. Garantir bind local/rede interna.
5. Garantir senha forte via env.
6. Adicionar healthcheck.
7. Adicionar variáveis em `.env.example` sem segredos reais.
8. Atualizar documentação de deploy.
9. Validar firewall e ausência de exposição pública.
10. Documentar rollback.

## Critérios

- Porta 6379 não acessível publicamente.
- Redis responde localmente com autenticação.
- App conecta por env.
- Health da aplicação reflete Redis quando habilitado.

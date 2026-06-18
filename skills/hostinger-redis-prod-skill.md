# Skill — Redis local na VPS Hostinger em produção

## Objetivo

Rodar Redis localmente dentro da VPS Hostinger em produção para o back-end Kronos, sem expor Redis publicamente.

## Topologias aceitas

### Opção A — App e Redis em Docker Compose

Preferida quando o back-end também roda em container.

```yaml
services:
  kronos-redis:
    image: redis:7.4-alpine
    container_name: kronos-redis
    restart: unless-stopped
    command:
      - redis-server
      - --appendonly
      - "yes"
      - --requirepass
      - ${REDIS_PASSWORD}
      - --protected-mode
      - "yes"
      - --maxmemory
      - ${REDIS_MAXMEMORY:-256mb}
      - --maxmemory-policy
      - noeviction
    volumes:
      - kronos_redis_data:/data
    networks:
      - kronos-network
    healthcheck:
      test: ["CMD", "redis-cli", "-a", "${REDIS_PASSWORD}", "ping"]
      interval: 10s
      timeout: 3s
      retries: 5

volumes:
  kronos_redis_data:

networks:
  kronos-network:
    driver: bridge
```

Config do app:

```env
REDIS_HOST=kronos-redis
REDIS_PORT=6379
REDIS_PASSWORD=senha-forte
REDIS_ENABLED=true
```

### Opção B — App nativo/systemd e Redis em Docker

Usar porta vinculada somente em localhost:

```yaml
ports:
  - "127.0.0.1:6379:6379"
```

Config do app:

```env
REDIS_HOST=127.0.0.1
REDIS_PORT=6379
REDIS_PASSWORD=senha-forte
REDIS_ENABLED=true
```

## Variáveis em `/etc/kronos/kronos.env`

Adicionar:

```env
REDIS_ENABLED=true
REDIS_HOST=127.0.0.1
REDIS_PORT=6379
REDIS_PASSWORD=trocar-por-senha-forte
REDIS_DATABASE=0
REDIS_TIMEOUT=2s
REDIS_KEY_PREFIX=kronos
REDIS_KEY_HASH_SECRET=trocar-por-segredo-forte
REDIS_SECURITY_FAIL_CLOSED=true
REDIS_CACHE_FAIL_OPEN=true
REDIS_CACHE_DEFAULT_TTL=5m
REDIS_CHECKIN_LOCK_TTL_SECONDS=60
REDIS_PASSWORD_RESET_TTL_MINUTES=15
REDIS_DASHBOARD_SUMMARY_TTL_SECONDS=60
REDIS_OWN_PROFILE_TTL_SECONDS=300
REDIS_RECORDS_TODAY_TTL_SECONDS=30
REDIS_RECORDS_RECENT_TTL_SECONDS=60
REDIS_GEOLOCATION_TTL_DAYS=7
```

## Firewall

Redis não pode ficar público.

Com UFW:

```bash
sudo ufw deny 6379/tcp
sudo ufw status verbose
```

Se usar Docker com bind `127.0.0.1`, ainda assim manter firewall bloqueando acesso externo.

## Validação operacional

```bash
redis-cli -a "$REDIS_PASSWORD" -h 127.0.0.1 -p 6379 ping
# esperado: PONG
```

Validar app:

```bash
curl -k https://api.seu-dominio.com/actuator/health
curl -k https://api.seu-dominio.com/actuator/prometheus | grep -i redis
```

## Regras de produção

- não commitar senha Redis;
- não usar senha fraca;
- não expor `6379` para internet;
- usar `restart: unless-stopped`;
- usar volume persistente para AOF quando blacklist/reset token precisam sobreviver a restart curto;
- monitorar memória;
- manter TTL em todas as chaves;
- documentar comando de rollback.

## Rollback

Rollback seguro:

1. alterar `REDIS_ENABLED=false` se a implementação tiver fallback;
2. reiniciar aplicação;
3. manter Redis rodando para não perder diagnóstico;
4. revisar logs/métricas;
5. corrigir e reabilitar.

Para fluxos críticos sem fallback, rollback deve ser por deploy da versão anterior.

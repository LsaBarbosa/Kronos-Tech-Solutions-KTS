# Riscos aceitos — Kronos

## RA-001 — Liveness biométrico desativado

| Campo | Valor |
|---|---|
| Risco | Login/validação facial com liveness desativado |
| Severidade inerente | Alta |
| Configuração | BIOMETRIC_LIVENESS_REQUIRED=false |
| Decisão | Aceito temporariamente |
| Justificativa | Decisão operacional atual do projeto |
| Responsável | Lucas SantAnna (CEO/CTO) |
| Data de Aceitação | 2026-06-21 |
| Assinado por | Lucas SantAnna (CEO/CTO) |
| Motivo da Aceitação | Decisão financeira operacional |
| Prazo de revisão | 2026-08-19 |
| Controles compensatórios | rate limit facial, consentimento biométrico, auditoria, logs minimizados, bloqueio por tentativas, alertas de falha, proteção de IP/dispositivo |
| Plano futuro | Reavaliar ativação de liveness após validação técnica e de UX |

### Observações operacionais

- O valor `false` não representa aprovação de segurança plena.
- O fluxo biométrico permanece ativo com controles compensatórios.
- A revisão do risco deve ocorrer antes do prazo acima ou em caso de mudança relevante no fluxo de autenticação facial.

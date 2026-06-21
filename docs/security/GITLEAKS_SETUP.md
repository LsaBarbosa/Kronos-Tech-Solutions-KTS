# Gitleaks Setup — Kronos Tech Solutions

## Overview

**Gitleaks** é uma ferramenta para detectar secrets (senhas, chaves, tokens) no histórico Git. O Kronos está configurado para rodá-lo automaticamente em **GitHub Actions (CI/CD)** e opcionalmente **localmente via pre-commit hooks**.

## Configuração atual

### GitHub Actions (CI/CD)
- ✅ **Back-end:** Roda em toda push/PR na branch homolog
- ✅ **Front-end:** Roda em toda push/PR na branch homolog
- 📋 **Config:** `.gitleaks.toml` no root de cada repo
- 📊 **Reports:** Uploaded como artifacts em caso de falha

### Local (Opcional)
- 📌 **Pre-commit hook:** `.husky/pre-commit` (detecta secrets antes do commit)
- 🔧 **Requer:** gitleaks instalado + husky configurado

---

## Instalação local (opcional)

### 1. Instalar Gitleaks

**Via Snap (Ubuntu/Debian):**
```bash
sudo snap install gitleaks
```

**Via Homebrew (macOS):**
```bash
brew install gitleaks
```

**Via download direto:**
```bash
# Download latest release
wget https://github.com/gitleaks/gitleaks/releases/download/v8.24.2/gitleaks-linux-x64
chmod +x gitleaks-linux-x64
sudo mv gitleaks-linux-x64 /usr/local/bin/gitleaks

# Verificar
gitleaks version
```

### 2. Configurar pre-commit hook (opcional)

Se usar **Husky** para git hooks:

```bash
# No diretório do projeto
npm install husky --save-dev
npx husky install
npx husky add .husky/pre-commit "bash .husky/pre-commit"
```

Ou simplesmente dar permissão ao hook:
```bash
chmod +x .husky/pre-commit
```

---

## Uso

### No GitHub Actions (automático)
Não requer ação. A cada push/PR:
1. Gitleaks roda automaticamente
2. Detecta secrets no histórico completo (`--all`)
3. Falha o build se encontrar secrets
4. Artifacts com report são uploaded

### Localmente (manual)

**Escanear histórico completo:**
```bash
gitleaks detect --source . --config .gitleaks.toml --redact
```

**Escanear apenas staged files (pre-commit):**
```bash
gitleaks protect --staged --config .gitleaks.toml
```

**Gerar relatório JSON:**
```bash
gitleaks detect \
  --source . \
  --config .gitleaks.toml \
  --report-format json \
  --report-path gitleaks-report.json
```

---

## Configuração (.gitleaks.toml)

O arquivo `.gitleaks.toml` define:

1. **Rules customizadas** para Kronos:
   - `JWT_SECRET`
   - `AWS_SECRET_ACCESS_KEY`
   - `DIGITAL_CERTIFICATE_PASSWORD`
   - `MAIL_PASSWORD`
   - Private keys (`-----BEGIN PRIVATE KEY`)

2. **Allowlist** (arquivos/padrões ignorados):
   - Testes (`src/test/`, `*.test.ts`)
   - Exemplos (`.env.example`)
   - Documentação

---

## Secrets detectados (severidade)

| Secret | Severidade | Padrão |
|--------|-----------|--------|
| JWT_SECRET | CRITICAL | `JWT_SECRET=...` |
| AWS_SECRET_ACCESS_KEY | CRITICAL | `AWS_SECRET_ACCESS_KEY=...` |
| DIGITAL_CERTIFICATE_PASSWORD | CRITICAL | `DIGITAL_CERTIFICATE_PASSWORD=...` |
| MAIL_PASSWORD | HIGH | `MAIL_PASSWORD=...` |
| Private Keys | CRITICAL | `-----BEGIN PRIVATE KEY` |

---

## Troubleshooting

### "gitleaks: command not found"
→ Instale gitleaks (ver Instalação acima)

### "Token CSRF ausente ou inválido"
→ Gitleaks pode estar detectando padrões em testes. Adicione ao allowlist em `.gitleaks.toml`

### "Pre-commit hook falhou"
→ Verificar relatório: `gitleaks detect --source . --redact --report-format json`

### Ignorar um secret específico
Adicione ao `.gitleaks.toml`:
```toml
[[allowlist]]
description = "Ignore specific test pattern"
paths = ["docs/example.md"]
regexes = ["test_jwt_token_123"]
```

---

## CI/CD Pipeline

Fluxo de detecção:

```
git push
    ↓
GitHub Actions triggered
    ↓
Gitleaks job starts
    ↓
gitleaks detect --all --config .gitleaks.toml
    ↓
Success? ✅ Continue to next job
Failed?  ❌ Block merge + notify
    ↓
Report uploaded as artifact
```

---

## References

- **Gitleaks:** https://github.com/gitleaks/gitleaks
- **Docs:** https://gitleaks.io/
- **Rules:** https://github.com/gitleaks/gitleaks/blob/master/config/default.toml

---

**Last updated:** 2026-06-21
**Maintained by:** Security Team

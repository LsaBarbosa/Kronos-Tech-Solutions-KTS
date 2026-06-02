# 🕐 Kronos - Local Claude Code Configuration (Example)

This file is a **template** for developers to create their own local configuration.

**Do NOT commit this file. Create your own `CLAUDE.local.md` in your local environment.**

---

## Purpose

Local configuration allows you to:
- Override default settings per developer
- Add personal preferences (model, skills)
- Configure local-only hooks
- Set up environment-specific paths

---

## How to Use

1. **Create your local file:**
   ```bash
   cp CLAUDE.local.example.md CLAUDE.local.md
   ```

2. **Edit `CLAUDE.local.md` with your preferences:**
   ```bash
   nano CLAUDE.local.md
   ```

3. **Claude Code will load both:**
   - `CLAUDE.md` (shared, committed)
   - `CLAUDE.local.md` (personal, in .gitignore)

4. **Your settings override defaults**

---

## Example Local Configuration

### Model Preferences
```markdown
## Model Override
Use Claude Opus for complex tasks (if available):
- `/fast` for faster iterations
- Default Haiku for quick reviews
```

### Local Skills
```markdown
## Additional Local Skills
- `kronos-local-test`: Run full test suite with coverage report
- `kronos-local-build`: Build JAR and run locally with Docker
```

### Environment Paths
```markdown
## Local Paths
- Database: postgres://localhost:5432/kronos_dev
- Logs: /tmp/kronos.log
- Metrics: http://localhost:3000 (Grafana)
```

### Permissions & Hooks
```markdown
## Additional Permissions (Local Only)
- Allow Docker compose locally
- Allow psql for local database

## Hooks
on_start: echo "🕐 Welcome back to Kronos development!"
on_change: ./gradlew checkstyleMain  # Auto-lint on changes
```

### Feature Flags
```markdown
## Feature Flags (for this session)
- ENABLE_LGPD_STRICT: true  # Strict LGPD checks
- DEBUG_BIOMETRIC: false    # Don't log biometric scores
- LOCAL_MOCKING: true       # Mock external services locally
```

---

## Template: Full Local Config

```markdown
# 🕐 Kronos - Local Configuration

## Developer Info
Name: Your Name
Email: your.email@company.com
Role: Backend/Frontend/Full-stack
Local Environment: macOS / Linux / Windows

## Model & Performance
Model: claude-opus-4-7  (for complex tasks)
Default: claude-haiku-4-5 (for quick reviews)
Fast mode: Enabled with `/fast` command

## Local Database
Host: localhost
Port: 5432
Database: kronos_dev
User: kronos_dev
Connection string: SAVED_LOCALLY_NOT_HERE

## Services
- API: http://localhost:8080
- Frontend: http://localhost:3000 (Vite dev server)
- Database: psql (local PostgreSQL)
- Grafana: http://localhost:3000
- Prometheus: http://localhost:9090

## Additional Skills
- kronos-local-docker: Build and run locally
- kronos-local-migrations: Test database migrations
- kronos-local-e2e: Run end-to-end tests

## Permissions Overrides
Allow locally:
- docker-compose up/down
- psql commands
- npm install (for frontend)

## Hooks
on_start: echo "✅ Kronos ready - run ./gradlew bootRun to start API"
on_test: ./gradlew test --info

## Preferences
- Skip API tests on each save: false
- Auto-format code: true
- Strict LGPD checks: true
- Mock external services: false (use real S3/Email in staging)

## Notes
- Always run ./gradlew test before pushing
- Use Flyway migrations for schema changes
- Check git status before committing sensitive data
```

---

## What NOT to Put in Local Config

❌ Real secrets (database passwords, API keys, JWT secrets)
❌ Production credentials
❌ Personal tokens or SSH keys
❌ Email addresses (use role/name instead)
❌ Committed code (keep it local only)

---

## GitIgnore Protection

The repository `.gitignore` includes:
```
CLAUDE.local.md
.claude/settings.local.json
.env
.env.*
secrets/
*.pem
*.key
*.p12
*.jks
```

So your local config is **never accidentally committed**.

---

## Troubleshooting

**Q: Claude Code is not loading my local settings**
A: Ensure file is named exactly `CLAUDE.local.md` (case-sensitive on Linux/macOS)

**Q: My local config conflicts with shared `CLAUDE.md`**
A: Local config is merged with shared config. Local values override shared defaults.

**Q: I want to reset to defaults**
A: Delete `CLAUDE.local.md` and restart Claude Code.

---

## Reference

- Shared config: `CLAUDE.md`
- Local config: `CLAUDE.local.md` (you create this)
- Project settings: `.claude/settings.json`
- Local settings: `.claude/settings.local.json` (ignored)
- Skills: `.claude/skills/` (shared, committed)

---

**Created:** 2026-06-02
**Version:** 1.0
**For:** Kronos SaaS Platform

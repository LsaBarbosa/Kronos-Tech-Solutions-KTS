#!/usr/bin/env bash
# deploy.sh — Kronos deployment script
# Usage: ./scripts/deploy.sh [--backend] [--frontend] [--all] [--skip-build]
# Default (no args): deploys both backend and frontend

set -euo pipefail

# ─── Paths ────────────────────────────────────────────────────────────────────
BACKEND_DIR="$(cd "$(dirname "$0")/.." && pwd)"
FRONTEND_DIR="/home/deploy/apps/Kronos-Tech-Solution-User-Plataform"
JAR_DEST="/opt/kronos/app/kronos-backend.jar"
JAR_BACKUP="/opt/kronos/app/kronos-backend.jar.bak"
JAR_STAGING="/opt/kronos/app/kronos-backend-new.jar"
FRONTEND_DEST="/var/www/kronos-platform"
HEALTH_URL="http://127.0.0.1:8081/actuator/health"
SERVICE="kronos-backend"
HEALTH_TIMEOUT=120   # seconds to wait for Spring Boot to be UP

# ─── Colors ───────────────────────────────────────────────────────────────────
RED='\033[0;31m'; GREEN='\033[0;32m'; YELLOW='\033[1;33m'
BLUE='\033[0;34m'; BOLD='\033[1m'; NC='\033[0m'

log()  { echo -e "${BLUE}[deploy]${NC} $*"; }
ok()   { echo -e "${GREEN}[ok]${NC} $*"; }
warn() { echo -e "${YELLOW}[warn]${NC} $*"; }
fail() { echo -e "${RED}[fail]${NC} $*" >&2; }

# ─── Argument parsing ─────────────────────────────────────────────────────────
DEPLOY_BACKEND=false
DEPLOY_FRONTEND=false
SKIP_BUILD=false

for arg in "$@"; do
  case "$arg" in
    --backend|-b)   DEPLOY_BACKEND=true ;;
    --frontend|-f)  DEPLOY_FRONTEND=true ;;
    --all|-a)       DEPLOY_BACKEND=true; DEPLOY_FRONTEND=true ;;
    --skip-build)   SKIP_BUILD=true ;;
    --help|-h)
      echo "Usage: $0 [--backend|-b] [--frontend|-f] [--all|-a] [--skip-build]"
      echo "  No flags = deploy both backend and frontend"
      exit 0 ;;
    *)
      fail "Unknown argument: $arg"
      exit 1 ;;
  esac
done

# Default: deploy both
if ! $DEPLOY_BACKEND && ! $DEPLOY_FRONTEND; then
  DEPLOY_BACKEND=true
  DEPLOY_FRONTEND=true
fi

echo -e "\n${BOLD}═══════════════════════════════════════${NC}"
echo -e "${BOLD}   Kronos Deploy — $(date '+%Y-%m-%d %H:%M:%S')${NC}"
echo -e "${BOLD}═══════════════════════════════════════${NC}\n"

STARTED_AT=$(date +%s)

# ─── Backend deploy ───────────────────────────────────────────────────────────
deploy_backend() {
  echo -e "\n${BOLD}▶ Backend${NC}"
  cd "$BACKEND_DIR"

  # 1. Build
  if ! $SKIP_BUILD; then
    log "Building JAR (tests skipped for deploy)..."
    ./gradlew clean build -x test -q
    ok "Build complete"
  else
    warn "Skipping build (--skip-build)"
  fi

  # Find built JAR
  BUILT_JAR=$(find "$BACKEND_DIR/build/libs" -name "*.jar" ! -name "*plain*" 2>/dev/null | head -1)
  if [[ -z "$BUILT_JAR" ]]; then
    fail "No JAR found in build/libs/. Run without --skip-build or build manually."
    exit 1
  fi
  log "JAR to deploy: $(basename "$BUILT_JAR") ($(du -h "$BUILT_JAR" | cut -f1))"

  # 2. Stage new JAR before touching the service
  log "Copying JAR to staging..."
  cp "$BUILT_JAR" "$JAR_STAGING"
  ok "Staged at $JAR_STAGING"

  # 3. Backup current JAR
  if [[ -f "$JAR_DEST" ]]; then
    cp "$JAR_DEST" "$JAR_BACKUP"
    ok "Current JAR backed up to $(basename "$JAR_BACKUP")"
  fi

  # 4. Stop service
  log "Stopping $SERVICE..."
  sudo systemctl stop "$SERVICE"
  ok "Service stopped"

  # 5. Atomic swap (rename = atomic on same filesystem)
  mv "$JAR_STAGING" "$JAR_DEST"
  ok "JAR replaced atomically"

  # 6. Start service
  log "Starting $SERVICE..."
  sudo systemctl start "$SERVICE"

  # 7. Wait for health check
  log "Waiting for health check (max ${HEALTH_TIMEOUT}s)..."
  ELAPSED=0
  while true; do
    HTTP_STATUS=$(curl -s -o /dev/null -w "%{http_code}" "$HEALTH_URL" 2>/dev/null || true)
    if [[ "$HTTP_STATUS" == "200" ]]; then
      HEALTH=$(curl -s "$HEALTH_URL" 2>/dev/null | python3 -c "import sys,json; d=json.load(sys.stdin); print(d.get('status','?'))" 2>/dev/null || echo "?")
      if [[ "$HEALTH" == "UP" ]]; then
        ok "Backend is UP (${ELAPSED}s)"
        break
      fi
    fi

    if (( ELAPSED >= HEALTH_TIMEOUT )); then
      fail "Health check timed out after ${HEALTH_TIMEOUT}s — initiating rollback"
      _rollback_backend
      exit 1
    fi

    sleep 3
    ELAPSED=$(( ELAPSED + 3 ))
    echo -ne "  waiting... ${ELAPSED}s\r"
  done

  # 8. Cleanup staging (if it survived)
  rm -f "$JAR_STAGING"
}

_rollback_backend() {
  warn "Rolling back to previous JAR..."
  sudo systemctl stop "$SERVICE" 2>/dev/null || true
  if [[ -f "$JAR_BACKUP" ]]; then
    mv "$JAR_BACKUP" "$JAR_DEST"
    sudo systemctl start "$SERVICE"
    warn "Rollback complete — previous JAR restored"
  else
    fail "No backup JAR found — manual intervention required"
  fi
}

# ─── Frontend deploy ──────────────────────────────────────────────────────────
deploy_frontend() {
  echo -e "\n${BOLD}▶ Frontend${NC}"
  cd "$FRONTEND_DIR"

  # 1. Build
  if ! $SKIP_BUILD; then
    log "Installing dependencies..."
    npm ci --prefer-offline -q 2>/dev/null || npm ci -q
    log "Building production bundle..."
    npm run build -- --mode production
    ok "Build complete"
  else
    warn "Skipping build (--skip-build)"
  fi

  if [[ ! -d "$FRONTEND_DIR/dist" ]]; then
    fail "dist/ not found. Build may have failed."
    exit 1
  fi

  # 2. Deploy to web root
  log "Deploying to $FRONTEND_DEST..."
  sudo rsync -a --delete \
    --exclude '.git' \
    "$FRONTEND_DIR/dist/" "$FRONTEND_DEST/"
  sudo chown -R www-data:www-data "$FRONTEND_DEST"
  ok "Files synced"

  # 3. Verify nginx config and reload
  log "Reloading nginx..."
  sudo nginx -t -q
  sudo systemctl reload nginx
  ok "Nginx reloaded"

  # 4. Spot-check the index.html is served
  HTTP_STATUS=$(curl -s -o /dev/null -w "%{http_code}" https://kronostechsolutions.com/ 2>/dev/null || true)
  if [[ "$HTTP_STATUS" == "200" ]]; then
    ok "Frontend responding: HTTP $HTTP_STATUS"
  else
    warn "Frontend responded HTTP $HTTP_STATUS — check nginx logs"
  fi
}

# ─── Run ──────────────────────────────────────────────────────────────────────
BACKEND_OK=false
FRONTEND_OK=false

if $DEPLOY_BACKEND; then
  if deploy_backend; then
    BACKEND_OK=true
  else
    fail "Backend deploy failed"
  fi
fi

if $DEPLOY_FRONTEND; then
  if deploy_frontend; then
    FRONTEND_OK=true
  else
    fail "Frontend deploy failed"
  fi
fi

# ─── Summary ──────────────────────────────────────────────────────────────────
ELAPSED=$(( $(date +%s) - STARTED_AT ))
echo -e "\n${BOLD}═══════════════════════════════════════${NC}"
echo -e "${BOLD}   Deploy Summary${NC}"
echo -e "${BOLD}═══════════════════════════════════════${NC}"

$DEPLOY_BACKEND  && { $BACKEND_OK  && echo -e "  Backend:  ${GREEN}✓ OK${NC}" || echo -e "  Backend:  ${RED}✗ FAILED${NC}"; }
$DEPLOY_FRONTEND && { $FRONTEND_OK && echo -e "  Frontend: ${GREEN}✓ OK${NC}" || echo -e "  Frontend: ${RED}✗ FAILED${NC}"; }
echo -e "  Tempo total: ${ELAPSED}s\n"

if ($DEPLOY_BACKEND && ! $BACKEND_OK) || ($DEPLOY_FRONTEND && ! $FRONTEND_OK); then
  exit 1
fi

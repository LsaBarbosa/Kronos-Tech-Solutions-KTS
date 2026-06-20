#!/usr/bin/env bash
set -Eeuo pipefail

ROOT_DIR="${1:-${SCAN_ROOT:-$(git rev-parse --show-toplevel)}}"
scanner="$ROOT_DIR/scripts/security/assert-no-secrets.sh"

tmp_repo="$(mktemp -d)"
cleanup() {
  rm -rf "$tmp_repo"
}
trap cleanup EXIT

cd "$tmp_repo"
git init -q
cat > .env.synthetic <<'EOF'
AWS_SECRET_ACCESS_KEY=THIS_IS_A_SYNTHETIC_TEST_SECRET_DO_NOT_USE_1234567890
JWT_SECRET=THIS_IS_A_SYNTHETIC_TEST_SECRET_DO_NOT_USE_1234567890
EOF
git add .env.synthetic

if bash "$scanner" "$tmp_repo"; then
  echo "[security] ERRO: scanner não detectou o segredo sintético" >&2
  exit 1
fi

echo "[security] scanner detectou o segredo sintético como esperado"

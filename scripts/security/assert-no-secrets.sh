#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)"
cd "$ROOT_DIR"

tmp_matches="$(mktemp)"
trap 'rm -f "$tmp_matches"' EXIT

trackable_files() {
  git ls-files -z \
    ':!:src/test/**' \
    ':!:**/*.test.ts' \
    ':!:**/*.test.tsx' \
    ':!:**/*.spec.ts' \
    ':!:**/*.spec.tsx' \
    ':!:segurança_auditoria.md' \
  | while IFS= read -r -d '' file; do
      case "$file" in
        .env*|*.env|*.yml|*.yaml|*.properties|*.xml|*.json|*.sh|Dockerfile|docker-compose*.yml|docker-compose*.yaml)
          printf '%s\0' "$file"
          ;;
      esac
    done
}

scan_pattern() {
  local label="$1"
  local pattern="$2"

  if trackable_files | xargs -0 grep -I -n -E "$pattern" >>"$tmp_matches" 2>/dev/null; then
    printf 'Potential secret pattern found: %s\n' "$label" >&2
  fi
}

scan_pattern "AWS access key" 'AWS_ACCESS_KEY_ID=(?!change-me|your-|example|test|dummy|placeholder|CHANGEME|<)'
scan_pattern "AWS secret access key" 'AWS_SECRET_ACCESS_KEY=(?!change-me|your-|example|test|dummy|placeholder|CHANGEME|<)'
scan_pattern "JWT secret" 'JWT_SECRET=(?!change-me|your-|example|test|dummy|placeholder|CHANGEME|<)'
scan_pattern "Mail password" 'MAIL_PASSWORD=(?!change-me|your-|example|test|dummy|placeholder|CHANGEME|<)'
scan_pattern "HERE API key" 'HERE_API_KEY=(?!change-me|your-|example|test|dummy|placeholder|CHANGEME|<)'
scan_pattern "Certificate password" 'DIGITAL_CERTIFICATE_PASSWORD=(?!change-me|your-|example|test|dummy|placeholder|CHANGEME|<)'
scan_pattern "LGPD log hash secret" 'LGPD_LOG_HASH_SECRET=(?!change-me|your-|example|test|dummy|placeholder|CHANGEME|<)'
scan_pattern "Private key material" 'PRIVATE KEY'
scan_pattern "Authorization bearer literal" 'Authorization:[[:space:]]*Bearer[[:space:]]+[A-Za-z0-9._~+/=-]{16,}'
scan_pattern "Literal password assignment" "(^|[[:space:]])password[[:space:]]*=[[:space:]]*[\"']?[A-Za-z0-9!@#$%^&*()_+=./:-]{8,}"

if [[ -s "$tmp_matches" ]]; then
  printf 'Sanitized findings:\n' >&2
  cut -d: -f1,2 "$tmp_matches" | sort -u >&2
  exit 1
fi

printf 'No tracked secret patterns detected.\n'

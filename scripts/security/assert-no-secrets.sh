#!/usr/bin/env bash
set -Eeuo pipefail

ROOT_DIR="${1:-${SCAN_ROOT:-$(git rev-parse --show-toplevel)}}"
cd "$ROOT_DIR"

if command -v rg >/dev/null 2>&1; then
  SEARCH_CMD=(rg --pcre2 --hidden --no-ignore-vcs --with-filename --line-number --color never --no-messages)
elif grep -P '' </dev/null >/dev/null 2>&1; then
  SEARCH_CMD=(grep -PIn)
else
  echo "[security] ERRO: instale ripgrep com PCRE2 ou grep com suporte a -P" >&2
  exit 2
fi

tmp_files="$(mktemp)"
tmp_matches="$(mktemp)"
trap 'rm -f "$tmp_files" "$tmp_matches"' EXIT

if [[ -n "${ASSERT_NO_SECRETS_EXTRA_FILES:-}" ]]; then
  while IFS= read -r extra_file; do
    [[ -n "$extra_file" ]] && printf '%s
' "$extra_file" >>"$tmp_files"
  done < <(printf '%s' "$ASSERT_NO_SECRETS_EXTRA_FILES" | tr ':' '
')
fi

git ls-files -z   ':!:src/test/**'   ':!:**/*.test.ts'   ':!:**/*.test.tsx'   ':!:**/*.spec.ts'   ':!:**/*.spec.tsx'   ':!:segurança_auditoria.md'   ':!:scripts/security/assert-no-secrets.sh'   ':!:scripts/security/test-assert-no-secrets.sh'   ':!:patch_blueprints/**' | while IFS= read -r -d '' file; do
  case "$file" in
    .env*|*.env|*.yml|*.yaml|*.properties|*.xml|*.json|*.sh|Dockerfile|docker-compose*.yml|docker-compose*.yaml)
      printf '%s
' "$file" >>"$tmp_files"
      ;;
  esac
done

if [[ ! -s "$tmp_files" ]]; then
  printf 'No tracked secret patterns detected.
'
  exit 0
fi

patterns=(
  "AWS_ACCESS_KEY_ID[[:space:]]*=[[:space:]]*([^[:space:]#]+)"
  "AWS_SECRET_ACCESS_KEY[[:space:]]*=[[:space:]]*([^[:space:]#]+)"
  "JWT_SECRET[[:space:]]*=[[:space:]]*([^[:space:]#]+)"
  "MAIL_PASSWORD[[:space:]]*=[[:space:]]*([^[:space:]#]+)"
  "HERE_API_KEY[[:space:]]*=[[:space:]]*([^[:space:]#]+)"
  "DIGITAL_CERTIFICATE_PASSWORD[[:space:]]*=[[:space:]]*([^[:space:]#]+)"
  "LGPD_LOG_HASH_SECRET[[:space:]]*=[[:space:]]*([^[:space:]#]+)"
  "Authorization:[[:space:]]*Bearer[[:space:]]+[A-Za-z0-9._~+/=-]{16,}"
  "PRIVATE KEY"
)

placeholder_pattern='^((change-me|CHANGE_ME|changeme|example|placeholder|<set-me>|\$\{[A-Z0-9_]+\})([-_].+)?)$'

scan_file() {
  local file="$1"
  local pattern="$2"
  "${SEARCH_CMD[@]}" "$pattern" "$file"
}

report_match() {
  local file="$1"
  local line="$2"
  local label="$3"
  printf '%s:%s %s
' "$file" "$line" "$label" >>"$tmp_matches"
}

while IFS= read -r file; do
  for pattern in "${patterns[@]}"; do
    set +e
    output=$(scan_file "$file" "$pattern" 2>/dev/null)
    status=$?
    set -e

    if [[ $status -eq 0 ]]; then
      case "$pattern" in
        AWS_ACCESS_KEY_ID*)
          label="AWS_ACCESS_KEY_ID"
          ;;
        AWS_SECRET_ACCESS_KEY*)
          label="AWS_SECRET_ACCESS_KEY"
          ;;
        JWT_SECRET*)
          label="JWT_SECRET"
          ;;
        MAIL_PASSWORD*)
          label="MAIL_PASSWORD"
          ;;
        HERE_API_KEY*)
          label="HERE_API_KEY"
          ;;
        DIGITAL_CERTIFICATE_PASSWORD*)
          label="DIGITAL_CERTIFICATE_PASSWORD"
          ;;
        LGPD_LOG_HASH_SECRET*)
          label="LGPD_LOG_HASH_SECRET"
          ;;
        Authorization:*)
          label="Authorization: Bearer"
          ;;
        *)
          label="PRIVATE KEY"
          ;;
      esac

      while IFS= read -r match; do
        [[ -z "$match" ]] && continue
        rest="${match#*:}"
        line="${rest%%:*}"
        content="${rest#*:}"
        if [[ "$content" =~ = ]]; then
          value="${content#*=}"
          value="${value## }"
          value="${value%% }"
          value="${value%%#*}"
          value="${value%% }"
          if [[ "$value" =~ $placeholder_pattern ]]; then
            continue
          fi
        fi
        report_match "$file" "$line" "$label"
      done <<<"$output"
    elif [[ $status -gt 1 ]]; then
      echo "[security] ERRO no scanner para '$pattern' em '$file'" >&2
      exit 2
    fi
  done
done <"$tmp_files"

if [[ -n "${ASSERT_NO_SECRETS_EXTRA_FILES:-}" ]]; then
  while IFS= read -r extra_file; do
    [[ -n "$extra_file" ]] || continue
    for pattern in "${patterns[@]}"; do
      set +e
      output=$(scan_file "$extra_file" "$pattern" 2>/dev/null)
      status=$?
      set -e

      if [[ $status -eq 0 ]]; then
        case "$pattern" in
          AWS_ACCESS_KEY_ID*)
            label="AWS_ACCESS_KEY_ID"
            ;;
          AWS_SECRET_ACCESS_KEY*)
            label="AWS_SECRET_ACCESS_KEY"
            ;;
          JWT_SECRET*)
            label="JWT_SECRET"
            ;;
          MAIL_PASSWORD*)
            label="MAIL_PASSWORD"
            ;;
          HERE_API_KEY*)
            label="HERE_API_KEY"
            ;;
          DIGITAL_CERTIFICATE_PASSWORD*)
            label="DIGITAL_CERTIFICATE_PASSWORD"
            ;;
          LGPD_LOG_HASH_SECRET*)
            label="LGPD_LOG_HASH_SECRET"
            ;;
          Authorization:*)
            label="Authorization: Bearer"
            ;;
          *)
            label="PRIVATE KEY"
            ;;
        esac

        while IFS= read -r match; do
          [[ -z "$match" ]] && continue
          rest="${match#*:}"
          line="${rest%%:*}"
          content="${rest#*:}"
          if [[ "$content" =~ = ]]; then
            value="${content#*=}"
            value="${value## }"
            value="${value%% }"
            value="${value%%#*}"
            value="${value%% }"
            if [[ "$value" =~ $placeholder_pattern ]]; then
              continue
            fi
          fi
          report_match "$extra_file" "$line" "$label"
        done <<<"$output"
      elif [[ $status -gt 1 ]]; then
        echo "[security] ERRO no scanner para '$pattern' em '$extra_file'" >&2
        exit 2
      fi
    done
  done < <(printf '%s' "$ASSERT_NO_SECRETS_EXTRA_FILES" | tr ':' '
')
fi

if [[ -s "$tmp_matches" ]]; then
  echo "[security] possíveis secrets encontrados:" >&2
  sort -u "$tmp_matches" >&2
  exit 1
fi

printf 'No tracked secret patterns detected.
'

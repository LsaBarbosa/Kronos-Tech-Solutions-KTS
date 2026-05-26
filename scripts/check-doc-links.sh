#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "$ROOT_DIR"

FAILED=0

while IFS= read -r md_file; do
  dir="$(dirname "$md_file")"

  while IFS= read -r link; do
    target="$(printf '%s\n' "$link" | sed -E 's/.*\(([^)]+)\).*/\1/')"

    if [[ "$target" =~ ^https?:// ]]; then
      continue
    fi

    if [[ "$target" =~ ^# ]]; then
      continue
    fi

    target="${target%%#*}"

    if [[ "$target" != *.md ]]; then
      continue
    fi

    if [[ "$target" = /* ]]; then
      resolved=".$target"
    else
      resolved="$dir/$target"
    fi

    if [[ ! -f "$resolved" ]]; then
      echo "Broken markdown link: $md_file -> $target"
      FAILED=1
    fi
  done < <(grep -oE '\[[^]]+\]\([^)]+\.md(#[^)]+)?\)' "$md_file" || true)
done < <(
  {
    printf '%s\n' "./README.md"
    printf '%s\n' "./PRE_PRODUCTION_CHECKLIST.md"
    find ./docs -name "*.md" | sort
  } | uniq
)

exit "$FAILED"

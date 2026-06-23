#!/usr/bin/env bash
set -euo pipefail

BASE_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"

BACKEND="/home/deploy/apps/Kronos-Tech-Solutions-KTS"
FRONTEND="/home/deploy/apps/Kronos-Tech-Solution-User-Plataform"
DOCS="/home/deploy/apps/kronos-business"

require_dir() {
  local dir="$1"
  if [[ ! -d "$dir" ]]; then
    echo "Diretório não encontrado: $dir" >&2
    exit 1
  fi
}

copy_common_to_backend() {
  mkdir -p "$BACKEND/.claude" "$BACKEND/docs_index" "$BACKEND/plan" "$BACKEND/prompts"
  cp -R "$BASE_DIR/.claude/"* "$BACKEND/.claude/"
  cp -R "$BASE_DIR/docs_index/"* "$BACKEND/docs_index/"
  cp -R "$BASE_DIR/plan/"* "$BACKEND/plan/"
  cp -R "$BASE_DIR/prompts/"* "$BACKEND/prompts/"
  cp "$BASE_DIR/README.md" "$BACKEND/README_CLAUDE_CTO_DEMO_SANDBOX.md"
}

copy_front_prompt() {
  mkdir -p "$FRONTEND/.claude" "$FRONTEND/docs_index" "$FRONTEND/prompts"
  cp -R "$BASE_DIR/.claude/rules" "$FRONTEND/.claude/"
  cp -R "$BASE_DIR/.claude/agents" "$FRONTEND/.claude/"
  cp -R "$BASE_DIR/.claude/subagents" "$FRONTEND/.claude/"
  cp -R "$BASE_DIR/docs_index/"* "$FRONTEND/docs_index/"
  cp -R "$BASE_DIR/prompts/"* "$FRONTEND/prompts/"
  cp "$BASE_DIR/README.md" "$FRONTEND/README_CLAUDE_CTO_DEMO_SANDBOX.md"
}

copy_docs_prompt() {
  mkdir -p "$DOCS/.claude" "$DOCS/docs_index" "$DOCS/plan" "$DOCS/prompts"
  cp -R "$BASE_DIR/.claude/rules" "$DOCS/.claude/"
  cp -R "$BASE_DIR/.claude/subagents" "$DOCS/.claude/"
  cp -R "$BASE_DIR/docs_index/"* "$DOCS/docs_index/"
  cp -R "$BASE_DIR/plan/"* "$DOCS/plan/"
  cp -R "$BASE_DIR/prompts/"* "$DOCS/prompts/"
  cp "$BASE_DIR/README.md" "$DOCS/README_CLAUDE_CTO_DEMO_SANDBOX.md"
}

validate_branch() {
  local dir="$1"
  local expected="$2"
  local current
  current="$(git -C "$dir" branch --show-current)"
  if [[ "$current" != "$expected" ]]; then
    echo "Branch inválida em $dir: atual=$current esperado=$expected" >&2
    exit 1
  fi
}

require_dir "$BACKEND"
require_dir "$FRONTEND"
require_dir "$DOCS"

validate_branch "$BACKEND" "homolog"
validate_branch "$FRONTEND" "homolog"
validate_branch "$DOCS" "main"

copy_common_to_backend
copy_front_prompt
copy_docs_prompt

echo "Pacote instalado nos repositórios."
echo "Próximo passo:"
echo "cd $BACKEND && claude"
echo "Depois cole o conteúdo de prompts/CLAUDE_CODE_MASTER_PROMPT.md"

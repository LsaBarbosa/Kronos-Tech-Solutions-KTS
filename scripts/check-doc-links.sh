#!/usr/bin/env bash
set -euo pipefail

python3 - <<'PY'
from pathlib import Path
import re
import sys

root = Path('.').resolve()
markdown_files = [
    p for p in root.rglob('*')
    if p.is_file() and p.suffix.lower() in {'.md', '.markdown'}
    and '.git' not in p.parts and 'build' not in p.parts and 'node_modules' not in p.parts
]

link_pattern = re.compile(r'\[[^\]]*\]\(([^)]+)\)')
issues = []

for md in markdown_files:
    text = md.read_text(encoding='utf-8', errors='ignore')
    for line_no, line in enumerate(text.splitlines(), start=1):
        for match in link_pattern.finditer(line):
            target = match.group(1).strip()
            if not target or target.startswith(('http://', 'https://', 'mailto:', '#', '/')):
                continue

            target = target.split('#', 1)[0].split('?', 1)[0].strip()
            if not target:
                continue

            candidate = (md.parent / target).resolve()
            if not candidate.exists():
                issues.append(f'{md}:{line_no}: missing link target -> {match.group(1)}')

if issues:
    print('\n'.join(issues))
    sys.exit(1)

print(f'documentation link check ok ({len(markdown_files)} markdown files)')
PY

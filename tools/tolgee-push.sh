#!/usr/bin/env bash
# Push iOS Swift tr() keys + fallbacks to Tolgee.
# Tolgee AI auto-translates italian → all 10 enabled languages.
set -euo pipefail
cd "$(dirname "$0")/.."

PROJECT_ID="${TOLGEE_PROJECT_ID:-10712}"
OUT_DIR="tools/tolgee-payload"
mkdir -p "$OUT_DIR"

echo "▶ Extracting i18n keys from Swift + Kotlin + Web source…"
python3 - <<'PY'
import re, json, pathlib

found = {}

def add(k, fb):
    if fb is None: return
    fb = fb.replace('\\"','"').replace('\\\\','\\').replace("\\'","'")
    # Keep first non-empty fallback we encounter.
    if k not in found or (not found[k] and fb):
        found[k] = fb

# iOS Swift:  tr("key", fallback: "value")
swift_root = pathlib.Path('iosApp/iosApp')
swift_pat = re.compile(r'tr\(\s*"([^"]+)"(?:\s*,\s*fallback:\s*"((?:[^"\\]|\\.)*)")?')
for f in swift_root.rglob('*.swift'):
    for m in swift_pat.finditer(f.read_text()):
        add(m.group(1), m.group(2))

# Android Kotlin:  tr("key", fallback = "value")  or  tr("key", "value")
kotlin_root = pathlib.Path('androidApp/src/main/kotlin')
# Negative lookbehind ensures we don't match str("…"), foo_tr("…"), etc.
kotlin_pat = re.compile(r'(?<![A-Za-z0-9_])tr\(\s*"([a-zA-Z0-9_.]+)"\s*,\s*(?:fallback\s*=\s*)?"((?:[^"\\]|\\.)*)"')
for f in kotlin_root.rglob('*.kt'):
    for m in kotlin_pat.finditer(f.read_text()):
        add(m.group(1), m.group(2))

# Web TypeScript/TSX:  t("key", "fallback", …)  or  t('key', 'fallback', …)
web_root = pathlib.Path('webApp/src')
web_pat = re.compile(
    r'(?<![A-Za-z0-9_])t\(\s*'
    r'(?:"([a-zA-Z0-9_.]+)"|\'([a-zA-Z0-9_.]+)\')\s*,\s*'
    r'(?:"((?:[^"\\]|\\.)*)"|\'((?:[^\'\\]|\\.)*)\')'
)
for ext in ('*.ts', '*.tsx'):
    for f in web_root.rglob(ext):
        for m in web_pat.finditer(f.read_text()):
            k = m.group(1) or m.group(2)
            fb = m.group(3) if m.group(3) is not None else m.group(4)
            add(k, fb)

# Web Astro:  <tag data-i18n="key">fallback</tag>
astro_pat = re.compile(
    r'data-i18n=(?:"([a-zA-Z0-9_.]+)"|\'([a-zA-Z0-9_.]+)\')[^>]*>([^<]*)<',
    re.DOTALL,
)
for f in web_root.rglob('*.astro'):
    for m in astro_pat.finditer(f.read_text()):
        k = m.group(1) or m.group(2)
        fb = (m.group(3) or '').strip()
        # Collapse internal whitespace runs (HTML semantics).
        fb = re.sub(r'\s+', ' ', fb)
        add(k, fb)

json.dump(dict(sorted(found.items())), open('tools/tolgee-payload/it.json','w'), ensure_ascii=False, indent=2)
print(f'  → {len(found)} keys extracted to tools/tolgee-payload/it.json')
PY

echo "▶ Pushing to Tolgee (KEEP mode — existing translations preserved)…"
cd "$OUT_DIR"
tolgee --project-id "$PROJECT_ID" --format JSON_I18NEXT push \
  --files-template "*.json" \
  --languages it \
  --no-strict-namespace \
  --force-mode KEEP
echo "✓ Done. AI auto-translation will populate the other 10 languages within ~minutes."

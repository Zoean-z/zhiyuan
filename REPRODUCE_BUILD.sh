#!/usr/bin/env bash
set -euo pipefail
ROOT="${1:-$(pwd)}"
cd "$ROOT"

echo "[1/8] git status"
git status --short || true

echo "[2/8] frontend deps"
cd frontend
npm ci
npm run build
cd ..

echo "[3/8] backend tests"
./mvnw -B test

echo "[4/8] backend package"
./mvnw -B -DskipTests package

echo "[5/8] copy static assets if needed"
mkdir -p src/main/resources/static

echo "[6/8] archive outputs"
OUT_DIR="${OUT_DIR:-/data}"
ZIP="$OUT_DIR/zhiyuan-complete-rebuilt.zip"
BUNDLE="$OUT_DIR/zhiyuan-complete-rebuilt.bundle"
SHA="$OUT_DIR/zhiyuan-complete-rebuilt.sha256"
rm -f "$ZIP" "$BUNDLE" "$SHA"
zip -qr "$ZIP" .
git bundle create "$BUNDLE" --all
sha256sum "$ZIP" "$BUNDLE" > "$SHA"

echo "[7/8] done"
ls -lh "$ZIP" "$BUNDLE" "$SHA"

echo "[8/8] sha256"
cat "$SHA"

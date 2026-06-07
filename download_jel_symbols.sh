#!/usr/bin/env bash
# Downloads SVG symbols for all jel values from waymarkedtrails-shields.
# Usage: ./download_jel_symbols.sh [jel-file] [out-dir] [not-found-file]
set -euo pipefail

JEL_FILE="${1:-seen_jel_values.txt}"
OUT_DIR="${2:-jel-symbols}"
NOT_FOUND_FILE="${3:-not-found-jel.txt}"

BASE_URL="https://raw.githubusercontent.com/waymarkedtrails/waymarkedtrails-shields/master/wmt_shields/data/jel"

if [ ! -f "$JEL_FILE" ]; then
  echo "ERROR: jel file not found: $JEL_FILE" >&2
  exit 1
fi

mkdir -p "$OUT_DIR"
: > "$NOT_FOUND_FILE"

found=0
missing=0

while IFS= read -r jel || [ -n "$jel" ]; do
  jel="${jel//[$'\r\n']}"
  [ -z "$jel" ] && continue

  # encode '+' as %2B for the URL path (only special char in jel values)
  encoded="${jel//+/%2B}"
  url="$BASE_URL/$encoded.svg"
  out="$OUT_DIR/$jel.svg"

  http_code=$(curl -sL -w "%{http_code}" -o "$out" "$url")

  if [ "$http_code" = "200" ]; then
    echo "OK      $jel"
    ((found++)) || true
  else
    echo "MISSING $jel"
    rm -f "$out"
    echo "$jel" >> "$NOT_FOUND_FILE"
    ((missing++)) || true
  fi
done < "$JEL_FILE"

echo ""
echo "Downloaded: $found  Missing: $missing"
if [ "$missing" -gt 0 ]; then
  echo "Missing symbols written to: $NOT_FOUND_FILE"
fi

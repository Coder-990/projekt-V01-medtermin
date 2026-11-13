#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")" || exit 1

OUT=alati/prije
mkdir -p "$OUT"

# Maven razrijesi verzije iz parenta/BOM-a i tranzitivne ovisnosti.
./mvnw -q -Psca cyclonedx:makeAggregateBom -DskipTests
test -s target/bom.json

if docker run --rm \
  --user "$(id -u):$(id -g)" \
  -v "$PWD:/src:ro" \
  -w /src ghcr.io/google/osv-scanner:latest \
  scan source -L /src/target/bom.json 2>&1 | tee "$OUT/osv-java.txt"; then
  status=0
else
  status=$?
fi

echo "===== OSV zavrsio sa statusom $status. Izvjestaj: $OUT/osv-java.txt ====="
exit "$status"

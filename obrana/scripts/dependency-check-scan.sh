#!/usr/bin/env bash
# OWASP Dependency-Check nad jar datotekama iz Maven projekta, rezultat u alati/poslije/
set -euo pipefail
cd "$(dirname "$0")" || exit 1

OUT=alati/poslije
DATA="$HOME/.dependency-check-data"
mkdir -p "$OUT" "$DATA" target/dependency

# Maven skine sve jar ovisnosti u target/dependency
./mvnw -q dependency:copy-dependencies -DoutputDirectory=target/dependency || exit 1

if [ -z "${NVD_API_KEY:-}" ]; then
  echo "UPOZORENJE: NVD_API_KEY nije postavljen, skeniranje ce biti sporo."
  KEY_ARG=()
else
  KEY_ARG=(--nvdApiKey "$NVD_API_KEY")
fi

docker run --rm \
  --user "$(id -u):$(id -g)" \
  -v "$PWD:/src" \
  -v "$DATA:/usr/share/dependency-check/data" \
  owasp/dependency-check:latest \
  --scan /src/target/dependency \
  --project medtermin \
  --format HTML --format JSON \
  --suppression /src/dependency-check-suppressions.xml \
  "${KEY_ARG[@]}" \
  -l "/src/$OUT/dependency-check.log" \
  --out "/src/$OUT"

echo "===== Gotovo. Izvjestaj: $OUT/dependency-check-report.html ====="

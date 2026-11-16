#!/usr/bin/env bash
# DAST – dinamičko testiranje pokrenute aplikacije: OWASP ZAP baseline (Docker).
#  1. build + pokretanje aplikacije u pozadini, čekanje na /api/health
#  2. zap-baseline.py: spider + pasivna pravila; ci/zap_hooks.py dodaje prijavu (token)
#     i ciljane provjere (preusmjeravanje nakon prijave, CORS) – vidi README
#  3. ZAP JSON -> SARIF (izvjestaji/zap.sarif)
source "$(dirname "$0")/zajednicko.sh"

ZAP_DIR="$IZVJESTAJI/zap"
mkdir -p "$ZAP_DIR"
chmod 777 "$ZAP_DIR"   # ZAP u kontejneru radi kao korisnik zap (uid 1000)
rm -f "$ZAP_DIR"/zap.json "$ZAP_DIR"/dast-provjere.json

# Testni korisnik za prijavu (seed korisnik iz T2; u stvarnom projektu: GitHub secret).
export DAST_KORISNIK="${DAST_KORISNIK:-ana}"
export DAST_LOZINKA="${DAST_LOZINKA:-Ana12345!}"

if [ "$STACK" = java ]; then
  URL="${DAST_URL:-http://localhost:8080}"
  # Izvršni Spring Boot JAR (u target/ mogu biti i drugi JAR-ovi, npr. dodatak za SpotBugs).
  izvrsni_jar() {
    local f
    for f in target/*.jar; do
      [ -f "$f" ] && unzip -p "$f" META-INF/MANIFEST.MF 2>/dev/null | grep '^Start-Class:' >/dev/null && { echo "$f"; return; }
    done
    return 0
  }
  jar=$(izvrsni_jar)
  if [ -z "$jar" ]; then
    "${MVN[@]}" -DskipTests package
    jar=$(izvrsni_jar)
  fi
  [ -n "$jar" ] || { echo "::error::Nije pronađen izvršni JAR u target/"; exit 1; }
  java -jar "$jar" --server.port="${URL##*:}" > "$IZVJESTAJI/aplikacija.log" 2>&1 &
elif [ "$STACK" = python ]; then
  URL="${DAST_URL:-http://localhost:8000}"
  python_okruzenje requirements.txt
  # python -m <paket>; port u varijablama PORT i APP_PORT (T2 projekti čitaju APP_PORT, zadano 8000)
  PORT="${URL##*:}" APP_PORT="${URL##*:}" "$VENV/bin/python" -m "$PAKET" > "$IZVJESTAJI/aplikacija.log" 2>&1 &
else
  URL="${DAST_URL:-http://localhost:5080}"
  dotnet run --project "$API_PROJEKT" -c Release --no-launch-profile --urls "$URL" \
    > "$IZVJESTAJI/aplikacija.log" 2>&1 &
fi
PID=$!
trap 'kill $PID 2>/dev/null || true' EXIT

echo "Čekam aplikaciju na $URL/api/health …"
for _ in $(seq 1 120); do
  if curl -fsS "$URL/api/health" >/dev/null 2>&1; then break; fi
  if ! kill -0 "$PID" 2>/dev/null; then
    echo "::error::Aplikacija se nije pokrenula"; tail -50 "$IZVJESTAJI/aplikacija.log"; exit 1
  fi
  sleep 2
done
curl -fsS "$URL/api/health" || { echo "::error::/api/health ne odgovara"; tail -50 "$IZVJESTAJI/aplikacija.log"; exit 1; }
echo

cp ci/zap_hooks.py ci/dast-putanje.txt "$ZAP_DIR/"
docker pull -q "$ZAP_SLIKA"
rc=0
# -I: ZAP ne vraća grešku za upozorenja (odluku donosi gate); -m: minute spidera
docker run --rm --network host -v "$ZAP_DIR:/zap/wrk:rw" \
  -e DAST_KORISNIK -e DAST_LOZINKA \
  "$ZAP_SLIKA" zap-baseline.py -t "$URL" -m "${ZAP_SPIDER_MINUTA:-1}" -I \
  -J zap.json -r zap.html -w zap.md --hook=/zap/wrk/zap_hooks.py || rc=$?
echo "zap-baseline izlazni kod: $rc (0 = OK, 1/2 = nalazi, 3 = greška)"
[ -f "$ZAP_DIR/zap.json" ] || { echo "::error::ZAP nije stvorio izvještaj"; exit 1; }
rm -f "$ZAP_DIR/zap_hooks.py" "$ZAP_DIR/dast-putanje.txt"

python3 ci/sarif.py zap "$ZAP_DIR/zap.json" "$IZVJESTAJI/zap.sarif"
ls -l "$ZAP_DIR"

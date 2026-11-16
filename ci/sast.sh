#!/usr/bin/env bash
# SAST – statička analiza izvornog koda.
#  * Semgrep (OWASP Top 10 + pravila za jezik) – oba stacka
#  * Java: SpotBugs + Find Security Bugs (profil -Psast iz pom.xml)
#  * .NET: Roslyn analizatori (Microsoft.CodeAnalysis.NetAnalyzers) s AnalysisMode=All
#  * Python: Bandit (sigurnosna pravila za Python) nad paketom aplikacije
# Skripta NE odlučuje o prolazu – to radi ci/gate.py. Ovdje samo nastaju izvještaji.
source "$(dirname "$0")/zajednicko.sh"

# ---------- Semgrep ----------
# Pravila iz registra (semgrep.dev, bez prijave) + vlastita regresijska pravila projekta (ci/semgrep/).
if [ "$STACK" = java ]; then
  SEMGREP_PRAVILA="${SEMGREP_PRAVILA:-p/owasp-top-ten p/java ci/semgrep}"
elif [ "$STACK" = python ]; then
  SEMGREP_PRAVILA="${SEMGREP_PRAVILA:-p/owasp-top-ten p/python ci/semgrep}"
else
  SEMGREP_PRAVILA="${SEMGREP_PRAVILA:-p/owasp-top-ten p/csharp ci/semgrep}"
fi
if [ ! -x "$ALATI/semgrep/bin/semgrep" ]; then
  python3 -m venv "$ALATI/semgrep"
  "$ALATI/semgrep/bin/pip" install -q "semgrep==$SEMGREP_VERZIJA"
fi
args=()
for p in $SEMGREP_PRAVILA; do args+=(--config "$p"); done
echo "Semgrep $SEMGREP_VERZIJA, pravila: $SEMGREP_PRAVILA"
# Datoteke koje se ne analiziraju: .semgrepignore (testovi, build izlazi, izvještaji).
"$ALATI/semgrep/bin/semgrep" scan "${args[@]}" --metrics=off --disable-version-check \
  --json-output="$IZVJESTAJI/semgrep.json" --sarif-output="$IZVJESTAJI/semgrep.sarif" .

# ---------- Java: SpotBugs + Find Security Bugs ----------
if [ "$STACK" = java ]; then
  # failOnError=false: nalaze ocjenjuje gate; ovdje samo XML + SARIF izvještaj.
  "${MVN[@]}" -Psast -DskipTests compile spotbugs:spotbugs \
    -Dspotbugs.sarifOutput=true -Dspotbugs.failOnError=false
  cp target/spotbugsXml.xml "$IZVJESTAJI/spotbugs.xml"
  [ -f target/spotbugsSarif.json ] && cp target/spotbugsSarif.json "$IZVJESTAJI/spotbugs.sarif"
fi

# ---------- .NET: Roslyn analizatori ----------
if [ "$STACK" = dotnet ]; then
  # AnalysisMode=All uključuje SVA CA pravila (i sigurnosna CA2xxx/CA3xxx/CA5xxx s analizom toka podataka).
  # ErrorLog zapisuje sve upozorenje u SARIF 2.1 (zarez u vrijednosti mora biti %2C).
  dotnet build "$API_PROJEKT" -c Release --no-incremental \
    -p:AnalysisMode=All -p:TreatWarningsAsErrors=false \
    "-p:ErrorLog=$IZVJESTAJI/roslyn.sarif%2Cversion=2.1" \
    -v:quiet "-clp:NoSummary;ErrorsOnly"
fi

# ---------- Python: Bandit ----------
if [ "$STACK" = python ]; then
  [ -x "$ALATI/bandit/bin/bandit" ] || python_alat bandit "bandit==$BANDIT_VERZIJA"
  # Izlazni kod: 0 = nema nalaza, 1 = ima nalaza (gate odlučuje), ostalo = greška alata.
  # Iznimke u kodu: "# nosec B608 – razlog" (gate ispisuje koliko ih je).
  rc=0
  "$ALATI/bandit/bin/bandit" -r "$PAKET" -f json -o "$IZVJESTAJI/bandit.json" -q || rc=$?
  if [ "$rc" -gt 1 ]; then echo "::error::Bandit nije uspio (izlazni kod $rc)"; exit "$rc"; fi
  python3 ci/sarif.py bandit "$IZVJESTAJI/bandit.json" "$IZVJESTAJI/bandit.sarif"
fi

ls -l "$IZVJESTAJI"

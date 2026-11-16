#!/usr/bin/env bash
# SCA – analiza ovisnosti (biblioteka trećih strana).
#  1. SBOM (CycloneDX JSON) – popis svih ovisnosti, uključujući tranzitivne
#  2. OSV-Scanner nad SBOM-om (baza osv.dev: GHSA, NVD, …)
#  3. .NET: dotnet list package --vulnerable --include-transitive (NuGet audit)
#     Python: pip-audit nad istim okruženjem (baza PyPI / PyPA advisory database)
source "$(dirname "$0")/zajednicko.sh"

# ---------- 1. SBOM ----------
if [ "$STACK" = java ]; then
  # Profil -Psca iz pom.xml (cyclonedx-maven-plugin); test ovisnosti nisu uključene.
  "${MVN[@]}" -Psca -DskipTests cyclonedx:makeAggregateBom
  cp target/bom.json "$IZVJESTAJI/sbom.cdx.json"
elif [ "$STACK" = python ]; then
  # Čisto okruženje SAMO s izvršnim ovisnostima (requirements.txt, i tranzitivne; bez pip-a i alata za razvoj).
  SCA_OKRUZENJE="$ALATI/sca-okruzenje"
  rm -rf "$SCA_OKRUZENJE"
  python3 -m venv --without-pip "$SCA_OKRUZENJE"
  python3 -m pip --python "$SCA_OKRUZENJE/bin/python" install -q --disable-pip-version-check -r requirements.txt
  [ -x "$ALATI/pyalati/bin/cyclonedx-py" ] || \
    python_alat pyalati "cyclonedx-bom==$CYCLONEDX_PY_VERZIJA" "pip-audit==$PIP_AUDIT_VERZIJA"
  projekt=(); grep -q '^\[project\]' pyproject.toml 2>/dev/null && projekt=(--pyproject pyproject.toml)
  "$ALATI/pyalati/bin/cyclonedx-py" environment "${projekt[@]}" --of JSON -o "$IZVJESTAJI/sbom.cdx.json" \
    "$SCA_OKRUZENJE/bin/python"
else
  dotnet restore "$SLN"
  if [ ! -x "$ALATI/dotnet/dotnet-CycloneDX" ]; then
    dotnet tool install --tool-path "$ALATI/dotnet" CycloneDX --version "$CYCLONEDX_DOTNET_VERZIJA"
  fi
  "$ALATI/dotnet/dotnet-CycloneDX" "$API_PROJEKT" -o "$IZVJESTAJI" -fn sbom.cdx.json -F Json \
    || "$ALATI/dotnet/dotnet-CycloneDX" "$API_PROJEKT" -o "$IZVJESTAJI" -fn sbom.cdx.json -j
fi

# ---------- 2. OSV-Scanner ----------
OSV="$ALATI/osv-scanner-$OSV_SCANNER_VERZIJA"
if [ ! -x "$OSV" ]; then
  base="https://github.com/google/osv-scanner/releases/download/v$OSV_SCANNER_VERZIJA"
  preuzmi "$base/osv-scanner_linux_amd64" "$OSV" "$base/osv-scanner_SHA256SUMS"
  chmod +x "$OSV"
fi
# Izlazni kod: 0 = nema nalaza, 1 = ima nalaza (gate odlučuje), ostalo = greška alata.
osv() { # format odrediste
  local rc=0
  "$OSV" scan source --config osv-scanner.toml -L "$IZVJESTAJI/sbom.cdx.json" --format "$1" > "$2" || rc=$?
  if [ "$rc" -gt 1 ]; then echo "::error::OSV-Scanner nije uspio (izlazni kod $rc)"; exit "$rc"; fi
}
osv json "$IZVJESTAJI/osv.json"
osv sarif "$IZVJESTAJI/osv.sarif"

# ---------- 3. .NET: NuGet audit ----------
if [ "$STACK" = dotnet ]; then
  dotnet list "$SLN" package --vulnerable --include-transitive --format json > "$IZVJESTAJI/nuget-audit.json"
  python3 ci/sarif.py nuget "$IZVJESTAJI/nuget-audit.json" "$IZVJESTAJI/nuget-audit.sarif"
fi

# ---------- 3. Python: pip-audit ----------
if [ "$STACK" = python ]; then
  paketi=$("$SCA_OKRUZENJE/bin/python" -c 'import sysconfig; print(sysconfig.get_paths()["purelib"])')
  # Izlazni kod: 0 = nema nalaza, 1 = ima nalaza (gate odlučuje); bez izvještaja gate zatvara vrata.
  "$ALATI/pyalati/bin/pip-audit" --path "$paketi" -f json -o "$IZVJESTAJI/pip-audit.json" --progress-spinner off \
    || echo "pip-audit: pronađene ranjivosti (ili greška) – odluka je na sigurnosnim vratima"
  [ -s "$IZVJESTAJI/pip-audit.json" ] || { echo "::error::pip-audit nije stvorio izvještaj"; exit 1; }
  python3 ci/sarif.py pip-audit "$IZVJESTAJI/pip-audit.json" "$IZVJESTAJI/pip-audit.sarif"
fi

ls -l "$IZVJESTAJI"

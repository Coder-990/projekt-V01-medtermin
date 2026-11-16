#!/usr/bin/env bash
# Tajne u kodu i povijesti (gitleaks). U git repozitoriju skenira SVE commitove
# (na CI-ju zato checkout s fetch-depth: 0) – tajna obrisana u novijem commitu i dalje je procurila.
source "$(dirname "$0")/zajednicko.sh"

GL="$ALATI/gitleaks-$GITLEAKS_VERZIJA"
if [ ! -x "$GL/gitleaks" ]; then
  mkdir -p "$GL"
  base="https://github.com/gitleaks/gitleaks/releases/download/v$GITLEAKS_VERZIJA"
  arhiva="gitleaks_${GITLEAKS_VERZIJA}_linux_x64.tar.gz"
  preuzmi "$base/$arhiva" "$GL/$arhiva" "$base/gitleaks_${GITLEAKS_VERZIJA}_checksums.txt"
  tar -xzf "$GL/$arhiva" -C "$GL" gitleaks
fi

# --redact: tajne se ne ispisuju u log/izvještaj; --exit-code 0: odluku donosi gate.
# Konfiguracija: .gitleaks.toml (zadana pravila + iznimke), .gitleaksignore (otisci prihvaćenih nalaza).
if git rev-parse --is-inside-work-tree >/dev/null 2>&1; then
  nacin=git
else
  nacin=dir
fi
"$GL/gitleaks" "$nacin" . --no-banner --redact --exit-code 0 \
  --report-format sarif --report-path "$IZVJESTAJI/gitleaks.sarif"
echo "gitleaks ($nacin): izvještaj u izvjestaji/gitleaks.sarif"

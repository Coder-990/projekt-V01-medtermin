#!/usr/bin/env bash
# Zajedničke postavke za sve ci/*.sh skripte (uključuje se s: source ci/zajednicko.sh).
# Skripte rade jednako na GitHub Actions i lokalno (Linux/macOS/WSL, bash 4+).
set -euo pipefail

KORIJEN=$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)
cd "$KORIJEN"

# Svi izvještaji idu u izvjestaji/ (ne commita se – vidi izvjestaji/.gitignore).
IZVJESTAJI="$KORIJEN/izvjestaji"
mkdir -p "$IZVJESTAJI"
[ -f "$IZVJESTAJI/.gitignore" ] || echo '*' > "$IZVJESTAJI/.gitignore"

# Alati se instaliraju izvan repozitorija (na CI-ju u RUNNER_TEMP), da ih skeneri ne bi analizirali.
if [ -n "${RUNNER_TEMP:-}" ]; then
  ALATI="$RUNNER_TEMP/devsecops-alati"
else
  ALATI="$KORIJEN/.ci-alati"
fi
mkdir -p "$ALATI"
[ -d "$KORIJEN/.ci-alati" ] && { [ -f "$KORIJEN/.ci-alati/.gitignore" ] || echo '*' > "$KORIJEN/.ci-alati/.gitignore"; }

# Verzije alata – zaključane (pinned). Dependabot ih ne vidi: provjeri ih jednom mjesečno.
SEMGREP_VERZIJA="${SEMGREP_VERZIJA:-1.183.0}"
GITLEAKS_VERZIJA="${GITLEAKS_VERZIJA:-8.30.1}"
OSV_SCANNER_VERZIJA="${OSV_SCANNER_VERZIJA:-2.7.0}"
CYCLONEDX_DOTNET_VERZIJA="${CYCLONEDX_DOTNET_VERZIJA:-6.3.1}"
BANDIT_VERZIJA="${BANDIT_VERZIJA:-1.9.4}"
PIP_AUDIT_VERZIJA="${PIP_AUDIT_VERZIJA:-2.10.1}"
CYCLONEDX_PY_VERZIJA="${CYCLONEDX_PY_VERZIJA:-7.6.0}"
ZAP_SLIKA="${ZAP_SLIKA:-ghcr.io/zaproxy/zaproxy:2.17.0}"

# Prepoznavanje stacka: Maven projekt (pom.xml), .NET solution (*.sln / *.slnx)
# ili Python projekt (pyproject.toml / requirements.txt + paket koji se pokreće s python -m <paket>).
if [ -f pom.xml ]; then
  STACK=java
  MVN=(bash ./mvnw -B -ntp)
elif compgen -G '*.sln' >/dev/null || compgen -G '*.slnx' >/dev/null; then
  STACK=dotnet
  SLN=$({ compgen -G '*.sln'; compgen -G '*.slnx'; } | head -1 || true)
  # API projekt: src/<Naziv>.Api/<Naziv>.Api.csproj (prvi projekt u src/ koji nije testni)
  API_PROJEKT=$({ compgen -G 'src/*/*.csproj' || true; } | grep -vi test | head -1 || true)
  [ -n "$API_PROJEKT" ] || { echo "::error::Nije pronađen API projekt u src/*/"; exit 2; }
  export DOTNET_CLI_TELEMETRY_OPTOUT=1 DOTNET_NOLOGO=1
elif [ -f pyproject.toml ] || [ -f requirements.txt ]; then
  STACK=python
  # Paket aplikacije: mapa u korijenu s __init__.py i __main__.py (npr. medtermin/ -> python -m medtermin)
  PAKET=$(for d in */; do d=${d%/}; [ -f "$d/__init__.py" ] && [ -f "$d/__main__.py" ] && echo "$d"; done \
    | grep -vxE 'tests?|ci|venv' | head -1 || true)
  [ -n "$PAKET" ] || { echo "::error::Nije pronađen paket aplikacije (mapa s __init__.py i __main__.py)"; exit 2; }
  # Virtualno okruženje aplikacije – isto kao lokalno (python -m venv .venv); alati imaju vlastita okruženja.
  VENV="${VENV:-$KORIJEN/.venv}"
  export PIP_DISABLE_PIP_VERSION_CHECK=1 PYTHONDONTWRITEBYTECODE=1
else
  echo "::error::Nije prepoznat projekt (očekuje se pom.xml, *.sln ili pyproject.toml/requirements.txt u korijenu repozitorija)."
  exit 2
fi

# Python: virtualno okruženje aplikacije (stvara se ako ne postoji) s ovisnostima iz zadane datoteke.
python_okruzenje() { # datoteka-ovisnosti
  [ -x "$VENV/bin/python" ] || python3 -m venv "$VENV"
  "$VENV/bin/python" -m pip install -q -r "$1"
}

# Python: alat u vlastitom virtualnom okruženju izvan repozitorija (kao Semgrep).
python_alat() { # ime paket==verzija [paket==verzija ...]
  local ime=$1; shift
  [ -x "$ALATI/$ime/bin/python" ] || python3 -m venv "$ALATI/$ime"
  "$ALATI/$ime/bin/python" -m pip install -q --disable-pip-version-check "$@"
}

# Ispis u sažetak GitHub Actions posla (lokalno samo na standardni izlaz).
sazetak() {
  if [ -n "${GITHUB_STEP_SUMMARY:-}" ]; then printf '%s\n' "$*" >> "$GITHUB_STEP_SUMMARY"; fi
  printf '%s\n' "$*"
}

# Preuzimanje datoteke s provjerom SHA-256 iz datoteke s kontrolnim zbrojevima istog izdanja.
preuzmi() { # url odrediste [url-kontrolnih-zbrojeva]
  local url=$1 odr=$2 sume=${3:-}
  curl -fsSL --retry 3 -o "$odr" "$url"
  if [ -n "$sume" ]; then
    local ocekivano
    ocekivano=$(curl -fsSL --retry 3 "$sume" | grep " $(basename "$url")\$" | awk '{print $1}' | head -1)
    if [ -z "$ocekivano" ]; then
      echo "::warning::Nema kontrolnog zbroja za $(basename "$url") – preskačem provjeru."
    else
      echo "$ocekivano  $odr" | sha256sum -c - >/dev/null || { echo "::error::Neispravan SHA-256 za $url"; exit 3; }
    fi
  fi
}

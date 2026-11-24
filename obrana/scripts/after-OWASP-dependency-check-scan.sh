#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")"
python3 dc-pregled.py alati/poslije/dependency-check-report.json | tee alati/poslije/dependency-check-java.txt

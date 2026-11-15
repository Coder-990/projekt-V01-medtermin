#!/usr/bin/env bash
cd "$(dirname "$0")" || exit 1

mkdir -p alati/poslije
docker run --rm \
  -v "$PWD:/src" \
  -v "$HOME/.m2:/root/.m2:ro" \
  -w /src aquasec/trivy:latest \
  fs --scanners vuln . | tee alati/poslije/trivy-java.txt

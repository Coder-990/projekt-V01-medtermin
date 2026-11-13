#!/usr/bin/env bash
cd "$(dirname "$0")" || exit 1

mkdir -p alati/prije
docker run --rm \
  -v "$PWD:/src" \
  -v "$HOME/.m2:/root/.m2:ro" \
  -w /src aquasec/trivy:latest \
  fs --scanners vuln . | tee alati/prije/trivy-java.txt

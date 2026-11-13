mkdir -p alati/prije
sudo chown -R $USER:$USER alati/
docker run --rm -v "$PWD:/src" -w /src semgrep/semgrep semgrep scan \
  --config p/owasp-top-ten --config p/security-audit --config p/java \
  --metrics=off --sarif -o /src/alati/prije/semgrep-java.sarif /src
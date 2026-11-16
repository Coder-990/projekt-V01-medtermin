#!/usr/bin/env python3
"""Pretvorba izvještaja koji nemaju SARIF u SARIF 2.1 (za artefakte i code scanning).

  python3 ci/sarif.py zap       izvjestaji/zap/zap.json      izvjestaji/zap.sarif
  python3 ci/sarif.py nuget     izvjestaji/nuget-audit.json  izvjestaji/nuget-audit.sarif
  python3 ci/sarif.py bandit    izvjestaji/bandit.json       izvjestaji/bandit.sarif
  python3 ci/sarif.py pip-audit izvjestaji/pip-audit.json    izvjestaji/pip-audit.sarif
"""
import json
import sys

RAZINA_ZAP = {3: "error", 2: "warning", 1: "note", 0: "none"}
RAZINA_NUGET = {"critical": "error", "high": "error", "moderate": "warning", "low": "note"}
RAZINA_BANDIT = {"HIGH": "error", "MEDIUM": "warning", "LOW": "note"}


def sarif(alat, url_alata, pravila, rezultati):
    return {"$schema": "https://json.schemastore.org/sarif-2.1.0.json", "version": "2.1.0",
            "runs": [{"tool": {"driver": {"name": alat, "informationUri": url_alata,
                                          "rules": list(pravila.values())}},
                      "results": rezultati}]}


def zap(ulaz):
    podaci = json.load(open(ulaz, encoding="utf-8"))
    sites = podaci.get("site", [])
    pravila, rezultati = {}, []
    for s in sites if isinstance(sites, list) else [sites]:
        for a in s.get("alerts", []):
            pid = str(a.get("pluginid"))
            pravila[pid] = {"id": pid, "name": a.get("alert") or a.get("name"),
                            "shortDescription": {"text": a.get("alert") or a.get("name") or pid},
                            "helpUri": f"https://www.zaproxy.org/docs/alerts/{pid}/",
                            "properties": {"cwe": a.get("cweid"), "risk": a.get("riskdesc")}}
            for i in (a.get("instances") or [])[:20]:
                rezultati.append({
                    "ruleId": pid, "level": RAZINA_ZAP.get(int(a.get("riskcode", 0)), "note"),
                    "message": {"text": f"{a.get('alert')}: {i.get('method', '')} {i.get('uri', '')} "
                                        f"{i.get('param', '')} {i.get('evidence', '')}".strip()},
                    "locations": [{"physicalLocation": {"artifactLocation": {"uri": i.get("uri", "")}}}]})
    return sarif("OWASP ZAP", "https://www.zaproxy.org/", pravila, rezultati)


def nuget(ulaz):
    podaci = json.load(open(ulaz, encoding="utf-8-sig"))
    pravila, rezultati = {}, []
    for proj in podaci.get("projects", []):
        for fw in proj.get("frameworks", []):
            for vrsta in ("topLevelPackages", "transitivePackages"):
                for p in fw.get(vrsta, []):
                    for v in p.get("vulnerabilities", []):
                        url = v.get("advisoryurl", "")
                        rid = url.rstrip("/").rsplit("/", 1)[-1] or "nuget-audit"
                        pravila[rid] = {"id": rid, "shortDescription": {"text": f"Ranjiv paket {p.get('id')}"},
                                        "helpUri": url}
                        rezultati.append({
                            "ruleId": rid, "level": RAZINA_NUGET.get(v.get("severity", "").lower(), "warning"),
                            "message": {"text": f"{p.get('id')} {p.get('resolvedVersion')} ({v.get('severity')}) {url}"},
                            "locations": [{"physicalLocation": {"artifactLocation": {
                                "uri": proj.get("path", "").replace("\\", "/")}}}]})
    return sarif("dotnet list package --vulnerable", "https://learn.microsoft.com/nuget/concepts/auditing-packages",
                 pravila, rezultati)


def bandit(ulaz):
    podaci = json.load(open(ulaz, encoding="utf-8"))
    pravila, rezultati = {}, []
    for r in podaci.get("results", []):
        pid = r.get("test_id", "bandit")
        oz, pouzdanost = str(r.get("issue_severity", "")).upper(), str(r.get("issue_confidence", "")).upper()
        pravila[pid] = {"id": pid, "name": r.get("test_name"),
                        "shortDescription": {"text": r.get("test_name") or pid},
                        "helpUri": r.get("more_info") or "https://bandit.readthedocs.io/",
                        "properties": {"cwe": (r.get("issue_cwe") or {}).get("id")}}
        redci = r.get("line_range") or [r.get("line_number", 1)]
        rezultati.append({
            "ruleId": pid, "level": RAZINA_BANDIT.get(oz, "warning"),
            "message": {"text": f"{r.get('issue_text')} (ozbiljnost {oz}, pouzdanost {pouzdanost})"},
            "locations": [{"physicalLocation": {
                "artifactLocation": {"uri": str(r.get("filename", "")).removeprefix("./")},
                "region": {"startLine": max(1, int(r.get("line_number") or 1)), "endLine": max(1, max(redci))}}}]})
    return sarif("Bandit", "https://bandit.readthedocs.io/", pravila, rezultati)


def pip_audit(ulaz):
    podaci = json.load(open(ulaz, encoding="utf-8"))
    pravila, rezultati = {}, []
    for dep in podaci.get("dependencies", []):
        vidjeno = set()
        for v in dep.get("vulns") or []:
            rid = v.get("id", "pip-audit")
            if rid in vidjeno:
                continue
            vidjeno.add(rid)
            pravila[rid] = {"id": rid, "shortDescription": {"text": f"Ranjiv paket {dep.get('name')}"},
                            "helpUri": f"https://osv.dev/vulnerability/{rid}"}
            popravak = ", ".join(v.get("fix_versions") or []) or "nema"
            rezultati.append({
                "ruleId": rid, "level": "error",
                "message": {"text": f"{dep.get('name')} {dep.get('version')}: "
                                    f"{', '.join([rid, *(v.get('aliases') or [])])}; popravljeno u: {popravak}"},
                "locations": [{"physicalLocation": {"artifactLocation": {"uri": "requirements.txt"}}}]})
    return sarif("pip-audit", "https://github.com/pypa/pip-audit", pravila, rezultati)


if __name__ == "__main__":
    vrsta, ulaz, izlaz = sys.argv[1:4]
    rezultat = {"zap": zap, "nuget": nuget, "bandit": bandit, "pip-audit": pip_audit}[vrsta](ulaz)
    with open(izlaz, "w", encoding="utf-8") as f:
        json.dump(rezultat, f, ensure_ascii=False, indent=2)
    print(f"{vrsta}: {len(rezultat['runs'][0]['results'])} rezultata -> {izlaz}")

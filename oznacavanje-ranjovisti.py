#!/usr/bin/env python3
"""Dodaje TODO(sec) komentare na mjesta ranjivosti iz izvjestaja.
Upotreba:
  python3 oznaci-ranjivosti.py          # samo ispis (probni rad)
  python3 oznaci-ranjivosti.py --apply  # stvarno upisuje komentare
"""
import json, sys, xml.etree.ElementTree as ET
from pathlib import Path

APPLY = "--apply" in sys.argv
ROOT = Path.cwd()
OUT = ROOT / "alati" / "prije"
MARK = "TODO(sec)"

findings = []      # (putanja_datoteke, linija, opis)
dep_findings = []  # ovisnosti iz Dependency-Checka

def flat(s, n=160):
    s = " ".join(str(s).split())
    return s if len(s) <= n else s[:n - 3] + "..."

# 1) Semgrep SARIF -> kod
sarif = OUT / "semgrep-java.sarif"
if sarif.exists():
    for run in json.loads(sarif.read_text(encoding="utf-8")).get("runs", []):
        for r in run.get("results", []):
            ploc = r["locations"][0]["physicalLocation"]
            uri = ploc["artifactLocation"]["uri"]
            line = ploc.get("region", {}).get("startLine", 1)
            findings.append((ROOT / uri, line,
                             f"semgrep {r.get('ruleId', '?')}: {flat(r['message']['text'])}"))

# 2) SpotBugs XML -> kod
sb = ROOT / "target" / "spotbugsXml.xml"
if sb.exists():
    for bi in ET.parse(sb).getroot().iter("BugInstance"):
        sl = bi.find("SourceLine[@primary='true']")
        if sl is None:
            sl = bi.find("SourceLine")
        if sl is None or not sl.get("sourcepath") or not sl.get("start"):
            continue
        sp = sl.get("sourcepath")
        matches = list(ROOT.glob(f"src/**/{sp}"))
        if not matches:
            continue
        msg = bi.findtext("LongMessage") or bi.get("type", "?")
        findings.append((matches[0], int(sl.get("start")),
                         f"spotbugs {bi.get('type', '?')}: {flat(msg)}"))

# 3) Dependency-Check JSON -> pom.xml (jedan TODO) + sazetak
dc = OUT / "dependency-check-report.json"
if dc.exists():
    for dep in json.loads(dc.read_text(encoding="utf-8")).get("dependencies", []):
        vulns = dep.get("vulnerabilities", [])
        if not vulns:
            continue
        def score(v):
            return (v.get("cvssv3") or {}).get("baseScore") or \
                   (v.get("cvssv2") or {}).get("score") or 0
        top = max(vulns, key=score)
        dep_findings.append((dep.get("fileName", "?"), len(vulns),
                             score(top), top.get("name", "?")))
    dep_findings.sort(key=lambda x: -x[2])

    summary = OUT / "ovisnosti-todo.md"
    lines = ["# Ranjive ovisnosti (Dependency-Check)", ""]
    for name, n, sc, top in dep_findings:
        lines.append(f"- {name}: {n} CVE, najgori {top} (CVSS {sc})")
    if APPLY:
        summary.write_text("\n".join(lines) + "\n", encoding="utf-8")
    print(f"Sazetak ovisnosti: {summary}")

    pom = ROOT / "pom.xml"
    if dep_findings and pom.exists():
        total = sum(x[1] for x in dep_findings)
        findings.append((pom, None,
                         f"ovisnosti: {len(dep_findings)} ranjivih jara, {total} CVE, "
                         f"vidi alati/prije/ovisnosti-todo.md"))

def comment(path, text):
    if path.suffix in (".xml", ".html"):
        return f"<!-- {text} -->"
    if path.suffix in (".properties", ".yml", ".yaml", ".py"):
        return f"# {text}"
    return f"// {text}"

# Grupiranje po datoteci
by_file = {}
for path, line, text in findings:
    if not path.exists():
        continue
    by_file.setdefault(path, set()).add((line, text))

changed = 0
for path, items in by_file.items():
    src = path.read_text(encoding="utf-8").splitlines()
    # Nalaze bez linije (pom.xml) stavi iznad <parent>, inace na liniju
    fixed = []
    for line, text in items:
        if line is None:
            idx = next((i + 1 for i, l in enumerate(src) if "<parent>" in l), 1)
            fixed.append((idx, text))
        else:
            fixed.append((line, text))
    # Od dna prema vrhu da brojevi linija ostanu ispravni
    for line, text in sorted(fixed, key=lambda x: -x[0]):
        tag = comment(path, f"{MARK}: {text}")
        i = min(max(line - 1, 0), len(src))
        prev = src[i - 1] if i > 0 else ""
        if MARK in prev and text in prev:
            continue  # vec oznaceno
        indent = ""
        if i < len(src):
            indent = src[i][: len(src[i]) - len(src[i].lstrip())]
        print(f"{path.relative_to(ROOT)}:{line}  {tag.strip()}")
        src.insert(i, indent + tag)
        changed += 1
    if APPLY:
        path.write_text("\n".join(src) + "\n", encoding="utf-8")

print(f"\nNalaza za oznaciti: {changed}")
print("Probni rad. Pokreni s --apply za upis." if not APPLY else "Upisano.")
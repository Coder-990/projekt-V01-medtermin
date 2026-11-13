#!/usr/bin/env python3
"""Oznacava nalaze za provjeru; ne potvrduje njihovu iskoristivost.

python3 oznacavanje-ranjovisti.py          # probni rad, bez pisanja
python3 oznacavanje-ranjovisti.py --apply  # upis TODO(sec) komentara i sazetka
"""
import argparse
import io
import json
import re
import tokenize
import xml.etree.ElementTree as ET
from collections import defaultdict
from pathlib import Path
from urllib.parse import unquote, urlparse

ROOT = Path(__file__).resolve().parent
OUT = ROOT / "alati" / "prije"
MARK = "TODO(sec)"
# Uklanjaju se samo komentari koje ovaj alat generira, ne korisnicki TODO-i.
GENERATED = re.compile(r'^\s*(?://|#|<!--) TODO\(sec\): (?:semgrep |spotbugs |ovisnosti:).*?(?: -->)?\s*$')


def flat(value, limit=240):
    value = " ".join(str(value).split())
    return value if len(value) <= limit else value[:limit - 3] + "..."


def clean_source(path):
    return [line for line in path.read_text(encoding="utf-8").splitlines()
            if not GENERATED.match(line)]


def source_path(uri):
    parsed = urlparse(uri)
    if parsed.scheme not in ("", "file"):
        raise ValueError(f"Nepodrzan URI: {uri}")
    name = unquote(parsed.path)
    if name.startswith("/src/"):
        path = ROOT / name[len("/src/"):]
    else:
        path = Path(name)
        if not path.is_absolute():
            path = ROOT / path
    path = path.resolve()
    if not path.is_relative_to(ROOT):
        raise ValueError(f"Putanja izvan projekta: {uri}")
    return path


def safe_anchors(path, lines):
    """Svaki red mapira na mjesto izvan viserednog stringa/komentara."""
    anchors = list(range(len(lines)))
    if path.suffix == ".py":
        for token in tokenize.generate_tokens(io.StringIO("\n".join(lines) + "\n").readline):
            if token.type == tokenize.STRING and token.end[0] > token.start[0]:
                for i in range(token.start[0], token.end[0]):
                    anchors[i] = token.start[0] - 1
    elif path.suffix == ".java":
        state, start = None, 0
        for row, line in enumerate(lines):
            if state:
                anchors[row] = start
            i = 0
            while i < len(line):
                if state == "block":
                    if line.startswith("*/", i):
                        state = None
                        i += 2
                    else:
                        i += 1
                elif state == "text":
                    if line[i] == "\\":
                        i += 2
                    elif line.startswith('"""', i):
                        state = None
                        i += 3
                    else:
                        i += 1
                elif line.startswith("//", i):
                    break
                elif line.startswith("/*", i):
                    state, start = "block", row
                    i += 2
                elif line.startswith('"""', i):
                    state, start = "text", row
                    i += 3
                elif line[i] in ('"', "'"):
                    quote = line[i]
                    i += 1
                    while i < len(line):
                        if line[i] == "\\":
                            i += 2
                        elif line[i] == quote:
                            i += 1
                            break
                        else:
                            i += 1
                else:
                    i += 1
    return anchors


def comment(path, text):
    if path.suffix == ".xml":
        return "<!-- " + text.replace("--", "- -") + " -->"
    return ("# " if path.suffix == ".py" else "// ") + text


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--apply", action="store_true")
    args = parser.parse_args()
    findings, dependencies, warnings = [], [], []
    counts = {}

    sarif = OUT / "semgrep-java.sarif"
    if sarif.exists():
        count = 0
        for run in json.loads(sarif.read_text(encoding="utf-8")).get("runs", []):
            for result in run.get("results", []):
                count += 1
                for location in result.get("locations", [])[:1]:
                    physical = location.get("physicalLocation", {})
                    uri = physical.get("artifactLocation", {}).get("uri")
                    if not uri:
                        warnings.append("Semgrep nalaz bez putanje")
                        continue
                    path = source_path(uri)
                    region = physical.get("region", {})
                    snippet = region.get("snippet", {}).get("text", "")
                    findings.append((path, int(region.get("startLine", 1)), snippet,
                                     f"semgrep {result.get('ruleId', '?')}: "
                                     + flat(result.get("message", {}).get("text", ""))))
        counts["Semgrep"] = count

    spotbugs = OUT / "spotbugs-java.xml"
    if spotbugs.exists():
        bugs = list(ET.parse(spotbugs).getroot().iter("BugInstance"))
        counts["SpotBugs"] = len(bugs)
        for bug in bugs:
            locations = [sl for sl in bug.iter("SourceLine") if sl.get("sourcepath") and sl.get("start")]
            sl = next((sl for sl in locations if sl.get("primary") == "true"), None)
            if sl is None:
                sl = next(iter(locations), None)
            if sl is None:
                warnings.append(f"SpotBugs {bug.get('type')}: nema lokacije")
                continue
            matches = list((ROOT / "src").glob("**/" + sl.get("sourcepath")))
            if len(matches) != 1:
                warnings.append(f"SpotBugs {sl.get('sourcepath')}: nema jednoznacne datoteke")
                continue
            category = bug.get("category", "?")
            message = bug.findtext("LongMessage") or bug.get("type", "?")
            findings.append((matches[0], int(sl.get("start")), "",
                             f"spotbugs {bug.get('type', '?')} [{category}; provjeriti nalaz]: {flat(message)}"))

    dc = OUT / "dependency-check-report.json"
    if dc.exists():
        count = 0
        for dep in json.loads(dc.read_text(encoding="utf-8")).get("dependencies", []):
            for vuln in dep.get("vulnerabilities", []):
                count += 1
                dependencies.append(("Dependency-Check", dep.get("fileName", "?"),
                                     vuln.get("name", "?"), vuln.get("severity", "?")))
        counts["Dependency-Check"] = count

    osv = OUT / "osv-java.txt"
    if osv.exists():
        count = 0
        for line in osv.read_text(encoding="utf-8").splitlines():
            cells = [part.strip() for part in line.strip().strip("|").split("|")]
            if len(cells) >= 7 and re.fullmatch(r"https://osv.dev/GHSA-[\w-]+", cells[0]):
                count += 1
                dependencies.append(("OSV", f"{cells[3]}@{cells[4]}", cells[0].rsplit("/", 1)[1],
                                     "CVSS " + cells[1] + "; fixed " + cells[5]))
        counts["OSV"] = count
        if not count:
            warnings.append("OSV: nije procitan nijedan tablicni nalaz; provjeri format izvjestaja")

    trivy = OUT / "trivy-java.txt"
    if trivy.exists():
        count = 0
        package, version, severity = "?", "?", "?"
        for line in trivy.read_text(encoding="utf-8").splitlines():
            cells = [part.strip() for part in line.split("│")[1:-1]]
            if len(cells) != 7:
                continue
            if cells[0]:
                package, version, severity = cells[0], "?", "?"
            version = cells[4] or version
            severity = cells[2] or severity
            if re.fullmatch(r"(?:CVE-\d{4}-\d+|GHSA-[\w-]+)", cells[1]):
                count += 1
                dependencies.append(("Trivy", f"{package}@{version}", cells[1], severity))
        counts["Trivy"] = count
        if not count:
            warnings.append("Trivy: nije procitan nijedan tablicni nalaz; provjeri format izvjestaja")

    # SCA izvjestaji ne dokazuju lokaciju buga u Java kodu: oznake idu u pom.xml.
    for scanner in ("Dependency-Check", "OSV", "Trivy"):
        if counts.get(scanner):
            findings.append((ROOT / "pom.xml", None, "",
                             f"ovisnosti: {scanner}: {counts[scanner]} nalaza za provjeru; "
                             "vidi alati/prije/ovisnosti-todo.md; prisutnost paketa nije dokaz iskoristivosti"))

    # Prvo izracunaj sve izmjene. Brojevi linija odnose se na kod bez nasih TODO-a.
    sources, grouped = {}, defaultdict(lambda: defaultdict(set))
    for path, line, snippet, text in findings:
        if not path.is_file() or path.suffix not in (".java", ".py", ".xml"):
            warnings.append(f"Preskocena datoteka: {path}")
            continue
        if path not in sources:
            sources[path] = clean_source(path)
        src = sources[path]
        if line is None:
            index = next((i for i, value in enumerate(src) if "<parent>" in value), 1)
        else:
            index = line - 1
            if snippet:
                wanted = [part.strip() for part in snippet.splitlines()]
                matches = [i for i in range(len(src) - len(wanted) + 1)
                           if [part.strip() for part in src[i:i + len(wanted)]] == wanted]
                if len(matches) == 1:
                    index = matches[0]
                elif index < 0 or index + len(wanted) > len(src) or [part.strip() for part in src[index:index + len(wanted)]] != wanted:
                    warnings.append(f"Zastarjela ili nejednoznacna Semgrep lokacija: {path.relative_to(ROOT)}:{line}; ponovi scan")
                    continue
            if not 0 <= index < len(src):
                warnings.append(f"Neispravna linija: {path.relative_to(ROOT)}:{line}")
                continue
            index = safe_anchors(path, src)[index]
        grouped[path][index].add(text)

    plans = {}
    for path, locations in grouped.items():
        src = sources[path].copy()
        for index in sorted(locations, reverse=True):
            indent = src[index][:len(src[index]) - len(src[index].lstrip())]
            tags = [indent + comment(path, f"{MARK}: {text}") for text in sorted(locations[index])]
            for tag in tags:
                print(f"{path.relative_to(ROOT)}:{index + 1}  {tag.strip()}")
            src[index:index] = tags
        plans[path] = "\n".join(src) + "\n"

    summary = ["# Ovisnosti — nalazi za provjeru", "",
               "> Ovo su automatski nalazi skenera, a ne potvrda iskoristivosti u aplikaciji.",
               "> Isti CVE moze se pojaviti u vise alata ili paketa. Brojevi se ne zbrajaju u broj jedinstvenih ranjivosti.", "",
               "Izvori: `dependency-check-report.json`, `osv-java.txt`, `trivy-java.txt` iz ovog direktorija.",
               "Parseri tekstualnih tablica podrzavaju trenutno spremljeni format; JSON je pouzdaniji za buduce skenove.",
               "SpotBugs nalazi mogu ukljucivati kvalitetu koda, a ne samo sigurnost. Za konkretne OSV prioritete vidi `osv-detailed.md`.", "",
               "| Skener | Broj nalaza u izvjestaju |", "| --- | ---: |"]
    summary += [f"| {scanner} | {count} |" for scanner, count in counts.items()]
    summary += ["", "## Ovisnosti", "", "| Skener | Paket / JAR | ID nalaza | Ocjena / podaci |", "| --- | --- | --- | --- |"]
    for row in sorted(set(dependencies)):
        summary.append("| " + " | ".join(flat(cell, 400).replace("|", "\\|") for cell in row) + " |")
    summary += ["", "## Ogranicenja lokacija", "",
                "Semgrep lokacije provjeravaju se prema snippetu kada postoji. SpotBugs linije vrijede za verziju izvornog koda iz vremena skeniranja; nakon drugih izmjena treba ponoviti analizu.",
                "Skripta normalizira samo vlastite generirane TODO komentare pri racunanju linija. Postojeci nalazi za samu ovu skriptu mogu zahtijevati novi Semgrep scan nakon njezine izmjene.", ""]
    if warnings:
        summary += ["## Upozorenja", ""] + ["- " + warning for warning in warnings] + [""]
    for warning in warnings:
        print("UPOZORENJE:", warning)
    if args.apply:
        for path, content in plans.items():
            if path.read_text(encoding="utf-8") != content:
                path.write_text(content, encoding="utf-8")
        (OUT / "ovisnosti-todo.md").write_text("\n".join(summary), encoding="utf-8")
    print("\nIzvjestaji:", ", ".join(f"{k}={v}" for k, v in counts.items()))
    print("TODO oznake:", sum(len(texts) for locs in grouped.values() for texts in locs.values()))
    print("Upisano." if args.apply else "Probni rad: bez pisanja. Za upis koristi --apply.")


if __name__ == "__main__":
    main()

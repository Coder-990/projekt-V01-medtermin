#!/usr/bin/env python3
"""Sigurnosna vrata (quality gate) – jedino mjesto gdje se odlučuje prolazi li cjevovod.

Čita izvještaje iz izvjestaji/ (Semgrep, SpotBugs / Roslyn / Bandit, OSV-Scanner, NuGet audit /
pip-audit, gitleaks, ZAP), primjenjuje POLITIKU (dolje) i iznimke iz ci/prihvaceno.txt te:
  * ispisuje tablicu nalaza (i u sažetak GitHub Actions posla),
  * zapisuje izvjestaji/gate.json,
  * završava s kodom 1 ako postoji ijedan BLOKIRAJUĆI nalaz.

Zatvoreno pri grešci (fail-closed): ako izvještaj nekog alata NEDOSTAJE ili je posao
pao, vrata NE prolaze – alat koji nije radio nije isto što i alat koji ništa nije našao.

Lokalno: python3 ci/gate.py   (GATE_PRESKOCI="dast" preskače kategoriju, npr. bez Dockera)
"""
import fnmatch
import json
import os
import re
import sys
import xml.etree.ElementTree as ET
from urllib.parse import unquote, urlparse

KORIJEN = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
IZV = os.path.join(KORIJEN, "izvjestaji")
PRIHVACENO = os.path.join(KORIJEN, "ci", "prihvaceno.txt")

# ============================ POLITIKA ============================
# Blokira samo VISOKA/KRITIČNA ozbiljnost; ostalo je informativno (vidljivo u izvještaju).
SEMGREP_BLOKIRA = {"ERROR", "CRITICAL", "HIGH"}          # extra.severity
SPOTBUGS_MAX_PRIORITET = 2                                # 1 = High, 2 = Normal (3 = Low -> info)
SPOTBUGS_SAMO_INFO = {                                    # informativni uzorci Find Security Bugs
    "SPRING_ENDPOINT", "CRLF_INJECTION_LOGS", "INFORMATION_EXPOSURE_THROUGH_AN_ERROR_MESSAGE",
}
ROSLYN_KATEGORIJA = "Security"                            # samo sigurnosna CA pravila
ROSLYN_SAMO_INFO = {                                      # pravila "za ručni pregled"
    "CA2100",  # Review SQL queries – javlja SVAKI ne-konstantni SQL; stvarne SQLi hvata CA3001
    "CA5391",  # antiforgery token – API s Bearer tokenom
    "CA5395",  # nedostaje HttpVerb atribut
}
BANDIT_BLOKIRA = {"HIGH"}                                 # issue_severity (MEDIUM/LOW -> info)
BANDIT_MIN_POUZDANOST = {"MEDIUM", "HIGH"}                # issue_confidence (LOW -> info)
OSV_PRAG_CVSS = 7.0                                       # CVSS >= 7.0 = High/Critical
NUGET_BLOKIRA = {"high", "critical"}
# pip-audit ne daje ozbiljnost: ocjena se uzima iz OSV-a za isti savjet (ID ili alias) uz isti prag;
# savjet koji OSV ne poznaje BLOKIRA (ozbiljnost nepoznata -> zatvoreno pri grešci).
ZAP_PRAG_RIZIKA = 3                                       # 3 = High (2 = Medium, 1 = Low)
# gitleaks: SVAKA pronađena tajna blokira.
# ==================================================================

KATEGORIJA = {"semgrep": "sast", "spotbugs": "sast", "roslyn": "sast", "bandit": "sast", "osv": "sca",
              "nuget": "sca", "pip-audit": "sca", "gitleaks": "tajne", "zap": "dast", "ci": "ci"}


def nalaz(alat, pravilo, ozbiljnost, datoteka="", redak=0, redak_do=0, poruka="", blokira=False, kontekst=None):
    od, do = int(redak or 0), int(redak_do or redak or 0)
    return {"alat": alat, "kategorija": KATEGORIJA[alat], "pravilo": pravilo, "ozbiljnost": str(ozbiljnost),
            "datoteka": datoteka, "redak": od, "redak_do": do,
            # redci u kojima se traži "odsječak" iz prihvaceno.txt (SpotBugs: cijela metoda)
            "kontekst": list(kontekst) if kontekst else [od, do],
            "poruka": " ".join(str(poruka).split())[:300], "status": "blokira" if blokira else "info",
            "obrazlozenje": ""}


def rel(putanja):
    """Putanja relativna na korijen repozitorija (izvještaji nastaju na različitim strojevima)."""
    if not putanja:
        return ""
    if putanja.startswith("file:"):
        putanja = unquote(urlparse(putanja).path)
    putanja = putanja.replace("\\", "/")
    k = KORIJEN.replace("\\", "/").rstrip("/") + "/"
    if putanja.startswith(k):
        return putanja[len(k):]
    if os.path.isabs(putanja):
        for oznaka in ("/src/", "/tests/"):
            if oznaka in putanja:
                return putanja[putanja.index(oznaka) + 1:]
    return putanja.lstrip("./")


def ucitaj_json(ime):
    with open(os.path.join(IZV, ime), encoding="utf-8-sig") as f:
        return json.load(f)


# ----------------------------- čitači -----------------------------
def semgrep():
    for r in ucitaj_json("semgrep.json").get("results", []):
        sev = r.get("extra", {}).get("severity", "")
        yield nalaz("semgrep", r["check_id"], sev, r.get("path", ""), r["start"]["line"], r["end"]["line"],
                    r.get("extra", {}).get("message", ""), sev.upper() in SEMGREP_BLOKIRA)


def spotbugs():
    # Izvještaj stvara naš build (pouzdan izvor); Python expat ne razrješava vanjske entitete.
    stablo = ET.parse(os.path.join(IZV, "spotbugs.xml"))  # nosemgrep
    for b in stablo.getroot().iter("BugInstance"):
        if b.get("category") != "SECURITY":
            continue
        # (Element bez djece je "lažan" u bool kontekstu – zato eksplicitna provjera na None.)
        sl = next((e for e in (b.find("SourceLine"), b.find("Method/SourceLine"), b.find("Class/SourceLine"))
                   if e is not None), None)
        putanja, od, do = "", 0, 0
        if sl is not None:
            putanja = "src/main/java/" + sl.get("sourcepath", "")
            od, do = sl.get("start", 0), sl.get("end", 0)
        metoda = b.find("Method/SourceLine")
        kontekst = (metoda.get("start"), metoda.get("end")) if metoda is not None and metoda.get("start") else None
        tip, prio = b.get("type"), int(b.get("priority", 3))
        poruka = (b.findtext("LongMessage") or b.findtext("ShortMessage") or tip)
        blok = prio <= SPOTBUGS_MAX_PRIORITET and tip not in SPOTBUGS_SAMO_INFO
        yield nalaz("spotbugs", tip, f"prioritet {prio}, rang {b.get('rank')}", putanja, od, do, poruka, blok,
                    kontekst=(int(kontekst[0]), int(kontekst[1])) if kontekst else None)


def roslyn():
    podaci = ucitaj_json("roslyn.sarif")
    vidjeno, ostalo = set(), 0
    for run in podaci.get("runs", []):
        kategorije = {r["id"]: r.get("properties", {}).get("category", "")
                      for r in run.get("tool", {}).get("driver", {}).get("rules", [])}
        for r in run.get("results", []):
            pid = r.get("ruleId", "")
            if kategorije.get(pid) != ROSLYN_KATEGORIJA:
                ostalo += 1
                continue
            loc = (r.get("locations") or [{}])[0].get("physicalLocation", {})
            reg = loc.get("region", {})
            putanja = rel(loc.get("artifactLocation", {}).get("uri", ""))
            kljuc = (pid, putanja, reg.get("startLine"))
            if kljuc in vidjeno:
                continue
            vidjeno.add(kljuc)
            n = nalaz("roslyn", pid, r.get("level", "warning"), putanja, reg.get("startLine"), reg.get("endLine"),
                      r.get("message", {}).get("text", ""), pid not in ROSLYN_SAMO_INFO)
            if r.get("suppressions"):
                n["status"], n["obrazlozenje"] = "prihvaceno", "potisnuto u kodu (#pragma / SuppressMessage)"
            yield n
    if ostalo:
        print(f"Roslyn: {ostalo} upozorenja izvan kategorije {ROSLYN_KATEGORIJA} (kvaliteta koda, ne utječu na vrata)")


def bandit():
    podaci = ucitaj_json("bandit.json")
    for g in podaci.get("errors", []) or []:  # datoteka koju Bandit nije analizirao = alat nije radio
        yield nalaz("ci", "bandit-nije-analizirao", "-", rel(g.get("filename", "")), 0, 0,
                    f"Bandit: {g.get('reason', 'greška analize')}", True)
    for r in podaci.get("results", []) or []:
        oz, pouzdanost = str(r.get("issue_severity", "")).upper(), str(r.get("issue_confidence", "")).upper()
        redci = r.get("line_range") or [r.get("line_number", 0)]
        yield nalaz("bandit", r.get("test_id", "?"), f"{oz}, pouzdanost {pouzdanost}", rel(r.get("filename", "")),
                    r.get("line_number"), max(redci), f"{r.get('test_name')}: {r.get('issue_text')}",
                    oz in BANDIT_BLOKIRA and pouzdanost in BANDIT_MIN_POUZDANOST)
    nosec = (podaci.get("metrics", {}).get("_totals", {}) or {}).get("nosec", 0)
    if nosec:
        print(f"Bandit: {nosec} nalaza potisnuto u kodu (# nosec) – provjerite obrazloženja u code reviewu")


def osv_grupe():
    """(paket@verzija, ID-ovi, aliasi, ozbiljnost, blokira, sažetak) za svaku grupu savjeta iz osv.json."""
    for res in ucitaj_json("osv.json").get("results", []) or []:
        for p in res.get("packages", []) or []:
            pkg = p.get("package", {})
            ime = f"{pkg.get('name')}@{pkg.get('version')}"
            vulns = {v["id"]: v for v in p.get("vulnerabilities", []) or []}
            for g in p.get("groups", []) or []:
                ids = g.get("ids") or []
                cvss = g.get("max_severity") or ""
                db = {str(vulns.get(i, {}).get("database_specific", {}).get("severity", "")).upper() for i in ids}
                try:
                    blok = float(cvss) >= OSV_PRAG_CVSS
                except ValueError:
                    blok = bool(db & {"HIGH", "CRITICAL"})
                sazetak = next((vulns[i].get("summary", "") for i in ids if i in vulns), "")
                oz = f"CVSS {cvss}" if cvss else "/".join(sorted(x for x in db if x)) or "nepoznato"
                aliasi = set(g.get("aliases") or []) | {a for i in ids for a in vulns.get(i, {}).get("aliases", [])}
                yield ime, ids, aliasi, oz, blok, sazetak


def osv():
    for ime, ids, _, oz, blok, sazetak in osv_grupe():
        glavni = next((i for i in ids if i.startswith("GHSA")), ids[0] if ids else "?")
        yield nalaz("osv", glavni, oz, ime, 0, 0, f"{ime}: {sazetak} ({', '.join(ids)})", blok)


def nuget():
    for proj in ucitaj_json("nuget-audit.json").get("projects", []):
        for fw in proj.get("frameworks", []):
            for vrsta in ("topLevelPackages", "transitivePackages"):
                for p in fw.get(vrsta, []):
                    for v in p.get("vulnerabilities", []):
                        url = v.get("advisoryurl", "")
                        sev = v.get("severity", "")
                        ime = f"{p.get('id')}@{p.get('resolvedVersion')}"
                        yield nalaz("nuget", url.rstrip("/").rsplit("/", 1)[-1] or "?", sev, ime, 0, 0,
                                    f"{ime} ({'izravna' if vrsta == 'topLevelPackages' else 'tranzitivna'}) {url}",
                                    sev.lower() in NUGET_BLOKIRA)


def pip_audit():
    # Ozbiljnost iz OSV-a (isti savjet pod ID-om ili aliasom); bez osv.json sve je "nepoznato" -> blokira.
    ocjene = {}
    try:
        for _, ids, aliasi, oz, blok, _ in osv_grupe():
            for i in [*ids, *aliasi]:
                if i not in ocjene or blok:
                    ocjene[i] = (oz, blok)
    except Exception:  # noqa: BLE001 – neispravan osv.json prijavljuje čitač osv(); ovdje samo nema ocjena
        pass
    for dep in ucitaj_json("pip-audit.json").get("dependencies", []) or []:
        ime = f"{dep.get('name')}@{dep.get('version')}"
        vidjeno = set()
        for v in dep.get("vulns", []) or []:
            if v.get("id") in vidjeno:
                continue
            vidjeno.add(v.get("id"))
            ids = [v.get("id", "?"), *(v.get("aliases") or [])]
            poznato = [ocjene[i] for i in ids if i in ocjene]
            if poznato:
                oz, blok = next((o for o in poznato if o[1]), poznato[0])
            else:
                oz, blok = "nepoznato (nema u OSV-u)", True
            glavni = next((i for i in ids if i.startswith("GHSA")), ids[0])
            popravak = ", ".join(v.get("fix_versions") or []) or "nema"
            yield nalaz("pip-audit", glavni, oz, ime, 0, 0,
                        f"{ime}: {', '.join(ids)}; popravljeno u: {popravak}", blok)


def gitleaks():
    for run in ucitaj_json("gitleaks.sarif").get("runs", []):
        for r in run.get("results", []):
            loc = (r.get("locations") or [{}])[0].get("physicalLocation", {})
            reg = loc.get("region", {})
            yield nalaz("gitleaks", r.get("ruleId", "?"), "kritično", rel(loc.get("artifactLocation", {}).get("uri", "")),
                        reg.get("startLine"), reg.get("endLine"), r.get("message", {}).get("text", ""), True)


def zap():
    podaci = ucitaj_json("zap/zap.json")
    sites = podaci.get("site", [])
    for s in sites if isinstance(sites, list) else [sites]:
        for a in s.get("alerts", []):
            rizik = int(a.get("riskcode", 0))
            inst = a.get("instances") or [{}]
            url = inst[0].get("uri", "")
            dokaz = inst[0].get("evidence", "")
            yield nalaz("zap", str(a.get("pluginid")), a.get("riskdesc", rizik), urlparse(url).path or url, 0, 0,
                        f"{a.get('alert') or a.get('name')} – {inst[0].get('method', '')} {url} "
                        f"({a.get('count', len(inst))}×) {dokaz}", rizik >= ZAP_PRAG_RIZIKA)
    dodatno = os.path.join(IZV, "zap", "dast-provjere.json")
    if os.path.exists(dodatno):
        for d in ucitaj_json("zap/dast-provjere.json"):
            yield nalaz("zap", "dast-cors", "High", urlparse(d.get("url", "")).path, 0, 0,
                        f"{d.get('naslov')} – {d.get('dokaz')}", int(d.get("rizik", 3)) >= ZAP_PRAG_RIZIKA)


CITACI = {"semgrep.json": semgrep, "spotbugs.xml": spotbugs, "roslyn.sarif": roslyn, "bandit.json": bandit,
          "osv.json": osv, "nuget-audit.json": nuget, "pip-audit.json": pip_audit, "gitleaks.sarif": gitleaks,
          "zap/zap.json": zap}
OCEKIVANO = {
    "java": ["semgrep.json", "spotbugs.xml", "osv.json", "gitleaks.sarif", "zap/zap.json"],
    "dotnet": ["semgrep.json", "roslyn.sarif", "osv.json", "nuget-audit.json", "gitleaks.sarif", "zap/zap.json"],
    "python": ["semgrep.json", "bandit.json", "osv.json", "pip-audit.json", "gitleaks.sarif", "zap/zap.json"],
}
KAT_IZVJESTAJA = {"semgrep.json": "sast", "spotbugs.xml": "sast", "roslyn.sarif": "sast", "bandit.json": "sast",
                  "osv.json": "sca", "nuget-audit.json": "sca", "pip-audit.json": "sca", "gitleaks.sarif": "tajne",
                  "zap/zap.json": "dast"}


def prepoznaj_stack():
    """Isto kao ci/zajednicko.sh: pom.xml -> java, *.sln -> dotnet, pyproject.toml/requirements.txt -> python."""
    if os.path.exists(os.path.join(KORIJEN, "pom.xml")):
        return "java"
    datoteke = os.listdir(KORIJEN)
    if any(d.endswith((".sln", ".slnx")) for d in datoteke):
        return "dotnet"
    if "pyproject.toml" in datoteke or "requirements.txt" in datoteke:
        return "python"
    return "dotnet"


# --------------------------- prihvaćeni nalazi ---------------------------
def ucitaj_prihvacene():
    pravila = []
    if not os.path.exists(PRIHVACENO):
        return pravila
    for i, redak in enumerate(open(PRIHVACENO, encoding="utf-8"), 1):
        if not redak.strip() or redak.lstrip().startswith("#"):
            continue
        dijelovi = [d.strip() for d in redak.rstrip("\n").split("|", 4)]
        if len(dijelovi) != 5 or not dijelovi[4] or not dijelovi[0] or not dijelovi[1]:
            print(f"::warning file=ci/prihvaceno.txt,line={i}::Neispravan redak (treba 5 polja i obrazloženje) – zanemaren")
            continue
        pravila.append({"redak": i, "alat": dijelovi[0], "pravilo": dijelovi[1], "datoteka": dijelovi[2],
                        "odsjecak": dijelovi[3], "obrazlozenje": dijelovi[4], "iskoristeno": 0})
    return pravila


def normaliziraj(t):
    return re.sub(r"\s+", " ", t).strip()


def kod_nalaza(n):
    if not n["datoteka"] or not (n.get("kontekst") or [n["redak"]])[0]:
        return None
    try:
        with open(os.path.join(KORIJEN, n["datoteka"]), encoding="utf-8-sig", errors="replace") as f:
            redci = f.read().splitlines()
    except OSError:
        return None
    od, do = n.get("kontekst") or [n["redak"], n["redak_do"]]
    return normaliziraj(" ".join(redci[max(1, od) - 1:max(od, do)]))


def odgovara(p, n):
    if p["alat"] not in ("*", n["alat"]):
        return False
    pravilo_kratko = n["pravilo"].rsplit(".", 1)[-1]
    if not (fnmatch.fnmatchcase(n["pravilo"], p["pravilo"]) or fnmatch.fnmatchcase(pravilo_kratko, p["pravilo"])):
        return False
    if p["datoteka"] not in ("", "*"):
        # bez "/" = samo ime datoteke; s "/" = cijela putanja ili njezin završetak (npr. Infrastruktura/Baza.cs)
        if "/" in p["datoteka"]:
            ok = fnmatch.fnmatchcase(n["datoteka"], p["datoteka"]) or fnmatch.fnmatchcase(n["datoteka"], "*/" + p["datoteka"])
        else:
            ok = fnmatch.fnmatchcase(n["datoteka"].rsplit("/", 1)[-1], p["datoteka"])
        if not ok:
            return False
    if p["odsjecak"]:
        kod = kod_nalaza(n)
        if kod is None or normaliziraj(p["odsjecak"]) not in kod:
            return False
    return True


# --------------------------------- main ---------------------------------
def main():
    stack = prepoznaj_stack()
    preskoci = set(os.environ.get("GATE_PRESKOCI", "").split())
    nalazi = []

    # Poslovi CI-ja (gate posao dobiva toJSON(needs)): pao posao = vrata ne prolaze.
    try:
        poslovi = json.loads(os.environ.get("REZULTATI_POSLOVA") or "{}")
    except json.JSONDecodeError:
        poslovi = {}
    for ime, info in poslovi.items():
        if info.get("result") != "success":
            nalazi.append(nalaz("ci", f"posao-{ime}", info.get("result"), "", 0, 0,
                                f"Posao '{ime}' nije uspio ({info.get('result')}) – pogledajte njegov log.", True))

    for izv in OCEKIVANO[stack]:
        if KAT_IZVJESTAJA[izv] in preskoci:
            print(f"Preskačem {izv} (GATE_PRESKOCI)")
            continue
        if not os.path.exists(os.path.join(IZV, izv)):
            nalazi.append(nalaz("ci", "nedostaje-izvjestaj", "-", f"izvjestaji/{izv}", 0, 0,
                                f"Nema izvještaja {izv} – alat nije radio, vrata se zatvaraju (fail-closed).", True))
            continue
        try:
            nalazi.extend(CITACI[izv]())
        except Exception as e:  # noqa: BLE001 – neispravan izvještaj također zatvara vrata
            nalazi.append(nalaz("ci", "neispravan-izvjestaj", "-", f"izvjestaji/{izv}", 0, 0, f"{izv}: {e}", True))

    prihvaceni = ucitaj_prihvacene()
    for n in nalazi:
        if n["status"] != "blokira" or n["alat"] == "ci":
            continue
        for p in prihvaceni:
            if odgovara(p, n):
                n["status"], n["obrazlozenje"] = "prihvaceno", f"{p['obrazlozenje']} (prihvaceno.txt:{p['redak']})"
                p["iskoristeno"] += 1
                break

    blokira = [n for n in nalazi if n["status"] == "blokira"]
    prihvaceno = [n for n in nalazi if n["status"] == "prihvaceno"]
    kategorije = {}
    for n in nalazi:
        k = kategorije.setdefault(n["kategorija"], {"blokira": 0, "prihvaceno": 0, "info": 0})
        k[n["status"]] += 1
    prolaz = not blokira

    def red(n):
        lok = n["datoteka"] + (f":{n['redak']}" if n["redak"] else "")
        return f"| {n['alat']} | `{n['pravilo'].rsplit('.', 1)[-1]}` | {n['ozbiljnost']} | {lok} | {n['poruka'][:140]} |"

    izlaz = [f"## Sigurnosna vrata: {'✅ PROLAZ' if prolaz else '❌ NE PROLAZI'}", "",
             "| kategorija | blokira | prihvaćeno | informativno |", "|---|---|---|---|"]
    for k in ("sast", "sca", "tajne", "dast", "ci"):
        if k in kategorije:
            v = kategorije[k]
            izlaz.append(f"| {k.upper()} | {v['blokira']} | {v['prihvaceno']} | {v['info']} |")
    zaglavlje = ["| alat | pravilo | ozbiljnost | mjesto | opis |", "|---|---|---|---|---|"]
    if blokira:
        izlaz += ["", "### Blokirajući nalazi", ""] + zaglavlje + [red(n) for n in blokira]
    if prihvaceno:
        izlaz += ["", "### Prihvaćeni nalazi (s obrazloženjem)", ""] + zaglavlje + \
                 [red(n)[:-1] + f" → *{n['obrazlozenje']}* |" for n in prihvaceno]
    info = [n for n in nalazi if n["status"] == "info"]
    if info:
        izlaz += ["", "<details><summary>Informativni nalazi (ne blokiraju)</summary>", ""] + zaglavlje + \
                 [red(n) for n in info[:200]] + ["", "</details>"]
    neiskoristeni = [p for p in prihvaceni if not p["iskoristeno"]]
    if neiskoristeni:
        izlaz += ["", "Nekorištene iznimke u ci/prihvaceno.txt (možda ih treba obrisati): " +
                  ", ".join(f"redak {p['redak']}" for p in neiskoristeni)]
    tekst = "\n".join(izlaz)
    print(tekst)
    if os.environ.get("GITHUB_STEP_SUMMARY"):
        with open(os.environ["GITHUB_STEP_SUMMARY"], "a", encoding="utf-8") as f:
            f.write(tekst + "\n")
    if os.environ.get("GITHUB_ACTIONS"):
        for n in blokira[:20]:
            mjesto = f"file={n['datoteka']},line={n['redak']}," if n["redak"] else ""
            print(f"::error {mjesto}title={n['alat']} {n['pravilo'].rsplit('.', 1)[-1]}::{n['poruka'][:200]}")

    os.makedirs(IZV, exist_ok=True)
    with open(os.path.join(IZV, "gate.json"), "w", encoding="utf-8") as f:
        json.dump({"prolaz": prolaz, "stack": stack, "kategorije": kategorije, "nalazi": nalazi},
                  f, ensure_ascii=False, indent=2)
    return 0 if prolaz else 1


if __name__ == "__main__":
    sys.exit(main())

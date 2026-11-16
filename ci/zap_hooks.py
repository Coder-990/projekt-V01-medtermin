"""Kuke (hooks) za zap-baseline.py – izvršavaju se UNUTAR ZAP Docker kontejnera.

ZAP baseline je pasivan: spider + pasivna pravila, bez napada. Neautenticiran spider
vidi samo /, /prijava i /api/health, pa ovdje dodajemo:
  1. prijavu testnog korisnika (POST /api/auth/login) i zaglavlje Authorization za
     sve daljnje zahtjeve (ZAP Replacer) – pasivna pravila tada vide i zaštićeni API,
  2. posjet putanjama iz dast-putanje.txt (API nema poveznica koje bi spider pratio),
  3. ciljanu provjeru preusmjeravanja nakon prijave: POST /prijava s vanjskim
     returnUrl – pasivno ZAP pravilo 10028 (Off-site Redirect, High) to prepoznaje,
  4. ciljanu CORS provjeru: zahtjev s Origin: https://zlo.example – ako ga poslužitelj
     reflektira uz Access-Control-Allow-Credentials, dodajemo ZAP upozorenje (High).
Lozinka i token se nikad ne ispisuju.
"""
import json
import os
import urllib.parse

ZLI_ORIGIN = "https://zlo.example"
ZLI_URL = "https://zlo.example/dast-provjera"
WRK = "/zap/wrk"


def _posalji(zap, metoda, url, zaglavlja=None, tijelo=""):
    """Šalje zahtjev KROZ ZAP (ulazi u povijest, pasivno se skenira), bez praćenja preusmjeravanja."""
    host = urllib.parse.urlsplit(url).netloc
    redci = [f"{metoda} {url} HTTP/1.1", f"Host: {host}"]
    for k, v in (zaglavlja or {}).items():
        redci.append(f"{k}: {v}")
    if tijelo:
        redci.append(f"Content-Length: {len(tijelo.encode('utf-8'))}")
    zahtjev = "\r\n".join(redci) + "\r\n\r\n" + tijelo
    poruke = zap.core.send_request(request=zahtjev, followredirects="false")
    return poruke[0] if isinstance(poruke, list) and poruke else poruke


def _zaglavlje(odgovor_zaglavlja, ime):
    for redak in odgovor_zaglavlja.splitlines():
        if ":" in redak and redak.split(":", 1)[0].strip().lower() == ime.lower():
            return redak.split(":", 1)[1].strip()
    return None


def _dodatni_nalaz(zap, poruka, naslov, opis, dokaz, cwe, rjesenje):
    """Dodaje upozorenje u ZAP; ako API to ne podržava, zapisuje u dast-provjere.json (čita ga gate)."""
    try:
        zap.alert.add_alert(messageid=poruka["id"], name=naslov, riskid="3", confidenceid="3",
                            description=opis, param="Origin", attack=ZLI_ORIGIN, evidence=dokaz,
                            solution=rjesenje, cweid=str(cwe), wascid="14")
        print(f"[dast] dodano upozorenje: {naslov}")
    except Exception as e:  # noqa: BLE001
        print(f"[dast] alert API nije dostupan ({e}); zapisujem u dast-provjere.json")
        putanja = os.path.join(WRK, "dast-provjere.json")
        nalazi = json.load(open(putanja)) if os.path.exists(putanja) else []
        nalazi.append({"naslov": naslov, "rizik": 3, "url": poruka.get("requestHeader", "").split(" ")[1],
                       "dokaz": dokaz, "cwe": cwe})
        json.dump(nalazi, open(putanja, "w"), ensure_ascii=False, indent=2)


def zap_started(zap, target):
    baza = target.rstrip("/")
    korisnik = os.environ.get("DAST_KORISNIK", "")
    lozinka = os.environ.get("DAST_LOZINKA", "")
    token = None

    # 1. Prijava (JSON API) -> token
    try:
        tijelo = json.dumps({"username": korisnik, "password": lozinka})
        odg = _posalji(zap, "POST", baza + "/api/auth/login", {"Content-Type": "application/json"}, tijelo)
        token = json.loads(odg.get("responseBody") or "{}").get("token")
        print("[dast] prijava testnog korisnika:", "uspjela" if token else "NIJE uspjela")
    except Exception as e:  # noqa: BLE001
        print(f"[dast] prijava nije uspjela: {e}")

    if token:
        try:
            zap.replacer.add_rule(description="dast-autorizacija", enabled="true", matchtype="REQ_HEADER",
                                  matchregex="false", matchstring="Authorization",
                                  replacement="Bearer " + token)
        except Exception as e:  # noqa: BLE001
            print(f"[dast] Replacer nije dostupan: {e}")

    # 2. Dodatne putanje (API) za pasivna pravila
    try:
        for redak in open(os.path.join(WRK, "dast-putanje.txt"), encoding="utf-8"):
            p = redak.split("#", 1)[0].strip()
            if p:
                zap.core.access_url(url=baza + p, followredirects="false")
    except Exception as e:  # noqa: BLE001
        print(f"[dast] putanje: {e}")

    # 3. Preusmjeravanje nakon prijave na vanjsku adresu (pasivno pravilo 10028)
    try:
        forma = urllib.parse.urlencode({"username": korisnik, "password": lozinka, "returnUrl": ZLI_URL})
        odg = _posalji(zap, "POST", baza + "/prijava", {"Content-Type": "application/x-www-form-urlencoded"}, forma)
        status = (odg.get("responseHeader") or "").split("\r\n")[0]
        print(f"[dast] /prijava s vanjskim returnUrl -> {status}, Location: "
              f"{_zaglavlje(odg.get('responseHeader') or '', 'Location')}")
    except Exception as e:  # noqa: BLE001
        print(f"[dast] provjera preusmjeravanja: {e}")

    # 4. CORS: reflektira li poslužitelj proizvoljan Origin uz vjerodajnice?
    if token:
        for metoda, dodatno in (("GET", {}), ("OPTIONS", {"Access-Control-Request-Method": "GET"})):
            try:
                zag = {"Origin": ZLI_ORIGIN, "Authorization": "Bearer " + token, **dodatno}
                odg = _posalji(zap, metoda, baza + "/api/ja", zag)
                rz = odg.get("responseHeader") or ""
                acao = _zaglavlje(rz, "Access-Control-Allow-Origin")
                acac = (_zaglavlje(rz, "Access-Control-Allow-Credentials") or "").lower()
                print(f"[dast] CORS {metoda} /api/ja: ACAO={acao} ACAC={acac or '-'}")
                if acao in (ZLI_ORIGIN, "*") and (acac == "true" or acao == "*"):
                    _dodatni_nalaz(
                        zap, odg, "CORS: dopušten proizvoljan Origin uz vjerodajnice",
                        "Poslužitelj vraća Access-Control-Allow-Origin s originom napadača "
                        "(i Access-Control-Allow-Credentials: true), pa zlonamjerna stranica može "
                        "čitati odgovore API-ja u ime prijavljenog korisnika.",
                        f"Access-Control-Allow-Origin: {acao}; Access-Control-Allow-Credentials: {acac}",
                        942, "Dopustite samo origine s popisa iz konfiguracije.")
                    break
            except Exception as e:  # noqa: BLE001
                print(f"[dast] CORS provjera ({metoda}): {e}")

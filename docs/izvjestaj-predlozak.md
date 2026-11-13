# Izvještaj o sigurnosnoj analizi i popravcima

**Secure Coding akademija · Tjedan 2 · projekt V01 – MedTermin – naručivanje pregleda**

| | |
|---|---|
| Polaznik | *Ime Prezime* |
| Varijanta | V01 |
| Tehnologija | *Java / Spring Boot*, *C# / .NET* ili *Python / FastAPI* |
| Datum | *dd. mm. 2026.* |
| Korišteni savjeti s Moodlea | *nijedan / razina 1 za … / razina 2 za …* |
| Korišteni AI asistenti | *ne / da – za što* |

## 1. Sažetak

*3–5 rečenica: koliko je ranjivosti pronađeno i ispravljeno, najkritičnije nalaze, stanje nakon popravaka,
pokrivenost koda testovima.*

| Pronađeno | Ispravljeno | Sigurnosnih testova (crveno → zeleno) | Pokrivenost koda |
|---|---|---|---|
| | | | % |

## 2. Metodologija i alati

| Alat | Vrsta (SAST/SCA/DAST/ručno) | Verzija | Naredba / postavke | Izvještaj prije | Izvještaj poslije |
|---|---|---|---|---|---|
| | | | | `alati/prije/…` | `alati/poslije/…` |

## 3. Model prijetnji (sažetak)

*Sažetak iz `model-prijetnji-predlozak.md`: najvažnija imovina, granice povjerenja, 5 najvećih rizika.
Cijeli model priloži ili uključi kao dodatak.*

## 4. Nalazi

*Za svaku ranjivost kopiraj tablicu. Redoslijed: od najviše prema najnižoj ozbiljnosti.*

### N-01: *kratki naslov*

| Polje | Opis |
|---|---|
| Kategorija | OWASP Top 10:2025 *Axx – naziv* · CWE-*xxx* |
| Lokacija | *endpoint (npr. `GET /api/…`), datoteka i metoda (npr. `…Controller.java` / `rezervacije.py`, `metoda()`)* |
| Kako je pronađeno | *alat i pravilo/nalaz (npr. Semgrep `…`, ZAP alert `…`) ili ručni pregled* |
| Ozbiljnost | *Kritična / Visoka / Srednja / Niska* + obrazloženje (ili CVSS 4.0 vektor) |
| Dokaz (reprodukcija) | *koraci; zahtjev i odgovor (isječak); snimka zaslona* |
| Uzrok | *zašto je kod ranjiv – konkretno, s isječkom koda* |
| Popravak | *što je promijenjeno i zašto je to ispravna obrana; isječak koda / commit* |
| Test | *naziv testa; pada na izvornom kodu ✔ / prolazi nakon popravka ✔* |
| Potvrda | *ponovno skeniranje: alat više ne prijavljuje nalaz / ručna provjera* |
| Preporuka | *kako spriječiti ovakve greške ubuduće (npr. pravilo u code reviewu, konfiguracija, biblioteka)* |

## 5. Lažno pozitivni i odbačeni nalazi alata

| Alat | Nalaz | Zašto nije stvarna ranjivost u ovoj aplikaciji |
|---|---|---|
| | | |

## 6. Usklađenost s OWASP ASVS 5.0.0

*Sažetak iz `asvs-kontrolna-lista.md`: broj zahtjeva koje aplikacija ispunjava prije i poslije popravaka;
zahtjevi koji ni nakon popravaka nisu ispunjeni i zašto.*

| | Ispunjeno | Djelomično | Nije ispunjeno | Nije primjenjivo |
|---|---|---|---|---|
| Prije popravaka | | | | |
| Poslije popravaka | | | | |

## 7. Testovi i pokrivenost koda

| Test | Ranjivost | Izvorni kod | Nakon popravka |
|---|---|---|---|
| | N-01 | pada | prolazi |

Pokrivenost koda (linije): *xx %* – alat: *JaCoCo / coverlet*. *Snimka ili isječak izvještaja.*

## 8. Zaključak

*Što je bilo najteže, što bi u stvarnom projektu napravio drugačije (procesi, alati u CI-ju, code review).*

## 9. Izjava

Izjavljujem da sam projekt izradio samostalno i da sve korištene izvore i pomoć navodim u ovom izvještaju.

*Ime Prezime, datum*

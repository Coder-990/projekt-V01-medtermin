# Model prijetnji (STRIDE) – MedTermin – naručivanje pregleda, varijanta V01

Model prijetnji radi se **prije** traženja ranjivosti: pomaže sustavno odrediti gdje tražiti. Na kraju projekta
ga dopuni stvarnim nalazima (stupac „Nalaz”).

## 1. Opis sustava

**Akteri:** *npr. anonimni posjetitelj, prijavljeni korisnik (KORISNIK), LIJECNIK, ADMIN*

**Komponente:** *npr. preglednik / HTTP klijent, REST API, HTML stranice (prijava, ispis), baza podataka,
datotečni sustav (dokumenti), „poštanski sandučić” (datoteke), log, vanjski servis (…)*

**Tokovi podataka i granice povjerenja:** *Nacrtaj jednostavan dijagram (DFD) – rukom, u draw.io ili kao tekst.
Označi gdje podaci prelaze granicu povjerenja (internet → API, API → baza, API → datotečni sustav, …).*

```
[Korisnik/preglednik] --HTTP--> | granica povjerenja | --> [API] --> [Baza]
                                                         |--> [Datoteke]
                                                         |--> [Log]
```

## 2. Imovina

| Imovina | Zašto je vrijedna | Tko joj smije pristupiti |
|---|---|---|
| *npr. lozinke korisnika* | | |
| *npr. osobni podaci (OIB, …)* | | |
| *npr. dokumenti korisnika* | | |
| | | |

## 3. Prijetnje po STRIDE kategorijama

| Element (endpoint / komponenta) | S – lažno predstavljanje | T – neovlaštena izmjena | R – poricanje | I – otkrivanje informacija | D – uskraćivanje usluge | E – podizanje ovlasti |
|---|---|---|---|---|---|---|
| `POST /api/auth/login` | | | | | | |
| `POST /prijava` | | | | | | |
| | | | | | | |

*Upiši konkretnu prijetnju u polje ili „–” ako nije primjenjivo. Prođi kroz sve endpointe iz `http/medtermin.http` i README-a.*

## 4. Procjena rizika i mjere

| # | Prijetnja | Vjerojatnost (1–3) | Utjecaj (1–3) | Rizik (V×U) | Postojeća zaštita | Potrebna mjera | Nalaz |
|---|---|---|---|---|---|---|---|
| P-01 | | | | | | | N-0? |
| | | | | | | | |

## 5. Zaključak

*Koje prijetnje su se pokazale stvarnim ranjivostima, koje su bile dobro zaštićene i što bi model trebao
uključivati u budućim verzijama aplikacije.*

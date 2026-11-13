[README.md](https://github.com/user-attachments/files/34775654/README.md)
# Projekt V01 – MedTermin – naručivanje pregleda

**Secure Coding akademija · Tjedan 2 · samostalni projekt**
Span Centar kibernetičke sigurnosti · mikrokvalifikacija „Implementacija sigurnosnih mehanizama u programskoj podršci”

> ⚠️ Ovo je **tvoja osobna varijanta** (oznaka **V01**). Svaki polaznik ima drugačiji skup ranjivosti –
> ne dijeli projekt ni rješenja s drugima. Aplikacija je **namjerno ranjiva**: pokreći je **samo lokalno** i nikada je
> ne izlaži na mrežu.

## 1. O aplikaciji

Web aplikacija privatne poliklinike. Pacijenti pregledavaju liječnike i slobodne termine, naručuju se na pregled i otkazuju rezervacije, pregledavaju i preuzimaju svoje nalaze te ispisuju potvrdu rezervacije. Liječnici mogu uvesti raspored termina iz XML datoteke.

Funkcionalnosti:

- popis liječnika sa sortiranjem i popis slobodnih termina
- rezervacija pregleda (s napomenom i OIB-om), pregled i otkazivanje vlastitih rezervacija
- pregled nalaza (JSON/PDF prikaz) i preuzimanje datoteke nalaza
- HTML potvrda rezervacije za ispis
- uvoz rasporeda termina iz XML-a (liječnik, administrator)
- korisničke postavke obavijesti u YAML formatu
- registracija, prijava (API i web obrazac), obnova zaboravljene lozinke e-mailom

Ista aplikacija postoji u tri tehnologije – **odaberi jednu** i radi samo u njoj:

| Mapa | Tehnologija | Port | Pokretanje |
|---|---|---|---|
| `java/` | Java 21, Spring Boot 3.5, Maven (wrapper), H2 | 8080 | `./mvnw spring-boot:run` |
| `dotnet/` | C#, .NET 10, ASP.NET Core, SQLite | 5080 | `dotnet run --project src/MedTermin.Api` |
| `python/` | Python 3.12+, FastAPI, SQLite | 8000 | `python -m medtermin` (u virtualnom okruženju) |

Baza je u memoriji i puni se početnim podacima pri svakom pokretanju. „E-pošta” (npr. za obnovu lozinke) se ne
šalje, nego sprema kao datoteka u `data/posta/`. Detalji: [`docs/postavljanje-okruzenja.md`](docs/postavljanje-okruzenja.md)
i `KAKO-POKRENUTI.md` u odabranoj mapi.

### Korisnici

| Korisničko ime | Lozinka | Uloga |
|---|---|---|
| `ana` | `Ana12345!` | KORISNIK |
| `marko` | `Marko12345!` | KORISNIK |
| `dr.horvat` | `Ovlasteni12345!` | LIJECNIK (liječnik) |
| `admin` | `Admin12345!` | ADMIN |

API prijava: `POST /api/auth/login` s `{"username":"ana","password":"Ana12345!"}` vraća token koji se šalje kao
`Authorization: Bearer <token>`. Web prijava: `http://localhost:<port>/prijava`.

## 2. Zadatak

U aplikaciju je namjerno ugrađeno **9 sigurnosnih ranjivosti** iz različitih kategorija OWASP Top 10:2025.
Dijelovi aplikacije koji nisu ranjivi napisani su ispravno – **ne „popravljaj” ono što nije pokvareno**, i svaki
nalaz potkrijepi dokazom.

1. **Postavi okruženje** i pokreni aplikaciju i postojeće testove (`./mvnw test` / `dotnet test` / `pytest`) – funkcionalni
   testovi moraju prolaziti.
2. **Model prijetnji** – opiši aplikaciju (akteri, komponente, granice povjerenja, imovina) i prođi kroz STRIDE
   ([`docs/model-prijetnji-predlozak.md`](docs/model-prijetnji-predlozak.md)).
3. **Skeniraj standardnim alatima** – barem jedan SAST, jedan SCA i jedan DAST alat – i **ručno pregledaj kod**
   ([`docs/alati.md`](docs/alati.md)). Spremi početne izvještaje alata.
4. Za **svaku** pronađenu ranjivost:
   1. **dokaži** je (koraci reprodukcije: zahtjev i odgovor),
   2. **analiziraj uzrok** (datoteka, metoda, zašto je ranjivo),
   3. **napiši test koji pada** na izvornom kodu (crveno),
   4. **ispravi** kod tako da test prolazi (zeleno), a funkcionalni testovi i dalje prolaze,
   5. **potvrdi** popravak ponovnim skeniranjem.
5. **Usklađenost sa standardom** – ispuni [`docs/asvs-kontrolna-lista.md`](docs/asvs-kontrolna-lista.md)
   (odabrani zahtjevi OWASP ASVS 5.0.0) za stanje **prije** i **poslije** popravaka.
6. **Izvještaj** – [`docs/izvjestaj-predlozak.md`](docs/izvjestaj-predlozak.md) ili `izvjestaj-predlozak.docx`.
7. Izmjeri **pokrivenost koda testovima** (JaCoCo / coverlet / pytest-cov – upute u `docs/alati.md`).

## 3. Pravila

- **Ne mijenjaj javno sučelje** aplikacije: URL-ove, nazive polja u JSON-u, nazive konfiguracijskih ključeva i
  tablica u bazi. Ocjenjivanje ih koristi. Smiješ dodavati nove klase, metode, ovisnosti i konfiguraciju.
- **Funkcionalni testovi** (paket/mapa `funkcionalno` / `Funkcionalno`, u Pythonu `tests/funkcionalno/`) se **ne mijenjaju** i moraju prolaziti –
  popravak ne smije ukloniti ni pokvariti funkcionalnost (brisanje endpointa nije popravak).
- Svoje testove stavi u zaseban paket/mapu, npr. `…/sigurnost/` (Java), `tests/MedTermin.Tests/Sigurnost/` (.NET)
  ili `tests/sigurnost/` (Python, s datotekama `test_*.py`). Primjer stila pisanja testa je u paketu/mapi `primjer` / `Primjer`.
- Alate pokreći **samo prema vlastitoj lokalnoj instanci** (`localhost`).
- Dopuštena je sva javna dokumentacija (OWASP Cheat Sheet Series, dokumentacija okvira…). Ako koristiš AI
  asistente, navedi to u izvještaju – svaki popravak moraš znati objasniti.
- **Savjeti** su na Moodleu (razina 1 – gdje tražiti, razina 2 – konkretnije). Smiješ ih koristiti, ali se
  otvaranje bilježi i ulazi u ocjenu („samostalno” / „uz povremenu pomoć” / „uz stalnu pomoć”).

## 4. Predaja – rok **nedjelja, 15. 11. 2026. u 23:59**

Na Moodle zadaću **„Predaja projekta – Tjedan 2”** predaj **jedan ZIP** naziva `V01-Ime-Prezime.zip`:

```
V01-Ime-Prezime.zip
├─ java/, dotnet/ ili python/ ← samo mapa u kojoj si radio, BEZ target/, bin/, obj/, .idea/, .vs/, .venv/, __pycache__/
├─ izvjestaj.pdf (ili .docx)  ← uključuje model prijetnji i ASVS listu (ili kao zasebne datoteke)
└─ alati/                     ← izvještaji alata prije i poslije (npr. ZAP HTML, Semgrep SARIF/JSON, OSV-Scanner)
```

## 5. Ocjenjivanje

Predaja se pokreće sa **skrivenim nastavničkim testovima** (provjeravaju je li svaka od 9 ranjivosti uklonjena i
nije li uvedena nova) i s tvojim testovima nad izvornim kodom (hvataju li ranjivosti). Kriteriji su iz programa
mikrokvalifikacije:

| Element | Izvrstan | Vrlo dobar | Dobar | Dovoljan |
|---|---|---|---|---|
| Pronalaženje i ispravljanje ranjivosti standardnim alatima | samostalno pronalazi i ispravlja sve | samostalno pronalazi sve, uz povremenu pomoć ispravlja | samostalno pronalazi sve, uz pomoć ispravlja | uz pomoć pronalazi i ispravlja |
| Pokrivanje ranjivosti jediničnim testovima | testovi za 100 % ispravljenih ranjivosti | 80 % | 60 % | 40 % |
| Pokrivenost koda jediničnim testovima | ≥ 80 % | ≥ 60 % | ≥ 40 % | ≥ 20 % |
| Usklađenost sa sigurnosnim standardima (ASVS, model prijetnji) | cjelovita, samostalno | cjelovita, uz povremenu pomoć | djelomična, uz povremenu pomoć | djelomična, uz stalnu pomoć |

Za prolaz projekta potrebno je ispraviti **barem 6 od 9** ranjivosti. Na prvom terminu 3. tjedna svaki polaznik
u **5 minuta** pokazuje i objašnjava jedan svoj popravak (kratka usmena provjera).

## 6. Preporučeni raspored tjedna

| Dan | Aktivnost |
|---|---|
| ponedjeljak–utorak | okruženje, upoznavanje aplikacije, model prijetnji, početna skeniranja (spremi izvještaje!) |
| srijeda | analiza nalaza, ručni pregled koda · **online konzultacije** (termin u Obavijestima na Moodleu) |
| četvrtak–petak | popravci i testovi (crveno → zeleno), jedan po jedan |
| subota–nedjelja | ponovna skeniranja, ASVS lista, pokrivenost koda, izvještaj, predaja |

Pitanja postavljaj na forumu **„Pitanja i odgovori – projekt”** (bez objavljivanja rješenja).

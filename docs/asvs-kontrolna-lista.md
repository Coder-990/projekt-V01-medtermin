# Kontrolna lista – OWASP ASVS 5.0.0 (odabrani zahtjevi) – MedTermin – naručivanje pregleda, varijanta V01

OWASP *Application Security Verification Standard* (ASVS) 5.0.0 je standard zahtjeva za provjeru sigurnosti web
aplikacija (razine L1–L3). Za ovaj projekt odabran je podskup zahtjeva. Za svaki zahtjev provjeri stanje
aplikacije **prije** i **poslije** svojih popravaka i upiši:

- **Da** – zahtjev je ispunjen · **Djelomično** · **Ne** · **N/P** – nije primjenjivo (obrazloži)
- **Dokaz** – kako si to provjerio (test, alat, isječak koda, zahtjev/odgovor).

Opisi su skraćeni prijevodi – mjerodavan je izvorni tekst standarda (github.com/OWASP/ASVS, oznaka `v5.0.0`).

| ID | Razina | Zahtjev (skraćeno) | Prije | Poslije | Dokaz |
|---|---|---|---|---|---|
| 1.2.1 | L1 | Izlazno kodiranje (HTTP odgovor, HTML, XML) primjereno je kontekstu u koji se podatak umeće. | | | |
| 1.2.4 | L1 | Upiti prema bazi koriste parametrizirane upite, ORM ili su na drugi način zaštićeni od SQL injectiona. | | | |
| 1.5.1 | L1 | XML parseri su konfigurirani restriktivno; nesigurne mogućnosti poput razrješavanja vanjskih entiteta su isključene. | | | |
| 1.5.2 | L2 | Deserijalizacija nepouzdanih podataka je sigurna (npr. dopušteni popis tipova objekata). | | | |
| 2.3.3 | L2 | Poslovna operacija uspije u cijelosti ili se vrati u prethodno ispravno stanje (transakcije). | | | |
| 2.3.4 | L2 | Resursi ograničene količine zaštićeni su zaključavanjem na razini poslovne logike (npr. dvostruka rezervacija). | | | |
| 3.4.1 | L1 | Odgovori sadrže zaglavlje `Strict-Transport-Security` (HSTS). | | | |
| 3.4.2 | L1 | `Access-Control-Allow-Origin` je fiksna vrijednost ili se zaglavlje `Origin` provjerava prema popisu dopuštenih. | | | |
| 3.4.3 | L2 | Odgovori sadrže zaglavlje `Content-Security-Policy`. | | | |
| 3.7.2 | L2 | Aplikacija automatski preusmjerava samo na odredišta s popisa dopuštenih (ili upozorava korisnika). | | | |
| 5.3.2 | L1 | Pri radu s putanjama datoteka koriste se interno generirani podaci; korisnička imena datoteka se strogo provjeravaju. | | | |
| 6.2.1 | L1 | Lozinke koje postavlja korisnik imaju najmanje 8 znakova. | | | |
| 6.4.3 | L2 | Postupak obnove zaboravljene lozinke je siguran. | | | |
| 8.2.1 | L1 | Pristup funkcijama ograničen je na korisnike s izričitim ovlastima. | | | |
| 8.2.2 | L1 | Pristup konkretnim podacima (objektima) ograničen je na korisnike s izričitim ovlastima (IDOR/BOLA). | | | |
| 8.3.1 | L1 | Pravila autorizacije provode se na pouzdanom sloju poslužitelja, ne na klijentu. | | | |
| 11.4.1 | L1 | Koriste se samo odobrene hash funkcije (ne MD5) za kriptografske namjene. | | | |
| 11.4.2 | L2 | Lozinke se pohranjuju odobrenom, računski zahtjevnom funkcijom za derivaciju ključa (npr. bcrypt, PBKDF2, Argon2). | | | |
| 15.1.2 | L2 | Postoji popis (npr. SBOM) svih komponenti trećih strana. | | | |
| 15.2.1 | L1 | Aplikacija sadrži samo komponente koje nisu prekoračile rok za ažuriranje (bez poznatih ranjivosti). | | | |
| 16.2.5 | L2 | Osjetljivi podaci se logiraju samo u skladu s razinom zaštite (npr. bez lozinki, maskirani osobni podaci). | | | |
| 16.3.1 | L2 | Sve operacije autentikacije se logiraju, uključujući uspješne i neuspješne pokušaje. | | | |
| 16.5.1 | L2 | Kod neočekivane ili sigurnosno osjetljive greške vraća se generička poruka (bez stack tracea i internih detalja). | | | |
| 16.5.3 | L2 | Aplikacija pri iznimci zakazuje sigurno – ne dolazi do „fail-open” ponašanja. | | | |
| 16.5.4 | L3 | Postoji zadnja linija obrane – rukovatelj koji hvata sve neuhvaćene iznimke. | | | |

**Napomena uz 3.4.1:** aplikacija se lokalno pokreće bez TLS-a; obrazloži kako bi se zahtjev ispunio u produkciji.

## Sažetak

| | Da | Djelomično | Ne | N/P |
|---|---|---|---|---|
| Prije popravaka | | | | |
| Poslije popravaka | | | | |

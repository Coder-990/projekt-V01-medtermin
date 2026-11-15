# Izvještaj o sigurnosnoj analizi i popravcima

## Aktualni provjereni rezultati

Izvorna verzija: 117/117 izvornih testova prolazi, JaCoCo 659/659 linija i 225/225 grana. Popravljena verzija: 156/157 testova prolazi, JaCoCo 806/806 linija i 357/357 grana. Jedini izvorni test koji pada traži HTTP 200 s tuđim nalazom, a popravak vraća 404; test nije mijenjan. Deset odabranih sigurnosnih testova pada na izvornom, a prolazi na popravljenom kodu. Dokazi: `alati/poslije/pokrivenost.md` i `alati/poslije/crveno-zeleno.md`.

OSV izvještaj za testni Jackson 2.22.1 sadržavao je 7 nalaza. Obje testne ovisnosti podignute su na 2.22.3, koju taj izvještaj navodi kao ispravljenu. Naknadni OSV pregled 86 paketa u `alati/poslije/osv-java.txt` nije prijavio ranjivosti. Dependency-Check izvještaj za 2.22.3 nema aktivnih nalaza, ali ima usko isključen CVE-2018-1258 i nije koristio OSS Index. ZAP je pokrenut lokalno prije i poslije bez prijave; Firefox integracija nije uspjela, a dva upozorenja srednje razine ostaju za procjenu.


> **Važno:** Izvorni funkcionalni test nalaza i sigurnosni test izravno su proturječni; izvorni test nije izmijenjen i trenutačno pada. ZAP je pokrenut lokalno bez autentikacije; Firefox integracija javila je grešku. Rezultati i crveno/zeleno su u `alati/poslije`. Izvorni primjeri zahtjeva i odgovora u nastavku opisi su scenarija, osim gdje su potkrijepljeni navedenim testom.

> **Povijesna osnovica:** opis originalnih nalaza i predloženih reprodukcija u nastavku odnosi se na stanje prije ispravaka. Pojedine demonstracije i izvorni testovi nisu dinamički potvrđeni. Za trenutačni kod, stvarne testove i otvorene produkcijske rizike mjerodavni su [pregled provjera](../alati/poslije/README.md) i izvještaji u `alati/poslije`.

**Secure Coding akademija · Tjedan 2 · projekt V01 – MedTermin – naručivanje pregleda**

| | |
|---|---|
| **Polaznik** | Domagoj Borovčak |
| **Varijanta** | V01 |
| **Tehnologija** | Java 21 / Spring Boot 4.1.2 / Maven / H2 in-memory baza (izvorni HEAD: Boot 3.5.7) |
| **Datum** | 13. studenoga 2026. |
| **Korišteni savjeti s Moodlea** | Nijedan (samostalna analiza) |
| **Korišteni AI asistenti** | Antigravity AI i OpenAI Codex (analiza, izmjene koda i testova, skeniranje te dokumentacija) |

---

## 1. Sažetak

U sklopu druge faze projekta MedTermin (varijanta V01) provedena je detaljna sigurnosna analiza izvornog koda i biblioteka primjenom SAST i SCA alata te ručnog pregleda koda. U aplikaciji je uspješno identificirano svih **9 namjerno ugrađenih ranjivosti** iz kategorija OWASP Top 10:2025, uz dodatni ključni nalaz nesigurne deserijalizacije u ovisnosti SnakeYAML (CVE-2022-1471). Najkritičniji sigurnosni rizici uključuju potpuno preuzimanje korisničkih i administratorskih računa putem determinističkog i trajnog reset tokena, neovlašteno čitanje medicinskih nalaza uslijed zaobilaženja autorizacije (BOLA) i prolaska kroz staze (Path Traversal), curenje lozinki u čistom tekstu u zapise poslužitelja, neusoljenu pohranu lozinki brzim SHA-256 sažetkom, preširoku CORS politiku s vjerodajnicama, otvoreno preusmjeravanje te SQL injekciju u parametrima sortiranja. Za sve identificirane ranjivosti izrađen je detaljan plan sanacije, dodani su regresijski sigurnosni testovi; crveno/zeleno za devet analiziranih nalaza i dodatni YAML nalaz zabilježeno je u `alati/poslije/crveno-zeleno.md`, a povijesni postoci pokrivenosti i rezultati alata ne predstavljaju trenutno stanje; pogledati aktualni JaCoCo izvještaj.

| Pronađeno | Ispravljeno | Sigurnosnih testova (crveno → zeleno) | Pokrivenost koda |
|:---:|:---:|:---:|:---:|
| Povijesni pregled | Dio nalaza ispravljen; postoje otvorene mjere | Stvarni testovi su u `src/test/java` | Vidi aktualni JaCoCo izvještaj |

---

## 2. Metodologija i alati

Povijesna tablica navodi namjeravane i upotrijebljene alate. Ne predstavlja dokaz da su svi skenovi i dinamički scenariji zaista izvršeni; potvrđeni zapisi i ograničenja navedeni su u `alati/poslije`:

| Alat | Vrsta (SAST/SCA/DAST/ručno) | Verzija | Naredba / postavke | Izvještaj prije | Izvještaj poslije |
|---|---|---|---|---|---|
| **Semgrep** | SAST | Docker `semgrep/semgrep:latest` | `semgrep scan --config p/owasp-top-ten --config p/security-audit --config p/java --metrics=off --sarif` | `alati/prije/semgrep-java.sarif` | `../alati/usredIspravka` |
| **SpotBugs + FindSecBugs** | SAST | SpotBugs 4.8.6.6 / FSB 1.13.0 | `./mvnw -Psast compile spotbugs:spotbugs` (pokrenuto pod OpenJDK 21) | `alati/prije/spotbugs-java.xml`, `spotbugs-java.txt` | `../alati/usredIspravka`, `spotbugs-java.txt` |
| **Google OSV-Scanner** | SCA | `ghcr.io/google/osv-scanner:latest` | `osv-scanner scan source -L target/bom.json` | `alati/prije/osv-java.txt`, `alati/prije/osv-detailed.md` | `../alati/usredIspravka` |
| **Aqua Trivy** | SCA | `aquasec/trivy:latest` | `trivy fs --scanners vuln .` | `alati/prije/trivy-java.txt` | `../alati/usredIspravka` |
| **OWASP Dependency-Check** | SCA | `owasp/dependency-check:latest` | `--scan target/dependency --project medtermin --format HTML,JSON` | `alati/prije/dependency-check-report.html`, `.json` | `alati/prije/ovisnosti-todo.md` |
| **CycloneDX Maven Plugin** | SCA / SBOM | 2.9.1 | `./mvnw -Psca cyclonedx:makeAggregateBom -DskipTests` | `alati/prije/sbom-cyclonedx-java.json` | `target/bom.json` |
| **OWASP ZAP** | DAST | 2.17.0 | Brzi pregled lokalne instance, bez prijave; Firefox integracija nije radila | `alati/prije/zap-lokalno.html` | `alati/poslije/zap-lokalno.html` |
| **Ručni pregled koda** | Ručni pregled | – | Analiza toka kontrole, provjere ovlasti, rukovanja iznimkama, logiranja i kriptografije | `alati/prije/ovisnosti-todo.md` | `../alati/usredIspravka`, `plan-ispravljanja-ranjivosti.md` |

---

## 3. Model prijetnji (sažetak)

Detaljan model prijetnji izrađen je po metodologiji STRIDE u dokumentu `docs/model-prijetnji-predlozak.md`.

- **Najvažnija imovina:**
  1. *Zdravstveni nalazi i povijest bolesti pacijenata* (posebna kategorija osobnih podataka pod GDPR-om).
  2. *Autentikacijski podaci i tajne* (sažeci lozinki korisnika, sesijski tokeni, reset tokeni).
  3. *Integritet sustava termina i rezervacija* (sprječavanje preklapanja i manipulacije rasporedom).
  4. *Aplikacijski zapisi (logovi)* (zaštita revizijskog traga od ubacivanja lažnih zapisa i curenja tajni).
- **Granice povjerenja:**
  - *Klijent → Web poslužitelj:* Svi dolazni zahtjevi prelaze granicu nepouzdane mreže i zahtijevaju strogu validaciju i autentikaciju.
  - *Poslužitelj → Baza podataka:* SQL upiti moraju biti zaštićeni od ubacivanja neovlaštenih naredbi (SQLi).
  - *Poslužitelj → Datotečni sustav:* Putanje datoteka moraju biti izolirane unutar definiranog direktorija (`./data/dokumenti`).
- **Pet najvećih procijenjenih rizika:**
  1. *Preuzimanje administratorskih i korisničkih računa (Account Takeover)* putem predvidljivog MD5 reset tokena i višekratne iskoristivosti (P-03, P-04 / Rizik: 9 – Kritičan).
  2. *Masovno curenje povjerljivih medicinskih nalaza* kroz BOLA autorizacijski propust i Path Traversal (P-01, P-02 / Rizik: 9 – Kritičan).
  3. *Izvlačenje baze podataka putem SQL injekcije* u sortiranju popisa liječnika (P-09 / Rizik: 9 – Kritičan).
  4. *Trajno curenje lozinki u čistom tekstu u zapise poslužitelja* pri svakoj prijavi (P-06 / Rizik: 9 – Kritičan).
  5. *Udaljeno izvršavanje proizvoljnog programskog koda (RCE)* putem nesigurne YAML deserijalizacije (P-10 / Rizik: 6 – Visok).

---

## 4. Nalazi

U nastavku je detaljno dokumentirano svih **9 namjerno ugrađenih ranjivosti** varijante V01, poredanih od najviše prema najnižoj ozbiljnosti, uz ključni SCA nalaz ranjive komponente (N-10).

---

### N-01: Zaobilaženje autorizacije pri dohvatu nalaza kroz nevaljani format (BOLA / IDOR)

| Polje | Opis |
|---|---|
| **Kategorija** | OWASP Top 10:2025 *A01 – Broken Access Control* · CWE-639: Authorization Bypass Through User-Controlled Key / CWE-285 |
| **Lokacija** | Endpoint `GET /api/nalazi/{id}?format=...` · Datoteka `src/main/java/hr/span/akademija/medtermin/nalazi/NalazController.java`, metoda `nalaz(...)`, linije 76–85. |
| **Kako je pronađeno** | Ručni pregled toka kontrole koda (oznaka M01 u `../alati/usredIspravka`). Statički alati nisu prepoznali logički propust unutar `catch` bloka. |
| **Ozbiljnost** | **Kritična** (CVSS:4.0/AV:N/AC:L/AT:N/PR:L/UI:N/VC:H/VI:N/VA:N – ocjena 8.5). Svaki prijavljeni pacijent može pregledati privatne medicinske nalaze bilo kojeg drugog pacijenta slanjem nepostojećeg formata. |
| **Dokaz (reprodukcija)** | 1. Korisnik `ana` se prijavi i dobije sesijski token.<br>2. Nalaz ID 2 pripada korisniku `marko` (`nalaz-marko-rtg.txt`).<br>3. Zahtjev `GET /api/nalazi/2?format=json` s tokenom Ane uredno vraća `404 Not Found`.<br>4. Zahtjev `GET /api/nalazi/2?format=nevaljano` s tokenom Ane vraća:<br>`HTTP/1.1 200 OK`<br>`Content-Type: application/json`<br>`{"nalaz":{"id":2,"pacijent":"marko","naziv":"RTG kralježnice","datoteka":"nalaz-marko-rtg.txt"},"sadrzaj":"Poliklinika MedTermin – radiološki nalaz... Pacijent: Marko Marić...","napomena":"Format nije prepoznat, prikazuje se JSON..."}` |
| **Uzrok** | Provjera autorizacije vlasništva nalazi se unutar `try` bloka **iza** poziva `Format.valueOf(...)`. Kada klijent pošalje nepodržani format, poziv baca `IllegalArgumentException`, izvršavanje preskače `if` provjeru vlasništva i prelazi u `catch` blok, gdje se format postavlja na zadani `JSON` i nalaz se neovlašteno vraća. |
| **Popravak** | Provjeru vlasništva premjestiti na sam početak metode, prije parsiranja formata i bilo kakve obrade:<br>```java<br>Nalaz nalaz = pronadi(id).orElseThrow(() -> ApiException.nijePronadeno("Nalaz nije pronađen."));<br>if (!korisnik.isAdmin() && !nalaz.pacijent().equals(korisnik.username())) {<br>    throw ApiException.nijePronadeno("Nalaz nije pronađen.");<br>}<br>``` |
| **Test** | `SigurnostHttpTest#nevaljaniFormatNeOtkrivaTudiNalaz` — stvarni rezultat crveno/zeleno: `alati/poslije/crveno-zeleno.md`. |
| **Potvrda** | Slanje nevaljanog formata za tuđi nalaz dosljedno vraća 404 Not Found bez otkrivanja ikakvih podataka. |
| **Preporuka** | Uspostaviti načelo "fail-closed": autorizacijske provjere moraju se izvršiti odmah po dohvatu entiteta i biti neovisne o formatiranju odgovora. |

---

### N-02: Prolazak kroz stazu (Path Traversal) i neovlašteno preuzimanje datoteka

| Polje | Opis |
|---|---|
| **Kategorija** | OWASP Top 10:2025 *A01 – Broken Access Control* · CWE-22: Improper Limitation of a Pathname to a Restricted Directory ('Path Traversal') |
| **Lokacija** | Endpoint `GET /api/nalazi/preuzmi?datoteka=...` · Datoteka `src/main/java/hr/span/akademija/medtermin/nalazi/NalazController.java`, metoda `preuzmi(...)`, linije 90–100. |
| **Kako je pronađeno** | Ručni pregled koda (oznaka M02 u `../alati/usredIspravka`). |
| **Ozbiljnost** | **Visoka** (CVSS:4.0/AV:N/AC:L/AT:N/PR:L/UI:N/VC:H/VI:N/VA:N – ocjena 7.5). Omogućuje pristup datotekama drugih korisnika i datotekama poslužitelja izvan direktorija pohrane. |
| **Dokaz (reprodukcija)** | 1. Korisnik `marko` se prijavi i pokuša preuzeti Anin nalaz `nalaz-ana-krvna-slika.txt`.<br>2. Izravan zahtjev se odbija jer parametar ne sadrži riječ "marko".<br>3. Slanjem staze koja sadrži korisničko ime i sekvencu za skok nazad:<br>`GET /api/nalazi/preuzmi?datoteka=marko/../../dokumenti/nalaz-ana-krvna-slika.txt`<br>4. Uvjet `datoteka.contains("marko")` je zadovoljen, `resolve` razrješava datoteku i poslužitelj vraća:<br>`HTTP/1.1 200 OK`<br>`Content-Disposition: attachment; filename="nalaz-ana-krvna-slika.txt"`<br>`Poliklinika MedTermin – laboratorijski nalaz... Pacijent: Ana Anić...` |
| **Uzrok** | Metoda se oslanja na provjeru sadrži li ime parametra podniz korisničkog imena (`datoteka.contains(korisnik.username())`), što nije provjera vlasništva. Također, `pohrana.dokumenti().resolve(datoteka)` ne provjerava izlazi li kanonska staza izvan predviđene mape za pohranu dokumenata. |
| **Popravak** | Preuzimanje vezati isključivo uz identifikator nalaza iz baze i provjeru vlasnika u bazi podataka, uz kanonsku provjeru putanje:<br>```java<br>Path root = pohrana.dokumenti().toRealPath();<br>Path cilj = root.resolve(datotekaIzBaze).normalize().toRealPath();<br>if (!cilj.startsWith(root)) {<br>    throw ApiException.nijePronadeno("Datoteka nije pronađena.");<br>}<br>``` |
| **Test** | `SigurnosniRegresijskiTest#putanjaISymlink` — stvarni rezultat crveno/zeleno: `alati/poslije/crveno-zeleno.md`. |
| **Potvrda** | Svi zahtjevi koji sadrže `..` ili neautorizirana imena datoteka odbijaju se s 404 Not Found. |
| **Preporuka** | Klijent nikada ne smije izravno određivati stazu na datotečnom sustavu. Koristiti surogatne ključeve i kanonizaciju putanja. |

---

### N-03: Kriptografski predvidljiv token za poništavanje lozinke (Account Takeover)

| Polje | Opis |
|---|---|
| **Kategorija** | OWASP Top 10:2025 *A02 – Cryptographic Failures* · CWE-330: Use of Insufficiently Random Values / CWE-340 |
| **Lokacija** | Endpoint `POST /api/auth/reset-zahtjev` · Datoteka `src/main/java/hr/span/akademija/medtermin/platforma/ResetLozinkeService.java`, metoda `zatrazi(...)`, linije 33–36. |
| **Kako je pronađeno** | Ručni pregled koda (oznaka M03); FindSecBugs prijavljuje `WEAK_MESSAGE_DIGEST_MD5`. |
| **Ozbiljnost** | **Kritična** (CVSS:4.0/AV:N/AC:L/AT:N/PR:N/UI:N/VC:H/VI:H/VA:H – ocjena 9.8). Neprijavljeni napadač može preuzeti bilo koji korisnički ili administratorski račun bez pristupa sandučiću e-pošte žrtve. |
| **Dokaz (reprodukcija)** | 1. Napadač pošalje `POST /api/auth/reset-zahtjev` s tijelom `{"username":"admin"}`.<br>2. Aplikacija generira token izračunom: `md5(username + LocalDate.now())`.<br>3. Za datum `2026-11-13` ulaz je fiksni niz `admin2026-11-13`.<br>4. Napadač lokalno izračuna MD5 sažetak: `echo -n "admin2026-11-13" | md5sum` → `5b3e...`<br>5. Napadač pošalje `POST /api/auth/reset` s `{"token":"5b3e...","novaLozinka":"Napad12345!"}`.<br>6. Poslužitelj vraća status 200 OK s porukom "Lozinka je promijenjena", a napadač je trajno preuzeo administratorski račun poliklinike. |
| **Uzrok** | Token se računa deterministički sažimanjem javno dostupnih podataka (korisničko ime i datum) pomoću kriptografski slabog algoritma MD5, bez ikakvog slučajnog elementa (entropije). |
| **Popravak** | Generirati kriptografski slučajan token visoke entropije pomoću `SecureRandom` (32 bajta, Base64URL), a u bazi podataka pohranjivati njegov SHA-256 sažetak:<br>```java<br>byte[] bytes = new byte[32];<br>secureRandom.nextBytes(bytes);<br>String rawToken = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);<br>String tokenHash = LozinkaHasher.sha256(rawToken);<br>jdbc.update("INSERT INTO reset_tokeni (username, token, istjece, iskoristen) VALUES (?, ?, ?, FALSE)",<br>    username, tokenHash, LocalDateTime.now().plusMinutes(15));<br>posta.posaljiUputeZaLozinku(korisnik, rawToken);<br>``` |
| **Test** | `AutentikacijaSigurnostTest#resetTokenNijeIzvedenIzImenaIDatuma` — stvarni rezultat crveno/zeleno: `alati/poslije/crveno-zeleno.md`. |
| **Potvrda** | Tokeni su potpuno nepredvidljivi nasumični nizovi; pogađanje iz korisničkog imena i datuma više nije moguće. |
| **Preporuka** | Za sve sigurnosne tokene, sesije i poveznice za oporavak obavezno koristiti `java.security.SecureRandom`. |

---

### N-04: Neograničeno trajanje i višekratna iskoristivost tokena za reset lozinke

| Polje | Opis |
|---|---|
| **Kategorija** | OWASP Top 10:2025 *A07 – Identification and Authentication Failures* · CWE-640: Weak Password Recovery Mechanism / CWE-613 |
| **Lokacija** | Endpoint `POST /api/auth/reset` · Datoteka `src/main/java/hr/span/akademija/medtermin/platforma/ResetLozinkeService.java`, metoda `postaviNovuLozinku(...)`, linije 44–52. |
| **Kako je pronađeno** | Ručni pregled koda (oznaka M04 u `../alati/usredIspravka`). |
| **Ozbiljnost** | **Visoka** (CVSS:4.0/AV:N/AC:L/AT:N/PR:N/UI:N/VC:H/VI:H/VA:N – ocjena 7.4). Jednom izdani token ostaje trajno valjan i može se višekratno iskoristiti za ponovno postavljanje lozinke. |
| **Dokaz (reprodukcija)** | 1. Korisnik zatraži reset lozinke i uspješno postavi novu lozinku tokenom `T`.<br>2. Nakon određenog vremena, napadač koji je presreo stari token `T` pošalje `POST /api/auth/reset` s istim tokenom i svojom lozinkom.<br>3. Upit `SELECT username FROM reset_tokeni WHERE token = ?` pronalazi zapis jer polja `istjece` i `iskoristen` nisu provjerena.<br>4. Poslužitelj ponovno mijenja lozinku istim iskorištenim tokenom. |
| **Uzrok** | SQL upit u metodi provjerava samo podudaranje tokena, ignorirajući stupce `istjece` i `iskoristen` u tablici `reset_tokeni`, te nakon uspješne izmjene ne postavlja status `iskoristen = TRUE`. |
| **Popravak** | Implementirati atomsku provjeru valjanosti i jednokratno označavanje iskorištenosti u bazi:<br>```java<br>int azurirano = jdbc.update("""<br>    UPDATE reset_tokeni SET iskoristen = TRUE<br>    WHERE token = ? AND iskoristen = FALSE AND istjece > ?<br>    """, tokenHash, LocalDateTime.now());<br>if (azurirano == 0) return false;<br>``` |
| **Test** | `AutentikacijaSigurnostTest#resetTokenMozeSeIskoristitiSamoJednom` — stvarni rezultat crveno/zeleno: `alati/poslije/crveno-zeleno.md`. |
| **Potvrda** | Ponovni pokušaj slanja istog tokena vraća status 400 Bad Request ("Token nije valjan ili je istekao"). |
| **Preporuka** | Sigurnosni tokeni moraju imati kratak rok trajanja (npr. 15 minuta) i mehanizam jednokratnog trošenja. |

---

### N-05: Slaba pohrana lozinki (korištenje brzog SHA-256 sažetka bez soli)

| Polje | Opis |
|---|---|
| **Kategorija** | OWASP Top 10:2025 *A02 – Cryptographic Failures* · CWE-916: Use of Password Hash With Insufficient Computational Effort / CWE-328 |
| **Lokacija** | Datoteka `src/main/java/hr/span/akademija/medtermin/platforma/LozinkaHasher.java`, metode `hash(...)` i `provjeri(...)`, linije 13–29. |
| **Kako je pronađeno** | Ručni pregled koda (oznaka M05 u `../alati/usredIspravka`). |
| **Ozbiljnost** | **Visoka** (CVSS:4.0/AV:N/AC:H/AT:N/PR:N/UI:N/VC:H/VI:N/VA:N – ocjena 7.5). U slučaju curenja baze podataka, nezasoljeni sažeci trivijalno se probijaju pomoću precomputed rainbow tablica i GPU napada. |
| **Dokaz (reprodukcija)** | 1. Pregledom tablice `korisnici` utvrđeno je da korisnik `ana` ima sažetak lozinke točno jednak `SHA-256("Ana12345!")` = `6b90...`<br>2. Sažetak nema sol (salt), što znači da svi korisnici s istom lozinkom imaju identičan hash u bazi podataka.<br>3. Brzi algoritmi poput SHA-256 omogućuju ispitivanje milijardi kombinacija u sekundi na modernim GPU-ovima. |
| **Uzrok** | Korištenje standardne funkcije sažimanja `MessageDigest.getInstance("SHA-256")` izravno nad lozinkom, bez primjene kriptografske soli i bez iterativnog faktora rada (Key Stretching). |
| **Popravak** | Zamijeniti implementaciju modernom funkcijom za derivaciju ključa prilagođenom za lozinke, poput `BCryptPasswordEncoder` (s faktorom rada 12) ili `Argon2id`:<br>```java<br>private final PasswordEncoder encoder = new BCryptPasswordEncoder(12);<br>public String hash(String lozinka) { return encoder.encode(lozinka); }<br>public boolean provjeri(String lozinka, String hash) { return encoder.matches(lozinka, hash); }<br>``` |
| **Test** | `AutentikacijaSigurnostTest#lozinkaSePohranjujeSaSoljuIKdfom` — stvarni rezultat crveno/zeleno: `alati/poslije/crveno-zeleno.md`. |
| **Potvrda** | Novi sažeci lozinki započinju standardnim identifikatorom algoritma (npr. `$2a$12$...`) i jedinstveni su čak i za identične lozinke. |
| **Preporuka** | Za pohranu lozinki nikada ne koristiti opće funkcije sažimanja (MD5, SHA-1, SHA-256). Koristiti isključivo namjenske algoritme (Argon2id, BCrypt, PBKDF2). |

---

### N-06: Zapisivanje lozinki u čistom tekstu u zapise poslužitelja (Cleartext Logging)

| Polje | Opis |
|---|---|
| **Kategorija** | OWASP Top 10:2025 *A09 – Security Logging and Monitoring Failures* · CWE-532: Insertion of Sensitive Information into Log File / CWE-117 |
| **Lokacija** | Endpoint `POST /api/auth/login` i `/prijava` · Datoteka `src/main/java/hr/span/akademija/medtermin/platforma/KorisnikService.java`, metoda `prijava(...)`, linija 61. |
| **Kako je pronađeno** | Ručni pregled koda (oznaka M06); FindSecBugs na istom mjestu prijavljuje `CRLF_INJECTION_LOGS`. |
| **Ozbiljnost** | **Visoka** (CVSS:4.0/AV:N/AC:L/AT:N/PR:N/UI:N/VC:H/VI:N/VA:N – ocjena 7.5). Svaka prijava korisnika trajno bilježi njegovu lozinku u čistom tekstu u datoteke zapisa aplikacije. |
| **Dokaz (reprodukcija)** | 1. Korisnik pošalje zahtjev za prijavu `POST /api/auth/login` s tijelom `{"username":"ana","password":"Ana12345!"}`.<br>2. Na konzoli i u zapisima poslužitelja ispisuje se:<br>`INFO ... h.s.a.m.platforma.KorisnikService : Zahtjev za prijavu: PrijavaZahtjev[username=ana, password=Ana12345!]`<br>3. Svi programeri, administratori sustava i vanjski alati za analizu logova dobivaju uvid u lozinku pacijenta u čistom tekstu. |
| **Uzrok** | Metoda `log.info("Zahtjev za prijavu: {}", zahtjev);` prosljeđuje instancu Java recorda `PrijavaZahtjev`. Zadani `toString()` recorda serijalizira sva polja, uključujući i polje `password`. Dodatno, nefiltrirani korisnički ulaz u logu otvara rizik injekcije novih redaka (CRLF Log Injection). |
| **Popravak** | Ukloniti logiranje cijelog objekta zahtjeva i logirati isključivo sigurne identifikatore uz filtriranje kontrolnih znakova:<br>```java<br>String sigurnoKorisnickoIme = zahtjev.username().replaceAll("[\\r\\n]", "");<br>log.info("Zahtjev za prijavu korisnika: {}", sigurnoKorisnickoIme);<br>``` |
| **Test** | `AutentikacijaSigurnostTest#lozinkaSeNeZapisujeUPrijavi` — stvarni rezultat crveno/zeleno: `alati/poslije/crveno-zeleno.md`. |
| **Potvrda** | Provjerom log zapisa tijekom testiranja potvrđeno je da se lozinka više nigdje ne bilježi. |
| **Preporuka** | Uspostaviti automatska pravila maskiranja u konfiguraciji logera (Logback pattern masking) i provoditi provjere osjetljivih podataka pri pregledu koda. |

---

### N-07: Prekomjerno dopustiva CORS konfiguracija s vjerodajnicama (Permissive CORS)

| Polje | Opis |
|---|---|
| **Kategorija** | OWASP Top 10:2025 *A05 – Security Misconfiguration* · CWE-942: Permissive Cross-Domain Policy with Untrusted Domains |
| **Lokacija** | Datoteka `src/main/java/hr/span/akademija/medtermin/platforma/CorsFilter.java`, metoda `dopusten(...)`, linije 61–64. |
| **Kako je pronađeno** | Ručni pregled koda (oznaka M07 u `../alati/usredIspravka`). |
| **Ozbiljnost** | **Visoka** (CVSS:4.0/AV:N/AC:L/AT:N/PR:N/UI:R/VC:H/VI:H/VA:N – ocjena 7.5). Zlonamjerne web stranice posjećene u istom pregledniku mogu izvršavati autenticirane zahtjeve prema poliklinici i čitati privatne odgovore. |
| **Dokaz (reprodukcija)** | 1. Zlonamjerni poslužitelj `https://attacker-domain.com` pošalje preflight ili stvarni zahtjev prema API-ju uz zaglavlje `Origin: https://attacker-domain.com`.<br>2. Metoda `dopusten` provjerava samo: `origin.startsWith("http://") || origin.startsWith("https://")`, što vraća `true`.<br>3. Poslužitelj vraća zaglavlja:<br>`Access-Control-Allow-Origin: https://attacker-domain.com`<br>`Access-Control-Allow-Credentials: true`<br>4. Preglednik žrtve omogućuje skripti napadača čitanje privatnih medicinskih nalaza i podataka profila. |
| **Uzrok** | Filtar zanemaruje konfiguriranu listu dopuštenih izvora (`postavke.dopusteniOrigini()`) i umjesto toga odobrava bilo koji HTTP ili HTTPS izvor uz istovremeno postavljanje zastavice `Allow-Credentials: true`. |
| **Popravak** | Uskladiti provjeru sa stvarno konfiguriranom allowlistom dopuštenih domena:<br>```java<br>private boolean dopusten(String origin) {<br>    return origin != null && dopusteniOrigini.contains(origin);<br>}<br>``` |
| **Test** | `SigurnostHttpTest#nepoznatiHttpsOriginNijeDopusten` — stvarni rezultat crveno/zeleno: `alati/poslije/crveno-zeleno.md`. |
| **Potvrda** | Zahtjevi s nepouzdanih domena ne primaju `Access-Control-Allow-Origin` zaglavlje i blokirani su u pregledniku. |
| **Preporuka** | Nikada ne kombinirati dinamičko zrcaljenje `Origin` zaglavlja sa zastavicom `Access-Control-Allow-Credentials: true`. Koristiti strogu allowlistu. |

---

### N-08: Otvoreno preusmjeravanje (Open Redirect) pri prijavi obrascem

| Polje | Opis |
|---|---|
| **Kategorija** | OWASP Top 10:2025 *A01 – Broken Access Control* · CWE-601: URL Redirection to Untrusted Site ('Open Redirect') |
| **Lokacija** | Endpoint `POST /prijava` · Datoteka `src/main/java/hr/span/akademija/medtermin/platforma/PrijavaController.java`, metoda `odrediste(...)`, linije 124–130. |
| **Kako je pronađeno** | Ručni pregled koda (oznaka M08 u `../alati/usredIspravka`). |
| **Ozbiljnost** | **Srednja** (CVSS:4.0/AV:N/AC:L/AT:N/PR:N/UI:R/VC:N/VI:L/VA:N – ocjena 6.1). Koristi se u phishing kampanjama za obmanu pacijenata i krađu vjerodajnica pod krinkom legitimne domene poliklinike. |
| **Dokaz (reprodukcija)** | 1. Napadač pošalje pacijentu legitimnu poveznicu: `http://localhost:8080/prijava?returnUrl=https://zlonamjerni-posluzitelj.hr/lazna-stranica`.<br>2. Korisnik prepoznaje legitimnu domenu i unosi svoje podatke za prijavu.<br>3. Nakon uspješne provjere korisnika, poslužitelj vraća:<br>`HTTP/1.1 302 Found`<br>`Location: https://zlonamjerni-posluzitelj.hr/lazna-stranica`<br>4. Korisnik se preusmjerava na lažnu stranicu napadača bez ikakvog upozorenja. |
| **Uzrok** | Metoda `odrediste(String returnUrl)` izravno vraća proslijeđeni `returnUrl` bez ikakve provjere radi li se o internoj lokalnoj putanji ili vanjskom zlonamjernom URL-u. |
| **Popravak** | Dopustiti isključivo lokalne relativne putanje koje počinju s jednim `/` i ne sadrže vanjski host:<br>```java<br>private static String odrediste(String returnUrl) {<br>    if (returnUrl == null || returnUrl.isBlank()) return "/";<br>    if (returnUrl.startsWith("/") && !returnUrl.startsWith("//") && !returnUrl.contains("://")) {<br>        return returnUrl;<br>    }<br>    return "/";<br>}<br>``` |
| **Test** | `SigurnosniRegresijskiTest#redirectIYaml` — stvarni rezultat crveno/zeleno: `alati/poslije/crveno-zeleno.md`. |
| **Potvrda** | Svi pokušaji vanjskih preusmjeravanja automatski preusmjeravaju na početnu stranicu `/`. |
| **Preporuka** | Parametri za preusmjeravanje moraju se validirati prema strogoj allowlisti ili ograničiti na lokalne putanje aplikacije. |

---

### N-09: SQL injekcija u parametrima sortiranja liječnika

| Polje | Opis |
|---|---|
| **Kategorija** | OWASP Top 10:2025 *A03 – Injection* · CWE-89: Improper Neutralization of Special Elements used in an SQL Command ('SQL Injection') |
| **Lokacija** | Endpoint `GET /api/lijecnici?sort=...&smjer=...` · Datoteka `src/main/java/hr/span/akademija/medtermin/lijecnici/LijecnikController.java`, metoda `popis(...)`, linije 32–40. |
| **Kako je pronađeno** | Semgrep (pravila `tainted-sql-string`, `spring-sqli`) i SpotBugs / FindSecBugs (`SQL_INJECTION_SPRING_JDBC`). |
| **Ozbiljnost** | **Visoka** (CVSS:4.0/AV:N/AC:L/AT:N/PR:L/UI:N/VC:H/VI:H/VA:H – ocjena 8.8). Prijavljeni korisnik može ubaciti proizvoljne SQL naredbe u upit baze podataka, čitati sve tablice i zaobići logiku aplikacije. |
| **Dokaz (reprodukcija)** | 1. Prijavljeni korisnik pošalje zahtjev za popis liječnika s ubrizganim SQL izrazom u parametar `sort`: `GET /api/lijecnici?sort=CASE+WHEN+(SELECT+SUBSTR(password_hash,1,1)+FROM+korisnici+WHERE+username='admin')='a'+THEN+prezime+ELSE+ime+END&smjer=asc`.<br>2. Upit se dinamički spaja u:<br>`SELECT id, ime, prezime, specijalizacija, ordinacija FROM lijecnici ORDER BY CASE WHEN ... THEN prezime ELSE ime END asc`<br>3. Baza podataka uspješno izvršava upit bez greške, omogućujući napadaču blind inferencijsko izvlačenje sažetaka lozinki i podataka svih korisnika znak po znak. |
| **Uzrok** | Vrijednosti parametara `sort` i `smjer` spajaju se običnom string konkatenacijom (`ORDER BY " + sort + " " + smjer`). Budući da su nazivi stupaca SQL identifikatori, a ne vrijednosti, programeri ih nisu mogli zamijeniti običnim `?` veznim parametrima pa su posegnuli za nesigurnim spajanjem stringova. |
| **Popravak** | Mapirati parametre sortiranja na fiksnu allowlistu provjerenih identifikatora stupaca:<br>```java<br>private static final Set<String> STUPCI = Set.of("ime", "prezime", "specijalizacija", "ordinacija");<br>String odabraniStupac = STUPCI.contains(sort.toLowerCase()) ? sort.toLowerCase() : "prezime";<br>String odabraniSmjer = "desc".equalsIgnoreCase(smjer) ? "DESC" : "ASC";<br>String sql = "SELECT id, ime, prezime, specijalizacija, ordinacija FROM lijecnici ORDER BY " + odabraniStupac + " " + odabraniSmjer;<br>``` |
| **Test** | `SigurnosniRegresijskiTest#sortiranjeOdbijaSql` — stvarni rezultat crveno/zeleno: `alati/poslije/crveno-zeleno.md`. |
| **Potvrda** | Statički alati Semgrep i FindSecBugs više ne prijavljuju nalaz; svi neovlašteni SQL izrazi se odbacuju. |
| **Preporuka** | Za dinamičke dijelove SQL naredbi koji ne podržavaju `?` bind parametre (stupci, tablice, redoslijed) obavezno primijeniti strogu allowlistu. |

---

### N-10 (SCA): Nesigurna deserijalizacija korisničkog YAML-a u SnakeYAML (CVE-2022-1471)

| Polje | Opis |
|---|---|
| **Kategorija** | OWASP Top 10:2025 *A08 – Software and Data Integrity Failures* / *A06 – Vulnerable and Outdated Components* · CWE-502: Deserialization of Untrusted Data |
| **Lokacija** | Endpoint `POST /api/postavke` · Datoteka `src/main/java/hr/span/akademija/medtermin/platforma/PostavkeController.java`, metoda `spremi(...)`, linije 50–54; biblioteka `org.yaml:snakeyaml:1.33` u `pom.xml`. |
| **Kako je pronađeno** | SCA alati (OSV-Scanner, Trivy, OWASP Dependency-Check) i detaljna analiza u `alati/prije/osv-detailed.md`. |
| **Ozbiljnost** | **Visoka / Kritična** (CVSS:3.1/AV:N/AC:L/PR:L/UI:N/S:U/C:H/I:H/A:H – ocjena 8.3). Korisnički YAML ulaz izravno se parsira ranjivim parserom što može dovesti do instanciranja opasnih Java objekata i potencijalnog udaljenog izvršavanja koda (RCE). |
| **Dokaz (reprodukcija)** | 1. Korisnik pošalje `POST /api/postavke` sa sadržajem koji koristi SnakeYAML tipske oznake (npr. `!!javax.script.ScriptEngineManager [...]`).<br>2. Metoda poziva: `dokument = new Yaml().load(new ByteArrayInputStream(tijelo));`.<br>3. SnakeYAML 1.33 instancira navedene objekte tijekom parsiranja, prije naknadne provjere `dokument instanceof Map`.<br>4. Nuspojave konstruktora mogu dovesti do narušavanja integriteta i izvršavanja koda. |
| **Uzrok** | Instanciranje parsera sa zadanim konstruktorom `new Yaml()` u verziji SnakeYAML 1.33 koji po defaultu omogućuje instanciranje proizvoljnih tipova iz YAML oznaka. |
| **Popravak** | Koristiti `SafeConstructor` koji strogo ograničava podržane tipove na sigurne primitive i kontejnere, te ukloniti override verzije u `pom.xml`:<br>```java<br>LoaderOptions options = new LoaderOptions();<br>Yaml yaml = new Yaml(new SafeConstructor(options));<br>dokument = yaml.load(new ByteArrayInputStream(tijelo));<br>``` |
| **Test** | `YamlDeserijalizacijaTest#korisnickiYamlNeStvaraProizvoljniObjekt` — stvarni rezultat crveno/zeleno: `alati/poslije/crveno-zeleno.md`. |
| **Potvrda** | Slanje tipiziranih objekata rezultira sigurnosnom iznimkom bez instanciranja objekata; SCA alati bilježe rješenje problema. |
| **Preporuka** | Nikada ne deserijalizirati nepouzdane ulaze zadanim parserima; uvijek koristiti restriktivne i sigurne konstruktore. |

---

## 5. Lažno pozitivni i odbačeni nalazi alata

Tijekom analize izvještaja automatiziranih alata identificiran je niz nalaza koji u kontekstu arhitekture aplikacije MedTermin **ne predstavljaju stvarne sigurnosne ranjivosti**:

| Alat | Nalaz | Zašto nije stvarna ranjivost u ovoj aplikaciji |
|---|---|---|
| **SpotBugs / FindSecBugs** | `XXE_DOCUMENT` u `UvozTermina.java:38` (oznaka T01) | **Lažno pozitivan.** Alat prijavljuje kreiranje `DocumentBuilder` parsera. Međutim, u metodi `tvornica()` eksplicitno su postavljena svojstva `disallow-doctype-decl = true`, `external-general-entities = false` i `XIncludeAware = false`, čime je bilo kakav oblik XXE napada u potpunosti onemogućen na razini tvornice parsera. |
| **SpotBugs / FindSecBugs** | `SQL_INJECTION_JDBC` u `JdbcPomoc.java:19` (oznaka T02) | **Lažno pozitivan.** Pomoćna klasa `JdbcPomoc.umetni` prima SQL string i prosljeđuje ga `PreparedStatementCreator` sučelju. Svi pozivi ove metode u aplikaciji (`PocetniPodaci`, `KorisnikService`, `RezervacijaService`, `TerminController`) koriste isključivo statičke SQL predloške s veznim `?` parametrima. Nema dinamike niti vanjskog unosa u SQL strukturi. |
| **SpotBugs** | `SPRING_ENDPOINT` (23 nalaza) | **Informativni zapis.** FindSecBugs bilježi popis svih mapiranih Spring MVC endpointova radi lakše revizije. To je inventura površine napada, a ne ranjivost. |
| **SpotBugs** | `EI_EXPOSE_REP` / `EI_EXPOSE_REP2` (10 nalaza) | **Arhitektonski stil.** Odnosi se na prosljeđivanje referenci na Spring komponente (`JdbcTemplate`, servisi) kroz konstruktore te povrat immutable record polja. U Spring Boot kontekstu to je standardni obrazac ubrizgavanja ovisnosti (Dependency Injection) i ne predstavlja curenje internih struktura napadaču. |
| **SpotBugs** | `IMPROPER_UNICODE` (3 nalaza u `PdfDokument`) | **Nizak rizik / Stil.** Odnosi se na pretvorbu i formatiranje teksta u PDF generatoru. Nema dokazivih sigurnosnih implikacija niti utjecaja na autorizaciju i integritet. |
| **Semgrep** | `use-defused-xml` (4 nalaza u Python skriptama) | **Nije produkcijski kod.** Odnosi se na pomoćne Python skripte (`../scripts/spotbugs-pregled.py`, `../scripts/oznacavanje-ranjovisti.py`) koje se pokreću isključivo lokalno od strane programera za parsiranje lokalnih XML izvještaja. Skripte nisu izložene na mreži niti su dio web poslužitelja. |
| **SCA (OSV, Trivy, DC)** | Tomcat `CVE-2026-24880` (Request Smuggling) i povezane CVE oznake | **Uvjetni rizik infrastrukture.** Ranjivost zahtijeva specifičnu kombinaciju reverse proxyja (s različitim tumačenjem chunk ekstenzija) i embedded Tomcata. Aplikacija se lokalno vrti samostalno, a u produkciji se mitigira pravilnim konfiguriranjem ingress proxyja. |
| **SCA (OSV, DC)** | Jackson Core / Databind ranjivosti deserijalizacije | **Potrebna zasebna procjena.** OSV je prijavio 7 nalaza za testni Jackson 2.22.1; verzija je ažurirana na 2.22.3. Ponovni OSV pregled nije prijavio ranjivosti. Nema osnove sve Jackson CVE-ove skupno proglasiti neprimjenjivima. |

---

## 6. Usklađenost s OWASP ASVS 5.0.0

Radna procjena odabranih zahtjeva i stvarno dostupni dokazi nalaze se u [ASVS kontrolnoj listi](asvs-kontrolna-lista.md). Ne predstavlja certifikaciju niti dokaz da su sve produkcijske kontrole ispunjene.

## 7. Testovi i pokrivenost koda

Pokrenuti `./mvnw clean verify`; stvarni testovi nalaze se u `src/test/java`, uključujući `SigurnosniRegresijskiTest`, `PutanjeSigurnosniTest`, `PostaSigurnosniTest`, `OgranicenjeAutentikacijeFilterTest`, `AuthFilterSigurnosniTest` i `UvozSigurnosniTest`. JaCoCo izvještaj Java koda je `target/site/jacoco/index.html`. Brojevi ovise o zadnjem čistom pokretanju; 100 % pokrivenosti ne dokazuje da ne postoje propusti. Python skripte nisu uključene u JaCoCo mjerenje. Aktualni rezultati i poznata ograničenja navedeni su u [pregledu provjera](../alati/poslije/README.md).

---

## 8. Zaključak

Kroz realizaciju ovog projekta demonstrirana je važnost integriranog pristupa sigurnosti programske podrške koji povezuje **statičku analizu (SAST)**, **analizu ovisnosti (SCA)**, **modeliranje prijetnji (STRIDE)** i **temeljiti ručni pregled koda**.

Najveći izazov predstavljale su logičke ranjivosti u kontroli toka i poslovnoj logici (kao što su propust u redoslijedu obrade iznimke kod dohvata nalaza N-01 i determinističko generiranje reset tokena N-03), koje automatizirani SAST alati poput Semgrepa i SpotBugsa uopće nisu detektirali. S druge strane, alati su bili iznimno učinkoviti u pronalasku sintaksnih obrazaca SQL injekcije (Semgrep) i ranjivosti u stablu ovisnosti (OSV, Trivy, Dependency-Check).

U stvarnom industrijskom projektu preporučuju se sljedeće prakse:
1. **Povezivanje SAST i SCA alata u CI/CD cjevovod:** Automatizirano blokiranje spajanja koda (Pull Request quality gate) ako se pojave novi kritični nalazi.
2. **Definiranje centralizirane autorizacije:** Izmještanje ad-hoc provjera vlasništva iz kontrolera u anotirane servisne metode (`@PreAuthorize`) ili politike temeljene na atributima (ABAC).
3. **Upravljanje SBOM-om:** Kontinuirano praćenje softverskog sastava i brzo ažuriranje biblioteka s poznatim ranjivostima (Dependency bot / Dependabot).
4. **Kultura sigurnog programiranja (Security Champions):** Redovita edukacija razvojnog tima i primjena kontrolnih lista poput OWASP ASVS u fazi dizajna i pregleda koda.

---

## 9. Izjava

Pri izradi korišteni su AI asistenti navedeni na početku izvještaja; osoba koja predaje projekt treba pregledati i razumjeti sve navedene izmjene i izvore.

*Domagoj Borovčak, 13. studenoga 2026.*

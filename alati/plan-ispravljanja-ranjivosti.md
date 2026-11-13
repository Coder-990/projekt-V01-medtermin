# Plan ispravljanja sigurnosnih problema

**Za developera · 13. 11. 2026. · Plan, bez primijenjenih popravaka**

Cilj je najprije spriječiti preuzimanje računa i čitanje tuđih zdravstvenih podataka, zatim urediti ostale zaštite i ovisnosti. Raditi u manjim, zasebnim promjenama kako bi se svaka mogla pregledati i provjeriti.

Oznake **M01–M12** odgovaraju postojećim `TODO(sec)` komentarima i [detaljnom pregledu koda](poslije/pregled-koda.md). Ručno pronađeni problemi nisu automatski nalazi OSV-a. Procijenjeni prioriteti nisu CVSS ocjene; iskorištavanje nije dinamički dokazivano.

## Kako koristiti plan

Za svaki korak prvo dodati regresijski test očekivanog sigurnog ponašanja koji na sadašnjem kodu pada, zatim napraviti popravak i provjeriti povezane funkcionalnosti. Koristiti testne račune i podatke. TODO zatvoriti tek kada je popravak provjeren; u opisu promjene navesti ID nalaza i rezultate provjere.

## 1. Reset lozinke — M03 i M04 zajedno

**Prioritet: kritičan.**

- **Mjesto / TODO:** [ResetLozinkeService.java](../src/main/java/hr/span/akademija/medtermin/platforma/ResetLozinkeService.java), `zatrazi` (M03) i `postaviNovuLozinku` (M04).
- **Izvor:** ručni pregled u [izvještaju](poslije/pregled-koda.md). Predvidljiv token iz usernamea i datuma; nema provjere isteka ni jednokratne potrošnje.
- **Popravak:** nasumičan token visoke entropije, pohrana njegova hasha, kratak rok valjanosti i atomska jednokratna potrošnja. Poništiti postojeće nesigurne reset tokene i razmotriti ograničavanje zahtjeva za reset.
- **Uvjet zatvaranja:** ispravan token radi samo jednom; istekao, nepoznat ili iskorišten token ne mijenja lozinku. Dva konkurentna pokušaja ne smiju oba uspjeti. Token se ne može izračunati iz usernamea i datuma.

## 2. Pristup nalazima i datotekama — M01 i M02

**Prioritet: kritičan za M01, visok za M02.**

- **Mjesto / TODO:** [NalazController.java](../src/main/java/hr/span/akademija/medtermin/nalazi/NalazController.java), `nalaz` (M01) i `preuzmi` (M02).
- **Izvor:** ručni pregled u [izvještaju](poslije/pregled-koda.md). Nevaljan format preskače autorizaciju; naziv koji sadrži username nije provjera vlasništva, a putanja nije ograničena na pohranu.
- **Popravak:** provjeriti vlasništvo prije obrade formata i svakog izlaza. Dokument dohvaćati preko zapisa i vlasnika u bazi. Osigurati da stvarna putanja ostaje unutar dopuštene pohrane, uključujući simboličke poveznice.
- **Uvjet zatvaranja:** korisnik ne može dobiti tuđi nalaz uz JSON, PDF ili nevaljan format. Preuzimanje odbija tuđe dokumente, izlazak preko `..`, apsolutne putanje i symlink koji izlazi iz pohrane. Predviđeni administratorski pristup i vlastiti dokumenti rade.

## 3. SQL injekcija pri sortiranju — M11

**Prioritet: visok.**

- **Mjesto / TODO:** [LijecnikController.java](../src/main/java/hr/span/akademija/medtermin/lijecnici/LijecnikController.java), `popis`, M11 i postojeći Semgrep komentari.
- **Izvori:** [Semgrep SARIF](poslije/semgrep-java.sarif), pravila `tainted-sql-string` i `spring-sqli`; [FindSecBugs](poslije/spotbugs-java.txt), `SQL_INJECTION_SPRING_JDBC`; ručna provjera toka podataka.
- **Popravak:** mapirati dopuštene vrijednosti na poznate SQL stupce i strogo ograničiti smjer na `ASC`/`DESC`. SQL identifikatori ne zamjenjuju se običnim bind parametrom `?`.
- **Uvjet zatvaranja:** sva podržana sortiranja rade; nepoznati stupci, smjerovi i SQL fragmenti odbijaju se bez izvršavanja proizvoljnog SQL-a.

## 4. Tajne i osobni podaci u logovima — M06 i M12

**Prioritet: visok za lozinke. Ovaj mali zahvat može teći paralelno s koracima 1–3.**

- **Mjesta / TODO:** [KorisnikService.java](../src/main/java/hr/span/akademija/medtermin/platforma/KorisnikService.java), `prijava` (M06); [RezervacijaService.java](../src/main/java/hr/span/akademija/medtermin/rezervacije/RezervacijaService.java), `kreiraj` (M12).
- **Izvori:** ručni pregled potvrđuje logiranje recorda s lozinkom odnosno OIB-om i napomenom. [FindSecBugs](poslije/spotbugs-java.txt) na tim mjestima upozorava na `CRLF_INJECTION_LOGS`.
- **Popravak:** ukloniti tajne i nepotrebne osobne podatke, logirati minimalne identifikatore događaja te sigurno obraditi kontrolne znakove. Parametrizirani SLF4J sam ne uklanja CR/LF.
- **Uvjet zatvaranja:** logovi uspješne i neuspješne prijave ne sadrže lozinku; rezervacije ne zapisuju OIB ni slobodnu napomenu; korisnički unos ne stvara lažne log zapise.
- **Ako je aplikacija već korištena:** pregledati pristup i zadržavanje starih logova te procijeniti potrebu promjene izloženih lozinki.

## 5. Nesigurno YAML učitavanje — CVE-2022-1471

**Prioritet: visok; povećati hitnost ako je nepouzdanom korisniku dostupan unos YAML-a.**

- **Mjesta / TODO:** [PostavkeController.java](../src/main/java/hr/span/akademija/medtermin/platforma/PostavkeController.java), `spremi`, TODO s `CVE-2022-1471`; [pom.xml](../pom.xml), komentari uz SnakeYAML ovisnost i `snakeyaml.version`.
- **Izvori:** [OSV analiza](prije/osv-detailed.md), [OSV](prije/osv-java.txt), [Trivy](prije/trivy-java.txt), [Dependency-Check](prije/dependency-check-report.json).
- **Popravak:** sigurno parsirati samo očekivane podatkovne tipove i uskladiti podržanu SnakeYAML verziju. Provjera `Map` nakon `load` nije dovoljna jer se objekti stvaraju tijekom parsiranja. Riješiti eksplicitni override `1.33`.
- **Uvjet zatvaranja:** očekivane postavke rade; neočekivani objektni tagovi i nevaljana struktura se odbijaju; provjereno ponašanje za prevelike/složene ulaze i prava pristupa endpointu.
- **Napomena:** samo promjena Boot parenta ne uklanja eksplicitni SnakeYAML override.

## 6. Hashiranje lozinki i životni vijek sesija — M05 i M09

**Prioritet: visok za zaštitu lozinki pri curenju baze i za opoziv ukradenih sesija.**

- **Mjesta / TODO:** [LozinkaHasher.java](../src/main/java/hr/span/akademija/medtermin/platforma/LozinkaHasher.java), `hash` (M05); [TokenStore.java](../src/main/java/hr/span/akademija/medtermin/platforma/TokenStore.java), M09.
- **Izvor:** [ručni pregled](poslije/pregled-koda.md). Brzi SHA-256 bez soli; sesije bez isteka/opoziva. Generiranje sesijskih tokena pomoću `SecureRandom` već je dobro.
- **Popravak:** Argon2id, BCrypt ili PBKDF2 uz sol i primjeren faktor rada, s planom migracije postojećih hasheva. Dodati rok sesije, opoziv nakon odjave/promjene lozinke i ograničenje pohrane tokena. Ovo se može podijeliti u dvije povezane promjene.
- **Uvjet zatvaranja:** nove lozinke koriste odabrani format; migracija starih računa je provjerena; istekao ili opozvan token ne autentificira korisnika; promjena/reset lozinke provodi dogovorenu politiku opoziva sesija.

## 7. Web zaštite — M07, M08 i M10

**Prioritet: srednji do visok, ovisno o produkcijskoj topologiji.**

| TODO / mjesto | Popravak | Uvjet zatvaranja |
| --- | --- | --- |
| M07, [CorsFilter.java](../src/main/java/hr/span/akademija/medtermin/platforma/CorsFilter.java), `dopusten` | Koristiti točnu konfiguriranu allowlistu origin-a. | Dopušten frontend radi; nepoznati origin ne dobiva dopuštenje za credentialed pristup. Provjeriti stvarne cookie/SameSite uvjete. |
| M08, [PrijavaController.java](../src/main/java/hr/span/akademija/medtermin/platforma/PrijavaController.java), `odrediste` | Prihvatiti samo provjerene lokalne putanje. | Vanjski URL-ovi i `//host` se odbijaju, legitimna lokalna odredišta rade. |
| M10, isti controller, `prijava` | Secure cookie za HTTPS produkciju, usklađen s TLS terminacijom. | Produkcijski cookie ima očekivane Secure, HttpOnly i SameSite postavke; lokalni razvoj je provjeren. |

Izvor svih triju je [ručni pregled](poslije/pregled-koda.md). CORS nalaz ne znači da će svaki vanjski web automatski dobiti Lax cookie.

## 8. Spring Boot i ostale ovisnosti — POM TODO oznake

**Zasebna migracija nakon hitnih popravaka, uz ranije rješavanje neposredno iskoristivih ranjivosti ovisnosti.**

- **Mjesto / TODO:** [pom.xml](../pom.xml), `TODO(sec): ovisnosti` i `TODO(sec): dependency-review`.
- **Plan i izvori:** [prijedlog nadogradnje](prije/prijedlog-nadogradnje.md), [pregled ovisnosti](prije/ovisnosti-todo.md), [OSV analiza](prije/osv-detailed.md), [Dependency-Check HTML](prije/dependency-check-report.html), [Trivy](prije/trivy-java.txt).
- **Popravak:** na dan implementacije provjeriti aktualnu podržanu stabilnu verziju Boota. Raniji dokument predlaže 4.1.2; ne tretirati tu verziju kao trajno najnoviju. Slijediti migracijske upute, uskladiti startere/Jackson i koristiti BOM gdje je primjereno.
- **Provjera primjenjivosti:** odvojiti produkcijske i testne ovisnosti, potvrditi modul/verziju za sumnjive CVE podudarnosti i uskladiti obuhvat SBOM-a s drugim alatima. Ne podizati svaki tranzitivni JAR naslijepo.
- **Uvjet zatvaranja:** cijeli build i testovi prolaze; provjerene ključne korisničke funkcije; novi SBOM i SCA izvještaji pokazuju preostale nalaze s obrazloženjem.

## 9. Završna provjera i preostala upozorenja

1. Pokrenuti funkcionalne i nove sigurnosne regresijske testove.
2. Ponoviti Semgrep i SpotBugs/FindSecBugs. Za postojeći SpotBugs koristiti kompatibilni JDK 21; prethodni pokušaj s JDK-om 25 nije uspio. Pregledati i upozorenja o nedostajućim simbolima.
3. Generirati novi SBOM i ponoviti OSV, Trivy i Dependency-Check s usporedivim obuhvatom, uključujući jasno označen testni opseg.
4. Rezultate spremiti u zaseban datirani poddirektorij `alati/poslije`, zajedno s commitom, verzijama alata, naredbama i statusom izvršavanja. Sačuvati izvornu osnovicu.
5. Povezati svaki zatvoreni TODO s popravkom i provjerom; preostale nalaze obrazložiti.

**Važno za usporedbu:** sadašnji [pregled u `poslije`](poslije/pregled-koda.md) i tamošnji izvještaji nastali su ponovnim skeniranjem prije popravaka. Ne predstavljaju dokaz ispravljenog stanja. Linije u reportima mogu biti pomaknute zbog dodanih komentara; povezivati i po klasi, metodi i pravilu.

Niži prioritet i zasebna trijaža:

- **T01 / `XXE_DOCUMENT`:** [UvozTermina.java](../src/main/java/hr/span/akademija/medtermin/termini/UvozTermina.java) već ima zabranu DOCTYPE-a i vanjskih entiteta. Sačuvati zaštite i potvrditi testom; nije potvrđena XXE ranjivost.
- **T02 / `SQL_INJECTION_JDBC`:** [JdbcPomoc.java](../src/main/java/hr/span/akademija/medtermin/platforma/JdbcPomoc.java) u pregledanim pozivima prima fiksni SQL s bind parametrima. Provjeravati ugovor pozivatelja; ne poistovjećivati s M11.
- **`SPRING_ENDPOINT`:** 23 informativna zapisa nisu 23 ranjivosti.
- **Python XML upozorenja:** pregledati `oznacavanje-ranjovisti.py` i `spotbugs-pregled.py` za obradu nepouzdanih izvještaja; razmotriti `defusedxml` i ograničenje veličine. To nisu javni Java endpointovi.
- Ostala upozorenja o referencama, Unicodeu, formatiranju, null vrijednostima i resursima provjeriti prema stvarnom učinku; ne rangirati ih samo prema nazivu kategorije alata.

Prethodno je izvorni projekt prošao 43 testa. To nije dokaz da sigurnosni slučajevi iz ovog plana već imaju testnu pokrivenost.

# Sigurnosni pregled koda — ponovna provjera

**Datum: 13. 11. 2026. Status: prijedlozi i TODO komentari, bez primijenjenih popravaka.**

Direktorij `poslije` ovdje označava novu provjeru nakon ranijih izvještaja. Ne predstavlja stanje nakon ispravljanja ranjivosti. Izvršivi kod, verzije ovisnosti i konfiguracija nisu promijenjeni. Oznake M01–M12 predstavljaju ručni pregled; gdje postoji i alatni nalaz, izvor je posebno naveden. Prioriteti su procjena za ovu aplikaciju, a ne dodijeljeni CVSS rezultati. Iskorištavanje nije dinamički dokazivano.

## Najvažniji nalazi i prijedlozi

| ID / prioritet | Mjesto | Dokaz i mogući učinak | Prijedlog, bez primjene |
| --- | --- | --- | --- |
| M01 / kritičan prioritet | `NalazController.nalaz` | Provjera vlasnika je nakon `Format.valueOf` unutar `try`. Nepoznat format odlazi u `catch` i vraća JSON bez provjere vlasnika. Prijavljen korisnik može dobiti tuđi zdravstveni nalaz ako postoji traženi ID/datoteka. | Provjeriti vlasništvo prije obrade formata i svakog izlaza; testirati tuđi nalaz s valjanim i nevaljanim formatom. |
| M02 / visok | `NalazController.preuzmi` | Naziv mora samo sadržavati username; `resolve` prihvaća putanje bez provjere granice direktorija. Moguće čitanje tuđih ili drugih datoteka dostupnih procesu, ovisno o nazivu, putanji i ovlastima. | Autorizacija prema zapisu dokumenta i vlasniku; kanonska putanja unutar pohrane, uključujući simboličke poveznice. |
| M03 / kritičan prioritet | `ResetLozinkeService.zatrazi` | Reset token je MD5 poznatog usernamea i datuma. Problem je predvidljiv ulaz, a ne samo slabost MD5. Uz postojeći zapis tokena moguće je preuzimanje računa. | Nasumičan token visoke entropije, hash tokena u bazi; ograničiti zahtjeve za reset. |
| M04 / visok | `ResetLozinkeService.postaviNovuLozinku` | Upit provjerava samo token. Polja `istjece` i `iskoristen` postoje u shemi, ali se ne koriste; token se nakon promjene ne troši. | Atomski provjeriti rok i jednokratnost, potrošiti token i poništiti stare tokene. |
| M05 / visok pri curenju baze | `LozinkaHasher.hash` | Lozinke su običan SHA-256 bez soli i faktora rada. Olakšano izvanmrežno pogađanje pri krađi baze. | Argon2id, BCrypt ili PBKDF2 uz sol i plan migracije. |
| M06 / visok | `KorisnikService.prijava` | `PrijavaZahtjev` je record s poljem `password`; logiranje cijelog recorda zapisuje lozinku u čistom tekstu. FindSecBugs na tom mjestu prijavljuje CRLF, a otkrivanje lozinke utvrđeno je ručno. | Ukloniti tajne iz logova; ograničiti pristup i razmotriti obradu već nastalih logova. Spriječiti umetanje novih redaka. |
| M07 / uvjetno visok | `CorsFilter.dopusten` | Svi HTTP(S) origini prolaze, konfigurirana lista se ne koristi, a odgovor dopušta credentials. | Točna allowlista. Stvarni napad ovisi i o autentikaciji, SameSite pravilima i topologiji domena; Lax cookie se ne šalje automatski svakom vanjskom webu. |
| M08 / srednji | `PrijavaController.odrediste` | Korisnički `returnUrl` izravno postaje odredište preusmjeravanja nakon prijave; pogodno za phishing. | Dopustiti samo provjerene lokalne putanje, bez apsolutnih URL-ova i `//host`. |
| M09 / srednji do visok | `TokenStore` | Nasumični tokeni su dobro generirani, ali nema isteka/opoziva ni ograničenja memorijske pohrane. Ukradeni token vrijedi do gubitka pohrane/restarta. | TTL i opoziv nakon odjave/promjene lozinke; ograničiti broj tokena. |
| M10 / ovisi o produkciji | `PrijavaController.prijava` | Cookie ima HttpOnly i SameSite=Lax, ali nema Secure. | Za HTTPS produkciju dodati Secure uz provjeru TLS terminacije i razvojne konfiguracije. |
| M11 / visok | `LijecnikController.popis` | Semgrep i FindSecBugs: parametri `sort` i `smjer` iz zahtjeva ulaze u SQL konkatenacijom. Doseg napada ovisi o bazi i SQL sintaksi; nije izvođen napad. | Mapirati dozvoljene stupce i strogo ASC/DESC. Nazivi stupaca ne mogu se zamijeniti običnim bind parametrom `?`. |
| M12 / srednji | `RezervacijaService.kreiraj` | Log cijelog recorda sadrži OIB i slobodnu napomenu; FindSecBugs upozorava i na CRLF. | Minimalan log bez OIB-a i sadržaja napomene; sigurna obrada kontrolnih znakova. |

TODO komentari s istim ID-ovima dodani su uz odgovarajuća mjesta u `src/main/java/hr/span/akademija/medtermin`. Nijedan predloženi popravak nije implementiran.

## Što su alati stvarno prijavili

### Semgrep

[Izvorni SARIF](semgrep-java.sarif): **6 nalaza**, 292 pravila, 64 pregledane mete. Dva nalaza označavaju istu SQL injekciju M11; četiri se odnose na Python XML parsiranje u `oznacavanje-ranjovisti.py` i `spotbugs-pregled.py`.

Python nalazi nisu ranjivosti javnog Java endpointa. Rizik ovisi o tome jesu li ulazni XML izvještaji nepouzdani te o parseru i verziji Expat-a. Prijedlog je za nepouzdane izvještaje koristiti `defusedxml` i ograničiti veličinu ulaza. Nije potvrđen XXE napad u tim skriptama. Skeniranje poštuje git ignore, ograničenja veličine i odabir datoteka; čist rezultat nije dokaz odsutnosti ranjivosti.

### SpotBugs s FindSecBugsom

[XML](spotbugs-java.xml) · [čitljiv popis](spotbugs-java.txt): **57 zapisa**.

| Vrsta | Broj | Tumačenje |
| --- | ---: | --- |
| SPRING_ENDPOINT | 23 | Informativna inventura endpointova, sama po sebi nije ranjivost. |
| CRLF_INJECTION_LOGS | 8 | Provjeriti izvor logiranog teksta. Posebno relevantni M06 i M12; parametrizirani SLF4J sam ne uklanja CR/LF. Validacija usernamea pri registraciji smanjuje taj rizik na toj putanji. |
| SQL_INJECTION_SPRING_JDBC | 1 | M11, nepouzdan unos u SQL. |
| SQL_INJECTION_JDBC | 1 | T02, pomoćnik `JdbcPomoc`: pregledani pozivi koriste fiksni SQL s bind parametrima; nije potvrđena dodatna injekcija. |
| XXE_DOCUMENT | 1 | T01, `UvozTermina`: već zabranjuje DOCTYPE, vanjske entitete i XInclude. Vjerojatan lažno pozitivan nalaz; očuvati zaštite i regresijski testirati. |
| IMPROPER_UNICODE | 3 | Pretvorba/usporedba teksta u nalazima/PDF-u; bez dokaza dodatne sigurnosne posljedice, niži prioritet. |
| EI_EXPOSE_REP / EI_EXPOSE_REP2 | 10 | Uglavnom mutable reference/Spring konstrukcijska injekcija; oznaka MALICIOUS_CODE nije dokaz napada. |
| Ostali | 10 | Formatiranje novih redaka (7), nepročitano polje (1), mogući null (1), zatvaranje JDBC resursa na iznimci (1). Za provjeru kvalitete/robusnosti, ne automatski kritične ranjivosti. |

Prvi pokušaj s JDK-om 25 nije uspio zbog nepodržane class verzije 69. Ponovljeno s JDK-om 21 završilo je uspješno. Ipak, alat prijavljuje nedostajuće simbole poput `makeConcatWithConstants`, `mapRow` i `createPreparedStatement`, pa obuhvat bytecode analize nije potpun. Odsutnost nalaza za reset token ili hash lozinke nije potvrda sigurnosti.

## Ovisnosti i pom.xml

Postojeći nalazi i TODO komentari ostaju povezani s [OSV analizom](../prije/osv-detailed.md), [pregledom ovisnosti](../prije/ovisnosti-todo.md) i [prijedlogom nadogradnje](../prije/prijedlog-nadogradnje.md). Stvarne verzije ostaju Spring Boot **3.5.7** i eksplicitni SnakeYAML **1.33**.

Posebno ostaje rizik nesigurnog YAML učitavanja u `PostavkeController` (CVE-2022-1471): naknadna provjera rezultata kao `Map` ne sprječava konstrukciju objekata tijekom parsiranja. Raniji TODO predlaže sigurni konstruktor i podržanu verziju. Nadogradnja parenta sama ne uklanja eksplicitni SnakeYAML override. Prijedlozi verzija nalaze se u povezanom dokumentu; ovdje nije provedeno novo SCA skeniranje ni nadogradnja.

## Ponovljivost i provjera

SpotBugs/FindSecBugs:

```bash
JAVA_HOME=/usr/lib/jvm/java-21-openjdk ./mvnw -q -Psast compile spotbugs:spotbugs
```

Semgrep je pokrenut u Dockeru s projektom montiranim read-only, pravilima `p/owasp-top-ten`, `p/security-audit` i `p/java`, uz isključene metrike i izlaz u SARIF.

Izvještaji su nastali prije dodavanja novih komentara: njihove izvorne linije mogu biti pomaknute u trenutnom kodu. Povezivanje zato treba raditi i prema klasi, metodi i ID-u pravila/TODO-a.

U prethodnoj provjeri izvornog, nenadograđenog projekta `clean verify` prošao je s 43 testa. Nakon ovog dodavanja komentara testovi nisu ponovno pokretani; postojeći testovi nisu dokaz pokrivenosti gore navedenih sigurnosnih slučajeva. Završno je uspoređen POM bez XML komentara i Java kod bez samostalnih linijskih komentara s HEAD-om, uz provjeru da testni izvori nisu promijenjeni.

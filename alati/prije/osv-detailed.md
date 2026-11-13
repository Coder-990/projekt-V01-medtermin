# OSV analiza ranjivosti — MedTermin

> **Glavni zaključak:** SnakeYAML ima najjasniji put od korisničkog ulaza do opasnog poziva. Većina ostalih nalaza zahtijeva značajke ili konfiguraciju koje nisu pronađene u pregledanom projektu. Ocjena biblioteke nije jednaka dokazanom riziku za aplikaciju.

| Podatak | Vrijednost |
| --- | --- |
| Datum analize | 13. studenoga 2026. |
| Izvještaj skenera | [osv-java.txt](osv-java.txt) |
| Projekt | MedTermin, Maven / Spring Boot 3.5.7 / Java 21 |
| Ulaz skenera | Maven CycloneDX SBOM: `target/bom.json` |
| Broj skeniranih komponenti | 45 |
| Pogođeni paketi | 12 |
| Nalazi | **60: 10 Critical, 21 High, 21 Medium, 8 Low** |
| Metoda procjene | Statički pregled koda, projektne konfiguracije i javnih OSV opisa svih 60 nalaza |
| Ograničenja | Bez pokušaja iskorištavanja i bez potvrde stvarne produkcijske konfiguracije |

## Sadržaj

- [Kako čitati rezultate](#kako-čitati-rezultate)
- [Sažetak prioriteta](#sažetak-prioriteta)
- [SnakeYAML: konkretan visok rizik](#1-snakeyaml-konkretan-visok-rizik)
- [Tomcat: uvjetni rizici](#2-tomcat-uvjetni-rizici)
- [Jackson: posebni putovi deserijalizacije](#3-jackson-posebni-putovi-deserijalizacije)
- [Spring Framework i Spring Boot](#4-spring-framework-i-spring-boot)
- [Logback i Log4j](#5-logback-i-log4j)
- [Preporučeni redoslijed rada](#preporučeni-redoslijed-rada)
- [Potpun popis nalaza](#potpun-popis-nalaza)
- [Granice zaključaka](#granice-zaključaka)

## Kako čitati rezultate

OSV uspoređuje identitet i verziju komponente s bazom poznatih ranjivosti. SBOM sadrži razriješene verzije izravnih i tranzitivnih ovisnosti. Scanner time potvrđuje prisutnost pogođene verzije, ali samim tim ne potvrđuje dostupnost ranjivog ponašanja kroz tvoju aplikaciju.

**CVSS** je opća ocjena ozbiljnosti ranjivosti. Ocjena 9,8 može biti manje hitna za konkretnu aplikaciju od ocjene 8,3 ako aplikacija ne koristi značajku potrebnu za prvi napad, a drugi ranjivi poziv izravno obrađuje korisnički unos.

U ovoj analizi koriste se sljedeće procjene:

| Procjena | Značenje |
| --- | --- |
| **Konkretan visok rizik** | U kodu postoji relevantan put od nepouzdanog ulaza do opasnog API-ja. Potpuno iskorištavanje nije dokazivano. |
| **Uvjetni rizik / provjeriti** | Primjenjivost ovisi o infrastrukturi, konfiguraciji ili obradi koju pregled nije dovoljno razjasnio. |
| **Nisu pronađeni potrebni uvjeti** | Pogođena verzija postoji, ali u pregledanom kodu i konfiguraciji nije pronađena značajka potrebna za opisani napad. |

**Važno:** „nisu pronađeni uvjeti” ne znači „zauvijek sigurno” niti je razlog za automatsko ignoriranje nalaza.

## Sažetak prioriteta

| Skupina | Broj nalaza | Procjena za MedTermin |
| --- | ---: | --- |
| SnakeYAML | 1 | **Prvi prioritet:** korisnički YAML ulazi u nesigurni `load`. |
| Tomcat | 19 | Request smuggling provjeriti uz stvarni reverse proxy; za većinu ostalih nalaza nisu pronađene potrebne značajke. |
| Jackson Core | 4 | Nisu pronađeni relevantni async/DataInput putovi; brojčane konverzije procijeniti prema stvarnom ciljnom tipu. |
| Jackson Databind | 12 | Nisu pronađene relevantne polimorfne postavke, anotacije ili posebni ulazni tipovi. |
| Spring Framework | 18 | Većinom nisu pronađeni potrebni uvjeti; multipart obradu dodatno provjeriti. |
| Spring Boot | 2 | Nisu pronađene trajne servlet sesije ni embedded Artemis potrebni za opisane scenarije. |
| Logback | 3 | Nisu pronađeni socket serveri za logove; jedan nalaz traži mogućnost izmjene konfiguracije. |
| Log4j API | 1 | Nije pronađena relevantna `MapMessage` JSON obrada. |
| **Ukupno** | **60** | **Broj nalaza nije broj potvrđenih napada na aplikaciju.** |

## 1. SnakeYAML: konkretan visok rizik

| Podatak | Vrijednost |
| --- | --- |
| Ranjivost | **CVE-2022-1471 / GHSA-mjmj-j48q-9wg2** |
| Paket i verzija | `org.yaml:snakeyaml:1.33` |
| CVSS u izvještaju | **8,3 — High** |
| Vrsta | Nesigurna deserijalizacija, potencijalno udaljeno izvršavanje koda (RCE) |
| Izvor | [OSV opis](https://osv.dev/vulnerability/GHSA-mjmj-j48q-9wg2) |

### Gdje je problem u aplikaciji

Endpoint `POST /api/postavke` u [PostavkeController.java](../../src/main/java/hr/span/akademija/medtermin/platforma/PostavkeController.java) prima korisničko tijelo zahtjeva i izvršava:

```java
dokument = new Yaml().load(new ByteArrayInputStream(tijelo));
```

U SnakeYAML-u 1.33 takvo učitavanje omogućuje konstrukciju Java objekata iz YAML oznaka. Ako postoje odgovarajuće klase i uvjeti, njihove nuspojave mogu dovesti do izvršavanja koda na poslužitelju.

### Zašto postojeće provjere nisu dovoljne

- Provjera `dokument instanceof Map` slijedi **nakon** učitavanja. Nuspojave konstrukcije objekata mogu nastati prije nje.
- Hvatanje `YAMLException` ne poništava nuspojave koje su se već dogodile.
- Endpoint traži prijavu kroz `PrijavljeniKorisnikResolver`, ali aplikacija ima javni `POST /api/auth/register`. Ako je registracija dostupna svima, napadač može dobiti običan račun.
- Nije pronađen zahtjev da korisnik mora biti administrator za spremanje vlastitih postavki.

**Procjena:** ovo je prvi prioritet jer je opasan način uporabe izravno vidljiv u kodu. Nije dokazano potpuno izvršavanje proizvoljnog koda; ono ovisi o classpathu, mrežnom pristupu i drugim uvjetima okruženja.

## 2. Tomcat: uvjetni rizici

Paket `org.apache.tomcat.embed:tomcat-embed-core:10.1.48` ima **19 nalaza**.

### Request smuggling: provjeriti reverse proxy

**CVE-2026-24880 / GHSA-563x-q5rq-57qp**, CVSS **7,5**, odnosi se na neispravno tumačenje chunk ekstenzije u HTTP zahtjevu. Ako proxy i Tomcat različito odrede granice zahtjeva, mogu nastati request smuggling scenariji.

To može omogućiti da dijelovi zahtjeva budu protumačeni drukčije nego što očekuju proxy pravila ili backend. Stvarna posljedica ovisi o cijelom lancu obrade.

**Za MedTermin:** u projektu nije potvrđeno koristi li stvarno okruženje Nginx, Apache, ingress ili drugi reverse proxy i kako prosljeđuje zahtjeve. Zato je nalaz važan za provjeru infrastrukture, ali iskorištavanje nije potvrđeno.

Izvor: [OSV opis](https://osv.dev/vulnerability/GHSA-563x-q5rq-57qp).

### Ostali Tomcat nalazi

| Značajka / problem | Procjena za pregledani projekt |
| --- | --- |
| DIGEST autentikacija: prihvat nepoznatog korisnika i replay | Nije pronađena DIGEST autentikacija. Koriste se vlastiti `AuthFilter`, `TokenStore` i korisnički servis. |
| Tomcat FORM autentikacija | HTML obrazac implementiran u `PrijavaController` nije Tomcat FORM autentikacija. Nije pronađena potrebna container konfiguracija. |
| Tomcat sigurnosna ograničenja po metodama i putanjama | Nisu pronađena potrebna Tomcat/container pravila. Aplikacija provjerava korisnika u vlastitom kodu. |
| HTTP/2 zaglavlja nisu validirana | HTTP/2 nije uključen u pregledanom `application.properties`. Vanjske postavke treba provjeriti. HTTP/2 na proxyju nije sam po sebi dokaz da proxy prema Tomcatu koristi HTTP/2. |
| TLS klijentski certifikati i OCSP | Nisu pronađene potrebne TLS/mTLS i Native/OCSP postavke. |
| AJP tajna i `LockOutRealm` | Nisu pronađene potrebne konfiguracije. |
| WebSocket klijent i WebDAV | Nisu pronađene relevantne uporabe ili servlet konfiguracije. |
| Rewrite i LoadBalancerDraining valveovi | Nisu pronađeni. |
| `JsonAccessLogValve`, clustering i Kubernetes token | Nisu pronađene relevantne postavke. |
| Nepotpuno logiranje efektivnog `web.xml` | Nije dokaz izravnog zaobilaženja prijave u ovoj aplikaciji; opis se odnosi na prikaz sigurnosne konfiguracije u logu. |

Primjer visokog CVSS-a bez pronađenog preduvjeta: **CVE-2026-43512**, ocjena **9,8**, zahtijeva DIGEST autentikaciju. Prisutnost Tomcata sama po sebi ne dokazuje da vlastita aplikacijska prijava ima taj propust.

Izvori: [DIGEST nalaz](https://github.com/advisories/GHSA-h6fc-48rj-7qqh), [Apache Tomcat sigurnosne objave](https://tomcat.apache.org/security-10.html).

## 3. Jackson: posebni putovi deserijalizacije

Pronađeno je **16 nalaza** u `jackson-core:2.19.2` i `jackson-databind:2.19.2`.

### Jackson Core — četiri nalaza

| Scenarij | Potrebni uvjeti | Procjena |
| --- | --- | --- |
| Zaobilaženje ograničenja duljine broja, uključujući naknadno otkrivenu varijantu | Non-blocking / async JSON parser | Aplikacija koristi Spring MVC; nije pronađena prilagodba na async Jackson parser. |
| Rast poruke greške i potrošnja memorije | Parser stvoren nad `DataInput` | Nije pronađen takav put. Uobičajeni `InputStream` put nije isti parser. |
| ReDoS u `NumberInput.looksLikeValidNumber()` | Nepouzdan dugačak string mora doći do te metode | Nije pronađen izravan poziv; ulazni modeli nemaju `float`/`double` polja. Nije dokazano da postojeći endpointi dosežu problematičan poziv. |

Za posljednji nalaz pregledan je i bytecode lokalnih Jackson 2.19.2 JAR-ova: postoje pozivi iz određenih konverzija brojeva, ali to nije dokaz da je svaka konverzija stringa u broj pogođena. Sam `Long terminId` nije dovoljan za takav zaključak.

### Jackson Databind — dvanaest nalaza

| Skupina | Potrebni uvjeti | Što je pronađeno u MedTerminu |
| --- | --- | --- |
| Zaobilaženje `PolymorphicTypeValidator` pravila kroz generičke tipove, nizove ili široke bazne tipove | Polimorfna deserijalizacija i relevantna pravila / anotacije | Nisu pronađene potrebne postavke ili anotacije. |
| Zaobilaženje `@JsonIgnore`, `@JsonIgnoreProperties` ili `@JsonView` | Određene kombinacije naming strategija, case-insensitive pravila ili `@JsonUnwrapped` | Nisu pronađene relevantne kombinacije. |
| DNS upiti tijekom deserijalizacije | Ulazni `InetAddress` ili `InetSocketAddress` | Nisu pronađena takva ulazna polja. |
| Nuspojave preko filesystem providera | Ulazni `Path` i odgovarajući provider s nuspojavama | Nije pronađeno vezanje korisničkog JSON-a u takav tip. |
| Skupa obrada XML datuma i trajanja | Ulazni `javax.xml.datatype.Duration` ili `XMLGregorianCalendar` | Nisu pronađeni. Interni `java.time.Duration` u simuliranom BIS klijentu nije isti tip niti je korisnički ulaz. |
| Kvadratna obrada object-ID referenci | `@JsonIdentityInfo` kolekcije / mape | Nisu pronađene. |
| Neograničen cache nepoznatih type-ID vrijednosti | Name-based polimorfizam s fallbackom / `defaultImpl` | Nije pronađen. |

**Procjena:** pogođene verzije postoje i treba ih održavati ažurnima, ali pregled ne potvrđuje da su opisani napadi dostupni kroz postojeće endpointe. Nema osnove zaključiti da samo zbog Jackson nalaza napadač već može izvršavati kod na MedTerminu.

## 4. Spring Framework i Spring Boot

Spring Framework 6.2.12 ima **18 nalaza**: `spring-core` jedan, `spring-expression` tri, `spring-web` jedan i `spring-webmvc` trinaest. Spring Boot 3.5.7 ima još **dva**.

| Skupina / primjer | Potrebni uvjeti | Procjena za MedTermin |
| --- | --- | --- |
| **CVE-2026-47884**, CVSS 9,8: SSRF/RCE kroz `XsltView` | XSLT prikazi, `/**` mapiranje s implicitnim nazivom prikaza | Nisu pronađeni. |
| **CVE-2026-47890**, CVSS 9,8: oštećenje SSE streama s fragmentima | SSE i fragmenti prikaza | Nisu pronađeni. |
| Stariji SSE nalaz | SSE obrada | Nije pronađena. |
| Tri SpEL nalaza: pozivanje metoda i DoS | Evaluacija izraza koje kontrolira korisnik | Nije pronađena. Interna uporaba Spring izraza nije automatski isti scenarij. |
| ReDoS kroz `AntPathMatcher` | Napadač kontrolira obrazac za podudaranje | Nije pronađeno. Kontrola URL putanje nije automatski kontrola obrasca. |
| SSRF kroz `UriComponentsBuilder` | Validacija nepouzdanog URL-a tim API-jem pa njegova uporaba | Nije pronađen relevantan poziv. BIS klijent trenutačno simulira odgovor. |
| XSS kroz JSP form tagove ili `JavaScriptUtils` | Uporaba pogođenih API-ja s nepouzdanim vrijednostima | Nije pronađena. |
| Script template prikazi | Java scripting engine i odgovarajući viewovi | Nisu pronađeni. |
| Open redirect s implicitnim nazivom viewa | Posebno `/**` mapiranje bez eksplicitnog naziva prikaza | Nije pronađeno. To ne procjenjuje druge, aplikacijske redirectove. |
| Statički resursi: traversal, cache poisoning, curenje podataka i DoS | Određene resource chain / cache / versioning / filesystem postavke; jedan nalaz traži Windows | Nisu pronađene potrebne prilagodbe za većinu opisanih scenarija. Stvarne vanjske postavke nisu potvrđene. |
| Multipart request smuggling | Odgovarajuća obrada multipart zahtjeva i kontekst obrade | **Dodatno provjeriti.** Nepostojanje upload endpointa nije dovoljno za potpuno isključenje multipart obrade. |
| Boot: predvidljivi privremeni direktorij i trajne sesije | Lokalni napadač, potrebna konfiguracija i uvjeti preko restarta | Trajne servlet sesije nisu uključene u pregledanoj konfiguraciji. Autentikacijski tokeni čuvaju se u vlastitom `TokenStore` u memoriji. |
| Boot: embedded Artemis direktorij | Embedded Artemis i lokalni napadač | Artemis nije pronađen među komponentama aplikacije. |

**Napomena o datotekama nalaza:** vlastito čitanje dokumenta kroz `Files.readAllBytes` ili `Files.readString` nije automatski uporaba Springova ranjivog static resource resolvera. Sigurnost tog aplikacijskog koda zasebno je pitanje.

Izvori za kritične primjere: [XsltView](https://github.com/advisories/GHSA-pc63-qcmh-9cmg), [SSE fragmenti](https://github.com/advisories/GHSA-j9f9-w8pj-32f8).

## 5. Logback i Log4j

| Paket | Broj | Što nalazi opisuju | Procjena |
| --- | ---: | --- | --- |
| `logback-core:1.5.20` | 3 | Ograničene deserijalizacijske probleme u socket serverima za logove te konstrukciju klasa nakon izmjene konfiguracije | Relevantni socket serveri nisu pronađeni. Jedan nalaz traži mogućnost pisanja konfiguracije. Niži prioritet u ovom projektu. |
| `log4j-api:2.24.3` | 1 | Neispravan JSON pri logiranju `MapMessage` s vrijednostima poput NaN i Infinity | Relevantna `MapMessage` JSON obrada nije pronađena. |

**Ovaj Log4j nalaz nije Log4Shell.** Prisutnost `log4j-api` paketa nije sama po sebi dokaz uporabe ranjivog Log4j Core/JNDI scenarija.

## Preporučeni redoslijed rada

1. **SnakeYAML:** ukloniti nesigurno učitavanje nepouzdanog YAML-a i odabrati podržanu verziju. Nakon budućeg popravka provjeriti da su dopuštene samo očekivane jednostavne strukture i da normalne postavke i dalje rade.
2. **Tomcat i infrastruktura:** utvrditi stvarni proxy lanac, backend HTTP protokol, TLS i vanjske postavke; posebno procijeniti request smuggling.
3. **Usklađeno ažuriranje ovisnosti:** planirati podržanu Spring Boot nadogradnju koja usklađuje Spring, Tomcat, Jackson i logging biblioteke, uz funkcionalne provjere.
4. **Ponoviti SBOM i OSV scan:** generirati novi SBOM nakon promjena i usporediti izvještaje.
5. **Dokumentirati neprimjenjivost:** uz eventualno prihvaćanje rizika zapisati konkretan razlog i uvjete koji bi promijenili procjenu. Ne ignorirati skupno sve nalaze samo zato što trenutačno nije pronađen put napada.

### Kako tumačiti stupac „Fixed version”

Verzije u izvještaju preuzete su iz metapodataka pojedinačnih nalaza. **Nisu gotov plan kompatibilne nadogradnje.** Primjerice, Spring 7.0.9 ne treba samostalno umetnuti u Spring Boot 3.5.7 samo zato što ga scanner prikazuje kao popravak. Prije nadogradnje treba provjeriti podržanu kombinaciju verzija, dostupne backportove i aktualne objave održavatelja.

## Potpun popis nalaza

Sljedeće tablice sadrže svih **60 nalaza** iz izvještaja. Nazivi su iz javnih OSV zapisa; CVSS i popravljena verzija preneseni su iz lokalnog izvještaja. Detaljna procjena primjenjivosti nalazi se u prethodnim odjeljcima.

### `ch.qos.logback:logback-core` — 1.5.20 (3 nalaza)

| CVE / OSV | CVSS | Opis | Fixed version u izvještaju |
| --- | ---: | --- | --- |
| [CVE-2026-10532](https://osv.dev/GHSA-jhq6-gfmj-v8fx)<br>GHSA-jhq6-gfmj-v8fx | 2.9 | Logback vulnerable to Object Injection through HardenedObjectInputStream modules | `1.5.34` |
| [CVE-2026-9828](https://osv.dev/GHSA-p47f-322f-whfh)<br>GHSA-p47f-322f-whfh | 1.2 | QOS.CH Sarl logback logback-core has a deserialization of untrusted data vulnerability | `1.5.33` |
| [CVE-2026-1225](https://osv.dev/GHSA-qqpg-mvqg-649v)<br>GHSA-qqpg-mvqg-649v | 1.8 | Logback allows an attacker to instantiate classes already present on the class path | `1.5.25` |

### `com.fasterxml.jackson.core:jackson-core` — 2.19.2 (4 nalaza)

| CVE / OSV | CVSS | Opis | Fixed version u izvještaju |
| --- | ---: | --- | --- |
| [CVE-2026-18401](https://osv.dev/GHSA-72hv-8253-57qq)<br>GHSA-72hv-8253-57qq | 6.9 | jackson-core: Number Length Constraint Bypass in Async Parser Leads to Potential DoS Condition | `2.21.1` |
| [CVE-2026-89425](https://osv.dev/GHSA-7hhh-6rmp-j9qf)<br>GHSA-7hhh-6rmp-j9qf | 7.5 | jackson-core: UTF8DataInputJsonParser._reportInvalidToken() missing maxErrorTokenLength limit -> unbounded StringBuilder growth (DoS) | `2.21.7` |
| [CVE-2026-89407](https://osv.dev/GHSA-p6pp-m3f8-5c89)<br>GHSA-p6pp-m3f8-5c89 | 7.5 |  jackson-core: ReDoS: quadratic backtracking in NumberInput.PATTERN_FLOAT via looksLikeValidNumber() | `2.21.7` |
| [CVE-2026-68494](https://osv.dev/GHSA-r7wm-3cxj-wff9)<br>GHSA-r7wm-3cxj-wff9 | 8.7 | jackson-core: Async parser maxNumberLength bypass via chunked digit accumulation (incomplete fix for GHSA-72hv-8253-57qq) | `2.21.4` |

### `com.fasterxml.jackson.core:jackson-databind` — 2.19.2 (12 nalaza)

| CVE / OSV | CVSS | Opis | Fixed version u izvještaju |
| --- | ---: | --- | --- |
| [CVE-2026-59888](https://osv.dev/GHSA-3pjw-73gf-8qr5)<br>GHSA-3pjw-73gf-8qr5 | 6.5 | jackson-databind: @JsonIgnore on a Record property is bypassed with a PropertyNamingStrategy | `2.21.4` |
| [CVE-2026-59889](https://osv.dev/GHSA-5gvw-p9qm-jgwh)<br>GHSA-5gvw-p9qm-jgwh | 6.5 | jackson-databind: @JsonView bypassed for @JsonUnwrapped container properties on deserialization | `2.21.5` |
| [CVE-2026-54515](https://osv.dev/GHSA-5jmj-h7xm-6q6v)<br>GHSA-5jmj-h7xm-6q6v | 5.3 | jackson-databind has case-insensitive deserialization bypasses per-property @JsonIgnoreProperties | `2.21.5` |
| [CVE-2026-91777](https://osv.dev/GHSA-cxp5-3px4-pw24)<br>GHSA-cxp5-3px4-pw24 | 7.5 | jackson-databind quadratic forward-reference completion  | `2.21.7` |
| [CVE-2026-83557](https://osv.dev/GHSA-gx83-3vf8-gh7j)<br>GHSA-gx83-3vf8-gh7j | 5.6 | jackson-databind: Comparable missing from DefaultBaseTypeLimitingValidator's unsafe base types (incomplete PolymorphicTypeValidator denylist) | `2.21.6` |
| [CVE-2026-54514](https://osv.dev/GHSA-hgj6-7826-r7m5)<br>GHSA-hgj6-7826-r7m5 | 5.3 | jackson-databind: InetSocketAddress deserialization triggers eager DNS resolution (SSRF) | `2.21.4` |
| [CVE-2026-54512](https://osv.dev/GHSA-j3rv-43j4-c7qm)<br>GHSA-j3rv-43j4-c7qm | 8.1 | jackson-databind has a PolymorphicTypeValidator bypass via generic type parameters that allows arbitrary class instantiation | `2.21.4` |
| [CVE-2026-68497](https://osv.dev/GHSA-q4xh-88c3-wmh7)<br>GHSA-q4xh-88c3-wmh7 | 7.5 | jackson-databind: Duration XMLGregorianCalendar Unbounded Number Parse DoS | `2.21.6` |
| [CVE-2026-54513](https://osv.dev/GHSA-rmj7-2vxq-3g9f)<br>GHSA-rmj7-2vxq-3g9f | 8.1 | jackson-databind has an array subtype allowlist bypass in BasicPolymorphicTypeValidator (allowIfSubTypeIsArray) | `2.21.4` |
| [CVE-2026-77310](https://osv.dev/GHSA-vvgp-rfg2-7rr6)<br>GHSA-vvgp-rfg2-7rr6 | 5.3 | jackson-databind: Incomplete fix for CVE-2026-54514: eager DNS resolution (SSRF) still present in InetAddress deserialization | `2.21.5` |
| [CVE-2026-19032](https://osv.dev/GHSA-wjgm-6hv5-3cvf)<br>GHSA-wjgm-6hv5-3cvf | 5.3 | jackson-databind: Path Deserialization Missing Scheme Allowlist for FileSystemProvider Resolution | `2.21.6` |
| [CVE-2026-91776](https://osv.dev/GHSA-wv8q-qhhj-9h54)<br>GHSA-wv8q-qhhj-9h54 | 7.5 | jackson-databind retains every unknown raw type ID  | `2.21.7` |

### `org.apache.logging.log4j:log4j-api` — 2.24.3 (1 nalaza)

| CVE / OSV | CVSS | Opis | Fixed version u izvještaju |
| --- | ---: | --- | --- |
| [CVE-2026-49844](https://osv.dev/GHSA-qv9r-c865-cp47)<br>GHSA-qv9r-c865-cp47 | 6.3 | Apache Log4j API: Improper encoding of non-finite floating-point values during MapMessage JSON serialization | `2.25.5` |

### `org.apache.tomcat.embed:tomcat-embed-core` — 10.1.48 (19 nalaza)

| CVE / OSV | CVSS | Opis | Fixed version u izvještaju |
| --- | ---: | --- | --- |
| [CVE-2026-24880](https://osv.dev/GHSA-563x-q5rq-57qp)<br>GHSA-563x-q5rq-57qp | 7.5 | Apache Tomcat has an HTTP Request/Response Smuggling vulnerability | `10.1.52` |
| [CVE-2026-43515](https://osv.dev/GHSA-5m62-pw8w-7w9f)<br>GHSA-5m62-pw8w-7w9f | 9.1 | Apache Tomcat - Security constraints not correctly applied | `10.1.55` |
| [CVE-2026-43513](https://osv.dev/GHSA-5mp6-jrq3-r938)<br>GHSA-5mp6-jrq3-r938 | 7.5 | Apache Tomcat: LockOutRealm treats user names as case-sensitive | `10.1.55` |
| [CVE-2026-55956](https://osv.dev/GHSA-5v3h-fjv2-54fg)<br>GHSA-5v3h-fjv2-54fg | 6.5 | Apache Tomcat has an Improper Authorization vulnerability | `10.1.56` |
| [CVE-2026-25854](https://osv.dev/GHSA-9m3c-qcxr-9x87)<br>GHSA-9m3c-qcxr-9x87 | 6.9 | Apache Tomcat has an Open Redirect vulnerability | `10.1.53` |
| [CVE-2026-43514](https://osv.dev/GHSA-9m89-8frq-c98c)<br>GHSA-9m89-8frq-c98c | 3.7 | Apache Tomcat - AJP secret compared in non-constant time | `10.1.55` |
| [CVE-2026-65905](https://osv.dev/GHSA-9xv2-5v5q-p794)<br>GHSA-9xv2-5v5q-p794 | 9.8 | Apache Tomcat's DIGEST authenticator has an Authentication Bypass by Capture-replay vulnerability | `10.1.58` |
| [CVE-2025-66614](https://osv.dev/GHSA-fpj8-gq4v-p354)<br>GHSA-fpj8-gq4v-p354 | 9.1 | Apache Tomcat - Client certificate verification bypass | `10.1.50` |
| [CVE-2026-42498](https://osv.dev/GHSA-fv25-8xcx-gqjc)<br>GHSA-fv25-8xcx-gqjc | 7.3 | Apache Tomcat - WebSocket authentication header exposure | `10.1.55` |
| [CVE-2026-65182](https://osv.dev/GHSA-gcx9-497g-6cp6)<br>GHSA-gcx9-497g-6cp6 | 9.1 | Apache Tomcat has an Improper Access Control, Incorrect Authorization vulnerability | `10.1.58` |
| [CVE-2026-41284](https://osv.dev/GHSA-gx5v-xp9w-j4cg)<br>GHSA-gx5v-xp9w-j4cg | 7.5 | Apache Tomcat: Unbounded read in WebDAV LOCK and  PROPFIND handling | `10.1.55` |
| [CVE-2026-68525](https://osv.dev/GHSA-h3x4-894j-xpx5)<br>GHSA-h3x4-894j-xpx5 | 9.1 | Apache Tomcat's FORM authentication process has an Incorrect Authorization vulnerability | `10.1.58` |
| [CVE-2026-43512](https://osv.dev/GHSA-h6fc-48rj-7qqh)<br>GHSA-h6fc-48rj-7qqh | 9.8 | Apache Tomcat - Digest authenticator will authenticate any unknown user | `10.1.55` |
| [CVE-2026-53404](https://osv.dev/GHSA-h792-v28v-ppgr)<br>GHSA-h792-v28v-ppgr | 7.3 | Apache Tomcat's rewrite valve has an Always-Incorrect Control Flow Implementation vulnerability | `10.1.56` |
| [CVE-2026-24734](https://osv.dev/GHSA-mgp5-rv84-w37q)<br>GHSA-mgp5-rv84-w37q | 7.5 | Apache Tomcat has an Improper Input Validation vulnerability | `10.1.52` |
| [CVE-2026-55276](https://osv.dev/GHSA-mqg3-r7h5-24x4)<br>GHSA-mqg3-r7h5-24x4 | 9.1 | Apache Tomcat has an Always-Incorrect Control Flow Implementation vulnerability | `10.1.56` |
| [CVE-2026-41293](https://osv.dev/GHSA-r29c-68gh-xp6x)<br>GHSA-r29c-68gh-xp6x | 9.8 | Apache Tomcat - HTTP/2 request headers not validated | `10.1.55` |
| [CVE-2026-34483](https://osv.dev/GHSA-rv64-5gf8-9qq8)<br>GHSA-rv64-5gf8-9qq8 | 7.5 | Apache Tomcat has an Improper Encoding or Escaping of Output vulnerability in the JsonAccessLogValve | `10.1.54` |
| [CVE-2026-34487](https://osv.dev/GHSA-x4m4-345f-5h5g)<br>GHSA-x4m4-345f-5h5g | 7.5 | Apache Tomcat vulnerable to Insertion of Sensitive Information into Log File | `10.1.54` |

### `org.springframework.boot:spring-boot` — 3.5.7 (1 nalaza)

| CVE / OSV | CVSS | Opis | Fixed version u izvještaju |
| --- | ---: | --- | --- |
| [CVE-2026-40973](https://osv.dev/GHSA-wwpq-f5c3-7hvx)<br>GHSA-wwpq-f5c3-7hvx | 7.0 | Spring Boot accepts predictable temp directory without ownership verification | `3.5.14` |

### `org.springframework.boot:spring-boot-autoconfigure` — 3.5.7 (1 nalaza)

| CVE / OSV | CVSS | Opis | Fixed version u izvještaju |
| --- | ---: | --- | --- |
| [CVE-2026-41001](https://osv.dev/GHSA-ggg2-9786-hwc8)<br>GHSA-ggg2-9786-hwc8 | 5.3 | Spring Boot: Predictable Temp Directory in Artemis Auto-configuration | `3.5.15` |

### `org.springframework:spring-core` — 6.2.12 (1 nalaza)

| CVE / OSV | CVSS | Opis | Fixed version u izvještaju |
| --- | ---: | --- | --- |
| [CVE-2026-41848](https://osv.dev/GHSA-659m-px2c-25wj)<br>GHSA-659m-px2c-25wj | 3.7 | Spring Framework Denial of Service via AntPathMatcher | `6.2.19` |

### `org.springframework:spring-expression` — 6.2.12 (3 nalaza)

| CVE / OSV | CVSS | Opis | Fixed version u izvještaju |
| --- | ---: | --- | --- |
| [CVE-2026-41852](https://osv.dev/GHSA-9f52-rjqv-25qv)<br>GHSA-9f52-rjqv-25qv | 3.7 | Spring Framework Arbitrary Method Invocation in SpEL Expressions | `6.2.19` |
| [CVE-2026-41850](https://osv.dev/GHSA-r5w3-xv2f-j59q)<br>GHSA-r5w3-xv2f-j59q | 7.5 | Spring Framework Algorithmic Denial of Service via SpEL Expressions | `6.2.19` |
| [CVE-2026-41851](https://osv.dev/GHSA-wxpp-56q6-5pcg)<br>GHSA-wxpp-56q6-5pcg | 5.3 | Spring Framework Denial of Service via Unbounded Cache in SpEL | `6.2.19` |

### `org.springframework:spring-web` — 6.2.12 (1 nalaza)

| CVE / OSV | CVSS | Opis | Fixed version u izvještaju |
| --- | ---: | --- | --- |
| [CVE-2026-41854](https://osv.dev/GHSA-7m2p-62gw-p8qq)<br>GHSA-7m2p-62gw-p8qq | 4.2 | Spring Framework Server-Side Request Forgery via UriComponentsBuilder | `6.2.19` |

### `org.springframework:spring-webmvc` — 6.2.12 (13 nalaza)

| CVE / OSV | CVSS | Opis | Fixed version u izvještaju |
| --- | ---: | --- | --- |
| [CVE-2026-41845](https://osv.dev/GHSA-3chg-m5w7-qfv5)<br>GHSA-3chg-m5w7-qfv5 | 7.1 | Spring Framework Cross-site Scripting via JavaScriptUtils | `6.2.19` |
| [CVE-2026-22737](https://osv.dev/GHSA-4773-3jfm-qmx3)<br>GHSA-4773-3jfm-qmx3 | 5.9 | Spring Framework Improper Path Limitation with Script View Templates | `6.2.17` |
| [CVE-2026-22735](https://osv.dev/GHSA-6hcq-hmm3-jj3c)<br>GHSA-6hcq-hmm3-jj3c | 2.6 | Spring MVC and WebFlux has Server Sent Event stream corruption | `6.2.17` |
| [CVE-2026-22745](https://osv.dev/GHSA-6p4f-wcwh-5vvm)<br>GHSA-6p4f-wcwh-5vvm | 5.3 | Spring MVC and WebFlux applications are vulnerable to Denial of Service attacks when resolving static resources | `6.2.18` |
| [CVE-2026-41843](https://osv.dev/GHSA-72pg-x5f8-j25j)<br>GHSA-72pg-x5f8-j25j | 5.9 | Spring Framework Path Traversal via Versioned Static Resources in Spring MVC and WebFlux | `6.2.19` |
| [CVE-2026-41846](https://osv.dev/GHSA-957g-f97v-vppc)<br>GHSA-957g-f97v-vppc | 5.9 | Spring Framework Cross-site Scripting via JSP Form Tags | `6.2.19` |
| [CVE-2026-41853](https://osv.dev/GHSA-cjpg-rgq5-fr37)<br>GHSA-cjpg-rgq5-fr37 | 5.3 | Spring Framework Multipart Request Smuggling in Spring MVC and WebFlux | `6.2.19` |
| [CVE-2026-41844](https://osv.dev/GHSA-h3qp-gqrc-q736)<br>GHSA-h3qp-gqrc-q736 | 4.2 | Spring Framework Open Redirect in Spring MVC and WebFlux | `6.2.19` |
| [CVE-2026-47890](https://osv.dev/GHSA-j9f9-w8pj-32f8)<br>GHSA-j9f9-w8pj-32f8 | 9.8 | Spring Framework Server Sent Event stream corruption while rendering fragments | `7.0.9` |
| [CVE-2026-41841](https://osv.dev/GHSA-mq64-j8f9-9gcj)<br>GHSA-mq64-j8f9-9gcj | 5.9 | Spring Framework Information Disclosure via Static Resource Cache in Spring MVC and WebFlux | `6.2.19` |
| [CVE-2026-47884](https://osv.dev/GHSA-pc63-qcmh-9cmg)<br>GHSA-pc63-qcmh-9cmg | 9.8 | Spring Framework Improper Path Limitation in XsltView | `7.0.9` |
| [CVE-2026-22741](https://osv.dev/GHSA-wg35-8jpf-2xv3)<br>GHSA-wg35-8jpf-2xv3 | 3.1 | Spring MVC and WebFlux applications are vulnerable to cache poisoning when resolving static resources. | `6.2.18` |
| [CVE-2026-41842](https://osv.dev/GHSA-x23c-287f-qqv5)<br>GHSA-x23c-287f-qqv5 | 7.5 | Spring Framework Denial of Service via Versioned Resources in Spring MVC and WebFlux | `6.2.19` |

### `org.yaml:snakeyaml` — 1.33 (1 nalaza)

| CVE / OSV | CVSS | Opis | Fixed version u izvještaju |
| --- | ---: | --- | --- |
| [CVE-2022-1471](https://osv.dev/GHSA-mjmj-j48q-9wg2)<br>GHSA-mjmj-j48q-9wg2 | 8.3 | SnakeYaml Constructor Deserialization Remote Code Execution | `2.0` |

## Granice zaključaka

- Analiza je snimka lokalnog izvještaja i pregledanog projekta na navedeni datum. Baze ranjivosti, ocjene i rasponi pogođenih verzija mogu se naknadno promijeniti.
- Nisu izvođeni exploit testovi, napadi opterećenjem ili potvrda udaljenog izvršavanja koda.
- Nisu potvrđene stvarne postavke produkcijskog proxyja, mreže, JVM parametara, varijabli okruženja i dodatnih konfiguracijskih datoteka izvan projekta.
- Nije proveden potpun sigurnosni audit aplikacije. OSV se bavi poznatim ranjivostima komponenti; ne pokriva automatski aplikacijske propuste autorizacije, CORS-a, SQL-a, XML-a ili rukovanja datotekama.
- Tijekom izrade ove analize nisu mijenjani izvorni kod ni konfiguracija aplikacije.

## Povezane datoteke i izvori

- [Izvorni OSV izvještaj](osv-java.txt)
- [OSV skripta](../../osv-scan.sh)
- [Maven konfiguracija](../../pom.xml)
- [Konfiguracija aplikacije](../../src/main/resources/application.properties)
- [Učitavanje YAML postavki](../../src/main/java/hr/span/akademija/medtermin/platforma/PostavkeController.java)
- [Provjera prijavljenog korisnika](../../src/main/java/hr/span/akademija/medtermin/platforma/PrijavljeniKorisnikResolver.java)
- [Autentikacijski filter](../../src/main/java/hr/span/akademija/medtermin/platforma/AuthFilter.java)
- [Autentikacijski endpointi](../../src/main/java/hr/span/akademija/medtermin/platforma/AuthController.java)
- [Pohrana autentikacijskih tokena](../../src/main/java/hr/span/akademija/medtermin/platforma/TokenStore.java)
- [OSV dokumentacija o manifestima i Maven razrješavanju](https://google.github.io/osv-scanner/supported-languages-and-lockfiles/)

Dokument je običan Markdown (`.md`): može se čitati kao tekst, otvoriti u IntelliJ/GitHub/VS Code Markdown prikazu ili naknadno pretvoriti u HTML/PDF. Ostali formati nisu generirani.

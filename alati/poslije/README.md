# Stanje sigurnosnih izvještaja

Izvještaji su snimke pojedinih pokretanja, s različitim obuhvatom i bazama; ne dokazuju da aplikacija nema ranjivosti. Ne mijenjati ručno strojne izvještaje: obnoviti ih odgovarajućim alatom.

## Dependency-Check i CVE-2018-1258

Raniji Dependency-Check 13.0.0 od 15. 11. 2026. pregledao je 81 JAR iz `target/dependency`: JSON i HTML izvještaj ne sadrže aktivne nalaze, ali bilježe jedan isključeni nalaz. Sonatype OSS Index nije radio jer nema vjerodajnica, pa rezultat ne uključuje njegovu analizu. NVD podaci i opseg skeniranja određuju nalaz.

CVE-2018-1258 (CWE-863) opisuje zaobilaženje autorizacije metoda uz **Spring Framework 5.0.5** i Spring Security: neovlašteni korisnik mogao bi pozvati zaštićenu metodu. U ovom projektu Spring Boot 4.1.2 koristi Spring Framework 7.0.10, dok je prijavljeni `spring-security-crypto` 7.1.2 samo modul za BCrypt u `LozinkaHasher`. Aplikacija ne koristi Springovu autorizaciju metoda; autorizaciju provjeravaju vlastiti filteri i kontroleri. NVD zapis u izvještaju sadržava opći CPE za Spring Security i specifični Spring Framework 5.0.5; povezivanje sa samim crypto JAR-om pogrešno označava ovaj projekt. Ovo je lažno pozitivan nalaz, ne ispravak ranjivog produkcijskog koda. Izvori: [NVD](https://nvd.nist.gov/vuln/detail/CVE-2018-1258) i [Spring](https://spring.io/security/cve-2018-1258).

`../../scripts/dependency-check-suppressions.xml` isključuje samo taj CVE i točan SHA-1 tog JAR-a; ostali CVE-ovi i druge verzije nisu isključeni. `../../pom.xml` sadrži TODO za ponovnu procjenu pri promjeni Spring ovisnosti. Pokrenuti `./dependency-check-scan.sh`, pa `./after-OWASP-dependency-check-scan.sh`: aktivni i isključeni nalazi prikazuju se odvojeno. Funkcionalni testovi `NalaziFunkcionalniTest` pokrivaju odbijanje tuđeg nalaza i administratorski pristup; `PlatformaJedinicniTest` provjerava BCrypt i migraciju starog hasha. Oni provjeravaju ponašanje aplikacije, a ne dokazuju odsutnost svih ranjivosti.

## Ostale datoteke

- `dependency-check-report.html` / `.json`: izvorni strojni izvještaji; `dependency-check-java.txt` je sažetak, a `dependency-check.log` zapis rada.
- `osv-java.txt`: prethodni OSV pregled prijavio je 7 nalaza u testnim Jackson 2.22.1 ovisnostima. One su podignute na 2.22.3, verziju navedenu kao ispravljenu za svih 7 nalaza; `sbom-cyclonedx-java.json` sadrži nove verzije. Novi OSV pregled nije proveden jer je automatska provjera odbila slanje popisa ovisnosti servisu OSV. Stari izvještaj ne opisuje novi SBOM; stanje nakon nadogradnje treba potvrditi ponovnim skeniranjem.
- `trivy-java.txt`: prethodna provjera `pom.xml`, bez prijavljenih nalaza; obuhvat nije isti kao kod OSV-a ili JAR skena.
- `semgrep-java.sarif`: prethodni Semgrep izvještaj bez nalaza. `spotbugs-java.xml` i `.txt`: ponovljeni pregled bez prijavljenih nalaza uz ciljane iznimke za osam injektiranih `JdbcTemplate` referenci; vidjeti ciljano isključenje `../../scripts/spotbugs-exclude.xml`.
- `../prije/zap-lokalno.html` i `zap-lokalno.html`: ZAP 2.17.0, aktivni brzi pregled lokalnih instanci izvorne i popravljene verzije. Nije korištena autentificirana sesija. Prije: 3 srednja, 1 niski i 2 informativna nalaza; poslije: 2 srednja i 3 informativna. Poslije ostaju upozorenja o odsutnosti anti-CSRF tokena i CSP `style-src unsafe-inline`: potrebna je dodatna procjena, ne smatraju se automatski potvrđenim ni otklonjenim ranjivostima. ZAP je u oba pokretanja prijavio grešku Firefox integracije i upozorenje o geckodriveru, pa JavaScript preglednik nije provjeren.
- `crveno-zeleno.md`: izvedeni sigurnosni testovi i njihovi rezultati na izvornom i popravljenom kodu.

## Testovi

Čisti `./mvnw clean verify` na Javi 21 izvršio je 157 testova: 156 prolazi, a jedan neizmijenjeni izvorni test očekuje otkrivanje tuđeg nalaza i pada jer ispravak to sprječava. JaCoCo je za Java produkcijski kod izmjerio 806/806 linija i 357/357 grana; zbog pada izvornog testa izvještaj se zasebno generira naredbom `./mvnw jacoco:report` i nalazi se u `../../target/site/jacoco/index.html`. Ovo ne uključuje Python ili shell skripte i pokrivenost ne jamči odsutnost propusta.

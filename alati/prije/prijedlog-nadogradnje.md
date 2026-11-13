# Prijedlog nadogradnje ovisnosti — bez primjene

**Status:** samo analiza i TODO komentari. Verzije, ponašanje aplikacije, testovi i Maven konfiguracija ostaju kao prije pokušaja nadogradnje. Sve funkcionalne promjene tog pokušaja povučene su na zahtjev korisnika.

## Predložena verzija Spring Boota

Na dan provjere, 13. studenoga 2026., službena dokumentacija i Maven Central navode **Spring Boot 4.1.2** kao najnovije stabilno izdanje. **4.2.0-RC2** je preview i nije prijedlog za ovu migraciju. Trenutačni parent u projektu ostaje **3.5.7**.

Prijelaz na Boot 4 zahtijeva planiranu migraciju startera i Jacksona te provjeru kompatibilnosti i testova. Nije dovoljan samo novi broj parent verzije. Spring Boot 3.5.16 posljednje je OSS izdanje grane 3.5, pa nije dugoročni cilj za održavanje bez komercijalne podrške.

Izvori:

- [Stabilna Spring Boot izdanja](https://docs.spring.io/spring-boot/index.html)
- [Maven Central metapodaci](https://repo.maven.apache.org/maven2/org/springframework/boot/spring-boot-starter-parent/maven-metadata.xml)
- [Migracijske upute za Boot 4](https://github.com/spring-projects/spring-boot/wiki/Spring-Boot-4.0-Migration-Guide)
- [Spring Boot 3.5.16 i kraj OSS podrške](https://spring.io/blog/2026/06/25/spring-boot-3-5-16-available-now/)

## Što treba povezati s pom.xml

| Mjesto | Izvor / povezanost | Prijedlog, bez primjene |
| --- | --- | --- |
| Boot parent 3.5.7 | OSV, Trivy i Dependency-Check nalazi za Boot, Spring, Tomcat, Jackson i logging | Planirati usklađenu migraciju na 4.1.2; ne mijenjati svaki tranzitivni JAR zasebno. |
| `snakeyaml.version=1.33` | CVE-2022-1471, sva tri SCA izvještaja | Ovaj override blokira podizanje SnakeYAML verzije promjenom parenta. Predložiti njegovo uklanjanje i podržanu verziju. |
| Izravna SnakeYAML ovisnost | `PostavkeController.spremi` → `new Yaml().load` nad korisničkim unosom | Predložiti `SafeConstructor` i regresijski test. Provjera `Map` nakon učitavanja nije dovoljna zaštita. Kod zasad ostaje isti. |
| Web starter | Tranzitivno povlači MVC, Jackson, Tomcat i logging | Pri budućoj migraciji razmotriti `spring-boot-starter-webmvc` i Jackson 3 prema Boot uputama. Trenutačno ostaje `spring-boot-starter-web`. |
| Test starter → AssertJ 3.27.6 | Dependency-Check CVE-2026-24400 | Predložiti AssertJ 3.27.7 ili kompatibilan Boot BOM koji ga upravlja. Upozorenje se odnosi na testni opseg i relevantnu XML obradu. |
| `spring-security-crypto` | Dependency-Check CVE-2018-1258 | Provjeriti primjenjivost: opis navodi Framework 5.0.5 i method security. Vlastiti `LozinkaHasher` trenutačno ne koristi ovu biblioteku; ne proglašavati nalaz potvrđenim niti mijenjati ovisnost bez odluke. |
| CycloneDX profil `sca` | Prethodni SBOM ima 45 komponenti, bez testnih ovisnosti; DC skenira i testne JAR-ove | Predložiti `includeTestScope=true` radi usporedivog obuhvata. Konfiguracija zasad nije promijenjena. |

## Pravila nastavka

- Ovaj dokument i TODO oznake predstavljaju prijedloge, a ne provedene popravke.
- Buduće promjene verzija, konfiguracije ili ponašanja rade se tek nakon izričitog zahtjeva za implementaciju.
- Izvještaji u `alati/prije` ostaju izvor nalaza za trenutačno nenadograđeni projekt.
- Rezultat „88 komponenti bez nalaza” odnosio se na privremeni, sada povučeni pokušaj nadogradnje. **Ne opisuje trenutačno stanje projekta.** Njegovi artefakti uklonjeni su iz projektnog direktorija `alati/poslije`.

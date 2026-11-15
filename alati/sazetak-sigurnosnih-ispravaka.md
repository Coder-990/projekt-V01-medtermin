# Sažetak sigurnosnih ispravaka

Ovaj dokument povezuje izmjene s izvornim zadacima iz [plana](plan-ispravljanja-ranjivosti.md). Izvorni TODO komentari sačuvani su u kodu kao povijesni opis stanja prije popravka; novi komentari objašnjavaju provedenu promjenu. Njihovo postojanje samo po sebi ne znači da je svaka provjera iz plana završena.

## M01 i M02 — zaštita nalaza i datoteka

- **Što:** U `NalazController` provjera vlasništva nalaza sada prethodi obradi formata. Preuzimanje traži odgovarajući zapis u bazi i provjerava vlasnika, osim za administratora. Stvarna putanja mora biti unutar mape pohrane; odbijaju se apsolutne putanje, izlazak iz mape i simboličke poveznice izvan pohrane. Kod pogrešnog formata ne ispisuje se sadržaj iznimke.
- **Zašto i rezultat:** Ranije je nevaljan format mogao preskočiti provjeru vlasništva, a ime datoteke nije dokaz vlasništva. Ispravak onemogućuje te načine pristupa tuđim nalazima i izlazak iz pohrane u provjerenim slučajevima.
- **Provjera:** `NalaziFunkcionalniTest`, `SigurnosniRegresijskiTest` i `PutanjeSigurnosniTest` pokrivaju vlasništvo, formate, administratorski pristup, putanje, symlink i nestanak datoteke tijekom čitanja.

## M03 i M04 — reset lozinke

- **Što:** `ResetLozinkeService` generira 32 nasumična bajta, korisniku šalje token, a u bazu sprema njegov SHA-256 sažetak s rokom od 15 minuta. Novi zahtjev opoziva prethodne tokene. Uvjetno ažuriranje unutar transakcije dopušta samo jednu potrošnju važećeg tokena prije promjene lozinke.
- **Zašto i rezultat:** Token više nije predvidljiv iz korisničkog imena i datuma; istekao ili već iskorišten token ne može ponovno promijeniti lozinku, ni pri testiranim istodobnim zahtjevima.
- **Provjera:** `PromjenaLozinkeFunkcionalniTest` i `SigurnosniRegresijskiTest` pokrivaju cijeli tok, ponovnu uporabu, istek, usporednu potrošnju i pohranu sažetka.
- **Preostaje:** Razmotriti ograničavanje zahtjeva za reset. Postojeća pošta je lokalna simulacija s HTTP localhost poveznicom; za produkciju su potrebni sigurna dostava i javni HTTPS URL.

## M05 — lozinke i migracija

- **Što:** `LozinkaHasher` za nove lozinke koristi BCrypt s faktorom 12 i nasumičnom soli. `KorisnikService` nakon uspješne prijave stare SHA-256 vrijednosti zamjenjuje BCrypt vrijednostima uz uvjet da se prethodni hash nije promijenio. Nove lozinke dulje od 72 UTF-8 bajta odbijaju se zbog BCrypt ograničenja; stare dulje lozinke još se mogu provjeriti starim algoritmom, ali se ne migriraju automatski.
- **Zašto i rezultat:** Umjesto brzog nesoljenog sažetka nove lozinke dobivaju sporiji soljeni hash, a postojeći korisnici mogu nastaviti s prijavom i migracijom. Odbijanje predugih lozinki sprečava prihvaćanje samo njihova početka.
- **Provjera:** `PlatformaJedinicniTest`, `SigurnosneGraneTest` i `SigurnosniRegresijskiTest` provjeravaju sol, staru provjeru, migraciju i granicu duljine.

## M06 i M12 — sadržaj logova

- **Što:** `KorisnikService` više ne zapisuje cijeli zahtjev za prijavu. `RezervacijaService` bilježi broj termina odnosno rezervacije bez OIB-a, napomene ili korisničkog teksta. Poruka o nevaljanom formatu nalaza ne zapisuje korisnički sadržaj iz iznimke.
- **Zašto i rezultat:** Lozinke, OIB i slobodna napomena više se ne izlažu kroz te zapise; uklanjanje proizvoljnog teksta sprečava i umetanje lažnih redaka u ove logove.
- **Provjera:** Funkcionalni testovi prijave, nalaza i rezervacija prolaze; preostale nalaze alata za logiranje treba zasebno pregledati. Ako su stari logovi korišteni, pregledati njihovo čuvanje i eventualnu izloženost lozinki.

## M07 — CORS

- **Što:** `CorsFilter` uspoređuje origin s točnim vrijednostima iz konfigurirane liste; `AppProperties.Cors` radi obrambenu kopiju liste.
- **Zašto i rezultat:** Nepoznati HTTP(S) origin više ne dobiva dozvolu za pristup s vjerodajnicama, a naknadna izmjena izvorne liste ne mijenja konfiguraciju.
- **Provjera:** `CorsFunkcionalniTest` provjerava dopušteni frontend i odbijanje nepoznatog origina.

## M08 i M10 — preusmjeravanje i kolačić

- **Što:** `PrijavaController` prihvaća lokalno odredište samo ako zadovoljava provjere i odbija vanjske adrese, `//`, obrnute kose crte, kontrolne znakove i kodirane vrijednosti. Kolačić je HttpOnly, SameSite=Lax i prema zadanim postavkama Secure. Za lokalni HTTP razvoj moguće je postaviti `APP_COOKIE_SECURE=false`.
- **Zašto i rezultat:** Smanjuje se rizik otvorenog preusmjeravanja i slanja sesijskog kolačića preko nezaštićene veze. Produkcija mora koristiti HTTPS i zadržati Secure.
- **Provjera:** `PrijavaObrascemFunkcionalniTest`, `SigurnosniRegresijskiTest` i `SigurnosneGraneTest` pokrivaju lokalna i odbijena odredišta; stvarnu TLS terminaciju i produkcijske postavke treba provjeriti pri postavljanju.

## M09 — sesije i odjava

- **Što:** `TokenStore` sesijama daje rok od osam sati, ograničava broj aktivnih tokena na 10 000 i uklanja istekle. `AuthController` dodaje odjavu koja opoziva token iz zaglavlja ili kolačića; `AuthFilter` dijeli postojeće metode njegova izdvajanja s odjavom. Promjena ili reset lozinke opoziva sve korisnikove sesije.
- **Zašto i rezultat:** Istekle i opozvane sesije više ne autentificiraju korisnika, a pohrana ima ograničenu veličinu.
- **Provjera:** `PlatformaJedinicniTest`, `SigurnosneGraneTest` i `SigurnosniRegresijskiTest` provjeravaju istek, limit, odjavu i opoziv nakon reseta.

## M11 — sortiranje liječnika

- **Što:** `LijecnikController` mapira traženi stupac na poznate SQL identifikatore, a smjer isključivo na `ASC` ili `DESC`; nepoznate vrijednosti odbija.
- **Zašto i rezultat:** Korisnički SQL fragment više ne ulazi u `ORDER BY`; uobičajena sortiranja ostaju dostupna.
- **Provjera:** `LijecniciFunkcionalniTest` i `SigurnosniRegresijskiTest` provjeravaju valjana sortiranja i odbijanje pokušaja SQL injekcije. Raniji SpotBugs izvještaj prijavio je potencijalnu injekciju zbog sastavljanja SQL-a; aktualni izvještaj i njegov obuhvat navedeni su u `poslije/README.md`.

## YAML — CVE-2022-1471, plan korak 5

- **Što:** U `pom.xml` uklonjen je eksplicitni SnakeYAML 1.33 override; verzija dolazi iz Spring Boot BOM-a. `PostavkeController` koristi `SafeConstructor`, ograničava veličinu i složenost YAML-a, zabranjuje duple ključeve i provjerava očekivane ključeve i vrijednosti.
- **Zašto i rezultat:** Parser ne bi smio stvarati proizvoljne Java objekte iz korisničkog YAML-a, a ograničenja smanjuju rizik preopterećenja i neočekivane strukture.
- **Provjera:** `PostavkeFunkcionalniTest`, `SigurnosniRegresijskiTest` i `SigurnosneGraneTest` pokrivaju postavke, nevaljane tagove, ulaz i prava pristupa. Potrebna je nova usporediva SCA provjera ovisnosti.

## T01 i pomoćni XML alati

- **Što:** Postojeće zabrane DOCTYPE-a i vanjskih entiteta u `UvozTermina` zadržane su i komentirane. `../scripts/spotbugs-pregled.py` i `../scripts/oznacavanje-ranjovisti.py` koriste `defusedxml` i ograničavaju veličinu XML izvještaja; ovisnost je navedena u `../scripts/requirements-security-tools.txt`.
- **Zašto i rezultat:** Sigurnosni test potvrđuje odbijanje XML-a s vanjskim entitetom; pomoćni alati sigurnije obrađuju nepouzdane izvještaje.
- **Provjera:** `UvozSigurnosniTest` i Python provjera sintakse. T02 i ostala upozorenja ostaju predmet odvojene provjere.

## Testovi i otvorene stavke

Čisti Maven testni skup od 15. 11. 2026. izvršio je 157 testova; 156 prolazi, a jedan izvorni funkcionalni test za tuđi nalaz pada jer očekuje ranjivo ponašanje. JaCoCo je izmjerio 100 % linija, grana, instrukcija, metoda i klasa za Java kod; Python skripte nisu uključene u taj broj. Izvještaj nastaje u `target/site/jacoco/index.html`.

Spring Boot ažuriran je na 4.1.2. Stanje izvještaja i pojedinačna ograničenja navedeni su u [pregledu alata](poslije/README.md). CVE-2018-1258 u Dependency-Checku označen je kao lažno pozitivan i isključen isključivo za određeni JAR; pogledati razloge i uvjete ponovne provjere u tom pregledu.

## Što je JaCoCo izvještaj i gdje se nalazi?

[JaCoCo](https://www.jacoco.org/jacoco/) je Maven dodatak koji tijekom izvršavanja testova bilježi koji su dijelovi Java koda izvršeni. Nakon testova iz tih podataka automatski izrađuje HTML izvještaj. Prikazana stranica `target/site/jacoco/hr.span.akademija.medtermin.platforma/index.html` pregled je **paketa `platforma`**: pokazuje klase tog paketa, postotak pokrivenih instrukcija i grana te broj nepokrivenih i pokrivenih redaka i metoda. Klik na klasu otvara detalje; klik na izvornu datoteku prikazuje označene retke.

Glavni izvještaj svih Java paketa nalazi se u `target/site/jacoco/index.html`, a podaci za automatiziranu obradu u `target/site/jacoco/jacoco.xml` i `jacoco.csv`. Datoteka `target/jacoco.exec` sadrži podatke prikupljene tijekom izvođenja testova. To su generirani rezultati builda, a ne dijelovi aplikacije ili ručno napisani izvorni kod. Naredba `./mvnw clean` briše mapu `target`; `./mvnw clean test` ponovno pokreće testove i generira izvještaj. Lokalni funkcionalni testovi trebaju mogućnost otvaranja lokalnog porta.

Zeleno označava izvršeni kod, crveno neizvršeni, a žuto djelomično pokrivene grane. Posljednji čisti Java izvještaj pokazao je 100 % pokrivenosti, no taj postotak govori koje su dijelove testovi izvršili, ne dokazuje da testovi otkrivaju sve moguće greške ili ranjivosti. Python skripte nisu uključene u JaCoCo mjerenje.

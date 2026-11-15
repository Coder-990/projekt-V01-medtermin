# Crveno i zeleno: sigurnosni testovi

15. 11. 2026., Java 21. Prije: privremena kopija Git `HEAD` (e72c770106bed84a9f3b1ce599ec144cc4ea67c3), s istim sigurnosnim testovima i testnom pomoćnom klasom; aplikacijski kod nije mijenjan. Poslije: trenutačni aplikacijski kod. Testovi koriste lokalnu, izdvojenu H2 bazu, privremene mape i lokalni HTTP port. Izvorni funkcionalni testovi nisu izmijenjeni.

U oba pokretanja odabrani su isti testovi:

```bash
./mvnw '-Dtest=AutentikacijaSigurnostTest,YamlDeserijalizacijaTest,SigurnostHttpTest#nevaljaniFormatNeOtkrivaTudiNalaz+nepoznatiHttpsOriginNijeDopusten,SigurnosniRegresijskiTest#sortiranjeOdbijaSql+redirectIYaml+putanjaISymlink' test
```

Prije: 10 testova, 10 padova, 0 pogrešaka; poslije: 10 testova, 0 padova. Rezultati su prikupljeni iz JUnit XML-a; donja tablica bilježi konkretne metode. Test nepredvidljivosti uspoređuje novi token sa starom determinističkom formulom, a ne dokazuje entropiju sam po sebi. Kod koristi `SecureRandom`.

| Test | Prije | Poslije |
|---|---|---|
| `YamlDeserijalizacijaTest#korisnickiYamlNeStvaraProizvoljniObjekt` | pada | prolazi |
| `AutentikacijaSigurnostTest#resetTokenMozeSeIskoristitiSamoJednom` | pada | prolazi |
| `AutentikacijaSigurnostTest#lozinkaSePohranjujeSaSoljuIKdfom` | pada | prolazi |
| `AutentikacijaSigurnostTest#lozinkaSeNeZapisujeUPrijavi(CapturedOutput)` | pada | prolazi |
| `AutentikacijaSigurnostTest#resetTokenNijeIzvedenIzImenaIDatuma` | pada | prolazi |
| `SigurnosniRegresijskiTest#redirectIYaml` | pada | prolazi |
| `SigurnosniRegresijskiTest#sortiranjeOdbijaSql` | pada | prolazi |
| `SigurnosniRegresijskiTest#putanjaISymlink` | pada | prolazi |
| `SigurnostHttpTest#nepoznatiHttpsOriginNijeDopusten` | pada | prolazi |
| `SigurnostHttpTest#nevaljaniFormatNeOtkrivaTudiNalaz` | pada | prolazi |

`redirectIYaml` prvo provjerava otvoreno preusmjeravanje; u izvornom kodu pada na toj provjeri pa njezin rezultat nije dokaz izvornog ponašanja YAML-a. Skenovi i ostala ograničenja navedeni su u `README.md`. Izvorni funkcionalni test za tuđi nalaz i novi sigurnosni test imaju međusobno suprotne zahtjeve; oni nisu oba prošla.

Dodatni SCA nalaz N-10 provjeren je zasebno: `YamlDeserijalizacijaTest#korisnickiYamlNeStvaraProizvoljniObjekt` na izvornom kodu pada jer bezopasni testni objekt bude instanciran; na popravljenom kodu prolazi i odgovor je 400. Pokretanje: `./mvnw -Dtest=YamlDeserijalizacijaTest test` na obje verzije.

## Rezultati provjerenih scenarija

Zahtjevi su u navedenim testnim metodama; koriste se samo stvoreni testni računi i datoteke. Podaci o tokenima i lozinkama namjerno se ne prepisuju u izvještaj.

| Scenarij | Izvorni kod | Popravljeni kod |
|---|---|---|
| `GET /api/nalazi/2?format=nevaljano` kao drugi korisnik | 200 | 404 |
| `GET /api/nalazi/preuzmi` za simboličku poveznicu izvan pohrane | 200 | 404 |
| Zahtjev za reset i usporedba tokena s MD5 imena i datuma | podudara se | ne podudara se |
| Dva `POST /api/auth/reset` s istim tokenom | drugi 200 | drugi 400 |
| Registracija i čitanje sažetka iz testne baze | nije BCrypt | BCrypt |
| Prijava i provjera snimljenog loga | sadrži lozinku | ne sadrži lozinku |
| CORS preflight s nepoznatim HTTPS originom | 204 | 403 |
| Prijava s `returnUrl=//evil.example` | preusmjeravanje na vanjski origin | preusmjeravanje na `/` |
| `GET /api/lijecnici` sa SQL izrazom u `sort` | 200 | 400 |
| YAML s oznakom bezopasne testne klase | konstruktor je pozvan | konstruktor nije pozvan, 400 |

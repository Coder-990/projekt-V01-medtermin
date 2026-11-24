# Crveno i zeleno: sigurnosni testovi

Ponovna provjera 24. 11. 2026., Java 21. Crveno: izdvojena kopija izvornog koda i njegova `pom.xml` iz commita `e72c770106bed84a9f3b1ce599ec144cc4ea67c3`, uz iste aktualne sigurnosne testove i `ApiTestBaza`. Zeleno: čista privremena kopija trenutačnog projekta. Oba pokretanja koriste lokalni HTTP i izdvojenu H2 bazu. Kompilirani su isti testni razredi; u izvornom pokretanju odabrani su testovi iz tablice, a u aktualnom cijeli skup. Verzije okvira i ovisnosti pripadaju odgovarajućim verzijama projekta.

Crveno: 11 testova, 11 padova, 0 pogrešaka. Zeleno: svih 11 navedenih testova prolazi unutar cjelokupnog skupa od 159 testova. Statusi su uspoređeni po metodi u JUnit XML izvještajima. Dnevnik izvornog izvođenja nije priložen jer stari kod u njemu zapisuje testnu lozinku.

| Nalaz | Test | Izvorni kod | Trenutačni kod |
|---|---|---|---|
| S02 – prolazak kroz putanju | `SigurnosniRegresijskiTest#putanjaISymlink` | pada | prolazi |
| S03 – otvoreno preusmjeravanje | `SigurnosniRegresijskiTest#redirectIYaml` | pada | prolazi |
| S04 – CORS | `SigurnostHttpTest#nepoznatiHttpsOriginNijeDopusten` | pada | prolazi |
| S06 – YAML | `YamlDeserijalizacijaTest#korisnickiYamlNeStvaraProizvoljniObjekt` | pada | prolazi |
| S07 – pohrana lozinki | `AutentikacijaSigurnostTest#lozinkaSePohranjujeSaSoljuIKdfom` | pada | prolazi |
| S08 – SQL sortiranje | `SigurnosniRegresijskiTest#sortiranjeOdbijaSql` | pada | prolazi |
| S11 – predvidljiv token | `AutentikacijaSigurnostTest#resetTokenNijeIzvedenIzImenaIDatuma` | pada | prolazi |
| S11 – višekratni token | `AutentikacijaSigurnostTest#resetTokenMozeSeIskoristitiSamoJednom` | pada | prolazi |
| S12 – lozinka u zapisima | `AutentikacijaSigurnostTest#lozinkaSeNeZapisujeUPrijavi` | pada | prolazi |
| S12 – WARN neuspjele prijave | `AutentikacijaSigurnostTest#neuspjelaPrijavaBiljeziWarnBezLozinke` | pada | prolazi |
| S13 – nevaljani format | `SigurnostHttpTest#nevaljaniFormatNeOtkrivaTudiNalaz` | pada | prolazi |

Naredba za izvorni kod (na privremenoj kopiji):

```bash
./mvnw -o '-Dtest=AutentikacijaSigurnostTest,YamlDeserijalizacijaTest,SigurnostHttpTest#nevaljaniFormatNeOtkrivaTudiNalaz+nepoznatiHttpsOriginNijeDopusten,SigurnosniRegresijskiTest#sortiranjeOdbijaSql+redirectIYaml+putanjaISymlink' test
```

Za trenutačni kod: `./mvnw -o clean test`. Rezultati se odnose na ovu lokalnu provjeru, ne na nedostupne nastavničke skrivene testove niti na GitHub Actions. `redirectIYaml` u izvornoj verziji pada već na provjeri preusmjeravanja; zaseban YAML test provjerava deserijalizaciju. Test slučajnosti pokazuje da se token razlikuje od stare MD5 formule, a sam ne dokazuje entropiju.

JaCoCo: 843/847 linija (**99,53 %**) i 392/408 grana (**96,08 %**); [CSV](jacoco-aktualno.csv) i [XML](jacoco-aktualno.xml). Različito mjerenje u nastavničkoj ocjeni 99,9 % odnosi se na raniju predaju. Pokrivenost ne dokazuje odsutnost ranjivosti.

Dopunska čista provjera 24. 11. 2026. na commitu d8ed685 i Jacksonu 2.23.0: 159/159 testova prolazi; JaCoCo 843/847 linija i 392/408 grana. Crvena provjera ostaje ranije dokumentirano pokretanje izvorne verzije.

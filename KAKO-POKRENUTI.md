# MedTermin – kako pokrenuti

Web aplikacija privatne poliklinike za naručivanje pregleda, pregled nalaza i ispis potvrda.
Spring Boot 3.5, Java 21, H2 baza u memoriji (podaci se pune pri svakom pokretanju).

## Preduvjeti

- JDK 21 (npr. Eclipse Temurin 21)
- Maven nije potreban: koristi se Maven Wrapper (`./mvnw`, na Windowsu `mvnw.cmd`)
- IntelliJ IDEA ili VS Code (Extension Pack for Java): otvori mapu projekta kao Maven projekt

## Pokretanje

```bash
./mvnw spring-boot:run
```

- početna stranica: http://localhost:8080/
- prijava obrascem: http://localhost:8080/prijava
- provjera rada: http://localhost:8080/api/health

Primjeri zahtjeva za REST API nalaze se u `http/medtermin.http` (IntelliJ HTTP Client, VS Code REST Client).

## Testovi

```bash
./mvnw test                          # svi testovi
./mvnw test -Dgroups=funkcionalno    # funkcionalni testovi
./mvnw test -Dgroups=primjer         # primjer testa
```

Izvještaj o pokrivenosti koda: `target/site/jacoco/index.html`.

Vlastite testove možeš temeljiti na klasi `ApiTestBaza` (pokreće aplikaciju na nasumičnom portu i nudi
pomoćne metode za prijavu i slanje zahtjeva) – pogledaj `primjer/PrimjerSigurnosnogTestaTest`.

## Alati za analizu (neobavezno)

```bash
./mvnw -Psast compile spotbugs:check       # SpotBugs + Find Security Bugs
./mvnw -Psast compile spotbugs:gui         # isto, s grafičkim sučeljem
./mvnw -Psca cyclonedx:makeAggregateBom    # popis ovisnosti (SBOM) u target/bom.json
```

`target/bom.json` možeš provjeriti alatima poput OSV-Scanner ili Trivy.

## Korisnici

| Korisničko ime | Lozinka | Uloga |
|---|---|---|
| ana | `Ana12345!` | KORISNIK |
| marko | `Marko12345!` | KORISNIK |
| dr.horvat | `Ovlasteni12345!` | LIJECNIK |
| admin | `Admin12345!` | ADMIN |

API prihvaća token u zaglavlju `Authorization: Bearer <token>` (dobiva se s `POST /api/auth/login`)
ili kolačić `SESSION` koji postavlja prijava obrascem.

## Konfiguracija (`src/main/resources/application.properties`)

| Ključ | Značenje | Zadano |
|---|---|---|
| `app.storage-dir` | mapa s datotekama nalaza | `./data/dokumenti` |
| `app.mailbox-dir` | mapa u koju se „šalju” e-poruke (tekstualne datoteke) | `./data/posta` |
| `app.cors.allowed-origins` | origini frontend aplikacije | `http://localhost:5173` |
| `spring.datasource.url` | baza | H2 u memoriji |

Poruke za promjenu lozinke ne šalju se stvarno, nego se zapisuju u `app.mailbox-dir`
(datoteka `<korisnik>-<vrijeme>.txt` s tokenom i poveznicom).

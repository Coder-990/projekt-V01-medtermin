# Obrana projekta — osobni podsjetnik

## Kojim redom otvaram datoteke

1. Ovaj dokument je glavni vodič: pročitaj uvod i pokreni obje aplikacije.
2. Za staru verziju na **8081** redom pokreći zahtjeve iz [obrana.http](obrana.http), zatim iz [obrana-dodatno.http](obrana-dodatno.http).
3. Za popravljenu verziju na **8080** ponovi isti redoslijed i usporedi odgovore. U obje HTTP datoteke promijeni port i ponovno se prijavi.
4. [Testovi i promjene](testovi-i-promjene.md#redoslijed-demonstracije) služe kao popratni podsjetnik: po potrebi ih otvori uz primjer da pokažeš kod i test. Njihov popis prati isti redoslijed.
5. Na kraju pogledaj dio **Ako pitaju za testove** u ovom dokumentu.

## Što ukratko reći

- Pregledao sam kod i pokrenuo sigurnosne alate; svaki nalaz provjerio sam u kodu.
- Popravio sam pristup tuđim nalazima, SQL sortiranje, CORS, YAML i zaštitu lozinki i tokena.
- Dodao sam sigurnosne testove i usporedio ih s izvornom verzijom. OSV nakon popravaka nije pronašao ranjive ovisnosti.
- Detalji, testovi i poveznice na kod: [testovi i promjene](testovi-i-promjene.md).

## Priprema: dvije aplikacije

**Terminal 1 — popravljena verzija, port 8080:** iz korijena projekta pokreni:

```bash
OBRANA_POSTA=$(mktemp -d /tmp/medtermin-posta.XXXXXX)
printf 'Mapa pošte: %s\n' "$OBRANA_POSTA"
APP_MAILBOX_DIR="$OBRANA_POSTA" ./mvnw spring-boot:run
```

**Terminal 2 — izvorna verzija, port 8081:** iz istog korijena projekta pokreni:

```bash
OBRANA_PRIJE=$(mktemp -d /tmp/medtermin-prije.XXXXXX)
git archive e72c770 | tar -x -C "$OBRANA_PRIJE"
cd "$OBRANA_PRIJE"
printf 'Mapa izvorne verzije: %s\n' "$OBRANA_PRIJE"
./mvnw spring-boot:run -Dspring-boot.run.arguments=--server.port=8081
```

- Pričekaj da se obje aplikacije pokrenu. Svaka ima svoju bazu i svoje tokene.
- U IntelliJ-u otvori [obrana.http](obrana.http). Klikni zelenu strelicu uz naziv zahtjeva.
- Na vrhu datoteke postavi `@host = http://localhost:8081` za **prije**, odnosno `http://localhost:8080` za **poslije**.
- Nakon svake promjene porta ponovno pokreni **1. Prijava Ane**. Pri uspjehu se token automatski sprema; ako prijava ne uspije, provjeri port i terminal.
- Korisnik za primjere: `ana` / `Ana12345!`. Na kraju zaustavi aplikacije s `Ctrl+C`.

## Demonstracija — prati redom

### 1. Stara verzija: port 8081

1. Otvori [obrana.http](obrana.http), postavi `@host = http://localhost:8081` i pokreni **1. Prijava Ane**. U terminalu 2 možeš vidjeti testnu lozinku u starom logu.
2. **2. Anin nalaz:** očekuj **200**. **3. Markov nalaz:** očekuj **200** i Markove podatke. Ana ne bi smjela vidjeti njegov nalaz.
3. **4. Valjano sortiranje:** očekuj **200**. **5. SQL izraz:** također **200** — stari kod prihvaća izraz u sortiranju. Primjer ne mijenja podatke.
4. **6. CORS preflight:** očekuj **204** i zaglavlje `Access-Control-Allow-Origin` za nepoznati origin.
5. Po želji pokreni **7. Prijava liječnika** i **8. Potvrda identiteta**: očekuj **200** i ulogu `LIJECNIK`.
6. Otvori [obrana-dodatno.http](obrana-dodatno.http), postavi `@host = http://localhost:8081`, pokreni **1. Prijava Ane**, pa **2. YAML**: očekuj **200**. Oznaka u primjeru je bezopasna i ne otvara mrežnu vezu.
7. **Ako ima vremena — lozinka i reset:** pokreni **3. Registracija**, pa **4. Prijava** i pogledaj terminal 2: u logu se vidi testna lozinka. Zatim pokreni **5. Zatraži reset**. U mapi `data/posta` unutar mape ispisane u terminalu 2 pronađi najnoviju poruku za registriranog korisnika i kopiraj vrijednost nakon `Token:` u `@resetToken` na vrhu HTTP datoteke. Pokreni **6. Prvo korištenje** i **7. Ponovno korištenje ISTOG tokena**: očekuj **200 oba puta**.

### 2. Popravljena verzija: port 8080

1. Vrati se u [obrana.http](obrana.http), postavi `@host = http://localhost:8080` i ponovno pokreni **1. Prijava Ane**: svaka aplikacija ima svoj token. U terminalu 1 vidiš da se lozinka više ne zapisuje.
2. **2. Anin nalaz:** **200**. **3. Markov nalaz:** **404**. Reci: „Prije je Ana mogla vidjeti Markov nalaz zbog nevaljanog formata; sada prvo provjeravam vlasnika.”
3. **4. Valjano sortiranje:** **200**. **5. SQL izraz:** **400**. Reci: „Sada prihvaćam samo dopuštene stupce i smjerove.”
4. **6. CORS preflight:** **403**. Reci: „Nepoznati origin više nije dopušten.”
5. Ako si pokazao liječnika, ponovno pokreni **7. Prijava liječnika** i **8. Potvrda identiteta**: **200** i `LIJECNIK` — njegova prijava i dalje radi.
6. U [obrana-dodatno.http](obrana-dodatno.http) postavi `@host = http://localhost:8080`, ponovno pokreni **1. Prijava Ane**, pa **2. YAML**: **400**. Reci: „Sigurni parser odbija Java oznaku.” Sam status ne dokazuje izvođenje koda; poziv konstruktora posebno je provjeren testom.
7. **Ako si pokazao lozinku i reset:** ponovno pokreni **3. Registracija** i **4. Prijava**, pa pogledaj terminal 1: lozinka se ne zapisuje. Pokreni **5. Zatraži reset**, pronađi novu poruku u mapi pošte ispisanoj u terminalu 1 i njezin token kopiraj u `@resetToken`. Pokreni **6. Prvo korištenje**: **200**, pa **7. Ponovno korištenje ISTOG tokena**: **400**. Reci: „Token je sada jednokratan i ima rok trajanja.”

- [Kod i testovi za primjere](testovi-i-promjene.md#sigurnosni-popravci-napad-promjena-i-test).
- Kada završiš, zaustavi obje aplikacije s `Ctrl+C`.

## Ako pitaju za testove

- Izvorni funkcionalni test za tuđi nalaz ostao je neizmijenjen prema pravilima: očekuje ranjivi odgovor **200** pa na popravljenoj verziji pada. Novi sigurnosni test očekuje **404**.
- U dokumentiranim mjerenjima JaCoCo je pokazao **100 % linija i grana** sa svim testovima na obje verzije. Pokrivenost ne znači da svi testovi prolaze niti da su svi napadi obuhvaćeni.
- Samo stari funkcionalni testovi na popravljenoj verziji: **87 od 88 prolazi**, pokrivenost **90,7 % linija i 69,5 % grana**. Više: [testovi i promjene](testovi-i-promjene.md), [crveno/zeleno](../alati/poslije/crveno-zeleno.md), [OSV izvještaj](../alati/poslije/osv-java.txt).

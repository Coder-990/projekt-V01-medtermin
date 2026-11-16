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

**U [obrana.http](obrana.http) postavi `@host = http://localhost:8081`. Brojevi ispod odgovaraju brojevima zahtjeva u toj datoteci.**

1. **Prijava Ane:** očekuj **200**. U terminalu 2 možeš vidjeti testnu lozinku u starom logu.
2. **Anin nalaz:** očekuj **200**.
3. **Markov nalaz:** očekuj **200** i Markove podatke. Ana ne bi smjela vidjeti njegov nalaz.
4. **Valjano sortiranje:** očekuj **200**.
5. **SQL izraz:** očekuj **200** — stari kod prihvaća izraz u sortiranju. Primjer ne mijenja podatke.
6. **CORS preflight:** očekuj **204** i zaglavlje `Access-Control-Allow-Origin` za nepoznati origin.
7. **Prijava liječnika (po želji):** očekuj **200**.
8. **Potvrda identiteta (ako si pokrenuo 7):** očekuj **200** i ulogu `LIJECNIK`.

**Zatim u [obrana-dodatno.http](obrana-dodatno.http) postavi `@host = http://localhost:8081`. Numeracija ponovno kreće od 1 jer je to druga HTTP datoteka.**

1. **Prijava Ane:** očekuj **200**; sprema token za YAML primjer.
2. **YAML:** očekuj **200**. Oznaka u primjeru je bezopasna i ne otvara mrežnu vezu.
3. **Registracija (ako ima vremena za lozinku i reset):** očekuj **201**; stvara novi testni račun za sljedeće korake.
4. **Prijava:** očekuj **200**. Pogledaj terminal 2: u logu se vidi testna lozinka.
5. **Zatraži reset:** očekuj **202**. U mapi `data/posta` unutar mape ispisane u terminalu 2 pronađi najnoviju poruku za registriranog korisnika. Kopiraj vrijednost nakon `Token:` u `@resetToken` na vrhu HTTP datoteke.
6. **Prvo korištenje:** očekuj **200**.
7. **Ponovno korištenje ISTOG tokena:** očekuj **200** — token se može ponovno upotrijebiti.

### 2. Popravljena verzija: port 8080

**Vrati se u [obrana.http](obrana.http) i postavi `@host = http://localhost:8080`. Ponovno se prijavi jer svaka aplikacija ima svoj token.**

1. **Prijava Ane:** očekuj **200**. U terminalu 1 vidiš da se lozinka više ne zapisuje.
2. **Anin nalaz:** očekuj **200**.
3. **Markov nalaz:** očekuj **404**. Reci: „Prije je Ana mogla vidjeti Markov nalaz zbog nevaljanog formata; sada prvo provjeravam vlasnika.”
4. **Valjano sortiranje:** očekuj **200**.
5. **SQL izraz:** očekuj **400**. Reci: „Sada prihvaćam samo dopuštene stupce i smjerove.”
6. **CORS preflight:** očekuj **403**. Reci: „Nepoznati origin više nije dopušten.”
7. **Prijava liječnika (ako si je pokazao prije):** očekuj **200**.
8. **Potvrda identiteta (ako si pokrenuo 7):** očekuj **200** i `LIJECNIK` — njegova prijava i dalje radi.

**Zatim u [obrana-dodatno.http](obrana-dodatno.http) postavi `@host = http://localhost:8080`. Ponovno krećeš od zahtjeva 1.**

1. **Prijava Ane:** očekuj **200**; sprema token ove instance za YAML primjer.
2. **YAML:** očekuj **400**. Reci: „Sigurni parser odbija Java oznaku.” Sam status ne dokazuje izvođenje koda; poziv konstruktora posebno je provjeren [testom](testovi-i-promjene.md#redoslijed-demonstracije).
3. **Registracija (ako si pokazao lozinku i reset):** očekuj **201**; stvara testni račun u ovoj instanci.
4. **Prijava:** očekuj **200**. Pogledaj terminal 1: lozinka se ne zapisuje.
5. **Zatraži reset:** očekuj **202**. Pronađi novu poruku u mapi pošte ispisanoj u terminalu 1 i njezin token kopiraj u `@resetToken`.
6. **Prvo korištenje:** očekuj **200**.
7. **Ponovno korištenje ISTOG tokena:** očekuj **400**. Reci: „Token je sada jednokratan i ima rok trajanja.”

- [Kod i testovi za primjere](testovi-i-promjene.md#sigurnosni-popravci-napad-promjena-i-test).
- Kada završiš, zaustavi obje aplikacije s `Ctrl+C`.

## Ako pitaju za testove

- Izvorni funkcionalni test za tuđi nalaz ostao je neizmijenjen prema pravilima: očekuje ranjivi odgovor **200** pa na popravljenoj verziji pada. Novi sigurnosni test očekuje **404**.
- U dokumentiranim mjerenjima JaCoCo je pokazao **100 % linija i grana** sa svim testovima na obje verzije. Pokrivenost ne znači da svi testovi prolaze niti da su svi napadi obuhvaćeni.
- Samo stari funkcionalni testovi na popravljenoj verziji: **87 od 88 prolazi**, pokrivenost **90,7 % linija i 69,5 % grana**. Više: [testovi i promjene](testovi-i-promjene.md), [crveno/zeleno](../alati/poslije/crveno-zeleno.md), [OSV izvještaj](../alati/poslije/osv-java.txt).

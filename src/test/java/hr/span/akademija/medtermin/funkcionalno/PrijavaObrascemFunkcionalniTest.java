package hr.span.akademija.medtermin.funkcionalno;

import static org.assertj.core.api.Assertions.assertThat;

import hr.span.akademija.medtermin.ApiTestBaza;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

@Tag("funkcionalno")
class PrijavaObrascemFunkcionalniTest extends ApiTestBaza {

    @Test
    @DisplayName("Početna stranica je HTML s nazivom aplikacije")
    void pocetnaStranica() {
        Odgovor odgovor = get("/", null);

        assertThat(odgovor.status()).isEqualTo(200);
        assertThat(odgovor.zaglavlje("Content-Type")).startsWith("text/html");
        assertThat(odgovor.tijelo()).contains("MedTermin").contains("/prijava");
    }

    @Test
    @DisplayName("Obrazac za prijavu sadrži polja i povratnu adresu")
    void obrazacZaPrijavu() {
        Odgovor odgovor = get("/prijava?returnUrl=" + enc("/moji"), null);

        assertThat(odgovor.status()).isEqualTo(200);
        assertThat(odgovor.tijelo())
                .contains("name=\"username\"")
                .contains("name=\"password\"")
                .contains("name=\"returnUrl\"")
                .contains("value=\"/moji\"");
    }

    @Test
    @DisplayName("Uspješna prijava obrascem preusmjerava na /moji i postavlja kolačić SESSION")
    void prijavaObrascemPreusmjerava() {
        Odgovor odgovor = postForma("/prijava",
                Map.of("username", "ana", "password", "Ana12345!", "returnUrl", "/moji"));

        assertThat(odgovor.status()).isEqualTo(302);
        assertThat(odgovor.zaglavlje("Location")).isEqualTo("/moji");
        String kolacic = odgovor.zaglavlje("Set-Cookie");
        assertThat(kolacic).startsWith("SESSION=").containsIgnoringCase("HttpOnly");
    }

    @Test
    @DisplayName("Kolačić SESSION vrijedi za API")
    void kolacicVrijediZaApi() {
        Odgovor prijava = postForma("/prijava", Map.of("username", "marko", "password", "Marko12345!",
                "returnUrl", "/"));
        String kolacic = prijava.zaglavlje("Set-Cookie").split(";")[0];

        Odgovor ja = posalji("GET", "/api/ja", null, null, null, "Cookie", kolacic);

        assertThat(ja.status()).isEqualTo(200);
        assertThat(ja.json().get("username").asText()).isEqualTo("marko");
    }

    @Test
    @DisplayName("Neuspjela prijava obrascem prikazuje poruku")
    void neuspjelaPrijavaObrascem() {
        Odgovor odgovor = postForma("/prijava", Map.of("username", "ana", "password", "pogresno", "returnUrl", "/"));

        assertThat(odgovor.status()).isEqualTo(200);
        assertThat(odgovor.tijelo()).contains("Neispravno korisničko ime ili lozinka.");
        assertThat(odgovor.zaglavlje("Set-Cookie")).isNull();
    }

    @Test
    @DisplayName("Prijava obrascem bez returnUrl preusmjerava na /")
    void prijavaBezReturnUrl() {
        Odgovor prazan = postForma("/prijava", Map.of("username", "ana", "password", "Ana12345!", "returnUrl", "   "));
        assertThat(prazan.status()).isEqualTo(302);
        assertThat(prazan.zaglavlje("Location")).isEqualTo("/");

        Odgovor bezParametra = postForma("/prijava", Map.of("username", "ana", "password", "Ana12345!"));
        assertThat(bezParametra.status()).isEqualTo(302);
        assertThat(bezParametra.zaglavlje("Location")).isEqualTo("/");
    }

    @Test
    @DisplayName("Obrazac za novu lozinku prikazuje formu i token")
    void obrazacZaResetLozinke() {
        Odgovor odgovorBezTokena = get("/reset", null);
        assertThat(odgovorBezTokena.status()).isEqualTo(200);
        assertThat(odgovorBezTokena.zaglavlje("Content-Type")).startsWith("text/html");
        assertThat(odgovorBezTokena.tijelo()).contains("Nova lozinka").contains("name=\"novaLozinka\"");

        Odgovor odgovorSTokenom = get("/reset?token=moj-test-token", null);
        assertThat(odgovorSTokenom.status()).isEqualTo(200);
        assertThat(odgovorSTokenom.tijelo()).contains("value=\"moj-test-token\"");
    }

    @Test
    @DisplayName("Uspješna promjena lozinke obrascem prikazuje potvrdu")
    void postResetUspjesan() {
        String korisnik = noviKorisnik("resetobrasca", "Stara12345!");
        postJson("/api/auth/reset-zahtjev", null, Map.of("username", korisnik));

        // Dohvaćamo token iz baze ili mape pošte
        String token = postJson("/api/auth/login", null, Map.of("username", "admin", "password", "Admin12345!"))
                .json().get("token").asText();

        // Kreiramo token putem reset-zahtjev i pročitamo iz pošte
        Odgovor zahtjev = postJson("/api/auth/reset-zahtjev", null, Map.of("username", korisnik));
        assertThat(zahtjev.status()).isEqualTo(202);

        // Za postavljanje koristimo stvarni token iz pošte
        try {
            java.util.List<java.nio.file.Path> poruke;
            try (java.util.stream.Stream<java.nio.file.Path> stream = java.nio.file.Files.list(mapaPoste())) {
                poruke = stream.filter(p -> p.getFileName().toString().startsWith(korisnik + "-")).toList();
            }
            assertThat(poruke).isNotEmpty();
            String sadrzaj = java.nio.file.Files.readString(poruke.get(0), java.nio.charset.StandardCharsets.UTF_8);
            java.util.regex.Matcher m = java.util.regex.Pattern.compile("Token:\\s*(\\S+)").matcher(sadrzaj);
            assertThat(m.find()).isTrue();
            String resetToken = m.group(1);

            Odgovor odgovor = postForma("/reset", Map.of("token", resetToken, "novaLozinka", "NovaLozinka123!"));
            assertThat(odgovor.status()).isEqualTo(200);
            assertThat(odgovor.tijelo()).contains("Lozinka je promijenjena").contains("/prijava");
        } catch (java.io.IOException e) {
            throw new java.io.UncheckedIOException(e);
        }
    }

    @Test
    @DisplayName("Promjena lozinke obrascem s nevaljanim tokenom prikazuje grešku")
    void postResetNeispravanToken() {
        Odgovor odgovor = postForma("/reset", Map.of("token", "nepostojeci-token", "novaLozinka", "NovaLozinka123!"));

        assertThat(odgovor.status()).isEqualTo(200);
        assertThat(odgovor.tijelo()).contains("Token nije valjan ili je istekao.");
    }

    @Test
    @DisplayName("Promjena lozinke obrascem s prekratkom lozinkom hvata ApiException i prikazuje poruku")
    void postResetPrekratkaLozinka() {
        Odgovor odgovor = postForma("/reset", Map.of("token", "bilo-koji", "novaLozinka", "kratko"));

        assertThat(odgovor.status()).isEqualTo(200);
        assertThat(odgovor.tijelo()).contains("Lozinka mora imati najmanje 8 znakova.");
    }
}

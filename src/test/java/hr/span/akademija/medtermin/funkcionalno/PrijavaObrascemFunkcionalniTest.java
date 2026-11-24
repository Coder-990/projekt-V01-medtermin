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
}

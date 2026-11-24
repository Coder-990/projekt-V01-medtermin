package hr.span.akademija.medtermin.funkcionalno;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.JsonNode;
import hr.span.akademija.medtermin.ApiTestBaza;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

@Tag("funkcionalno")
class PostavkeFunkcionalniTest extends ApiTestBaza {

    @Test
    @DisplayName("Spremanje postavki obavijesti u YAML formatu")
    void spremanjePostavki() {
        String token = prijava("marko", "Marko12345!");
        String yaml = """
                podsjetnik: true
                satiPrije: 24
                kanal: email
                """;

        Odgovor odgovor = posalji("POST", "/api/postavke", token, "application/yaml", yaml);

        assertThat(odgovor.status()).isEqualTo(200);
        JsonNode postavke = odgovor.json().get("postavke");
        assertThat(postavke.get("podsjetnik").asBoolean()).isTrue();
        assertThat(postavke.get("satiPrije").asInt()).isEqualTo(24);
        assertThat(postavke.get("kanal").asText()).isEqualTo("email");
    }

    @Test
    @DisplayName("Spremljene postavke se mogu dohvatiti")
    void dohvatPostavki() {
        String token = prijava("ana", "Ana12345!");
        String yaml = "podsjetnik: false\nsatiPrije: 48\nkanal: sms\n";

        Odgovor spremanje = posalji("POST", "/api/postavke", token, "text/plain", yaml);
        Odgovor dohvat = get("/api/postavke", token);

        assertThat(spremanje.status()).isEqualTo(200);
        JsonNode postavke = dohvat.json().get("postavke");
        assertThat(postavke.get("podsjetnik").asBoolean()).isFalse();
        assertThat(postavke.get("satiPrije").asInt()).isEqualTo(48);
        assertThat(postavke.get("kanal").asText()).isEqualTo("sms");
    }

    @Test
    @DisplayName("Nepoznat kanal obavijesti vraća 400")
    void nepoznatKanal() {
        String token = prijava("ana", "Ana12345!");

        Odgovor odgovor = posalji("POST", "/api/postavke", token, "application/yaml", "kanal: golub\n");

        assertThat(odgovor.status()).isEqualTo(400);
    }
}

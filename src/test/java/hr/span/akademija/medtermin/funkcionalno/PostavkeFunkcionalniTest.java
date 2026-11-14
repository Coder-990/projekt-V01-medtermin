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

    @Test
    @DisplayName("Korisnik bez spremljenih postavki dobiva zadane postavke")
    void zadanePostavke() {
        String korisnik = noviKorisnik("novipostavke", "Lozinka12345!");
        String token = prijava(korisnik, "Lozinka12345!");

        Odgovor odgovor = get("/api/postavke", token);

        assertThat(odgovor.status()).isEqualTo(200);
        JsonNode postavke = odgovor.json().get("postavke");
        assertThat(postavke.get("podsjetnik").asBoolean()).isTrue();
        assertThat(postavke.get("satiPrije").asInt()).isEqualTo(24);
        assertThat(postavke.get("kanal").asText()).isEqualTo("email");
    }

    @Test
    @DisplayName("Neispravna sintaksa YAML-a vraća 400")
    void neispravanYamlSintaksa() {
        String token = prijava("ana", "Ana12345!");

        Odgovor odgovor = posalji("POST", "/api/postavke", token, "application/yaml", "postavke: [nezatvorena lista");

        assertThat(odgovor.status()).isEqualTo(400);
        assertThat(odgovor.json().get("greska").asText()).isEqualTo("Neispravan YAML.");
    }

    @Test
    @DisplayName("YAML koji nije mapa vraća 400")
    void yamlNijeMapa() {
        String token = prijava("ana", "Ana12345!");

        Odgovor odgovorTekst = posalji("POST", "/api/postavke", token, "application/yaml", "samo tekstualna linija");
        assertThat(odgovorTekst.status()).isEqualTo(400);
        assertThat(odgovorTekst.json().get("greska").asText()).isEqualTo("Neispravne postavke.");

        Odgovor odgovorLista = posalji("POST", "/api/postavke", token, "application/yaml", "- element1\n- element2");
        assertThat(odgovorLista.status()).isEqualTo(400);
    }

    @Test
    @DisplayName("Neispravan broj satiPrije vraća 400")
    void neispravanBrojSatiPrije() {
        String token = prijava("ana", "Ana12345!");

        Odgovor manji = posalji("POST", "/api/postavke", token, "application/yaml", "satiPrije: 0\n");
        assertThat(manji.status()).isEqualTo(400);

        Odgovor veci = posalji("POST", "/api/postavke", token, "application/yaml", "satiPrije: 200\n");
        assertThat(veci.status()).isEqualTo(400);
    }

    @Test
    @DisplayName("Neispravan tip podatka u YAML konfiguraciji vraća 400")
    void neispravanTipPodatka() {
        String token = prijava("ana", "Ana12345!");

        Odgovor pogresanBool = posalji("POST", "/api/postavke", token, "application/yaml", "podsjetnik: [neki, niz]\n");
        assertThat(pogresanBool.status()).isEqualTo(400);

        Odgovor pogresanBroj = posalji("POST", "/api/postavke", token, "application/yaml", "satiPrije: 'tekst'\n");
        assertThat(pogresanBroj.status()).isEqualTo(400);
    }
}

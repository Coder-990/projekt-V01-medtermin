package hr.span.akademija.medtermin.funkcionalno;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.JsonNode;
import hr.span.akademija.medtermin.ApiTestBaza;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

@Tag("funkcionalno")
class UvozTerminaFunkcionalniTest extends ApiTestBaza {

    private static final String XML = """
            <?xml version="1.0" encoding="UTF-8"?>
            <termini>
              <termin>
                <lijecnikId>1</lijecnikId>
                <pocetak>2026-12-01T09:00</pocetak>
                <kapacitet>1</kapacitet>
                <napomena>Jutarnji termin</napomena>
              </termin>
            </termini>
            """;

    @Test
    @DisplayName("Liječnik uvozi termine iz XML-a")
    void lijecnikUvoziTermine() {
        String token = prijava("dr.horvat", "Ovlasteni12345!");

        Odgovor odgovor = posalji("POST", "/api/termini/uvoz", token, "application/xml", XML);

        assertThat(odgovor.status()).isEqualTo(200);
        JsonNode tijelo = odgovor.json();
        assertThat(tijelo.get("uvezeno").asInt()).isEqualTo(1);
        JsonNode termin = tijelo.get("termini").get(0);
        assertThat(termin.get("napomena").asText()).isEqualTo("Jutarnji termin");
        long id = termin.get("id").asLong();
        boolean vidljiv = false;
        for (JsonNode t : get("/api/termini?lijecnikId=1", token).json()) {
            vidljiv |= t.get("id").asLong() == id;
        }
        assertThat(vidljiv).isTrue();
    }

    @Test
    @DisplayName("Administrator može uvoziti termine")
    void administratorUvozi() {
        String token = prijava("admin", "Admin12345!");

        Odgovor odgovor = posalji("POST", "/api/termini/uvoz", token, "application/xml", XML);

        assertThat(odgovor.status()).isEqualTo(200);
    }

    @Test
    @DisplayName("Korisnik s ulogom KORISNIK ne može uvoziti termine")
    void korisnikNeMozeUvoziti() {
        String token = prijava("ana", "Ana12345!");

        Odgovor odgovor = posalji("POST", "/api/termini/uvoz", token, "application/xml", XML);

        assertThat(odgovor.status()).isEqualTo(403);
    }

    @Test
    @DisplayName("Neispravan XML vraća 400")
    void neispravanXml() {
        String token = prijava("dr.horvat", "Ovlasteni12345!");

        Odgovor odgovor = posalji("POST", "/api/termini/uvoz", token, "application/xml", "<termini><termin>");

        assertThat(odgovor.status()).isEqualTo(400);
    }
}

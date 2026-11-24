package hr.span.akademija.medtermin.sigurnost.dodatno;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.JsonNode;
import hr.span.akademija.medtermin.ApiTestBaza;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

@Tag("sigurnost")
class UvozTerminaFunkcionalniTestDodatniTest extends ApiTestBaza {

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
    @DisplayName("XML s neispravnim podacima (npr. ne-numerički ID liječnika) vraća 400")
    void xmlNeispravniPodaci() {
        String token = prijava("dr.horvat", "Ovlasteni12345!");
        String xml = """
                <termini>
                  <termin>
                    <lijecnikId>nije_broj</lijecnikId>
                    <pocetak>2026-12-01T09:00</pocetak>
                    <kapacitet>5</kapacitet>
                  </termin>
                </termini>
                """;

        Odgovor odgovor = posalji("POST", "/api/termini/uvoz", token, "application/xml", xml);

        assertThat(odgovor.status()).isEqualTo(400);
        assertThat(odgovor.json().get("greska").asText()).isEqualTo("Neispravni podaci u terminu 1.");
    }

    @Test
    @DisplayName("XML s nedostajućom vrijednosti obaveznog polja vraća 400")
    void xmlNedostajePolje() {
        String token = prijava("dr.horvat", "Ovlasteni12345!");
        String xml = """
                <termini>
                  <termin>
                    <lijecnikId>1</lijecnikId>
                    <pocetak></pocetak>
                    <kapacitet>5</kapacitet>
                  </termin>
                </termini>
                """;

        Odgovor odgovor = posalji("POST", "/api/termini/uvoz", token, "application/xml", xml);

        assertThat(odgovor.status()).isEqualTo(400);
        assertThat(odgovor.json().get("greska").asText()).isEqualTo("Neispravni podaci u terminu 1.");
    }

    @Test
    @DisplayName("XML u kojem je obavezni element potpuno izostavljen vraća 400")
    void xmlPotpunoIzostavljenElement() {
        String token = prijava("dr.horvat", "Ovlasteni12345!");
        String xml = """
                <termini>
                  <termin>
                    <lijecnikId>1</lijecnikId>
                    <kapacitet>5</kapacitet>
                  </termin>
                </termini>
                """;

        Odgovor odgovor = posalji("POST", "/api/termini/uvoz", token, "application/xml", xml);

        assertThat(odgovor.status()).isEqualTo(400);
        assertThat(odgovor.json().get("greska").asText()).isEqualTo("Neispravni podaci u terminu 1.");
    }

    @Test
    @DisplayName("Uvoz termina s nepoznatim liječnikom vraća 400")
    void nepoznatLijecnik() {
        String token = prijava("dr.horvat", "Ovlasteni12345!");
        String xml = """
                <termini>
                  <termin>
                    <lijecnikId>99999</lijecnikId>
                    <pocetak>2026-12-01T09:00</pocetak>
                    <kapacitet>5</kapacitet>
                  </termin>
                </termini>
                """;

        Odgovor odgovor = posalji("POST", "/api/termini/uvoz", token, "application/xml", xml);

        assertThat(odgovor.status()).isEqualTo(400);
        assertThat(odgovor.json().get("greska").asText()).isEqualTo("Nepoznat liječnik: 99999");
    }

    @Test
    @DisplayName("Uvoz termina s neispravnim kapacitetom (<1 ili >50) vraća 400")
    void neispravanKapacitet() {
        String token = prijava("dr.horvat", "Ovlasteni12345!");
        String xmlPremalo = """
                <termini>
                  <termin>
                    <lijecnikId>1</lijecnikId>
                    <pocetak>2026-12-01T09:00</pocetak>
                    <kapacitet>0</kapacitet>
                  </termin>
                </termini>
                """;
        Odgovor premalo = posalji("POST", "/api/termini/uvoz", token, "application/xml", xmlPremalo);
        assertThat(premalo.status()).isEqualTo(400);
        assertThat(premalo.json().get("greska").asText()).isEqualTo("Kapacitet termina mora biti između 1 i 50.");

        String xmlPrevise = """
                <termini>
                  <termin>
                    <lijecnikId>1</lijecnikId>
                    <pocetak>2026-12-01T09:00</pocetak>
                    <kapacitet>51</kapacitet>
                  </termin>
                </termini>
                """;
        Odgovor previse = posalji("POST", "/api/termini/uvoz", token, "application/xml", xmlPrevise);
        assertThat(previse.status()).isEqualTo(400);
    }

    @Test
    @DisplayName("Uvoz termina bez napomene prolazi i postavlja null napomenu")
    void uvozBezNapomene() {
        String token = prijava("dr.horvat", "Ovlasteni12345!");
        String xml = """
                <termini>
                  <termin>
                    <lijecnikId>1</lijecnikId>
                    <pocetak>2027-03-01T10:00</pocetak>
                    <kapacitet>5</kapacitet>
                  </termin>
                </termini>
                """;

        Odgovor odgovor = posalji("POST", "/api/termini/uvoz", token, "application/xml", xml);

        assertThat(odgovor.status()).isEqualTo(200);
        assertThat(odgovor.json().get("termini").get(0).get("napomena").isNull()).isTrue();
    }
}

package hr.span.akademija.medtermin.funkcionalno;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.JsonNode;
import hr.span.akademija.medtermin.ApiTestBaza;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

@Tag("funkcionalno")
class NalaziFunkcionalniTest extends ApiTestBaza {

    @Test
    @DisplayName("Moji nalazi sadrže samo nalaze prijavljenog korisnika")
    void mojiNalazi() {
        String token = prijava("ana", "Ana12345!");

        Odgovor odgovor = get("/api/nalazi/moji", token);

        assertThat(odgovor.status()).isEqualTo(200);
        assertThat(odgovor.json().size()).isEqualTo(1);
        JsonNode nalaz = odgovor.json().get(0);
        assertThat(nalaz.get("id").asLong()).isEqualTo(1);
        assertThat(nalaz.get("naziv").asText()).isEqualTo("Krvna slika");
        assertThat(nalaz.get("datoteka").asText()).isEqualTo("nalaz-ana-krvna-slika.txt");
    }

    @Test
    @DisplayName("Preuzimanje vlastite datoteke nalaza")
    void preuzimanjeVlastiteDatoteke() {
        String token = prijava("ana", "Ana12345!");

        Odgovor odgovor = get("/api/nalazi/preuzmi?datoteka=nalaz-ana-krvna-slika.txt", token);

        assertThat(odgovor.status()).isEqualTo(200);
        assertThat(odgovor.tijelo()).contains("Pacijent: Ana Anić").contains("Hemoglobin");
    }

    @Test
    @DisplayName("Nalaz u JSON formatu sadrži metapodatke i sadržaj")
    void nalazJson() {
        String token = prijava("marko", "Marko12345!");

        Odgovor odgovor = get("/api/nalazi/2?format=json", token);

        assertThat(odgovor.status()).isEqualTo(200);
        assertThat(odgovor.json().get("naziv").asText()).isEqualTo("RTG kralježnice");
        assertThat(odgovor.json().get("sadrzaj").asText()).contains("Pacijent: Marko Marić");
    }

    @Test
    @DisplayName("Nalaz u PDF formatu")
    void nalazPdf() {
        String token = prijava("ana", "Ana12345!");

        Odgovor odgovor = get("/api/nalazi/1?format=pdf", token);

        assertThat(odgovor.status()).isEqualTo(200);
        assertThat(odgovor.zaglavlje("Content-Type")).startsWith("application/pdf");
        assertThat(odgovor.tijelo()).startsWith("%PDF");
    }

    @Test
    @DisplayName("Bez parametra format nalaz se vraća kao JSON")
    void zadaniFormat() {
        String token = prijava("ana", "Ana12345!");

        Odgovor odgovor = get("/api/nalazi/1", token);

        assertThat(odgovor.status()).isEqualTo(200);
        assertThat(odgovor.json().get("sadrzaj").asText()).contains("Hemoglobin");
    }
}

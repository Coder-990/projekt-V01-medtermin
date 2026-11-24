package hr.span.akademija.medtermin.sigurnost.dodatno;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.JsonNode;
import hr.span.akademija.medtermin.ApiTestBaza;
import hr.span.akademija.medtermin.nalazi.NalazController;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

@Tag("sigurnost")
class NalaziFunkcionalniTestDodatniTest extends ApiTestBaza {

    @Autowired
    private JdbcTemplate jdbc;

    @Test
    @DisplayName("Format procitaj ispravno parsira formate i prazne vrijednosti")
    void formatProcitajMetoda() throws Exception {
        java.lang.reflect.Method m = NalazController.Format.class.getDeclaredMethod("procitaj", String.class);
        m.setAccessible(true);
        assertThat(m.invoke(null, "json")).isEqualTo(Optional.of(NalazController.Format.JSON));
        assertThat(m.invoke(null, "PDF")).isEqualTo(Optional.of(NalazController.Format.PDF));
        assertThat(m.invoke(null, "nepoznato")).isEqualTo(Optional.empty());
        org.assertj.core.api.Assertions.assertThatThrownBy(() -> {
            try {
                m.invoke(null, (Object) null);
            } catch (java.lang.reflect.InvocationTargetException e) {
                throw e.getCause();
            }
        }).isInstanceOf(NullPointerException.class);
    }

    @Test
    @DisplayName("Dohvat nepostojećeg nalaza vraća 404")
    void nepostojeciNalaz() {
        String token = prijava("ana", "Ana12345!");
        Odgovor odgovor = get("/api/nalazi/99999", token);

        assertThat(odgovor.status()).isEqualTo(404);
        assertThat(odgovor.json().get("greska").asText()).isEqualTo("Nalaz nije pronađen.");
    }

    @Test
    @DisplayName("Korisnik ne može dohvatiti tuđi nalaz sa specifičnim formatom (404)")
    void tudiNalazSFormatomVraca404() {
        String tokenAna = prijava("ana", "Ana12345!");
        Odgovor odgovor = get("/api/nalazi/2?format=json", tokenAna);

        assertThat(odgovor.status()).isEqualTo(404);
        assertThat(odgovor.json().get("greska").asText()).isEqualTo("Nalaz nije pronađen.");
    }

    @Test
    @DisplayName("Administrator može vidjeti bilo čiji nalaz")
    void adminMozeVidjetiBiloCijiNalaz() {
        String adminToken = prijava("admin", "Admin12345!");

        Odgovor nalaz1 = get("/api/nalazi/1?format=json", adminToken);
        assertThat(nalaz1.status()).isEqualTo(200);

        Odgovor nalaz2 = get("/api/nalazi/2?format=pdf", adminToken);
        assertThat(nalaz2.status()).isEqualTo(200);
    }

    @Test
    @DisplayName("Preuzimanje tuđe datoteke vraća 404")
    void preuzimanjeTudeDatotekeVraca404() {
        String tokenAna = prijava("ana", "Ana12345!");
        Odgovor odgovor = get("/api/nalazi/preuzmi?datoteka=nalaz-marko-rtg.txt", tokenAna);

        assertThat(odgovor.status()).isEqualTo(404);
        assertThat(odgovor.json().get("greska").asText()).isEqualTo("Datoteka nije pronađena.");
    }

    @Test
    @DisplayName("Preuzimanje nepostojeće datoteke s vlastitim imenom vraća 404")
    void preuzimanjeNepostojeceDatotekeVraca404() {
        String tokenAna = prijava("ana", "Ana12345!");
        Odgovor odgovor = get("/api/nalazi/preuzmi?datoteka=nalaz-ana-nepostojeca.txt", tokenAna);

        assertThat(odgovor.status()).isEqualTo(404);
        assertThat(odgovor.json().get("greska").asText()).isEqualTo("Datoteka nije pronađena.");
    }

    @Test
    @DisplayName("Administrator može preuzeti bilo čiju datoteku")
    void adminMozePreuzetiDatoteku() {
        String adminToken = prijava("admin", "Admin12345!");
        Odgovor odgovor = get("/api/nalazi/preuzmi?datoteka=nalaz-marko-rtg.txt", adminToken);

        assertThat(odgovor.status()).isEqualTo(200);
    }

    @Test
    @DisplayName("Nalaz čija datoteka ne postoji na disku vraća 404")
    void datotekaNalazaNePostojiNaDisku() {
        jdbc.update("INSERT INTO nalazi (pacijent, naziv, datoteka) VALUES ('ana', 'Izgubljeni nalaz', 'nepostojeca-datoteka-xyz.txt')");
        Long id = jdbc.queryForObject("SELECT id FROM nalazi WHERE datoteka = 'nepostojeca-datoteka-xyz.txt'", Long.class);

        String tokenAna = prijava("ana", "Ana12345!");
        Odgovor odgovor = get("/api/nalazi/" + id, tokenAna);

        assertThat(odgovor.status()).isEqualTo(404);
        assertThat(odgovor.json().get("greska").asText()).isEqualTo("Datoteka nalaza nije pronađena.");
    }

}

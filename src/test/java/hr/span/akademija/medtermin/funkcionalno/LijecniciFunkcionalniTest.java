package hr.span.akademija.medtermin.funkcionalno;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.JsonNode;
import hr.span.akademija.medtermin.ApiTestBaza;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

@Tag("funkcionalno")
class LijecniciFunkcionalniTest extends ApiTestBaza {

    private List<String> polje(Odgovor odgovor, String naziv) {
        List<String> vrijednosti = new ArrayList<>();
        for (JsonNode lijecnik : odgovor.json()) {
            vrijednosti.add(lijecnik.get(naziv).asText());
        }
        return vrijednosti;
    }

    @Test
    @DisplayName("Popis liječnika sortiran po prezimenu uzlazno")
    void poPrezimenuUzlazno() {
        String token = prijava("ana", "Ana12345!");

        Odgovor odgovor = get("/api/lijecnici?sort=prezime&smjer=asc", token);

        assertThat(odgovor.status()).isEqualTo(200);
        assertThat(polje(odgovor, "prezime"))
                .containsExactly("Babić", "Horvat", "Jurić", "Kovačević", "Novak", "Vuković");
    }

    @Test
    @DisplayName("Popis liječnika sortiran po prezimenu silazno")
    void poPrezimenuSilazno() {
        String token = prijava("ana", "Ana12345!");

        Odgovor odgovor = get("/api/lijecnici?sort=prezime&smjer=desc", token);

        assertThat(odgovor.status()).isEqualTo(200);
        assertThat(polje(odgovor, "prezime"))
                .containsExactly("Vuković", "Novak", "Kovačević", "Jurić", "Horvat", "Babić");
    }

    @Test
    @DisplayName("Popis liječnika sortiran po imenu i specijalizaciji")
    void poImenuISpecijalizaciji() {
        String token = prijava("ana", "Ana12345!");

        Odgovor poImenu = get("/api/lijecnici?sort=ime&smjer=asc", token);
        Odgovor poSpecijalizaciji = get("/api/lijecnici?sort=specijalizacija&smjer=asc", token);

        assertThat(polje(poImenu, "ime")).containsExactly("Ema", "Ivan", "Luka", "Maja", "Petra", "Tomislav");
        assertThat(polje(poSpecijalizaciji, "specijalizacija").get(0)).isEqualTo("Dermatologija");
    }

    @Test
    @DisplayName("Zadano sortiranje je po prezimenu uzlazno")
    void zadanoSortiranje() {
        String token = prijava("ana", "Ana12345!");

        Odgovor odgovor = get("/api/lijecnici", token);

        assertThat(odgovor.status()).isEqualTo(200);
        assertThat(polje(odgovor, "prezime")).hasSize(6).first().isEqualTo("Babić");
    }

    @Test
    @DisplayName("Popis liječnika traži prijavu")
    void trazPrijavu() {
        assertThat(get("/api/lijecnici", null).status()).isEqualTo(401);
    }
}

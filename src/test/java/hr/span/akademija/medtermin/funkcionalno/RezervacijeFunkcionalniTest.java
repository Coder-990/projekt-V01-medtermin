package hr.span.akademija.medtermin.funkcionalno;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.JsonNode;
import hr.span.akademija.medtermin.ApiTestBaza;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

@Tag("funkcionalno")
class RezervacijeFunkcionalniTest extends ApiTestBaza {

    /** Termin 7 je rezerviran za posebne provjere i ne koristi se u ovim testovima. */
    private static final long IZUZETI_TERMIN = 7;

    private long slobodanTermin(String token) {
        for (JsonNode termin : get("/api/termini", token).json()) {
            if (termin.get("id").asLong() != IZUZETI_TERMIN && termin.get("slobodno").asInt() > 0) {
                return termin.get("id").asLong();
            }
        }
        return noviTermin(5);
    }

    private long noviTermin(int kapacitet) {
        String lijecnik = prijava("dr.horvat", "Ovlasteni12345!");
        String xml = "<termini><termin><lijecnikId>1</lijecnikId><pocetak>2027-02-01T08:00</pocetak>"
                + "<kapacitet>" + kapacitet + "</kapacitet><napomena>Dodatni termin</napomena></termin></termini>";
        Odgovor odgovor = posalji("POST", "/api/termini/uvoz", lijecnik, "application/xml", xml);
        assertThat(odgovor.status()).isEqualTo(200);
        return odgovor.json().get("termini").get(0).get("id").asLong();
    }

    private JsonNode termin(String token, long id) {
        for (JsonNode termin : get("/api/termini", token).json()) {
            if (termin.get("id").asLong() == id) {
                return termin;
            }
        }
        throw new AssertionError("Termin " + id + " nije pronađen");
    }

    private List<Long> mojeRezervacije(String token) {
        List<Long> id = new ArrayList<>();
        for (JsonNode rezervacija : get("/api/rezervacije/moje", token).json()) {
            id.add(rezervacija.get("id").asLong());
        }
        return id;
    }

    private Odgovor rezerviraj(String token, long terminId, String napomena) {
        return postJson("/api/rezervacije", token,
                Map.of("terminId", terminId, "napomena", napomena, "oib", "48291736504"));
    }

    @Test
    @DisplayName("Termini liječnika prikazuju kapacitet i slobodna mjesta")
    void terminiLijecnika() {
        String token = prijava("ana", "Ana12345!");

        Odgovor odgovor = get("/api/termini?lijecnikId=1", token);

        assertThat(odgovor.status()).isEqualTo(200);
        assertThat(odgovor.json().size()).isGreaterThanOrEqualTo(3);
        for (JsonNode termin : odgovor.json()) {
            assertThat(termin.get("lijecnikId").asLong()).isEqualTo(1);
            assertThat(termin.get("slobodno").asInt())
                    .isEqualTo(termin.get("kapacitet").asInt() - termin.get("zauzeto").asInt());
        }
    }

    @Test
    @DisplayName("Nova rezervacija vraća 201 i vidljiva je u mojim rezervacijama")
    void novaRezervacija() {
        String token = prijava("ana", "Ana12345!");
        long terminId = slobodanTermin(token);

        Odgovor odgovor = rezerviraj(token, terminId, "Prvi pregled");

        assertThat(odgovor.status()).isEqualTo(201);
        long id = odgovor.json().get("id").asLong();
        assertThat(odgovor.json().get("napomena").asText()).isEqualTo("Prvi pregled");
        assertThat(mojeRezervacije(token)).contains(id);
    }

    @Test
    @DisplayName("Rezervacija popunjenog termina vraća 409")
    void popunjenTermin() {
        long terminId = noviTermin(1);
        String ana = prijava("ana", "Ana12345!");
        String marko = prijava("marko", "Marko12345!");
        assertThat(rezerviraj(ana, terminId, "Kontrola").status()).isEqualTo(201);

        Odgovor odgovor = rezerviraj(marko, terminId, "Kontrola");

        assertThat(odgovor.status()).isEqualTo(409);
        assertThat(odgovor.json().get("greska").asText()).isEqualTo("Nema slobodnih mjesta.");
    }

    @Test
    @DisplayName("Neispravan OIB vraća 400")
    void neispravanOib() {
        String token = prijava("ana", "Ana12345!");

        Odgovor odgovor = postJson("/api/rezervacije", token,
                Map.of("terminId", slobodanTermin(token), "napomena", "Kontrola", "oib", "123"));

        assertThat(odgovor.status()).isEqualTo(400);
    }

    @Test
    @DisplayName("Vlasnik može otkazati vlastitu rezervaciju")
    void otkazivanjeVlastiteRezervacije() {
        String token = prijava("ana", "Ana12345!");
        long terminId = slobodanTermin(token);
        long id = rezerviraj(token, terminId, "Otkazat ću").json().get("id").asLong();
        int zauzetoPrije = termin(token, terminId).get("zauzeto").asInt();

        Odgovor odgovor = delete("/api/rezervacije/" + id, token);

        assertThat(odgovor.status()).isEqualTo(204);
        assertThat(mojeRezervacije(token)).doesNotContain(id);
        assertThat(termin(token, terminId).get("zauzeto").asInt()).isEqualTo(zauzetoPrije - 1);
    }

    @Test
    @DisplayName("Potvrda za ispis sadrži podatke rezervacije")
    void ispisVlastiteRezervacije() {
        String token = prijava("ana", "Ana12345!");
        long id = rezerviraj(token, slobodanTermin(token), "Molim jutarnji termin").json().get("id").asLong();

        Odgovor odgovor = get("/ispis/rezervacije/" + id, token);

        assertThat(odgovor.status()).isEqualTo(200);
        assertThat(odgovor.zaglavlje("Content-Type")).startsWith("text/html");
        assertThat(odgovor.tijelo()).contains("Potvrda rezervacije").contains("Molim jutarnji termin");
    }

    @Test
    @DisplayName("Rezervacija bez termina vraća 400")
    void rezervacijaBezTermina() {
        String token = prijava("ana", "Ana12345!");
        java.util.Map<String, Object> tijelo = new java.util.HashMap<>();
        tijelo.put("terminId", null);
        tijelo.put("oib", "48291736504");
        tijelo.put("napomena", "Test");

        Odgovor odgovor = postJson("/api/rezervacije", token, tijelo);

        assertThat(odgovor.status()).isEqualTo(400);
        assertThat(odgovor.json().get("greska").asText()).isEqualTo("Termin je obavezan.");
    }

    @Test
    @DisplayName("Rezervacija bez OIB-a vraća 400")
    void rezervacijaBezOiba() {
        String token = prijava("ana", "Ana12345!");
        java.util.Map<String, Object> tijelo = new java.util.HashMap<>();
        tijelo.put("terminId", slobodanTermin(token));
        tijelo.put("oib", null);
        tijelo.put("napomena", "Test");

        Odgovor odgovor = postJson("/api/rezervacije", token, tijelo);

        assertThat(odgovor.status()).isEqualTo(400);
        assertThat(odgovor.json().get("greska").asText()).isEqualTo("OIB mora imati 11 znamenki.");
    }

    @Test
    @DisplayName("Predugačka napomena vraća 400")
    void predugaNapomena() {
        String token = prijava("ana", "Ana12345!");
        String dugaNapomena = "a".repeat(1001);

        Odgovor odgovor = postJson("/api/rezervacije", token,
                Map.of("terminId", slobodanTermin(token), "napomena", dugaNapomena, "oib", "48291736504"));

        assertThat(odgovor.status()).isEqualTo(400);
        assertThat(odgovor.json().get("greska").asText()).isEqualTo("Napomena smije imati najviše 1000 znakova.");
    }

    @Test
    @DisplayName("Rezervacija nepostojećeg termina vraća 404")
    void rezervacijaNepostojecegTermina() {
        String token = prijava("ana", "Ana12345!");

        Odgovor odgovor = postJson("/api/rezervacije", token,
                Map.of("terminId", 99999L, "napomena", "Kontrola", "oib", "48291736504"));

        assertThat(odgovor.status()).isEqualTo(404);
        assertThat(odgovor.json().get("greska").asText()).isEqualTo("Termin nije pronađen.");
    }

    @Test
    @DisplayName("Otkazivanje nepostojeće rezervacije vraća 404")
    void otkazivanjeNepostojeceRezervacije() {
        String token = prijava("ana", "Ana12345!");

        Odgovor odgovor = delete("/api/rezervacije/99999", token);

        assertThat(odgovor.status()).isEqualTo(404);
        assertThat(odgovor.json().get("greska").asText()).isEqualTo("Rezervacija nije pronađena.");
    }

    @Test
    @DisplayName("Korisnik ne može otkazati tuđu rezervaciju (404)")
    void otkazivanjeTudeRezervacije() {
        String ana = prijava("ana", "Ana12345!");
        String marko = prijava("marko", "Marko12345!");
        long id = rezerviraj(ana, slobodanTermin(ana), "Ana termin").json().get("id").asLong();

        Odgovor odgovor = delete("/api/rezervacije/" + id, marko);

        assertThat(odgovor.status()).isEqualTo(404);
        assertThat(odgovor.json().get("greska").asText()).isEqualTo("Rezervacija nije pronađena.");
    }

    @Test
    @DisplayName("Administrator može otkazati tuđu rezervaciju")
    void adminOtkazujeTuduRezervaciju() {
        String ana = prijava("ana", "Ana12345!");
        String admin = prijava("admin", "Admin12345!");
        long id = rezerviraj(ana, slobodanTermin(ana), "Za otkaz od admina").json().get("id").asLong();

        Odgovor odgovor = delete("/api/rezervacije/" + id, admin);

        assertThat(odgovor.status()).isEqualTo(204);
    }

    @Test
    @DisplayName("Ispis nepostojeće rezervacije vraća 404")
    void ispisNepostojeceRezervacije() {
        String token = prijava("ana", "Ana12345!");

        Odgovor odgovor = get("/ispis/rezervacije/99999", token);

        assertThat(odgovor.status()).isEqualTo(404);
        assertThat(odgovor.json().get("greska").asText()).isEqualTo("Rezervacija nije pronađena.");
    }

    @Test
    @DisplayName("Korisnik ne može ispisati tuđu rezervaciju (404)")
    void ispisTudeRezervacije() {
        String ana = prijava("ana", "Ana12345!");
        String marko = prijava("marko", "Marko12345!");
        long id = rezerviraj(ana, slobodanTermin(ana), "Anin pregled").json().get("id").asLong();

        Odgovor odgovor = get("/ispis/rezervacije/" + id, marko);

        assertThat(odgovor.status()).isEqualTo(404);
        assertThat(odgovor.json().get("greska").asText()).isEqualTo("Rezervacija nije pronađena.");
    }

    @Test
    @DisplayName("Administrator može ispisati tuđu rezervaciju")
    void adminMozeIspisatiTuduRezervaciju() {
        String ana = prijava("ana", "Ana12345!");
        String admin = prijava("admin", "Admin12345!");
        long id = rezerviraj(ana, slobodanTermin(ana), "Za admin ispis").json().get("id").asLong();

        Odgovor odgovor = get("/ispis/rezervacije/" + id, admin);

        assertThat(odgovor.status()).isEqualTo(200);
        assertThat(odgovor.tijelo()).contains("Za admin ispis");
    }

    @Test
    @DisplayName("Ispis rezervacije s null napomenom ispravno generira HTML")
    void ispisRezervacijeSNullNapomenom() {
        String token = prijava("ana", "Ana12345!");
        java.util.Map<String, Object> tijelo = new java.util.HashMap<>();
        tijelo.put("terminId", slobodanTermin(token));
        tijelo.put("oib", "48291736504");
        tijelo.put("napomena", null);

        Odgovor kreiranje = postJson("/api/rezervacije", token, tijelo);
        long id = kreiranje.json().get("id").asLong();

        Odgovor odgovor = get("/ispis/rezervacije/" + id, token);
        assertThat(odgovor.status()).isEqualTo(200);
    }
}

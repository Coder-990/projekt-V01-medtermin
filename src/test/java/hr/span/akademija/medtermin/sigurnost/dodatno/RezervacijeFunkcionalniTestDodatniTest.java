package hr.span.akademija.medtermin.sigurnost.dodatno;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.JsonNode;
import hr.span.akademija.medtermin.ApiTestBaza;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

@Tag("sigurnost")
class RezervacijeFunkcionalniTestDodatniTest extends ApiTestBaza {

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

package hr.span.akademija.medtermin.sigurnost.dodatno;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.JsonNode;
import hr.span.akademija.medtermin.ApiTestBaza;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

@Tag("sigurnost")
class AutentikacijaFunkcionalniTestDodatniTest extends ApiTestBaza {

    @Test
    @DisplayName("Registracija s neispravnim korisničkim imenom vraća 400")
    void registracijaNeispravnoIme() {
        Odgovor preKratko = postJson("/api/auth/register", null, Map.of("username", "ab", "password", "Valid12345!",
                "email", "test@primjer.hr", "ime", "Test", "prezime", "Test"));
        assertThat(preKratko.status()).isEqualTo(400);
        assertThat(preKratko.json().get("greska").asText()).contains("Korisničko ime mora imati");

        Odgovor nedozvoljeniZnakovi = postJson("/api/auth/register", null, Map.of("username", "user#123",
                "password", "Valid12345!", "email", "test@primjer.hr", "ime", "Test", "prezime", "Test"));
        assertThat(nedozvoljeniZnakovi.status()).isEqualTo(400);
    }

    @Test
    @DisplayName("Registracija s neispravnom adresom e-pošte vraća 400")
    void registracijaNeispravanEmail() {
        Odgovor odgovor = postJson("/api/auth/register", null, Map.of("username", "novikorisnik99",
                "password", "Valid12345!", "email", "nema-at-domenu", "ime", "Test", "prezime", "Test"));

        assertThat(odgovor.status()).isEqualTo(400);
        assertThat(odgovor.json().get("greska").asText()).isEqualTo("Neispravna adresa e-pošte.");
    }

    @Test
    @DisplayName("Dohvat /api/ja bez prijave ili s praznim zaglavljima vraća 401")
    void neautoriziraniDohvatJa() {
        Odgovor bezPrijave = get("/api/ja", null);
        assertThat(bezPrijave.status()).isEqualTo(401);

        Odgovor prazanBearer = posalji("GET", "/api/ja", null, null, null, "Authorization", "Bearer ");
        assertThat(prazanBearer.status()).isEqualTo(401);

        Odgovor basicAuth = posalji("GET", "/api/ja", null, null, null, "Authorization", "Basic dXNlcjpwYXNz");
        assertThat(basicAuth.status()).isEqualTo(401);

        Odgovor nevazanKolacic = posalji("GET", "/api/ja", null, null, null, "Cookie", "NEBITAN=12345; DRUGI=abc");
        assertThat(nevazanKolacic.status()).isEqualTo(401);

        Odgovor prazanSessionKolacic = posalji("GET", "/api/ja", null, null, null, "Cookie", "SESSION=   ");
        assertThat(prazanSessionKolacic.status()).isEqualTo(401);
    }

    @Test
    @DisplayName("Prijava praznim podacima vraća 401")
    void prijavaPraznimPodacima() {
        java.util.Map<String, Object> tijelo = new java.util.HashMap<>();
        tijelo.put("username", null);
        tijelo.put("password", null);

        Odgovor odgovor = postJson("/api/auth/login", null, tijelo);
        assertThat(odgovor.status()).isEqualTo(401);
    }
}

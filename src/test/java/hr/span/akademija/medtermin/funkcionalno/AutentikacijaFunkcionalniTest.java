package hr.span.akademija.medtermin.funkcionalno;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.JsonNode;
import hr.span.akademija.medtermin.ApiTestBaza;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

@Tag("funkcionalno")
class AutentikacijaFunkcionalniTest extends ApiTestBaza {

    @Test
    @DisplayName("GET /api/health radi bez prijave")
    void healthBezPrijave() {
        Odgovor odgovor = get("/api/health", null);

        assertThat(odgovor.status()).isEqualTo(200);
        assertThat(odgovor.json().get("status").asText()).isEqualTo("UP");
    }

    @Test
    @DisplayName("Prijava ispravnim podacima vraća token i ulogu")
    void prijavaIspravnimPodacima() {
        Odgovor odgovor = postJson("/api/auth/login", null, Map.of("username", "ana", "password", "Ana12345!"));

        assertThat(odgovor.status()).isEqualTo(200);
        JsonNode tijelo = odgovor.json();
        assertThat(tijelo.get("token").asText()).isNotBlank();
        assertThat(tijelo.get("username").asText()).isEqualTo("ana");
        assertThat(tijelo.get("uloga").asText()).isEqualTo("KORISNIK");
    }

    @Test
    @DisplayName("Prijava pogrešnom lozinkom vraća 401 i poruku")
    void prijavaPogresnomLozinkom() {
        Odgovor odgovor = postJson("/api/auth/login", null, Map.of("username", "ana", "password", "kriva-lozinka"));

        assertThat(odgovor.status()).isEqualTo(401);
        assertThat(odgovor.json().get("greska").asText()).isEqualTo("Neispravno korisničko ime ili lozinka.");
    }

    @Test
    @DisplayName("GET /api/ja vraća podatke prijavljenog korisnika")
    void podaciPrijavljenogKorisnika() {
        String token = prijava("ana", "Ana12345!");

        Odgovor odgovor = get("/api/ja", token);

        assertThat(odgovor.status()).isEqualTo(200);
        JsonNode ja = odgovor.json();
        assertThat(ja.get("username").asText()).isEqualTo("ana");
        assertThat(ja.get("ime").asText()).isEqualTo("Ana");
        assertThat(ja.get("prezime").asText()).isEqualTo("Anić");
        assertThat(ja.get("email").asText()).isEqualTo("ana@primjer.hr");
        assertThat(ja.get("uloga").asText()).isEqualTo("KORISNIK");
    }

    @Test
    @DisplayName("Privilegirani korisnik dr.horvat ima ulogu LIJECNIK")
    void lijecnikImaUlogu() {
        Odgovor odgovor = postJson("/api/auth/login", null,
                Map.of("username", "dr.horvat", "password", "Ovlasteni12345!"));

        assertThat(odgovor.status()).isEqualTo(200);
        assertThat(odgovor.json().get("uloga").asText()).isEqualTo("LIJECNIK");
    }

    @Test
    @DisplayName("Registracija novog korisnika i prijava")
    void registracijaIPrijava() {
        String username = noviKorisnik("pacijent", "Pacijent12345!");

        String token = prijava(username, "Pacijent12345!");
        Odgovor ja = get("/api/ja", token);

        assertThat(ja.status()).isEqualTo(200);
        assertThat(ja.json().get("username").asText()).isEqualTo(username);
        assertThat(ja.json().get("uloga").asText()).isEqualTo("KORISNIK");
    }

    @Test
    @DisplayName("Registracija zauzetog korisničkog imena vraća 409")
    void registracijaZauzetogImena() {
        Odgovor odgovor = postJson("/api/auth/register", null, Map.of("username", "ana", "password", "Nesto12345!",
                "email", "druga.ana@primjer.hr", "ime", "Ana", "prezime", "Druga"));

        assertThat(odgovor.status()).isEqualTo(409);
    }

    @Test
    @DisplayName("Registracija s prekratkom lozinkom vraća 400")
    void registracijaKratkaLozinka() {
        Odgovor odgovor = postJson("/api/auth/register", null, Map.of("username", "kratka1", "password", "abc",
                "email", "kratka@primjer.hr", "ime", "Kratka", "prezime", "Lozinka"));

        assertThat(odgovor.status()).isEqualTo(400);
    }

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

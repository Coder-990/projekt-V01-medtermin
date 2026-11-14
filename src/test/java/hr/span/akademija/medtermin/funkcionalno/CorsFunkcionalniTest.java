package hr.span.akademija.medtermin.funkcionalno;

import static org.assertj.core.api.Assertions.assertThat;

import hr.span.akademija.medtermin.ApiTestBaza;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

@Tag("funkcionalno")
class CorsFunkcionalniTest extends ApiTestBaza {

    private static final String FRONTEND = "http://localhost:5173";

    @Test
    @DisplayName("Frontend s http://localhost:5173 dobiva CORS zaglavlja")
    void dopusteniOrigin() {
        Odgovor odgovor = posalji("GET", "/api/health", null, null, null, "Origin", FRONTEND);

        assertThat(odgovor.status()).isEqualTo(200);
        assertThat(odgovor.zaglavlje("Access-Control-Allow-Origin")).isEqualTo(FRONTEND);
        assertThat(odgovor.zaglavlje("Access-Control-Allow-Credentials")).isEqualTo("true");
    }

    @Test
    @DisplayName("Preflight zahtjev s dopuštenog origina je prihvaćen")
    void preflightDopustenogOrigina() {
        Odgovor odgovor = posalji("OPTIONS", "/api/ja", null, null, null,
                "Origin", FRONTEND, "Access-Control-Request-Method", "GET",
                "Access-Control-Request-Headers", "authorization");

        assertThat(odgovor.status()).isBetween(200, 299);
        assertThat(odgovor.zaglavlje("Access-Control-Allow-Origin")).isEqualTo(FRONTEND);
    }

    @Test
    @DisplayName("Preflight zahtjev s HTTPS dopuštenog origina je prihvaćen")
    void preflightHttpsOrigina() {
        Odgovor odgovor = posalji("OPTIONS", "/api/ja", null, null, null,
                "Origin", "https://sigurni-portal.hr", "Access-Control-Request-Method", "GET");

        assertThat(odgovor.status()).isEqualTo(204);
        assertThat(odgovor.zaglavlje("Access-Control-Allow-Origin")).isEqualTo("https://sigurni-portal.hr");
    }

    @Test
    @DisplayName("Preflight zahtjev s nedopuštenog protokola/origina vraća 403")
    void preflightNedopustenogOrigina() {
        Odgovor odgovorFtp = posalji("OPTIONS", "/api/ja", null, null, null,
                "Origin", "ftp://evil.com", "Access-Control-Request-Method", "GET");
        assertThat(odgovorFtp.status()).isEqualTo(403);

        Odgovor odgovorPrazan = posalji("OPTIONS", "/api/ja", null, null, null,
                "Origin", "   ", "Access-Control-Request-Method", "GET");
        assertThat(odgovorPrazan.status()).isEqualTo(403);
    }

    @Test
    @DisplayName("Zahtjev bez Origin zaglavlja se normalno izvršava bez CORS zaglavlja")
    void zahtjevBezOrigina() {
        Odgovor odgovor = get("/api/health", null);
        assertThat(odgovor.status()).isEqualTo(200);
        assertThat(odgovor.zaglavlje("Access-Control-Allow-Origin")).isNull();
    }

    @Test
    @DisplayName("OPTIONS zahtjev bez Access-Control-Request-Method nije preflight")
    void optionsBezRequestMetode() {
        Odgovor odgovor = posalji("OPTIONS", "/api/health", null, null, null, "Origin", FRONTEND);
        assertThat(odgovor.status()).isBetween(200, 299);
    }

    @Test
    @DisplayName("GET zahtjev s nedopuštenim originom prolazi bez CORS zaglavlja")
    void getZahtjevSNedopustenimOriginom() {
        Odgovor odgovor = posalji("GET", "/api/health", null, null, null, "Origin", "ftp://evil.com");
        assertThat(odgovor.status()).isEqualTo(200);
        assertThat(odgovor.zaglavlje("Access-Control-Allow-Origin")).isNull();
    }
}

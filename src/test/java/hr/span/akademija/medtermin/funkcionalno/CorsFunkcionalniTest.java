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
}

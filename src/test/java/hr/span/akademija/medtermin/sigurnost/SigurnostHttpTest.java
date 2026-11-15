package hr.span.akademija.medtermin.sigurnost;

import static org.assertj.core.api.Assertions.assertThat;

import hr.span.akademija.medtermin.ApiTestBaza;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class SigurnostHttpTest extends ApiTestBaza {
    @Test
    void nevaljaniFormatNeOtkrivaTudiNalaz() {
        String token = prijava("ana", "Ana12345!");
        assertThat(get("/api/nalazi/2?format=nevaljano", token).status()).isEqualTo(404);
    }

    @Test
    void nepoznatiHttpsOriginNijeDopusten() {
        Odgovor odgovor = posalji("OPTIONS", "/api/ja", null, null, null,
                "Origin", "https://zlonamjerna.example", "Access-Control-Request-Method", "GET");
        assertThat(odgovor.status()).isEqualTo(403);
        assertThat(odgovor.zaglavlje("Access-Control-Allow-Origin")).isNull();
    }

    @Test
    void yamlOdbijaNeispravneBrojeveIKljuceve() {
        String token = prijava("ana", "Ana12345!");
        for (String yaml : List.of("satiPrije: 4294967320", "satiPrije: 24.5", "null: true")) {
            assertThat(posalji("POST", "/api/postavke", token, "application/yaml", yaml).status()).isEqualTo(400);
        }
    }

    @Test
    void csrfZastitaZaKolacic() {
        Odgovor login = postForma("/prijava", Map.of("username", "ana", "password", "Ana12345!"));
        String kolacic = login.zaglavlje("Set-Cookie").split(";")[0];
        assertThat(posalji("POST", "/api/postavke", null, "application/yaml", "kanal: sms",
                "Cookie", kolacic, "Origin", "https://zlonamjerna.example").status()).isEqualTo(403);
        assertThat(posalji("POST", "/api/postavke", null, "application/yaml", "kanal: sms",
                "Cookie", kolacic).status()).isEqualTo(403);
        assertThat(posalji("POST", "/api/postavke", null, "application/yaml", "kanal: email",
                "Cookie", kolacic, "Origin", "http://localhost:" + port).status()).isEqualTo(200);
    }

    @Test
    void sigurnosnaZaglavlja() {
        for (String put : List.of("/prijava", "/reset?token=primjer")) {
            Odgovor odgovor = get(put, null);
            assertThat(odgovor.zaglavlje("Content-Security-Policy")).contains("default-src 'none'").contains("form-action 'self'");
            assertThat(odgovor.zaglavlje("Cache-Control")).contains("no-store");
            assertThat(odgovor.zaglavlje("Referrer-Policy")).isEqualTo("no-referrer");
            assertThat(odgovor.zaglavlje("X-Content-Type-Options")).isEqualTo("nosniff");
        }
    }
}

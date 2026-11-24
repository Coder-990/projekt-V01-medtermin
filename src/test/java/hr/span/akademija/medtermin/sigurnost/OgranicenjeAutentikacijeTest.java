package hr.span.akademija.medtermin.sigurnost;

import static org.assertj.core.api.Assertions.assertThat;
import hr.span.akademija.medtermin.ApiTestBaza;
import java.util.Map;
import org.junit.jupiter.api.Test;

@org.springframework.test.context.TestPropertySource(properties = {"app.auth-limit.login=10", "app.auth-limit.reset=5"}, inheritProperties = false)
class OgranicenjeAutentikacijeTest extends ApiTestBaza {
    @Test void korisniciNaIstojAdresiNisuMedusobnoBlokirani() {
        for (int i = 0; i < 10; i++) {
            assertThat(postJson("/api/auth/login", null, Map.of("username", "napadac", "password", "neispravno")).status()).isEqualTo(401);
        }
        assertThat(postJson("/api/auth/login", null, Map.of("username", "napadac", "password", "neispravno")).status()).isEqualTo(429);
        assertThat(postForma("/prijava", Map.of("username", "napadac", "password", "neispravno")).status()).isEqualTo(429);
        assertThat(postJson("/api/auth/login", null, Map.of("username", "ana", "password", "Ana12345!")).status()).isEqualTo(200);
        assertThat(postForma("/prijava", Map.of("username", "marko", "password", "Marko12345!")).status()).isEqualTo(302);
    }

    @Test void ogranicavaBruteForcePrijavu() {
        for (int i = 0; i < 10; i++) {
            assertThat(postJson("/api/auth/login", null, Map.of("username", "nepostojeci", "password", "neispravno")).status()).isEqualTo(401);
        }
        assertThat(postJson("/api/auth/login", null, Map.of("username", "nepostojeci", "password", "neispravno")).status()).isEqualTo(429);
    }
}

package hr.span.akademija.medtermin.sigurnost;

import static org.assertj.core.api.Assertions.assertThat;
import hr.span.akademija.medtermin.ApiTestBaza;
import java.util.Map;
import org.junit.jupiter.api.Test;

@org.springframework.test.context.TestPropertySource(properties = {"app.auth-limit.login=10", "app.auth-limit.reset=5"}, inheritProperties = false)
class OgranicenjeAutentikacijeTest extends ApiTestBaza {
    @Test void ogranicavaBruteForcePrijavu() {
        for (int i = 0; i < 10; i++) {
            assertThat(postJson("/api/auth/login", null, Map.of("username", "nepostojeci", "password", "neispravno")).status()).isEqualTo(401);
        }
        assertThat(postJson("/api/auth/login", null, Map.of("username", "nepostojeci", "password", "neispravno")).status()).isEqualTo(429);
    }
}

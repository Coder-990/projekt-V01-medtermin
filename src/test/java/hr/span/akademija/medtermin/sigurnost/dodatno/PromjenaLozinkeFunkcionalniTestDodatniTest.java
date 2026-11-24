package hr.span.akademija.medtermin.sigurnost.dodatno;

import static org.assertj.core.api.Assertions.assertThat;

import hr.span.akademija.medtermin.ApiTestBaza;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

@Tag("sigurnost")
class PromjenaLozinkeFunkcionalniTestDodatniTest extends ApiTestBaza {

    private static final Pattern TOKEN = Pattern.compile("Token:\\s*(\\S+)");

    private List<Path> porukeZa(String username) throws IOException {
        if (!Files.isDirectory(mapaPoste())) {
            return List.of();
        }
        try (Stream<Path> datoteke = Files.list(mapaPoste())) {
            return datoteke.filter(p -> p.getFileName().toString().startsWith(username + "-")).toList();
        }
    }

    @Test
    @DisplayName("Zahtjev za reset s praznim ili null korisničkim imenom vraća 202")
    void resetZahtjevPraznoIme() {
        Odgovor odgovorPrazno = postJson("/api/auth/reset-zahtjev", null, Map.of("username", ""));
        assertThat(odgovorPrazno.status()).isEqualTo(202);

        java.util.Map<String, Object> tijeloNull = new java.util.HashMap<>();
        tijeloNull.put("username", null);
        Odgovor odgovorNull = postJson("/api/auth/reset-zahtjev", null, tijeloNull);
        assertThat(odgovorNull.status()).isEqualTo(202);
    }

    @Test
    @DisplayName("Reset lozinke s praznim ili null tokenom vraća 400")
    void resetPrazanToken() {
        Odgovor odgovorPrazan = postJson("/api/auth/reset", null,
                Map.of("token", "   ", "novaLozinka", "Nova12345!"));
        assertThat(odgovorPrazan.status()).isEqualTo(400);

        java.util.Map<String, Object> tijeloNull = new java.util.HashMap<>();
        tijeloNull.put("token", null);
        tijeloNull.put("novaLozinka", "Nova12345!");
        Odgovor odgovorNull = postJson("/api/auth/reset", null, tijeloNull);
        assertThat(odgovorNull.status()).isEqualTo(400);
    }
}

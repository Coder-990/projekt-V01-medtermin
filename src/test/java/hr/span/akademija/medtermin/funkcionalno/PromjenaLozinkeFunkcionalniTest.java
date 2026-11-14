package hr.span.akademija.medtermin.funkcionalno;

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

@Tag("funkcionalno")
class PromjenaLozinkeFunkcionalniTest extends ApiTestBaza {

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
    @DisplayName("Cijeli tok: zahtjev, poruka u mapi pošte, nova lozinka, prijava")
    void cijeliTokPromjeneLozinke() throws IOException {
        String username = noviKorisnik("zaboravni", "Stara12345!");

        Odgovor zahtjev = postJson("/api/auth/reset-zahtjev", null, Map.of("username", username));

        assertThat(zahtjev.status()).isEqualTo(202);
        assertThat(zahtjev.json().get("poruka").asText())
                .isEqualTo("Ako korisnik postoji, poslali smo upute na e-mail.");
        List<Path> poruke = porukeZa(username);
        assertThat(poruke).hasSize(1);
        String poruka = Files.readString(poruke.get(0), StandardCharsets.UTF_8);
        assertThat(poruka).contains("/reset?token=");
        Matcher m = TOKEN.matcher(poruka);
        assertThat(m.find()).isTrue();

        Odgovor reset = postJson("/api/auth/reset", null, Map.of("token", m.group(1), "novaLozinka", "Nova12345!"));

        assertThat(reset.status()).isEqualTo(200);
        assertThat(reset.json().get("poruka").asText()).isEqualTo("Lozinka je promijenjena.");
        assertThat(postJson("/api/auth/login", null, Map.of("username", username, "password", "Nova12345!"))
                .status()).isEqualTo(200);
        assertThat(postJson("/api/auth/login", null, Map.of("username", username, "password", "Stara12345!"))
                .status()).isEqualTo(401);
    }

    @Test
    @DisplayName("Zahtjev za nepostojećeg korisnika također vraća 202, bez poruke")
    void nepostojeciKorisnik() throws IOException {
        Odgovor zahtjev = postJson("/api/auth/reset-zahtjev", null, Map.of("username", "nepostojeci"));

        assertThat(zahtjev.status()).isEqualTo(202);
        assertThat(porukeZa("nepostojeci")).isEmpty();
    }

    @Test
    @DisplayName("Izmišljeni token vraća 400")
    void izmisljeniToken() {
        Odgovor reset = postJson("/api/auth/reset", null,
                Map.of("token", "ovo-nije-token", "novaLozinka", "Nova12345!"));

        assertThat(reset.status()).isEqualTo(400);
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

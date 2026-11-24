package hr.span.akademija.medtermin.sigurnost;

import static org.assertj.core.api.Assertions.assertThat;

import hr.span.akademija.medtermin.ApiTestBaza;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.security.MessageDigest;
import java.time.LocalDate;
import java.util.HexFormat;
import java.util.Map;
import java.util.regex.Pattern;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.jdbc.core.JdbcTemplate;

@ExtendWith(OutputCaptureExtension.class)
class AutentikacijaSigurnostTest extends ApiTestBaza {
    @Autowired JdbcTemplate jdbc;

    private String zatraziToken(String user) throws Exception {
        assertThat(postJson("/api/auth/reset-zahtjev", null, Map.of("username", user)).status()).isEqualTo(202);
        try (var poruke = Files.list(mapaPoste())) {
            var poruka = poruke.filter(p -> p.getFileName().toString().startsWith(user + "-"))
                    .findFirst().orElseThrow();
            var matcher = Pattern.compile("Token:\\s*(\\S+)").matcher(Files.readString(poruka));
            assertThat(matcher.find()).isTrue();
            return matcher.group(1);
        }
    }

    @Test
    void resetTokenNijeIzvedenIzImenaIDatuma() throws Exception {
        String user = noviKorisnik("tokenrandom", "Pocetna123!");
        String token = zatraziToken(user);
        String predvidljivi = HexFormat.of().formatHex(MessageDigest.getInstance("MD5")
                .digest((user + LocalDate.now()).getBytes(StandardCharsets.UTF_8)));
        assertThat(token).isNotEqualTo(predvidljivi);
    }

    @Test
    void resetTokenMozeSeIskoristitiSamoJednom() throws Exception {
        String user = noviKorisnik("tokenjednom", "Pocetna123!");
        String token = zatraziToken(user);
        assertThat(postJson("/api/auth/reset", null, Map.of("token", token, "novaLozinka", "Nova12345!"))
                .status()).isEqualTo(200);
        assertThat(postJson("/api/auth/reset", null, Map.of("token", token, "novaLozinka", "Druga12345!"))
                .status()).isEqualTo(400);
    }

    @Test
    void lozinkaSePohranjujeSaSoljuIKdfom() {
        String user = noviKorisnik("kdf", "Pocetna123!");
        String hash = jdbc.queryForObject("SELECT password_hash FROM korisnici WHERE username = ?", String.class, user);
        assertThat(hash).startsWith("$2");
    }

    @Test
    void lozinkaSeNeZapisujeUPrijavi(CapturedOutput izlaz) {
        String lozinka = "TajnaLog123!";
        String user = noviKorisnik("log", lozinka);
        prijava(user, lozinka);
        assertThat(izlaz.getAll()).doesNotContain(lozinka);
    }
    @Test
    void neuspjelaPrijavaBiljeziWarnBezLozinke(CapturedOutput izlaz) {
        String lozinka = "NeZapisivatiLozinku123!";
        String korisnickoIme = "nepostojeci\r\nLAZNI";
        assertThat(postJson("/api/auth/login", null,
                Map.of("username", korisnickoIme, "password", lozinka)).status()).isEqualTo(401);
        assertThat(izlaz.getAll()).containsPattern("(?m)^.*\\bWARN\\b.*Neuspjela prijava za korisnika: nepostojeci__LAZNI$")
                .doesNotContain(lozinka, korisnickoIme);
    }

}

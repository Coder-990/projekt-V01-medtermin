package hr.span.akademija.medtermin.sigurnost;

import static org.assertj.core.api.Assertions.assertThat;

import hr.span.akademija.medtermin.ApiTestBaza;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.CountDownLatch;
import java.util.regex.Pattern;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

class SigurnosniRegresijskiTest extends ApiTestBaza {
    @Autowired JdbcTemplate jdbc;

    @Test void sortiranjeOdbijaSql() {
        String token = prijava("ana", "Ana12345!");
        // Izvorna implementacija izvršava valjan SQL izraz umjesto da ga odbije.
        assertThat(get("/api/lijecnici?sort=" + enc("CASE WHEN 1=1 THEN prezime ELSE ime END"), token).status()).isEqualTo(400);
        assertThat(get("/api/lijecnici?sort=" + enc("ime; DROP TABLE korisnici") , token).status()).isEqualTo(400);
        assertThat(get("/api/lijecnici?smjer=" + enc("asc, id"), token).status()).isEqualTo(400);
        assertThat(get("/api/lijecnici?sort=ordinacija&smjer=desc", token).status()).isEqualTo(200);
        assertThat(get("/api/lijecnici?sort=id", token).status()).isEqualTo(200);
    }

    @Test void redirectIYaml() {
        assertThat(postForma("/prijava", Map.of("username", "ana", "password", "Ana12345!", "returnUrl", "//evil.example"))
                .zaglavlje("Location")).isEqualTo("/");
        String token = prijava("ana", "Ana12345!");
        assertThat(posalji("POST", "/api/postavke", token, "application/yaml", "!!java.net.URL [http://example.org]").status()).isEqualTo(400);
        assertThat(posalji("POST", "/api/postavke", token, "application/yaml", "podsjetnik: true\n".repeat(2000)).status()).isEqualTo(400);
        assertThat(posalji("POST", "/api/postavke", null, "application/yaml", "podsjetnik: true").status()).isEqualTo(401);
    }

    @Test void putanjaISymlink() throws Exception {
        String token = prijava("ana", "Ana12345!");
        Path vani = Files.createTempFile("medtermin-izvan", ".txt");
        Files.writeString(vani, "TAJNO");
        Path link = mapaDokumenata().resolve("ana-link.txt");
        Files.deleteIfExists(link);
        Files.createSymbolicLink(link, vani);
        jdbc.update("INSERT INTO nalazi (pacijent, naziv, datoteka) VALUES (?, ?, ?)", "ana", "test", "ana-link.txt");
        assertThat(get("/api/nalazi/preuzmi?datoteka=ana-link.txt", token).status()).isEqualTo(404);
        assertThat(get("/api/nalazi/preuzmi?datoteka=" + enc("../ana-link.txt"), token).status()).isEqualTo(404);
        assertThat(get("/api/nalazi/preuzmi?datoteka=" + enc(vani.toString()), token).status()).isEqualTo(404);
    }

    @Test void istekliResetIMigracijaIOdjava() throws Exception {
        String user = noviKorisnik("legacy", "Stara12345!");
        String legacy = java.util.HexFormat.of().formatHex(java.security.MessageDigest.getInstance("SHA-256")
                .digest("Stara12345!".getBytes(java.nio.charset.StandardCharsets.UTF_8)));
        jdbc.update("UPDATE korisnici SET password_hash = ? WHERE username = ?", legacy, user);
        String sesija = prijava(user, "Stara12345!");
        assertThat(jdbc.queryForObject("SELECT password_hash FROM korisnici WHERE username = ?", String.class, user)).startsWith("$2");
        assertThat(postJson("/api/auth/logout", sesija, Map.of()).status()).isEqualTo(204);
        assertThat(get("/api/ja", sesija).status()).isEqualTo(401);
        postJson("/api/auth/reset-zahtjev", null, Map.of("username", user));
        Path poruka;
        try (var datoteke = Files.list(mapaPoste())) {
            poruka = datoteke.filter(p -> p.getFileName().toString().startsWith(user + "-")).findFirst().orElseThrow();
        }
        var m = Pattern.compile("Token:\\s*(\\S+)").matcher(Files.readString(poruka));
        assertThat(m.find()).isTrue();
        jdbc.update("UPDATE reset_tokeni SET istjece = ? WHERE username = ?", java.time.LocalDateTime.now().minusMinutes(1), user);
        assertThat(postJson("/api/auth/reset", null, Map.of("token", m.group(1), "novaLozinka", "Nova12345!")).status()).isEqualTo(400);
        assertThat(prijava(user, "Stara12345!")).isNotBlank();
    }

    @Test void promjenaLozinkeOpozivaSveResetTokene() throws Exception {
        String user = noviKorisnik("resetvise", "Stara12345!");
        String t1 = "token-jedan", t2 = "token-dva";
        java.security.MessageDigest digest = java.security.MessageDigest.getInstance("SHA-256");
        for (String t : java.util.List.of(t1, t2)) {
            String hash = java.util.HexFormat.of().formatHex(digest.digest(t.getBytes(java.nio.charset.StandardCharsets.UTF_8)));
            jdbc.update("INSERT INTO reset_tokeni (username, token, istjece) VALUES (?, ?, ?)",
                    user, hash, java.time.LocalDateTime.now().plusMinutes(15));
        }
        assertThat(postJson("/api/auth/reset", null, Map.of("token", t1, "novaLozinka", "Nova12345!")).status()).isEqualTo(200);
        assertThat(postJson("/api/auth/reset", null, Map.of("token", t2, "novaLozinka", "Druga12345!")).status()).isEqualTo(400);
    }

    @Test void resetJeJednokratanIOpozivaSesiju() throws Exception {
        String user = noviKorisnik("reset", "Stara12345!");
        String staraSesija = prijava(user, "Stara12345!");
        postJson("/api/auth/reset-zahtjev", null, Map.of("username", user));
        Path poruka;
        try (var datoteke = Files.list(mapaPoste())) {
            poruka = datoteke.filter(p -> p.getFileName().toString().startsWith(user + "-")).findFirst().orElseThrow();
        }
        var matcher = Pattern.compile("Token:\\s*(\\S+)").matcher(Files.readString(poruka));
        assertThat(matcher.find()).isTrue();
        String token = matcher.group(1);
        assertThat(jdbc.queryForObject("SELECT token FROM reset_tokeni WHERE username = ?", String.class, user)).isNotEqualTo(token);
        var start = new CountDownLatch(1);
        try (var pool = Executors.newFixedThreadPool(2)) {
            var prvi = pool.submit(() -> { start.await(); return postJson("/api/auth/reset", null, Map.of("token", token, "novaLozinka", "Nova12345!")).status(); });
            var drugi = pool.submit(() -> { start.await(); return postJson("/api/auth/reset", null, Map.of("token", token, "novaLozinka", "Nova12345!")).status(); });
            start.countDown();
            assertThat(java.util.List.of(prvi.get(), drugi.get())).containsExactlyInAnyOrder(200, 400);
        }
        assertThat(get("/api/ja", staraSesija).status()).isEqualTo(401);
    }
}

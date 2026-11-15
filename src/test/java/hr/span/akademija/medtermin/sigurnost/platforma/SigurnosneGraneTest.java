package hr.span.akademija.medtermin.platforma;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.*;
import java.util.HexFormat;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockHttpServletRequest;
import jakarta.servlet.http.Cookie;

class SigurnosneGraneTest {
    static class Sat extends Clock {
        Instant vrijeme = Instant.parse("2026-01-01T00:00:00Z");
        public ZoneId getZone() { return ZoneOffset.UTC; }
        public Clock withZone(ZoneId zone) { return this; }
        public Instant instant() { return vrijeme; }
    }

    @Test void ogranicenjeSesijaIPrikupljanjeIsteklih() {
        Sat sat = new Sat();
        TokenStore store = new TokenStore(sat);
        var k = new PrijavljeniKorisnik(1, "ana", "KORISNIK");
        for (int i = 0; i < 10_000; i++) store.izdaj(k);
        assertThatThrownBy(() -> store.izdaj(k)).isInstanceOf(IllegalStateException.class);
        sat.vrijeme = sat.vrijeme.plus(Duration.ofHours(8));
        assertThat(store.korisnik(store.izdaj(k))).isEqualTo(k);
    }

    @Test void sigurnostLozinkeIStariDugiHash() throws Exception {
        var hasher = new LozinkaHasher();
        assertThat(hasher.provjeri(null, "hash")).isFalse();
        assertThat(hasher.provjeri("x".repeat(73), hasher.hash("kratka123"))).isFalse();
        assertThatThrownBy(() -> KorisnikService.provjeriLozinku("x".repeat(73))).isInstanceOf(ApiException.class);
        String lozinka = "x".repeat(73);
        String hash = HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(lozinka.getBytes(StandardCharsets.UTF_8)));
        JdbcTemplate jdbc = mock(JdbcTemplate.class);
        var korisnik = new KorisnikService.Korisnik(1, "ana", hash, "a@b.hr", "Ana", "A", "KORISNIK", null);
        when(jdbc.query(anyString(), any(org.springframework.jdbc.core.RowMapper.class), anyString())).thenReturn(List.of(korisnik));
        var service = new KorisnikService(jdbc, hasher);
        assertThat(service.prijava(new KorisnikService.PrijavaZahtjev("ana", lozinka))).isPresent();
        verify(jdbc, never()).update(anyString(), any(), any(), any());
        service.promijeniLozinku("ana", "sigurna123");
    }

    @Test void nepodrzaniShaSigurnoPrijavljujeGresku() {
        try (var digest = mockStatic(MessageDigest.class, CALLS_REAL_METHODS)) {
            digest.when(() -> MessageDigest.getInstance("SHA-256")).thenThrow(new NoSuchAlgorithmException());
            assertThatThrownBy(() -> new LozinkaHasher().provjeri("x", "0".repeat(64)))
                    .isInstanceOf(IllegalStateException.class).hasMessageContaining("SHA-256");
        }
    }

    @Test void odbijenaOdredistaIYAML() throws Exception {
        var method = PrijavaController.class.getDeclaredMethod("odrediste", String.class);
        method.setAccessible(true);
        for (String url : List.of("https://evil.test", "//evil.test", "/\\evil", "/a\nb", "/%2f%2fevil")) {
            assertThat(method.invoke(null, url)).isEqualTo("/");
        }
        assertThat(method.invoke(null, "/moji?stranica=2")).isEqualTo("/moji?stranica=2");
        var postavke = new PostavkeController(mock(JdbcTemplate.class));
        assertThatThrownBy(() -> postavke.spremi("nepoznato: 1".getBytes(StandardCharsets.UTF_8),
                new PrijavljeniKorisnik(1, "ana", "KORISNIK"))).isInstanceOf(ApiException.class);
    }

    @Test void odjavaKolacicem() {
        TokenStore store = new TokenStore();
        var k = new PrijavljeniKorisnik(1, "ana", "KORISNIK");
        String token = store.izdaj(k);
        MockHttpServletRequest zahtjev = new MockHttpServletRequest();
        zahtjev.setCookies(new Cookie(AuthFilter.KOLACIC, token));
        var controller = new AuthController(mock(KorisnikService.class), store, mock(ResetLozinkeService.class));
        assertThat(controller.logout(zahtjev, k).getStatusCode().value()).isEqualTo(204);
        assertThat(store.korisnik(token)).isNull();
    }
}

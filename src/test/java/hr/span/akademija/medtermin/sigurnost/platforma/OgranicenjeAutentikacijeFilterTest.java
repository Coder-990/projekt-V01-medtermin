package hr.span.akademija.medtermin.platforma;

import static org.assertj.core.api.Assertions.assertThat;
import java.time.*;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.util.ReflectionTestUtils;

class OgranicenjeAutentikacijeFilterTest {
    static class Sat extends Clock {
        Instant vrijeme = Instant.parse("2026-01-01T00:00:00Z");
        public ZoneId getZone() { return ZoneOffset.UTC; }
        public Clock withZone(ZoneId zone) { return this; }
        public Instant instant() { return vrijeme; }
    }

    private int salji(OgranicenjeAutentikacijeFilter f, String path, String adresa, int statusOdgovora) throws Exception {
        var req = new MockHttpServletRequest("POST", path);
        req.setServletPath(path);
        req.setRemoteAddr(adresa);
        var res = new MockHttpServletResponse();
        f.doFilter(req, res, (request, response) -> ((MockHttpServletResponse) response).setStatus(statusOdgovora));
        return res.getStatus();
    }

    @Test
    void ogranicenjeJePoKorisnikuIIpTeIstjece() {
        Sat sat = new Sat();
        var filter = new OgranicenjeAutentikacijeFilter(sat);
        ReflectionTestUtils.setField(filter, "prijave", 1);
        filter.neuspjelaPrijava("ip-a", "ana");
        assertThat(filter.prijavaBlokirana("ip-a", "ana")).isTrue();
        assertThat(filter.prijavaBlokirana("ip-a", "marko")).isFalse();
        assertThat(filter.prijavaBlokirana("ip-b", "ana")).isFalse();
        sat.vrijeme = sat.vrijeme.plus(Duration.ofMinutes(15));
        assertThat(filter.prijavaBlokirana("ip-a", "ana")).isFalse();
        filter.neuspjelaPrijava("ip-a", "ana");
        filter.uspjesnaPrijava("ip-a", "ana");
        assertThat(filter.prijavaBlokirana("ip-a", "ana")).isFalse();
        ReflectionTestUtils.setField(filter, "prijave", 0);
        assertThat(filter.prijavaBlokirana("ip-b", "marko")).isTrue();
    }

    @Test
    void uspjesanResetPonistavaPrethodneNeuspjehe() throws Exception {
        var filter = new OgranicenjeAutentikacijeFilter(Clock.fixed(Instant.EPOCH, ZoneOffset.UTC));
        ReflectionTestUtils.setField(filter, "reseti", 2);
        assertThat(salji(filter, "/api/auth/reset", "a", 400)).isEqualTo(400);
        assertThat(salji(filter, "/api/auth/reset", "a", 200)).isEqualTo(200);
        assertThat(salji(filter, "/api/auth/reset", "a", 400)).isEqualTo(400);
        assertThat(salji(filter, "/api/auth/reset", "a", 400)).isEqualTo(400);
        assertThat(salji(filter, "/api/auth/reset", "a", 400)).isEqualTo(429);
    }

    @Test
    void punaTablicaOdbijaNovogKlijenta() throws Exception {
        var filter = new OgranicenjeAutentikacijeFilter(Clock.fixed(Instant.EPOCH, ZoneOffset.UTC));
        for (int i = 0; i < 10_000; i++) {
            filter.zabiljeziNeuspjeh("prijava", "ip-" + i);
        }
        ReflectionTestUtils.setField(filter, "prijave", 1);
        assertThat(salji(filter, "/api/auth/login", "novi", 401)).isEqualTo(401);
    }
}

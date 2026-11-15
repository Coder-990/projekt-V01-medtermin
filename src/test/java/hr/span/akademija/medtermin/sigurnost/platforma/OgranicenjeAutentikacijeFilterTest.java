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
    private int salji(OgranicenjeAutentikacijeFilter f, String path, String adresa) throws Exception {
        var req = new MockHttpServletRequest("POST", path);
        req.setServletPath(path);
        req.setRemoteAddr(adresa);
        var res = new MockHttpServletResponse();
        f.doFilter(req, res, (request, response) -> { });
        return res.getStatus();
    }
    @Test void granicaIstjeceINeispravnaKonfiguracijaOdbija() throws Exception {
        Sat sat = new Sat();
        var filter = new OgranicenjeAutentikacijeFilter(sat);
        ReflectionTestUtils.setField(filter, "prijave", 1);
        assertThat(salji(filter, "/prijava", "a")).isEqualTo(200);
        assertThat(salji(filter, "/api/auth/login", "a")).isEqualTo(429);
        assertThat(salji(filter, "/api/auth/reset-zahtjev", "a")).isEqualTo(200);
        sat.vrijeme = sat.vrijeme.plus(Duration.ofMinutes(15));
        assertThat(salji(filter, "/prijava", "a")).isEqualTo(200);
        ReflectionTestUtils.setField(filter, "prijave", 0);
        assertThat(salji(filter, "/prijava", "b")).isEqualTo(429);
    }
    @Test void punaTablicaOdbijaNovogKlijenta() throws Exception {
        var filter = new OgranicenjeAutentikacijeFilter(Clock.fixed(Instant.EPOCH, ZoneOffset.UTC));
        for (int i = 0; i < 10_000; i++) assertThat(salji(filter, "/prijava", "ip-" + i)).isEqualTo(200);
        assertThat(salji(filter, "/prijava", "novi")).isEqualTo(429);
    }
}

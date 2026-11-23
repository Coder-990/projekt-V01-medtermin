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
    void granicaIstjeceINeispravnaKonfiguracijaOdbija() throws Exception {
        Sat sat = new Sat();
        var filter = new OgranicenjeAutentikacijeFilter(sat);
        ReflectionTestUtils.setField(filter, "prijave", 1);

        // Prvi neuspjeli pokušaj (401) prolazi lanac i bilježi neuspjeh
        assertThat(salji(filter, "/api/auth/login", "a", 401)).isEqualTo(401);

        // Drugi pokušaj je blokiran jer je limit 1
        assertThat(salji(filter, "/api/auth/login", "a", 200)).isEqualTo(429);

        // Reset nije blokiran jer je drugi tip
        assertThat(salji(filter, "/api/auth/reset-zahtjev", "a", 200)).isEqualTo(200);

        // Nakon isteka prozora, ponovno je dopušteno
        sat.vrijeme = sat.vrijeme.plus(Duration.ofMinutes(15));
        assertThat(salji(filter, "/api/auth/login", "a", 401)).isEqualTo(401);

        // Ako je limit 0, odmah blokira
        ReflectionTestUtils.setField(filter, "prijave", 0);
        assertThat(salji(filter, "/api/auth/login", "b", 200)).isEqualTo(429);
    }

    @Test
    void punaTablicaOdbijaNovogKlijenta() throws Exception {
        var filter = new OgranicenjeAutentikacijeFilter(Clock.fixed(Instant.EPOCH, ZoneOffset.UTC));
        for (int i = 0; i < 10_000; i++) {
            filter.zabiljeziNeuspjeh("prijava", "ip-" + i);
        }
        ReflectionTestUtils.setField(filter, "prijave", 1);
        // Kad je tablica puna, novi klijent ne može zabilježiti novi prozor
        assertThat(salji(filter, "/api/auth/login", "novi", 401)).isEqualTo(401);
    }
}

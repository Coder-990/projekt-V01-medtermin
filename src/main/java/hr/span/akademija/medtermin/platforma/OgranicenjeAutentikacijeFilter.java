package hr.span.akademija.medtermin.platforma;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Lokalno ograničenje neuspjelih prijava po korisničkom imenu i izravnoj IP adresi.
 * Zahtjeve za reset obrađuje filtar; prijave provjerava KorisnikService nakon čitanja imena.
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 5)
public class OgranicenjeAutentikacijeFilter extends OncePerRequestFilter {

    private static final Duration TRAJANJE = Duration.ofMinutes(15);
    private static final int MAKS_KLIJENATA = 10_000;

    private record Prozor(Instant istjece, int neuspjeliPokusaji) { }

    private record Kljuc(String vrsta, String adresa, String korisnik) { }

    private final Map<Kljuc, Prozor> neuspjeliProzori = new HashMap<>();
    private final Clock sat;

    @Value("${app.auth-limit.login:10}")
    private int prijave = 10;

    @Value("${app.auth-limit.reset:50}")
    private int reseti = 50;

    public OgranicenjeAutentikacijeFilter() {
        this(Clock.systemUTC());
    }

    OgranicenjeAutentikacijeFilter(Clock sat) {
        this.sat = sat;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        String vrsta = vrsta(request);
        String adresa = request.getRemoteAddr();

        if (vrsta != null && jeBlokiran(vrsta, adresa)) {
            response.setHeader(HttpHeaders.RETRY_AFTER, Long.toString(TRAJANJE.toSeconds()));
            response.sendError(429, "Previše neuspjelih pokušaja. Pokušajte kasnije.");
            return;
        }

        chain.doFilter(request, response);

        if (vrsta != null) {
            int status = response.getStatus();
            if (status == HttpServletResponse.SC_BAD_REQUEST) {
                zabiljeziNeuspjeh(vrsta, adresa);
            } else if (status >= 200 && status < 400) {
                ponistiNeuspjehe(vrsta, adresa);
            }
        }
    }

    private static String vrsta(HttpServletRequest request) {
        if (!"POST".equals(request.getMethod())) return null;
        return switch (request.getServletPath()) {
            case "/api/auth/reset-zahtjev", "/api/auth/reset", "/reset" -> "reset";
            default -> null;
        };
    }

    private static String imeZaOgranicenje(String ime) {
        return ime != null && ime.matches("[A-Za-z0-9._-]{3,50}") ? ime : null;
    }

    public synchronized boolean prijavaBlokirana(String adresa, String ime) {
        Instant sada = sat.instant();
        neuspjeliProzori.values().removeIf(p -> !p.istjece().isAfter(sada));
        Prozor prozor = neuspjeliProzori.get(new Kljuc("prijava", adresa, imeZaOgranicenje(ime)));
        return prijave < 1 || (prozor != null && prozor.neuspjeliPokusaji() >= prijave);
    }

    public synchronized void neuspjelaPrijava(String adresa, String ime) {
        Kljuc kljuc = new Kljuc("prijava", adresa, imeZaOgranicenje(ime));
        Prozor prozor = neuspjeliProzori.get(kljuc);
        if (prozor != null && prozor.istjece().isAfter(sat.instant())) {
            neuspjeliProzori.put(kljuc, new Prozor(prozor.istjece(), prozor.neuspjeliPokusaji() + 1));
        } else if (prozor != null || neuspjeliProzori.size() < MAKS_KLIJENATA) {
            neuspjeliProzori.put(kljuc, new Prozor(sat.instant().plus(TRAJANJE), 1));
        }
    }

    public synchronized void uspjesnaPrijava(String adresa, String ime) {
        neuspjeliProzori.remove(new Kljuc("prijava", adresa, imeZaOgranicenje(ime)));
    }

    private synchronized boolean jeBlokiran(String vrsta, String adresa) {
        Instant sada = sat.instant();
        neuspjeliProzori.values().removeIf(p -> !p.istjece().isAfter(sada));
        Kljuc kljuc = new Kljuc(vrsta, adresa, null);
        Prozor postojeci = neuspjeliProzori.get(kljuc);
        int limit = "prijava".equals(vrsta) ? prijave : reseti;
        if (limit < 1) return true;
        return postojeci != null && postojeci.neuspjeliPokusaji() >= limit;
    }

    public synchronized void zabiljeziNeuspjeh(String vrsta, String adresa) {
        Instant sada = sat.instant();
        Kljuc kljuc = new Kljuc(vrsta, adresa, null);
        Prozor postojeci = neuspjeliProzori.get(kljuc);
        if (postojeci == null) {
            if (neuspjeliProzori.size() < MAKS_KLIJENATA) {
                neuspjeliProzori.put(kljuc, new Prozor(sada.plus(TRAJANJE), 1));
            }
        } else {
            neuspjeliProzori.put(kljuc, new Prozor(postojeci.istjece(), postojeci.neuspjeliPokusaji() + 1));
        }
    }

    public synchronized void ponistiNeuspjehe(String vrsta, String adresa) {
        neuspjeliProzori.remove(new Kljuc(vrsta, adresa, null));
    }
}

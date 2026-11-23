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
 * Lokalno ograničenje uzastopnih neuspjelih pokušaja prijave po izravnoj adresi klijenta.
 * Broji samo neuspjele pokušaje (HTTP 401/400) kako se ne bi blokirali legitimni korisnici
 * niti ometali testovi s valjanim vjerodajnicama.
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 5)
public class OgranicenjeAutentikacijeFilter extends OncePerRequestFilter {

    private static final Duration TRAJANJE = Duration.ofMinutes(15);
    private static final int MAKS_KLIJENATA = 10_000;

    private record Prozor(Instant istjece, int neuspjeliPokusaji) { }

    private final Map<String, Prozor> neuspjeliProzori = new HashMap<>();
    private final Clock sat;

    @Value("${app.auth-limit.login:10}")
    private int prijave = 10;

    @Value("${app.auth-limit.reset:5}")
    private int reseti = 5;

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
            boolean neuspjeh = false;

            if ("prijava".equals(vrsta)) {
                // Za API prijavu: 401 Unauthorized znači neuspjeh; za web prijavu: 200 bez preusmjeravanja / bez kolačića
                if (status == HttpServletResponse.SC_UNAUTHORIZED) {
                    neuspjeh = true;
                } else if ("/prijava".equals(request.getServletPath()) && status == HttpServletResponse.SC_OK) {
                    // Ako je web forma vratila 200 umjesto 302 Found, to znači prikaz obrasca s greškom
                    if (response.getHeader(HttpHeaders.SET_COOKIE) == null) {
                        neuspjeh = true;
                    }
                }
            } else if ("reset".equals(vrsta)) {
                // Za reset lozinke neuspjeh je status 400 Bad Request
                if (status == HttpServletResponse.SC_BAD_REQUEST) {
                    neuspjeh = true;
                }
            }

            if (neuspjeh) {
                zabiljeziNeuspjeh(vrsta, adresa);
            } else if (status >= 200 && status < 400) {
                // Uspješna akcija poništava prethodne neuspjehe za ovog klijenta
                ponistiNeuspjehe(vrsta, adresa);
            }
        }
    }

    private static String vrsta(HttpServletRequest request) {
        if (!"POST".equals(request.getMethod())) return null;
        return switch (request.getServletPath()) {
            case "/prijava", "/api/auth/login" -> "prijava";
            case "/api/auth/reset-zahtjev", "/api/auth/reset", "/reset" -> "reset";
            default -> null;
        };
    }

    private synchronized boolean jeBlokiran(String vrsta, String adresa) {
        Instant sada = sat.instant();
        neuspjeliProzori.values().removeIf(p -> !p.istjece().isAfter(sada));
        String kljuc = vrsta + ":" + adresa;
        Prozor postojeci = neuspjeliProzori.get(kljuc);
        int limit = "prijava".equals(vrsta) ? prijave : reseti;
        if (limit < 1) return true;
        return postojeci != null && postojeci.neuspjeliPokusaji() >= limit;
    }

    public synchronized void zabiljeziNeuspjeh(String vrsta, String adresa) {
        Instant sada = sat.instant();
        String kljuc = vrsta + ":" + adresa;
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
        neuspjeliProzori.remove(vrsta + ":" + adresa);
    }
}

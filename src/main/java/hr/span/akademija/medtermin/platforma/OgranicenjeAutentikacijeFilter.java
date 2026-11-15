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

/** Lokalno ograničenje pokušaja po izravnoj adresi klijenta; ne čita nepouzdani X-Forwarded-For. */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 5)
public class OgranicenjeAutentikacijeFilter extends OncePerRequestFilter {
    private static final Duration TRAJANJE = Duration.ofMinutes(15);
    private static final int MAKS_KLIJENATA = 10_000;
    private record Prozor(Instant istjece, int broj) { }
    private final Map<String, Prozor> prozori = new HashMap<>();
    private final Clock sat;
    @Value("${app.auth-limit.login:10}")
    private int prijave = 10;
    @Value("${app.auth-limit.reset:5}")
    private int reseti = 5;

    public OgranicenjeAutentikacijeFilter() { this(Clock.systemUTC()); }
    OgranicenjeAutentikacijeFilter(Clock sat) { this.sat = sat; }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        String vrsta = vrsta(request);
        if (vrsta != null && !dopusti(vrsta, request.getRemoteAddr())) {
            response.setHeader(HttpHeaders.RETRY_AFTER, Long.toString(TRAJANJE.toSeconds()));
            response.sendError(429, "Previše pokušaja. Pokušajte kasnije.");
            return;
        }
        chain.doFilter(request, response);
    }

    private static String vrsta(HttpServletRequest request) {
        if (!"POST".equals(request.getMethod())) return null;
        return switch (request.getServletPath()) {
            case "/prijava", "/api/auth/login" -> "prijava";
            case "/api/auth/reset-zahtjev", "/api/auth/reset", "/reset" -> "reset";
            default -> null;
        };
    }

    private synchronized boolean dopusti(String vrsta, String adresa) {
        Instant sada = sat.instant();
        prozori.values().removeIf(p -> !p.istjece().isAfter(sada));
        String kljuc = vrsta + ":" + adresa;
        Prozor postojeci = prozori.get(kljuc);
        int limit = "prijava".equals(vrsta) ? prijave : reseti;
        if (limit < 1) return false;
        if (postojeci == null) {
            // Kad je memorijska tablica puna, nove pokušaje odbijamo umjesto
            // nekontroliranog rasta ili izbacivanja ograničenja drugih klijenata.
            if (prozori.size() >= MAKS_KLIJENATA) return false;
            prozori.put(kljuc, new Prozor(sada.plus(TRAJANJE), 1));
            return true;
        }
        if (postojeci.broj() >= limit) return false;
        prozori.put(kljuc, new Prozor(postojeci.istjece(), postojeci.broj() + 1));
        return true;
    }
}

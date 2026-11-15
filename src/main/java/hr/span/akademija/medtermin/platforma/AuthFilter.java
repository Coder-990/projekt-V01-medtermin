package hr.span.akademija.medtermin.platforma;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Iz zaglavlja {@code Authorization: Bearer <token>} ili kolačića {@code SESSION} određuje prijavljenog
 * korisnika i sprema ga u atribut zahtjeva. Hoće li endpoint tražiti prijavu, odlučuje kontroler
 * (parametar tipa {@link PrijavljeniKorisnik}).
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 10)
public class AuthFilter extends OncePerRequestFilter {

    public static final String ATRIBUT = AuthFilter.class.getName() + ".korisnik";
    public static final String KOLACIC = "SESSION";

    private final TokenStore tokeni;
    private java.util.List<String> dopusteniOrigini = java.util.List.of();

    @org.springframework.beans.factory.annotation.Autowired
    void postaviPostavke(AppProperties postavke) {
        dopusteniOrigini = postavke.dopusteniOrigini();
    }

    public AuthFilter(TokenStore tokeni) {
        this.tokeni = tokeni;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        String token = izZaglavlja(request);
        boolean kolacic = token == null;
        if (kolacic) {
            token = izKolacica(request);
        }
        PrijavljeniKorisnik korisnik = tokeni.korisnik(token);
        // CSRF: preglednik automatski šalje SESSION kolačić. Za promjene podataka
        // zahtijevamo točan same-origin ili konfigurirani frontend Origin; Bearer
        // zahtjevi ne koriste automatske kolačiće i zadržavaju postojeći API ugovor.
        if (kolacic && korisnik != null && !sigurnaMetoda(request.getMethod())
                && !pouzdanOrigin(request)) {
            response.sendError(HttpServletResponse.SC_FORBIDDEN);
            return;
        }
        if (korisnik != null) {
            request.setAttribute(ATRIBUT, korisnik);
        }
        chain.doFilter(request, response);
    }

    private static boolean sigurnaMetoda(String metoda) {
        return "GET".equals(metoda) || "HEAD".equals(metoda) || "OPTIONS".equals(metoda);
    }

    private boolean pouzdanOrigin(HttpServletRequest request) {
        String origin = request.getHeader(HttpHeaders.ORIGIN);
        if (origin == null || origin.isBlank()) return false;
        if (dopusteniOrigini.contains(origin)) return true;
        String scheme = request.getScheme();
        int port = request.getServerPort();
        String host = request.getServerName();
        if (host.indexOf(':') >= 0 && !host.startsWith("[")) host = "[" + host + "]";
        boolean zadaniPort = ("http".equals(scheme) && port == 80) || ("https".equals(scheme) && port == 443);
        String vlastiti = scheme + "://" + host + (zadaniPort ? "" : ":" + port);
        return vlastiti.equals(origin);
    }

    // M09: Izdvajanje tokena dostupno je i odjavi, kako bi opozvala isti token kao autentifikacija.
    static String izZaglavlja(HttpServletRequest request) {
        String zaglavlje = request.getHeader(HttpHeaders.AUTHORIZATION);
        if (zaglavlje != null && zaglavlje.regionMatches(true, 0, "Bearer ", 0, 7)) {
            String token = zaglavlje.substring(7).trim();
            return token.isEmpty() ? null : token;
        }
        return null;
    }

    // M09: Odjava prepoznaje i sesiju predanu kolačićem.
    static String izKolacica(HttpServletRequest request) {
        Cookie[] kolacici = request.getCookies();
        if (kolacici == null) {
            return null;
        }
        for (Cookie kolacic : kolacici) {
            if (KOLACIC.equals(kolacic.getName()) && !kolacic.getValue().isBlank()) {
                return kolacic.getValue();
            }
        }
        return null;
    }
}

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

    public AuthFilter(TokenStore tokeni) {
        this.tokeni = tokeni;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        String token = izZaglavlja(request);
        if (token == null) {
            token = izKolacica(request);
        }
        PrijavljeniKorisnik korisnik = tokeni.korisnik(token);
        if (korisnik != null) {
            request.setAttribute(ATRIBUT, korisnik);
        }
        chain.doFilter(request, response);
    }

    private static String izZaglavlja(HttpServletRequest request) {
        String zaglavlje = request.getHeader(HttpHeaders.AUTHORIZATION);
        if (zaglavlje != null && zaglavlje.regionMatches(true, 0, "Bearer ", 0, 7)) {
            String token = zaglavlje.substring(7).trim();
            return token.isEmpty() ? null : token;
        }
        return null;
    }

    private static String izKolacica(HttpServletRequest request) {
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

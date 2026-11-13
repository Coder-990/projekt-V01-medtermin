package hr.span.akademija.medtermin.platforma;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * CORS za frontend aplikaciju (npr. Vite na {@code http://localhost:5173}) koja API poziva s kolačićem
 * ili tokenom. Odgovara i na preflight zahtjeve ({@code OPTIONS}).
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class CorsFilter extends OncePerRequestFilter {

    private static final String METODE = "GET, POST, PUT, DELETE, OPTIONS";
    private static final String ZAGLAVLJA = "Authorization, Content-Type";

    private final List<String> dopusteniOrigini;

    public CorsFilter(AppProperties postavke) {
        this.dopusteniOrigini = postavke.dopusteniOrigini();
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        String origin = request.getHeader(HttpHeaders.ORIGIN);
        boolean preflight = HttpMethod.OPTIONS.matches(request.getMethod())
                && request.getHeader(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD) != null;
        if (origin == null) {
            chain.doFilter(request, response);
            return;
        }
        response.addHeader(HttpHeaders.VARY, HttpHeaders.ORIGIN);
        if (dopusten(origin)) {
            response.setHeader(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN, origin);
            response.setHeader(HttpHeaders.ACCESS_CONTROL_ALLOW_CREDENTIALS, "true");
            if (preflight) {
                response.setHeader(HttpHeaders.ACCESS_CONTROL_ALLOW_METHODS, METODE);
                response.setHeader(HttpHeaders.ACCESS_CONTROL_ALLOW_HEADERS, ZAGLAVLJA);
                response.setHeader(HttpHeaders.ACCESS_CONTROL_MAX_AGE, "3600");
                response.setStatus(HttpServletResponse.SC_NO_CONTENT);
                return;
            }
        } else if (preflight) {
            response.setStatus(HttpServletResponse.SC_FORBIDDEN);
            return;
        }
        chain.doFilter(request, response);
    }

    private boolean dopusten(String origin) {
        // TODO(sec): M07 [rucni pregled]: Koristiti tocnu konfiguriranu allowlistu origin-a; sada su svi HTTP(S) origini dopusteni uz credentials. Iskoristivost ovisi i o cookie/SameSite pravilima. Izvor/detalji: alati/poslije/pregled-koda.md.
        return origin.startsWith("http://") || origin.startsWith("https://");
    }
}

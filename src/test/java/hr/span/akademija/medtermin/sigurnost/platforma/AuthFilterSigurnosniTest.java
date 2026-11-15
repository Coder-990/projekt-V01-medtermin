package hr.span.akademija.medtermin.platforma;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

import jakarta.servlet.FilterChain;
import jakarta.servlet.http.Cookie;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

class AuthFilterSigurnosniTest {
    private void provjeri(String metoda, String shema, String host, int port, String origin, int status)
            throws Exception {
        TokenStore store = mock(TokenStore.class);
        when(store.korisnik("token")).thenReturn(new PrijavljeniKorisnik(1, "ana", "KORISNIK"));
        AuthFilter filter = new AuthFilter(store);
        filter.postaviPostavke(new AppProperties("test", "tmp", "tmp", new AppProperties.Cors(List.of("https://frontend.example"))));
        MockHttpServletRequest zahtjev = new MockHttpServletRequest(metoda, "/api/postavke");
        zahtjev.setScheme(shema);
        zahtjev.setServerName(host);
        zahtjev.setServerPort(port);
        zahtjev.setCookies(new Cookie("SESSION", "token"));
        if (origin != null) zahtjev.addHeader("Origin", origin);
        MockHttpServletResponse odgovor = new MockHttpServletResponse();
        FilterChain lanac = mock(FilterChain.class);
        filter.doFilter(zahtjev, odgovor, lanac);
        assertThat(odgovor.getStatus()).isEqualTo(status);
        if (status == 200) verify(lanac).doFilter(zahtjev, odgovor);
        else verifyNoInteractions(lanac);
    }

    @Test void provjeravaSigurneMetodeIPouzdaneOrigine() throws Exception {
        provjeri("HEAD", "http", "localhost", 80, null, 200);
        provjeri("OPTIONS", "http", "localhost", 80, null, 200);
        provjeri("POST", "http", "localhost", 80, " ", 403);
        provjeri("POST", "http", "localhost", 80, "https://frontend.example", 200);
        provjeri("POST", "http", "localhost", 80, "http://localhost", 200);
        provjeri("POST", "https", "localhost", 443, "https://localhost", 200);
        provjeri("POST", "https", "localhost", 8443, "https://localhost:8443", 200);
        provjeri("POST", "http", "::1", 80, "http://[::1]", 200);
        provjeri("POST", "http", "[::1]", 80, "http://[::1]", 200);
        provjeri("POST", "https", "localhost", 443, "https://drugi.example", 403);
    }
}

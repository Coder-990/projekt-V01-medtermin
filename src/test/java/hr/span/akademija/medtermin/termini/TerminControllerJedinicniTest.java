package hr.span.akademija.medtermin.termini;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import hr.span.akademija.medtermin.platforma.ApiException;
import hr.span.akademija.medtermin.platforma.PrijavljeniKorisnik;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;

class TerminControllerJedinicniTest {

    @Test
    @DisplayName("TerminController uvezi baca ApiException kad queryForObject vrati null")
    void terminControllerNullLijecnik() {
        JdbcTemplate jdbc = mock(JdbcTemplate.class);
        UvozTermina uvoz = mock(UvozTermina.class);
        TerminController controller = new TerminController(jdbc, uvoz);

        when(uvoz.procitaj(any())).thenReturn(
                List.of(new UvozTermina.Stavka(1L, LocalDateTime.now(), 5, "Napomena")));
        when(jdbc.queryForObject(anyString(), eq(Integer.class), eq(1L))).thenReturn(null);

        PrijavljeniKorisnik korisnik = new PrijavljeniKorisnik(1L, "dr.horvat", "LIJECNIK");
        assertThatThrownBy(() -> controller.uvezi(new byte[0], korisnik))
                .isInstanceOf(ApiException.class)
                .hasMessage("Nepoznat liječnik: 1");
    }
}

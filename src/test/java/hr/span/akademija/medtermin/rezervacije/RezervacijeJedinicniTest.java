package hr.span.akademija.medtermin.rezervacije;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import hr.span.akademija.medtermin.platforma.ApiException;
import hr.span.akademija.medtermin.platforma.AppProperties;
import hr.span.akademija.medtermin.platforma.PrijavljeniKorisnik;
import hr.span.akademija.medtermin.rezervacije.RezervacijaService.NovaRezervacija;
import hr.span.akademija.medtermin.rezervacije.RezervacijaService.Rezervacija;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;

class RezervacijeJedinicniTest {

    @Test
    @DisplayName("BolnickiSustavKlijent baca IllegalStateException na prekinutu dretvu")
    void bolnickiSustavKlijentPrekid() {
        BolnickiSustavKlijent klijent = new BolnickiSustavKlijent();
        Thread.currentThread().interrupt();

        assertThatThrownBy(() -> klijent.upisiURaspored(1L, "ana"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Poziv prema BIS-u je prekinut");

        // Očisti interrupted status
        Thread.interrupted();
    }

    @Test
    @DisplayName("RezervacijaService vraća zauzeto mjesto pri padu BIS-a")
    void rezervacijaServiceRollback() {
        JdbcTemplate jdbc = mock(JdbcTemplate.class);
        BolnickiSustavKlijent bis = mock(BolnickiSustavKlijent.class);
        RezervacijaService service = new RezervacijaService(jdbc, bis);

        PrijavljeniKorisnik korisnik = new PrijavljeniKorisnik(1L, "ana", "KORISNIK");
        NovaRezervacija zahtjev = new NovaRezervacija(10L, "Napomena", "48291736504");

        // Termin postoji
        when(jdbc.queryForObject(anyString(), eq(Integer.class), eq(10L))).thenReturn(1);
        // Uvećanje broja zauzetih uspijeva
        when(jdbc.update(anyString(), eq(10L))).thenReturn(1);
        // BIS poziv baca RuntimeException
        when(bis.upisiURaspored(10L, "ana")).thenThrow(new RuntimeException("BIS je nedostupan"));

        assertThatThrownBy(() -> service.kreiraj(zahtjev, korisnik))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("BIS je nedostupan");

        // Provjeri da je pozvan UPDATE za dekrement zauzeto
        verify(jdbc).update("UPDATE termini SET zauzeto = zauzeto - 1 WHERE id = ?", 10L);
    }

    @Test
    @DisplayName("RezervacijaService kreiraj s null zahtjevom baca ApiException")
    void rezervacijaServiceNullZahtjev() {
        JdbcTemplate jdbc = mock(JdbcTemplate.class);
        BolnickiSustavKlijent bis = mock(BolnickiSustavKlijent.class);
        RezervacijaService service = new RezervacijaService(jdbc, bis);

        PrijavljeniKorisnik korisnik = new PrijavljeniKorisnik(1L, "ana", "KORISNIK");

        assertThatThrownBy(() -> service.kreiraj(null, korisnik))
                .isInstanceOf(ApiException.class)
                .hasMessage("Termin je obavezan.");
    }

    @Test
    @DisplayName("RezervacijaService kreiraj kad queryForObject vrati null za postojanje termina")
    void rezervacijaServiceTerminNullPostoji() {
        JdbcTemplate jdbc = mock(JdbcTemplate.class);
        BolnickiSustavKlijent bis = mock(BolnickiSustavKlijent.class);
        RezervacijaService service = new RezervacijaService(jdbc, bis);

        PrijavljeniKorisnik korisnik = new PrijavljeniKorisnik(1L, "ana", "KORISNIK");
        NovaRezervacija zahtjev = new NovaRezervacija(10L, "Napomena", "48291736504");

        when(jdbc.queryForObject(anyString(), eq(Integer.class), eq(10L))).thenReturn(null);

        assertThatThrownBy(() -> service.kreiraj(zahtjev, korisnik))
                .isInstanceOf(ApiException.class)
                .hasMessage("Termin nije pronađen.");
    }

    @Test
    @DisplayName("RezervacijaController kreiraj baca 404 ako se stvorena rezervacija ne pronađe")
    void rezervacijaControllerKreirajNepronadeno() {
        RezervacijaService service = mock(RezervacijaService.class);
        RezervacijaController controller = new RezervacijaController(service);

        PrijavljeniKorisnik korisnik = new PrijavljeniKorisnik(1L, "ana", "KORISNIK");
        NovaRezervacija zahtjev = new NovaRezervacija(10L, "Napomena", "48291736504");

        when(service.kreiraj(zahtjev, korisnik)).thenReturn(999L);
        when(service.pronadi(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> controller.kreiraj(zahtjev, korisnik))
                .isInstanceOf(ApiException.class)
                .hasMessage("Rezervacija nije pronađena.");
    }

    @Test
    @DisplayName("IspisController potvrda kad su pocetak i napomena null")
    void ispisControllerNullPolja() {
        RezervacijaService service = mock(RezervacijaService.class);
        AppProperties postavke = new AppProperties("MedTermin", "dir", "dir", null);
        IspisController controller = new IspisController(service, postavke);

        PrijavljeniKorisnik korisnik = new PrijavljeniKorisnik(1L, "ana", "KORISNIK");
        Rezervacija r = new Rezervacija(1L, 10L, "ana", "dr. Horvat", null, "BIS-1", null);

        when(service.pronadi(1L)).thenReturn(Optional.of(r));

        ResponseEntity<String> odgovor = controller.potvrda(1L, korisnik);

        assertThat(odgovor.getStatusCode().value()).isEqualTo(200);
        assertThat(odgovor.getBody()).contains("Potvrda rezervacije br. 1");
    }
}

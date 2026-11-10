package hr.span.akademija.medtermin.rezervacije;

import hr.span.akademija.medtermin.platforma.ApiException;
import hr.span.akademija.medtermin.platforma.PrijavljeniKorisnik;
import hr.span.akademija.medtermin.rezervacije.RezervacijaService.NovaRezervacija;
import hr.span.akademija.medtermin.rezervacije.RezervacijaService.Rezervacija;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/rezervacije")
public class RezervacijaController {

    private final RezervacijaService rezervacije;

    public RezervacijaController(RezervacijaService rezervacije) {
        this.rezervacije = rezervacije;
    }

    @PostMapping
    public ResponseEntity<Rezervacija> kreiraj(@RequestBody NovaRezervacija zahtjev, PrijavljeniKorisnik korisnik) {
        long id = rezervacije.kreiraj(zahtjev, korisnik);
        Rezervacija rezervacija = rezervacije.pronadi(id)
                .orElseThrow(() -> ApiException.nijePronadeno("Rezervacija nije pronađena."));
        return ResponseEntity.status(HttpStatus.CREATED).body(rezervacija);
    }

    @GetMapping("/moje")
    public List<Rezervacija> moje(PrijavljeniKorisnik korisnik) {
        return rezervacije.rezervacijeKorisnika(korisnik.username());
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> obrisi(@PathVariable long id, PrijavljeniKorisnik korisnik) {
        rezervacije.obrisi(id, korisnik);
        return ResponseEntity.noContent().build();
    }
}

package hr.span.akademija.medtermin.platforma;

import hr.span.akademija.medtermin.platforma.KorisnikService.Korisnik;
import hr.span.akademija.medtermin.platforma.KorisnikService.PrijavaZahtjev;
import hr.span.akademija.medtermin.platforma.KorisnikService.RegistracijaZahtjev;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
public class AuthController {

    private final KorisnikService korisnici;
    private final TokenStore tokeni;
    private final ResetLozinkeService reset;

    public AuthController(KorisnikService korisnici, TokenStore tokeni, ResetLozinkeService reset) {
        this.korisnici = korisnici;
        this.tokeni = tokeni;
        this.reset = reset;
    }

    public record ResetZahtjev(String username) {
    }

    public record NovaLozinka(String token, String novaLozinka) {
    }

    @GetMapping("/health")
    public Map<String, String> health() {
        return Map.of("status", "UP");
    }

    @PostMapping("/auth/login")
    public ResponseEntity<Map<String, String>> login(@RequestBody PrijavaZahtjev zahtjev) {
        Optional<Korisnik> korisnik = korisnici.prijava(zahtjev);
        if (korisnik.isEmpty()) {
            return GreskeHandler.greska(HttpStatus.UNAUTHORIZED, "Neispravno korisničko ime ili lozinka.");
        }
        Korisnik k = korisnik.get();
        String token = tokeni.izdaj(k.prijavljeni());
        return ResponseEntity.ok(Map.of("token", token, "username", k.username(), "uloga", k.uloga()));
    }

    @PostMapping("/auth/register")
    public ResponseEntity<Map<String, Object>> register(@RequestBody RegistracijaZahtjev zahtjev) {
        Korisnik k = korisnici.registriraj(zahtjev);
        Map<String, Object> tijelo = new LinkedHashMap<>();
        tijelo.put("id", k.id());
        tijelo.put("username", k.username());
        return ResponseEntity.status(HttpStatus.CREATED).body(tijelo);
    }

    @PostMapping("/auth/reset-zahtjev")
    public ResponseEntity<Map<String, String>> resetZahtjev(@RequestBody ResetZahtjev zahtjev) {
        if (zahtjev != null && zahtjev.username() != null && !zahtjev.username().isBlank()) {
            reset.zatrazi(zahtjev.username().trim());
        }
        return ResponseEntity.status(HttpStatus.ACCEPTED)
                .body(Map.of("poruka", "Ako korisnik postoji, poslali smo upute na e-mail."));
    }

    @PostMapping("/auth/reset")
    public ResponseEntity<Map<String, String>> reset(@RequestBody NovaLozinka zahtjev) {
        if (zahtjev == null || !reset.postaviNovuLozinku(zahtjev.token(), zahtjev.novaLozinka())) {
            return GreskeHandler.greska(HttpStatus.BAD_REQUEST, "Token nije valjan ili je istekao.");
        }
        return ResponseEntity.ok(Map.of("poruka", "Lozinka je promijenjena."));
    }

    @GetMapping("/ja")
    public Map<String, Object> ja(PrijavljeniKorisnik prijavljeni) {
        Korisnik k = korisnici.pronadi(prijavljeni.username())
                .orElseThrow(() -> new ApiException(HttpStatus.UNAUTHORIZED, "Potrebna je prijava."));
        Map<String, Object> podaci = new LinkedHashMap<>();
        podaci.put("username", k.username());
        podaci.put("ime", k.ime());
        podaci.put("prezime", k.prezime());
        podaci.put("email", k.email());
        podaci.put("uloga", k.uloga());
        return podaci;
    }
}

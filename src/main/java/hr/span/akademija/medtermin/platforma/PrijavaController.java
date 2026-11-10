package hr.span.akademija.medtermin.platforma;

import hr.span.akademija.medtermin.platforma.KorisnikService.Korisnik;
import hr.span.akademija.medtermin.platforma.KorisnikService.PrijavaZahtjev;
import java.util.Optional;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

/** HTML stranice: početna, prijava obrascem (postavlja kolačić SESSION) i promjena lozinke tokenom. */
@Controller
public class PrijavaController {

    private final KorisnikService korisnici;
    private final TokenStore tokeni;
    private final ResetLozinkeService reset;
    private final AppProperties postavke;

    public PrijavaController(KorisnikService korisnici, TokenStore tokeni, ResetLozinkeService reset,
                             AppProperties postavke) {
        this.korisnici = korisnici;
        this.tokeni = tokeni;
        this.reset = reset;
        this.postavke = postavke;
    }

    @GetMapping("/")
    public ResponseEntity<String> pocetna() {
        String tijelo = """
                <h1>%s</h1>
                <p>Naručivanje pregleda, pregled nalaza i ispis potvrda.</p>
                <ul>
                <li><a href="/prijava">Prijava</a></li>
                <li><a href="/api/health">Stanje sustava</a></li>
                </ul>
                """.formatted(Html.escape(postavke.naziv()));
        return Html.odgovor(Html.stranica(postavke.naziv(), tijelo));
    }

    @GetMapping("/prijava")
    public ResponseEntity<String> obrazac(@RequestParam(required = false) String returnUrl) {
        return Html.odgovor(obrazacPrijave(returnUrl, null));
    }

    @PostMapping("/prijava")
    public ResponseEntity<String> prijava(@RequestParam(required = false) String username,
                                          @RequestParam(required = false) String password,
                                          @RequestParam(required = false) String returnUrl) {
        Optional<Korisnik> korisnik = korisnici.prijava(new PrijavaZahtjev(username, password));
        if (korisnik.isEmpty()) {
            return Html.odgovor(obrazacPrijave(returnUrl, "Neispravno korisničko ime ili lozinka."));
        }
        String token = tokeni.izdaj(korisnik.get().prijavljeni());
        ResponseCookie kolacic = ResponseCookie.from(AuthFilter.KOLACIC, token)
                .httpOnly(true)
                .sameSite("Lax")
                .path("/")
                .build();
        return ResponseEntity.status(HttpStatus.FOUND)
                .header(HttpHeaders.LOCATION, odrediste(returnUrl))
                .header(HttpHeaders.SET_COOKIE, kolacic.toString())
                .build();
    }

    @GetMapping("/reset")
    public ResponseEntity<String> obrazacZaLozinku(@RequestParam(required = false) String token) {
        return Html.odgovor(obrazacNoveLozinke(token, null));
    }

    @PostMapping("/reset")
    public ResponseEntity<String> novaLozinka(@RequestParam(required = false) String token,
                                              @RequestParam(required = false) String novaLozinka) {
        boolean promijenjena;
        try {
            promijenjena = reset.postaviNovuLozinku(token, novaLozinka);
        } catch (ApiException e) {
            return Html.odgovor(obrazacNoveLozinke(token, e.getMessage()));
        }
        if (!promijenjena) {
            return Html.odgovor(obrazacNoveLozinke(token, "Token nije valjan ili je istekao."));
        }
        return Html.odgovor(Html.stranica("Lozinka je promijenjena",
                "<h1>Lozinka je promijenjena</h1>\n<p><a href=\"/prijava\">Prijava</a></p>"));
    }

    private String obrazacPrijave(String returnUrl, String poruka) {
        String tijelo = """
                <h1>Prijava</h1>
                %s
                <form method="post" action="/prijava">
                <label>Korisničko ime <input name="username" autocomplete="username" required></label>
                <label>Lozinka <input name="password" type="password" autocomplete="current-password" required></label>
                <input type="hidden" name="returnUrl" value="%s">
                <p><button type="submit">Prijavi se</button></p>
                </form>
                """.formatted(poruka == null ? "" : "<p class=\"poruka\">" + Html.escape(poruka) + "</p>",
                Html.escape(returnUrl));
        return Html.stranica("Prijava – " + postavke.naziv(), tijelo);
    }

    private String obrazacNoveLozinke(String token, String poruka) {
        String tijelo = """
                <h1>Nova lozinka</h1>
                %s
                <form method="post" action="/reset">
                <input type="hidden" name="token" value="%s">
                <label>Nova lozinka <input name="novaLozinka" type="password" autocomplete="new-password" required></label>
                <p><button type="submit">Spremi</button></p>
                </form>
                """.formatted(poruka == null ? "" : "<p class=\"poruka\">" + Html.escape(poruka) + "</p>",
                Html.escape(token));
        return Html.stranica("Nova lozinka – " + postavke.naziv(), tijelo);
    }

    private static String odrediste(String returnUrl) {
        if (returnUrl == null || returnUrl.isBlank()) {
            return "/";
        }
        return returnUrl;
    }
}

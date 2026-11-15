package hr.span.akademija.medtermin.platforma;

import hr.span.akademija.medtermin.platforma.KorisnikService.Korisnik;
import hr.span.akademija.medtermin.platforma.KorisnikService.PrijavaZahtjev;
import java.util.Optional;
import org.springframework.beans.factory.annotation.Value;
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
    @Value("${app.cookie-secure:true}")
    private boolean secureCookie;

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
                <h1>%s</h1>%n\
                <p>Naručivanje pregleda, pregled nalaza i ispis potvrda.</p>%n\
                <ul>%n\
                <li><a href="/prijava">Prijava</a></li>%n\
                <li><a href="/api/health">Stanje sustava</a></li>%n\
                </ul>%n\
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
        // Izvorni nalaz ispod opisuje stanje prije ispravka; objašnjenje promjene slijedi.
        // TODO(sec): M10 [rucni pregled]: Za HTTPS produkciju predloziti Secure cookie; HttpOnly/SameSite vec postoje. Uskladiti s TLS terminacijom i lokalnim HTTP razvojem. Izvor/detalji: alati/poslije/pregled-koda.md.
        // M10: Secure sprečava slanje kolačića preko običnog HTTP-a; HttpOnly i
        // SameSite=Lax ostaju uključeni. app.cookie-secure zadano je true, a za
        // lokalni HTTP razvoj APP_COOKIE_SECURE=false. Produkcija treba HTTPS i true;
        // postavku treba uskladiti s načinom na koji se TLS završava u infrastrukturi.
        ResponseCookie kolacic = ResponseCookie.from(AuthFilter.KOLACIC, token)
                .secure(secureCookie)
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
                <h1>Prijava</h1>%n\
                %s%n\
                <form method="post" action="/prijava">%n\
                <label>Korisničko ime <input name="username" autocomplete="username" required></label>%n\
                <label>Lozinka <input name="password" type="password" autocomplete="current-password" required></label>%n\
                <input type="hidden" name="returnUrl" value="%s">%n\
                <p><button type="submit">Prijavi se</button></p>%n\
                </form>%n\
                """.formatted(poruka == null ? "" : "<p class=\"poruka\">" + Html.escape(poruka) + "</p>",
                Html.escape(returnUrl));
        return Html.stranica("Prijava – " + postavke.naziv(), tijelo);
    }

    private String obrazacNoveLozinke(String token, String poruka) {
        String tijelo = """
                <h1>Nova lozinka</h1>%n\
                %s%n\
                <form method="post" action="/reset">%n\
                <input type="hidden" name="token" value="%s">%n\
                <label>Nova lozinka <input name="novaLozinka" type="password" autocomplete="new-password" required></label>%n\
                <p><button type="submit">Spremi</button></p>%n\
                </form>%n\
                """.formatted(poruka == null ? "" : "<p class=\"poruka\">" + Html.escape(poruka) + "</p>",
                Html.escape(token));
        return Html.stranica("Nova lozinka – " + postavke.naziv(), tijelo);
    }

    private static String odrediste(String returnUrl) {
        if (returnUrl == null || returnUrl.isBlank()) {
            return "/";
        }
        // Izvorni nalaz ispod opisuje stanje prije ispravka; objašnjenje promjene slijedi.
        // TODO(sec): M08 [rucni pregled]: Otvoreno preusmjeravanje; prihvatiti samo provjerene lokalne putanje i odbiti apsolutne URL-ove i //host. Izvor/detalji: alati/poslije/pregled-koda.md.
        // M08: Ranije je returnUrl bez provjere postao Location zaglavlje, što je
        // omogućavalo preusmjeravanje na vanjsku stranicu. Prihvaćamo samo putanju
        // koja počinje jednim '/', bez obrnute kose crte, kontrolnih i '%' znakova.
        // Sumnjive vrijednosti vraćaju korisnika na '/'; lokalni put poput /moji radi.
        if (!returnUrl.startsWith("/") || returnUrl.startsWith("//")
                || returnUrl.indexOf('\\') >= 0 || returnUrl.chars().anyMatch(Character::isISOControl)
                || returnUrl.indexOf('%') >= 0) {
            return "/";
        }
        return returnUrl;
    }
}

package hr.span.akademija.medtermin.rezervacije;

import hr.span.akademija.medtermin.platforma.ApiException;
import hr.span.akademija.medtermin.platforma.AppProperties;
import hr.span.akademija.medtermin.platforma.Html;
import hr.span.akademija.medtermin.platforma.PrijavljeniKorisnik;
import hr.span.akademija.medtermin.rezervacije.RezervacijaService.Rezervacija;
import java.time.format.DateTimeFormatter;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.util.HtmlUtils;

/** Potvrda rezervacije za ispis (HTML). */
@Controller
public class IspisController {

    private static final DateTimeFormatter VRIJEME = DateTimeFormatter.ofPattern("dd.MM.yyyy. 'u' HH:mm");

    private final RezervacijaService rezervacije;
    private final AppProperties postavke;

    public IspisController(RezervacijaService rezervacije, AppProperties postavke) {
        this.rezervacije = rezervacije;
        this.postavke = postavke;
    }

    @GetMapping("/ispis/rezervacije/{id}")
    public ResponseEntity<String> potvrda(@PathVariable long id, PrijavljeniKorisnik korisnik) {
        Rezervacija r = rezervacije.pronadi(id)
                .filter(x -> korisnik.isAdmin() || x.pacijent().equals(korisnik.username()))
                .orElseThrow(() -> ApiException.nijePronadeno("Rezervacija nije pronađena."));
        String html = """
                <!DOCTYPE html>%n\
                <html lang="hr">%n\
                <head>%n\
                <meta charset="UTF-8">%n\
                <title>Potvrda rezervacije br. %d</title>%n\
                <style>%n\
                body { font-family: system-ui, sans-serif; max-width: 40rem; margin: 2rem auto; }%n\
                dt { font-weight: bold; margin-top: .5rem; }%n\
                </style>%n\
                </head>%n\
                <body>%n\
                <h1>%s</h1>%n\
                <h2>Potvrda rezervacije br. %d</h2>%n\
                <dl>%n\
                <dt>Pacijent</dt><dd>%s</dd>%n\
                <dt>Liječnik</dt><dd>%s</dd>%n\
                <dt>Termin</dt><dd>%s</dd>%n\
                <dt>Napomena</dt><dd>%s</dd>%n\
                </dl>%n\
                <p>Molimo dođite 10 minuta prije termina i ponesite iskaznicu zdravstvenog osiguranja.</p>%n\
                </body>%n\
                </html>%n\
                """.formatted(r.id(), tekst(postavke.naziv()), r.id(), tekst(r.pacijent()), tekst(r.lijecnik()),
                r.pocetak() == null ? "" : VRIJEME.format(r.pocetak()), tekst(r.napomena()));
        return ResponseEntity.ok()
                .contentType(Html.TEXT_HTML_UTF8)
                .header("Content-Security-Policy", "default-src 'none'; style-src 'unsafe-inline'")
                .body(html);
    }

    private static String tekst(String vrijednost) {
        return vrijednost == null ? "" : HtmlUtils.htmlEscape(vrijednost, "UTF-8");
    }
}

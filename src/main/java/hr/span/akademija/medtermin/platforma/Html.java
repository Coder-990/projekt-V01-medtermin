package hr.span.akademija.medtermin.platforma;

import java.nio.charset.StandardCharsets;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.util.HtmlUtils;

/** Jednostavne HTML stranice (početna, prijava, promjena lozinke). */
public final class Html {

    public static final MediaType TEXT_HTML_UTF8 = new MediaType("text", "html", StandardCharsets.UTF_8);

    private Html() {
    }

    public static String escape(String tekst) {
        return tekst == null ? "" : HtmlUtils.htmlEscape(tekst, "UTF-8");
    }

    public static String stranica(String naslov, String tijelo) {
        return """
                <!DOCTYPE html>%n\
                <html lang="hr">%n\
                <head>%n\
                <meta charset="UTF-8">%n\
                <title>%s</title>%n\
                <style>%n\
                body { font-family: system-ui, sans-serif; max-width: 40rem; margin: 2rem auto; padding: 0 1rem; }%n\
                label { display: block; margin-top: .75rem; }%n\
                .poruka { color: #b00020; }%n\
                </style>%n\
                </head>%n\
                <body>%n\
                %s%n\
                </body>%n\
                </html>%n\
                """.formatted(escape(naslov), tijelo);
    }

    public static ResponseEntity<String> odgovor(String html) {
        // CSP ograničava učitavanje vanjskih sadržaja i slanje obrazaca na vlastiti
        // origin; inline stil je potreban postojećim stranicama bez JavaScripta.
        return ResponseEntity.ok().contentType(TEXT_HTML_UTF8)
                .header("Content-Security-Policy", "default-src 'none'; style-src 'unsafe-inline'; form-action 'self'; base-uri 'none'; frame-ancestors 'none'")
                .body(html);
    }
}

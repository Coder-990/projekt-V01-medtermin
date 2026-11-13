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
        // TODO(sec): spotbugs VA_FORMAT_STRING_USES_NEWLINE [BAD_PRACTICE; provjeriti nalaz]: Format string should use %n rather than \n in hr.span.akademija.medtermin.platforma.Html.stranica(String, String)
        return """
                <!DOCTYPE html>
                <html lang="hr">
                <head>
                <meta charset="UTF-8">
                <title>%s</title>
                <style>
                body { font-family: system-ui, sans-serif; max-width: 40rem; margin: 2rem auto; padding: 0 1rem; }
                label { display: block; margin-top: .75rem; }
                .poruka { color: #b00020; }
                </style>
                </head>
                <body>
                %s
                </body>
                </html>
                """.formatted(escape(naslov), tijelo);
    }

    public static ResponseEntity<String> odgovor(String html) {
        return ResponseEntity.ok().contentType(TEXT_HTML_UTF8).body(html);
    }
}

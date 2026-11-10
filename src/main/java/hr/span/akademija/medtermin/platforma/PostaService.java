package hr.span.akademija.medtermin.platforma;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Service;

/**
 * Slanje e-pošte. U lokalnom okruženju poruka se ne šalje nego se zapisuje kao tekstualna datoteka
 * u mapu {@code app.mailbox-dir}.
 */
@Service
public class PostaService {

    private static final Logger log = LoggerFactory.getLogger(PostaService.class);
    private static final DateTimeFormatter OZNAKA_VREMENA = DateTimeFormatter.ofPattern("yyyyMMddHHmmssSSS");

    private final Pohrana pohrana;
    private final AppProperties postavke;
    private final Environment okruzenje;

    public PostaService(Pohrana pohrana, AppProperties postavke, Environment okruzenje) {
        this.pohrana = pohrana;
        this.postavke = postavke;
        this.okruzenje = okruzenje;
    }

    public void posaljiUputeZaLozinku(KorisnikService.Korisnik korisnik, String token) {
        String port = okruzenje.getProperty("local.server.port", okruzenje.getProperty("server.port", "8080"));
        String poveznica = "http://localhost:" + port + "/reset?token=" + URLEncoder.encode(token, StandardCharsets.UTF_8);
        String poruka = """
                Za: %s
                Predmet: %s – promjena lozinke

                Poštovani,

                zaprimili smo zahtjev za promjenu lozinke za korisnički račun %s.
                Novu lozinku možete postaviti na poveznici:
                %s

                Token: %s

                Ako niste zatražili promjenu lozinke, zanemarite ovu poruku.
                """.formatted(korisnik.email(), postavke.naziv(), korisnik.username(), poveznica, token);
        Path datoteka = pohrana.posta()
                .resolve(korisnik.username() + "-" + LocalDateTime.now().format(OZNAKA_VREMENA) + ".txt");
        try {
            Files.createDirectories(pohrana.posta());
            Files.writeString(datoteka, poruka, StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
        log.info("Poslana poruka za promjenu lozinke korisniku {}", korisnik.username());
    }
}

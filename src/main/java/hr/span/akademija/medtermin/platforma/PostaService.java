package hr.span.akademija.medtermin.platforma;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.PosixFilePermission;
import java.nio.file.attribute.PosixFilePermissions;
import java.util.Set;
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
    private static final Set<PosixFilePermission> PRIVATNE_DOZVOLE =
            PosixFilePermissions.fromString("rw-------");

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
                Za: %s%n\
                Predmet: %s – promjena lozinke%n\
                %n\
                Poštovani,%n\
                %n\
                zaprimili smo zahtjev za promjenu lozinke za korisnički račun %s.%n\
                Novu lozinku možete postaviti na poveznici:%n\
                %s%n\
                %n\
                Token: %s%n\
                %n\
                Ako niste zatražili promjenu lozinke, zanemarite ovu poruku.%n\
                """.formatted(korisnik.email(), postavke.naziv(), korisnik.username(), poveznica, token);
        try {
            Path mapa = pohrana.posta();
            // Datoteke s reset tokenima služe samo za lokalni razvoj. Ne mijenjamo
            // postojeće dozvole: odbijamo nesigurnu ili simbolički povezanu mapu.
            Files.createDirectories(mapa, PosixFilePermissions.asFileAttribute(
                    PosixFilePermissions.fromString("rwx------")));
            if (Files.isSymbolicLink(mapa) || !Files.getFileStore(mapa).supportsFileAttributeView("posix")) {
                throw new IOException("Lokalna pošta zahtijeva privatnu POSIX mapu bez simboličke poveznice.");
            }
            Set<PosixFilePermission> dozvoleMape = Files.getPosixFilePermissions(mapa);
            if (dozvoleMape.stream().anyMatch(d -> d.name().startsWith("GROUP_") || d.name().startsWith("OTHERS_"))) {
                throw new IOException("Mapa pošte dostupna je drugim korisnicima.");
            }
            if (!korisnik.username().matches("[A-Za-z0-9._-]{3,50}")) {
                throw new IOException("Neispravno korisničko ime za lokalnu poštu.");
            }
            // CREATE_NEW putem createTempFile sprječava koliziju dvaju zahtjeva;
            // token se stvara s pravima 0600, bez čitanja za grupu i ostale.
            Path datoteka = Files.createTempFile(mapa, korisnik.username() + "-", ".txt",
                    PosixFilePermissions.asFileAttribute(PRIVATNE_DOZVOLE));
            Files.writeString(datoteka, poruka, StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
        // CRLF_INJECTION_LOGS: ne zapisujemo ime iz baze; token i sadržaj poruke
        // nikada ne izlaze u aplikacijski log.
        log.info("Pripremljena lokalna poruka za promjenu lozinke");
    }
}

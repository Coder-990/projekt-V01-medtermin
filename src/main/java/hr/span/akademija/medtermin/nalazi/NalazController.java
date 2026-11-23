package hr.span.akademija.medtermin.nalazi;

import hr.span.akademija.medtermin.platforma.ApiException;
import hr.span.akademija.medtermin.platforma.Pohrana;
import hr.span.akademija.medtermin.platforma.PrijavljeniKorisnik;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.DirectoryStream;
import java.nio.file.SecureDirectoryStream;
import java.nio.file.LinkOption;
import java.nio.file.OpenOption;
import java.nio.file.StandardOpenOption;
import java.nio.file.attribute.BasicFileAttributeView;
import java.nio.channels.Channels;
import java.util.Set;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/nalazi")
public class NalazController {

    private static final Logger log = LoggerFactory.getLogger(NalazController.class);
    private static final MediaType TEXT_PLAIN_UTF8 = new MediaType("text", "plain", StandardCharsets.UTF_8);

    public record Nalaz(long id, String pacijent, String naziv, String datoteka) {
    }

    public enum Format {
        JSON, PDF;

        static Optional<Format> procitaj(String vrijednost) {
            return Arrays.stream(values()).filter(f -> f.name().equalsIgnoreCase(vrijednost.trim())).findFirst();
        }
    }

    private static final RowMapper<Nalaz> MAPPER = (rs, i) -> new Nalaz(rs.getLong("id"), rs.getString("pacijent"),
            rs.getString("naziv"), rs.getString("datoteka"));

    private final JdbcTemplate jdbc;
    private final Pohrana pohrana;

    public NalazController(JdbcTemplate jdbc, Pohrana pohrana) {
        this.jdbc = jdbc;
        this.pohrana = pohrana;
    }

    @GetMapping("/moji")
    public List<Nalaz> moji(PrijavljeniKorisnik korisnik) {
        return jdbc.query("SELECT id, pacijent, naziv, datoteka FROM nalazi WHERE pacijent = ? ORDER BY id",
                MAPPER, korisnik.username());
    }

    /** Nalaz u JSON obliku (metapodaci i sadržaj) ili kao PDF: {@code GET /api/nalazi/1?format=pdf}. */
    @GetMapping("/{id}")
    public ResponseEntity<?> nalaz(@PathVariable long id, @RequestParam(required = false) String format,
                                   PrijavljeniKorisnik korisnik) throws IOException {
        Nalaz nalaz = pronadi(id).orElseThrow(() -> ApiException.nijePronadeno("Nalaz nije pronađen."));
        // M01: Ranije je provjera vlasnika bila unutar obrade formata; nepoznat format
        // preskočio ju je i vratio JSON tuđeg nalaza. Sada prije izbora JSON-a ili PDF-a
        // dopuštamo samo vlasnika ili administratora, a ostalima vraćamo 404.
        // Pokriveno u NalaziFunkcionalniTest.nevaljaniFormatVracaNalazUzNapomenu.
        if (!korisnik.isAdmin() && !nalaz.pacijent().equals(korisnik.username())) {
            throw ApiException.nijePronadeno("Nalaz nije pronađen.");
        }
        String trazeni = format == null ? "json" : format;
        Format odabrani = Format.JSON;
        String napomena = null;
        try {
            odabrani = Format.valueOf(trazeni.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            log.warn("Neuspjela obrada formata za nalaz {}", id);
            // M01/M06: Ne vraćamo niti logiramo tekst iznimke koji može sadržavati korisnički unos.
            napomena = "Format nije prepoznat, prikazuje se JSON.";
        }
        return prikazi(nalaz, odabrani, napomena);
    }

    /** Preuzimanje datoteke nalaza: {@code GET /api/nalazi/preuzmi?datoteka=nalaz-ana-krvna-slika.txt}. */
    @GetMapping("/preuzmi")
    public ResponseEntity<byte[]> preuzmi(@RequestParam String datoteka, PrijavljeniKorisnik korisnik)
            throws IOException {
        // M02: Naziv koji sadrži korisničko ime nije dokaz vlasništva: mogao bi biti
        // naziv tuđeg dokumenta. Zato tražimo zapis u tablici nalazi i za običnog
        // korisnika zahtijevamo podudaranje pacijenta; administrator može dohvatiti
        // i tuđi registrirani dokument. Nepostojeći ili nedopušten zapis vraća 404.
        Integer broj = korisnik.isAdmin()
                ? jdbc.queryForObject("SELECT COUNT(*) FROM nalazi WHERE datoteka = ?", Integer.class, datoteka)
                : jdbc.queryForObject("SELECT COUNT(*) FROM nalazi WHERE datoteka = ? AND pacijent = ?", Integer.class, datoteka, korisnik.username());
        if (broj == null || broj == 0) {
            throw ApiException.nijePronadeno("Datoteka nije pronađena.");
        }
        byte[] sadrzaj = procitajSigurno(datoteka);
        ContentDisposition privitak = ContentDisposition.attachment()
                .filename(datoteka, StandardCharsets.UTF_8)
                .build();
        return ResponseEntity.ok()
                .contentType(TEXT_PLAIN_UTF8)
                .header(HttpHeaders.CONTENT_DISPOSITION, privitak.toString())
                .body(sadrzaj);
    }

    // M02: Ime je jedna komponenta, bez apsolutne putanje ili podmapa. DirectoryStream
    // drži otvorenu mapu, a SecureDirectoryStream otvara datoteku relativno na taj
    // deskriptor uz NOFOLLOW_LINKS. Tako zamjena imena symlinkom između provjere i
    // čitanja ne može preusmjeriti čitanje izvan pohrane. Ako OS nema sigurni stream,
    // odbijamo čitanje umjesto povratka na nesigurnu provjeru pa otvaranje.
    private byte[] procitajSigurno(String naziv) throws IOException {
        Path relativna = Path.of(naziv);
        if (relativna.isAbsolute() || relativna.getNameCount() != 1
                || naziv.equals(".") || naziv.equals("..")) {
            throw ApiException.nijePronadeno("Datoteka nalaza nije pronađena.");
        }
        try (DirectoryStream<Path> mapa = Files.newDirectoryStream(pohrana.dokumenti())) {
            if (!(mapa instanceof SecureDirectoryStream<Path> sigurnaMapa)) {
                throw new IOException("Datotečni sustav ne podržava sigurno otvaranje dokumenata.");
            }
            BasicFileAttributeView pogled = sigurnaMapa.getFileAttributeView(relativna,
                    BasicFileAttributeView.class, LinkOption.NOFOLLOW_LINKS);
            if (!pogled.readAttributes().isRegularFile()) {
                throw ApiException.nijePronadeno("Datoteka nalaza nije pronađena.");
            }
            try (var kanal = sigurnaMapa.newByteChannel(relativna,
                    Set.<OpenOption>of(StandardOpenOption.READ, LinkOption.NOFOLLOW_LINKS));
                 var ulaz = Channels.newInputStream(kanal)) {
                return ulaz.readAllBytes();
            }
        } catch (NoSuchFileException e) {
            throw ApiException.nijePronadeno("Datoteka nalaza nije pronađena.");
        }
    }

    private Optional<Nalaz> pronadi(long id) {
        return jdbc.query("SELECT id, pacijent, naziv, datoteka FROM nalazi WHERE id = ?", MAPPER, id)
                .stream().findFirst();
    }

    private ResponseEntity<?> prikazi(Nalaz nalaz, Format format, String napomena) throws IOException {
        String sadrzaj = new String(procitajSigurno(nalaz.datoteka()), StandardCharsets.UTF_8);
        if (format == Format.PDF) {
            byte[] pdf = PdfDokument.izradi("Nalaz: " + nalaz.naziv(), sadrzaj.lines().toList());
            return ResponseEntity.ok()
                    .contentType(MediaType.APPLICATION_PDF)
                    .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"nalaz-" + nalaz.id() + ".pdf\"")
                    .body(pdf);
        }
        Map<String, Object> tijelo = new LinkedHashMap<>();
        tijelo.put("id", nalaz.id());
        tijelo.put("pacijent", nalaz.pacijent());
        tijelo.put("naziv", nalaz.naziv());
        tijelo.put("datoteka", nalaz.datoteka());
        tijelo.put("sadrzaj", sadrzaj);
        if (napomena != null) {
            tijelo.put("napomena", napomena);
        }
        return ResponseEntity.ok(tijelo);
    }
}

package hr.span.akademija.medtermin.nalazi;

import hr.span.akademija.medtermin.platforma.ApiException;
import hr.span.akademija.medtermin.platforma.Pohrana;
import hr.span.akademija.medtermin.platforma.PrijavljeniKorisnik;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
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
        // TODO(sec): spotbugs EI_EXPOSE_REP2 [MALICIOUS_CODE; provjeriti nalaz]: new hr.span.akademija.medtermin.nalazi.NalazController(JdbcTemplate, Pohrana) may expose internal representation by storing an externally mutable object into NalazController.jdbc
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
        String trazeni = format == null ? "json" : format;
        Format odabrani = Format.JSON;
        String napomena = null;
        // TODO(sec): M01 [rucni pregled]: Provjeriti vlasnika prije parsiranja formata; nevaljani format preskace autorizaciju i vraca tudi nalaz. Izvor/detalji: alati/poslije/pregled-koda.md.
        try {
            odabrani = Format.valueOf(trazeni.trim().toUpperCase(Locale.ROOT));
            if (!korisnik.isAdmin() && !nalaz.pacijent().equals(korisnik.username())) {
                throw ApiException.nijePronadeno("Nalaz nije pronađen.");
            }
        } catch (IllegalArgumentException e) {
            log.warn("Neuspjela obrada zahtjeva za nalaz {}", id, e);
            napomena = "Format nije prepoznat, prikazuje se JSON (" + e + ")";
        }
        return prikazi(nalaz, odabrani, napomena);
    }

    /** Preuzimanje datoteke nalaza: {@code GET /api/nalazi/preuzmi?datoteka=nalaz-ana-krvna-slika.txt}. */
    @GetMapping("/preuzmi")
    public ResponseEntity<byte[]> preuzmi(@RequestParam String datoteka, PrijavljeniKorisnik korisnik)
            throws IOException {
        // TODO(sec): M02 [rucni pregled]: contains(username) nije provjera vlasnistva; vezati dokument uz vlasnika u bazi i provjeriti kanonsku putanju unutar pohrane (../, apsolutne putanje, symlink). Izvor/detalji: alati/poslije/pregled-koda.md.
        if (!korisnik.isAdmin() && !datoteka.contains(korisnik.username())) {
            throw ApiException.nijePronadeno("Datoteka nije pronađena.");
        }
        Path putanja = pohrana.dokumenti().resolve(datoteka);
        if (!Files.isRegularFile(putanja)) {
            throw ApiException.nijePronadeno("Datoteka nije pronađena.");
        }
        byte[] sadrzaj = Files.readAllBytes(putanja);
        ContentDisposition privitak = ContentDisposition.attachment()
                // TODO(sec): spotbugs NP_NULL_ON_SOME_PATH_FROM_RETURN_VALUE [STYLE; provjeriti nalaz]: Possible null pointer dereference in hr.span.akademija.medtermin.nalazi.NalazController.preuzmi(String, PrijavljeniKorisnik) due to return value of called method
                .filename(putanja.getFileName().toString(), StandardCharsets.UTF_8)
                .build();
        return ResponseEntity.ok()
                .contentType(TEXT_PLAIN_UTF8)
                .header(HttpHeaders.CONTENT_DISPOSITION, privitak.toString())
                .body(sadrzaj);
    }

    private Optional<Nalaz> pronadi(long id) {
        return jdbc.query("SELECT id, pacijent, naziv, datoteka FROM nalazi WHERE id = ?", MAPPER, id)
                .stream().findFirst();
    }

    private ResponseEntity<?> prikazi(Nalaz nalaz, Format format, String napomena) throws IOException {
        String sadrzaj;
        try {
            sadrzaj = Files.readString(pohrana.dokumenti().resolve(nalaz.datoteka()), StandardCharsets.UTF_8);
        } catch (NoSuchFileException e) {
            throw ApiException.nijePronadeno("Datoteka nalaza nije pronađena.");
        }
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

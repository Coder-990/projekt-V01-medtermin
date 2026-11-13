package hr.span.akademija.medtermin.termini;

import hr.span.akademija.medtermin.platforma.ApiException;
import hr.span.akademija.medtermin.platforma.JdbcPomoc;
import hr.span.akademija.medtermin.platforma.PrijavljeniKorisnik;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/termini")
public class TerminController {

    private static final Logger log = LoggerFactory.getLogger(TerminController.class);

    public record Termin(long id, long lijecnikId, String lijecnik, LocalDateTime pocetak, int kapacitet,
                         int zauzeto, int slobodno, String napomena) {
    }

    private static final String SELECT = "SELECT t.id, t.lijecnik_id, l.ime, l.prezime, t.pocetak, t.kapacitet, "
            + "t.zauzeto, t.napomena FROM termini t JOIN lijecnici l ON l.id = t.lijecnik_id";

    private static final RowMapper<Termin> MAPPER = (rs, i) -> new Termin(
            rs.getLong("id"), rs.getLong("lijecnik_id"),
            "dr. " + rs.getString("ime") + " " + rs.getString("prezime"),
            rs.getObject("pocetak", LocalDateTime.class),
            rs.getInt("kapacitet"), rs.getInt("zauzeto"), rs.getInt("kapacitet") - rs.getInt("zauzeto"),
            rs.getString("napomena"));

    private final JdbcTemplate jdbc;
    private final UvozTermina uvoz;

    public TerminController(JdbcTemplate jdbc, UvozTermina uvoz) {
        // TODO(sec): spotbugs EI_EXPOSE_REP2 [MALICIOUS_CODE; provjeriti nalaz]: new hr.span.akademija.medtermin.termini.TerminController(JdbcTemplate, UvozTermina) may expose internal representation by storing an externally mutable object into TerminController.jdbc
        this.jdbc = jdbc;
        this.uvoz = uvoz;
    }

    /** Termini (svi ili jednog liječnika), npr. {@code GET /api/termini?lijecnikId=1}. */
    @GetMapping
    public List<Termin> popis(@RequestParam(required = false) Long lijecnikId, PrijavljeniKorisnik korisnik) {
        if (lijecnikId != null) {
            return jdbc.query(SELECT + " WHERE t.lijecnik_id = ? ORDER BY t.pocetak, t.id", MAPPER, lijecnikId);
        }
        return jdbc.query(SELECT + " ORDER BY t.pocetak, t.id", MAPPER);
    }

    /** Uvoz termina iz XML-a (liječnik ili administrator). */
    @PostMapping("/uvoz")
    @Transactional
    public Map<String, Object> uvezi(@RequestBody byte[] xml, PrijavljeniKorisnik korisnik) {
        if (!korisnik.imaUlogu("LIJECNIK", "ADMIN")) {
            throw ApiException.zabranjeno();
        }
        List<UvozTermina.Stavka> stavke = uvoz.procitaj(xml);
        for (UvozTermina.Stavka s : stavke) {
            Integer lijecnika = jdbc.queryForObject("SELECT COUNT(*) FROM lijecnici WHERE id = ?", Integer.class,
                    s.lijecnikId());
            if (lijecnika == null || lijecnika == 0) {
                throw ApiException.neispravno("Nepoznat liječnik: " + s.lijecnikId());
            }
            if (s.kapacitet() < 1 || s.kapacitet() > 50) {
                throw ApiException.neispravno("Kapacitet termina mora biti između 1 i 50.");
            }
        }
        List<Map<String, Object>> uvezeni = new ArrayList<>();
        for (UvozTermina.Stavka s : stavke) {
            long id = JdbcPomoc.umetni(jdbc,
                    "INSERT INTO termini (lijecnik_id, pocetak, kapacitet, zauzeto, napomena) VALUES (?, ?, ?, 0, ?)",
                    s.lijecnikId(), s.pocetak(), s.kapacitet(), s.napomena());
            Map<String, Object> termin = new LinkedHashMap<>();
            termin.put("id", id);
            termin.put("pocetak", s.pocetak());
            termin.put("napomena", s.napomena());
            uvezeni.add(termin);
        }
        log.info("Korisnik {} uvezao je {} termina", korisnik.username(), uvezeni.size());
        Map<String, Object> odgovor = new LinkedHashMap<>();
        odgovor.put("uvezeno", uvezeni.size());
        odgovor.put("termini", uvezeni);
        return odgovor;
    }
}

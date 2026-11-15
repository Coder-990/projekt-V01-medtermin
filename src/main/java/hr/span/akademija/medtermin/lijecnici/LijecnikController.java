package hr.span.akademija.medtermin.lijecnici;

import hr.span.akademija.medtermin.platforma.PrijavljeniKorisnik;
import hr.span.akademija.medtermin.platforma.ApiException;
import java.util.Locale;
import java.util.List;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/lijecnici")
public class LijecnikController {

    public record Lijecnik(long id, String ime, String prezime, String specijalizacija, String ordinacija) {
    }

    private static final RowMapper<Lijecnik> MAPPER = (rs, i) -> new Lijecnik(rs.getLong("id"),
            rs.getString("ime"), rs.getString("prezime"), rs.getString("specijalizacija"), rs.getString("ordinacija"));


    private final JdbcTemplate jdbc;

    public LijecnikController(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    /** Popis liječnika, npr. {@code GET /api/lijecnici?sort=prezime&smjer=asc}. */
    @GetMapping
    public List<Lijecnik> popis(@RequestParam(defaultValue = "prezime") String sort,
                                @RequestParam(defaultValue = "asc") String smjer,
                                PrijavljeniKorisnik korisnik) {
        // Izvorni nalaz ispod opisuje stanje prije ispravka; objašnjenje promjene slijedi.
        // TODO(sec): semgrep java.spring.security.injection.tainted-sql-string.tainted-sql-string: User data flows into this manually-constructed SQL string. User data can be safely inserted into SQL strings using prepared statements or an object-relational mapper (ORM). Manually-constructed SQL strings is a possible indicator of SQL ...
        // Izvorni nalaz ispod opisuje stanje prije ispravka; objašnjenje promjene slijedi.
        // TODO(sec): M11 [Semgrep + FindSecBugs SQL_INJECTION_SPRING_JDBC; rucna potvrda toka]: sort i smjer dolaze iz zahtjeva; mapirati stupce na allowlistu i smjer na ASC/DESC. SQL identifikatori se ne bindaju s ?. Izvor/detalji: alati/poslije/pregled-koda.md.
        // Izvorni nalaz ispod opisuje stanje prije ispravka; objašnjenje promjene slijedi.
        // TODO(sec): semgrep java.spring.security.audit.spring-sqli.spring-sqli: Detected a string argument from a public method contract in a raw SQL statement. This could lead to SQL injection if variables in the SQL statement are not properly sanitized. Use a prepared statements (java.sql.PreparedStatement) instea...
        // M11: Ranije su sort i smjer iz zahtjeva izravno dodavani u SQL, pa je
        // napadač mogao poslati SQL fragment. Identifikatori stupaca ne mogu se
        // zamijeniti običnim bind parametrom: switch mapira samo poznate stupce,
        // drugi switch dopušta samo ASC ili DESC, a nepoznate vrijednosti vraćaju 400.
        // SQL se sastavlja isključivo iz tih fiksnih vrijednosti.
        String stupac = switch (sort.toLowerCase(Locale.ROOT)) {
            case "id" -> "id";
            case "ime" -> "ime";
            case "prezime" -> "prezime";
            case "specijalizacija" -> "specijalizacija";
            case "ordinacija" -> "ordinacija";
            default -> throw ApiException.neispravno("Neispravno sortiranje.");
        };
        String smjerSql = switch (smjer.toLowerCase(Locale.ROOT)) {
            case "asc" -> "ASC";
            case "desc" -> "DESC";
            default -> throw ApiException.neispravno("Neispravan smjer sortiranja.");
        };
        String sql = "SELECT id, ime, prezime, specijalizacija, ordinacija FROM lijecnici ORDER BY " + stupac + " " + smjerSql;
        return jdbc.query(sql, MAPPER);
    }
}

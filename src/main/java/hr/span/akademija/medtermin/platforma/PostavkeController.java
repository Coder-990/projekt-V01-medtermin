package hr.span.akademija.medtermin.platforma;

import java.io.ByteArrayInputStream;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.error.YAMLException;

/**
 * Postavke obavijesti korisnika u YAML obliku, npr.
 * <pre>
 * podsjetnik: true
 * satiPrije: 24
 * kanal: email
 * </pre>
 */
@RestController
@RequestMapping("/api/postavke")
public class PostavkeController {

    private static final Set<String> KANALI = Set.of("email", "sms");

    private final JdbcTemplate jdbc;

    public PostavkeController(JdbcTemplate jdbc) {
        // TODO(sec): spotbugs EI_EXPOSE_REP2 [MALICIOUS_CODE; provjeriti nalaz]: new hr.span.akademija.medtermin.platforma.PostavkeController(JdbcTemplate) may expose internal representation by storing an externally mutable object into PostavkeController.jdbc
        this.jdbc = jdbc;
    }

    @GetMapping
    public Map<String, Object> dohvati(PrijavljeniKorisnik korisnik) {
        List<Map<String, Object>> spremljene = jdbc.query(
                "SELECT podsjetnik, sati_prije, kanal FROM postavke WHERE username = ?",
                (rs, i) -> postavke(rs.getBoolean("podsjetnik"), rs.getInt("sati_prije"), rs.getString("kanal")),
                korisnik.username());
        return Map.of("postavke", spremljene.isEmpty() ? postavke(true, 24, "email") : spremljene.get(0));
    }

    @PostMapping
    public Map<String, Object> spremi(@RequestBody byte[] tijelo, PrijavljeniKorisnik korisnik) {
        Object dokument;
        try {
            // TODO(sec): OSV/Trivy/Dependency-Check CVE-2022-1471: nepouzdan YAML ulazi u new Yaml().load;
            // predloziti SafeConstructor i podrzanu verziju; provjera Map nakon load ne sprjecava nuspojave.
            dokument = new Yaml().load(new ByteArrayInputStream(tijelo));
        } catch (YAMLException e) {
            throw ApiException.neispravno("Neispravan YAML.");
        }
        if (!(dokument instanceof Map<?, ?> mapa)) {
            throw ApiException.neispravno("Neispravne postavke.");
        }
        boolean podsjetnik = vrijednost(mapa, "podsjetnik", Boolean.class, Boolean.TRUE);
        int satiPrije = vrijednost(mapa, "satiPrije", Number.class, 24).intValue();
        String kanal = vrijednost(mapa, "kanal", String.class, "email");
        if (satiPrije < 1 || satiPrije > 168 || !KANALI.contains(kanal)) {
            throw ApiException.neispravno("Neispravne postavke.");
        }
        jdbc.update("MERGE INTO postavke (username, podsjetnik, sati_prije, kanal) KEY (username) VALUES (?, ?, ?, ?)",
                korisnik.username(), podsjetnik, satiPrije, kanal);
        return Map.of("postavke", postavke(podsjetnik, satiPrije, kanal));
    }

    private static <T> T vrijednost(Map<?, ?> mapa, String kljuc, Class<T> tip, T zadano) {
        Object vrijednost = mapa.get(kljuc);
        if (vrijednost == null) {
            return zadano;
        }
        if (!tip.isInstance(vrijednost)) {
            throw ApiException.neispravno("Neispravne postavke.");
        }
        return tip.cast(vrijednost);
    }

    private static Map<String, Object> postavke(boolean podsjetnik, int satiPrije, String kanal) {
        Map<String, Object> postavke = new LinkedHashMap<>();
        postavke.put("podsjetnik", podsjetnik);
        postavke.put("satiPrije", satiPrije);
        postavke.put("kanal", kanal);
        return postavke;
    }
}

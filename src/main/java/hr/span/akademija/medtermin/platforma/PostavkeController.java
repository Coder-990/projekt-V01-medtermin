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
import org.yaml.snakeyaml.LoaderOptions;
import org.yaml.snakeyaml.constructor.SafeConstructor;
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
        if (tijelo.length > 16_384) throw ApiException.neispravno("Prevelik YAML.");
        Object dokument;
        try {
            LoaderOptions options = new LoaderOptions();
            options.setMaxAliasesForCollections(0);
            options.setNestingDepthLimit(5);
            options.setCodePointLimit(16_384);
            options.setAllowDuplicateKeys(false);
            dokument = new Yaml(new SafeConstructor(options)).load(new ByteArrayInputStream(tijelo));
        } catch (YAMLException e) {
            throw ApiException.neispravno("Neispravan YAML.");
        }
        if (!(dokument instanceof Map<?, ?> mapa)) {
            throw ApiException.neispravno("Neispravne postavke.");
        }
        // Korak 5: Odbijamo nepoznate ključeve kako bismo spremili samo očekivanu strukturu.
        if (!mapa.keySet().stream().allMatch(k -> k instanceof String && Set.of("podsjetnik", "satiPrije", "kanal").contains(k))) {
            throw ApiException.neispravno("Neispravne postavke.");
        }
        boolean podsjetnik = vrijednost(mapa, "podsjetnik", Boolean.class, Boolean.TRUE);
        // Samo cijeli broj u rasponu dopuštenih sati: intValue() na Long/Double
        // mogao bi prešutno odrezati decimalu ili prepuniti broj i promijeniti vrijednost.
        int satiPrije = vrijednost(mapa, "satiPrije", Integer.class, 24);
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

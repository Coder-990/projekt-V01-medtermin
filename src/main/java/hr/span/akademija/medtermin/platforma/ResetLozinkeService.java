package hr.span.akademija.medtermin.platforma;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDate;
import java.util.HexFormat;
import java.util.List;
import java.util.Optional;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

/** Zaboravljena lozinka: token se šalje e-poštom, a njime se postavlja nova lozinka. */
@Service
public class ResetLozinkeService {

    private final JdbcTemplate jdbc;
    private final KorisnikService korisnici;
    private final PostaService posta;

    public ResetLozinkeService(JdbcTemplate jdbc, KorisnikService korisnici, PostaService posta) {
        // TODO(sec): spotbugs EI_EXPOSE_REP2 [MALICIOUS_CODE; provjeriti nalaz]: new hr.span.akademija.medtermin.platforma.ResetLozinkeService(JdbcTemplate, KorisnikService, PostaService) may expose internal representation by storing an externally mutable object into ResetLozinkeService.jdbc
        this.jdbc = jdbc;
        this.korisnici = korisnici;
        this.posta = posta;
    }

    public void zatrazi(String username) {
        Optional<KorisnikService.Korisnik> korisnik = korisnici.pronadi(username);
        if (korisnik.isEmpty()) {
            return;
        }
        // TODO(sec): M03 [rucni pregled]: Token iz username + datum je predvidljiv; predlozen SecureRandom token visoke entropije i pohrana njegova hasha. Izvor/detalji: alati/poslije/pregled-koda.md.
        String token = sazetak("MD5", username + LocalDate.now());
        jdbc.update("INSERT INTO reset_tokeni (username, token) VALUES (?, ?)", username, token);
        posta.posaljiUputeZaLozinku(korisnik.get(), token);
    }

    public boolean postaviNovuLozinku(String token, String novaLozinka) {
        KorisnikService.provjeriLozinku(novaLozinka);
        if (token == null || token.isBlank()) {
            return false;
        }
        // TODO(sec): M04 [rucni pregled]: Provjeriti istjece i iskoristen te atomski potrositi token; trenutan upit dopusta ponovnu uporabu bez isteka. Izvor/detalji: alati/poslije/pregled-koda.md.
        List<String> vlasnici = jdbc.queryForList(
                "SELECT username FROM reset_tokeni WHERE token = ?", String.class, token.trim());
        if (vlasnici.isEmpty()) {
            return false;
        }
        korisnici.promijeniLozinku(vlasnici.get(0), novaLozinka);
        return true;
    }

    private static String sazetak(String algoritam, String vrijednost) {
        try {
            MessageDigest md = MessageDigest.getInstance(algoritam);
            return HexFormat.of().formatHex(md.digest(vrijednost.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(algoritam + " nije dostupan", e);
        }
    }
}

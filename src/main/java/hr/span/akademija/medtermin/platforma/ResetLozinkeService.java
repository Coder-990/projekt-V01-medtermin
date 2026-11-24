package hr.span.akademija.medtermin.platforma;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.security.SecureRandom;
import java.util.Base64;
import org.springframework.transaction.annotation.Transactional;
import java.util.HexFormat;
import java.util.List;
import java.util.Optional;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

/** Zaboravljena lozinka: token se šalje e-poštom, a njime se postavlja nova lozinka. */
@Service
public class ResetLozinkeService {

    // SecureRandom se ponovno koristi; entropija tokena ostaje 32 nasumična bajta.
    private final SecureRandom random = new SecureRandom();
    private final JdbcTemplate jdbc;
    private final KorisnikService korisnici;
    private final PostaService posta;

    public ResetLozinkeService(JdbcTemplate jdbc, KorisnikService korisnici, PostaService posta) {
        this.jdbc = jdbc;
        this.korisnici = korisnici;
        this.posta = posta;
    }

    @Transactional
    public void zatrazi(String username) {
        Optional<KorisnikService.Korisnik> korisnik = korisnici.pronadi(username);
        if (korisnik.isEmpty()) {
            return;
        }
        byte[] bytes = new byte[32];
        random.nextBytes(bytes);
        String token = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
        jdbc.update("UPDATE reset_tokeni SET iskoristen = TRUE WHERE username = ?", username);
        jdbc.update("INSERT INTO reset_tokeni (username, token, istjece) VALUES (?, ?, ?)",
                username, sazetak("SHA-256", token), LocalDateTime.now().plusMinutes(15));
        posta.posaljiUputeZaLozinku(korisnik.get(), token);
    }

    @Transactional
    public boolean postaviNovuLozinku(String token, String novaLozinka) {
        KorisnikService.provjeriLozinku(novaLozinka);
        if (token == null || token.isBlank()) {
            return false;
        }
        String hash = sazetak("SHA-256", token.trim());
        int potroseno = jdbc.update("UPDATE reset_tokeni SET iskoristen = TRUE WHERE token = ? AND iskoristen = FALSE AND istjece > ?",
                hash, LocalDateTime.now());
        if (potroseno != 1) {
            return false;
        }
        List<String> vlasnici = jdbc.queryForList("SELECT username FROM reset_tokeni WHERE token = ?", String.class, hash);
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

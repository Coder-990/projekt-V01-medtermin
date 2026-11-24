package hr.span.akademija.medtermin.platforma;

import java.util.Optional;
import java.util.regex.Pattern;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

/** Korisnički računi: prijava, registracija i promjena lozinke. */
@Service
public class KorisnikService {

    private static final Logger log = LoggerFactory.getLogger(KorisnikService.class);
    private static final Pattern KORISNICKO_IME = Pattern.compile("[A-Za-z0-9._-]{3,50}");
    public static final int MIN_DULJINA_LOZINKE = 8;

    private static final RowMapper<Korisnik> MAPPER = (rs, i) -> new Korisnik(
            rs.getLong("id"), rs.getString("username"), rs.getString("password_hash"), rs.getString("email"),
            rs.getString("ime"), rs.getString("prezime"), rs.getString("uloga"), rs.getString("oib"));

    private final JdbcTemplate jdbc;
    private final LozinkaHasher hasher;
    private TokenStore tokeni;
    private OgranicenjeAutentikacijeFilter ogranicenje;

    @Autowired
    void postaviOgranicenje(OgranicenjeAutentikacijeFilter ogranicenje) { this.ogranicenje = ogranicenje; }

    @Autowired
    void postaviTokenStore(TokenStore tokeni) { this.tokeni = tokeni; }

    public KorisnikService(JdbcTemplate jdbc, LozinkaHasher hasher) {
        this.jdbc = jdbc;
        this.hasher = hasher;
    }

    public record Korisnik(long id, String username, String passwordHash, String email, String ime,
                           String prezime, String uloga, String oib) {

        public PrijavljeniKorisnik prijavljeni() {
            return new PrijavljeniKorisnik(id, username, uloga);
        }
    }

    public record PrijavaZahtjev(String username, String password) {
    }

    public record RegistracijaZahtjev(String username, String password, String email, String ime, String prezime) {
    }

    public Optional<Korisnik> pronadi(String username) {
        if (username == null) {
            return Optional.empty();
        }
        return jdbc.query("SELECT id, username, password_hash, email, ime, prezime, uloga, oib "
                + "FROM korisnici WHERE username = ?", MAPPER, username).stream().findFirst();
    }

    public Optional<Korisnik> prijava(PrijavaZahtjev zahtjev) {
        if (zahtjev == null || zahtjev.username() == null || zahtjev.password() == null) {
            return Optional.empty();
        }
        RequestAttributes atributi = RequestContextHolder.getRequestAttributes();
        ServletRequestAttributes http = atributi instanceof ServletRequestAttributes a ? a : null;
        String adresa = http == null ? null : http.getRequest().getRemoteAddr();
        if (ogranicenje != null && adresa != null && ogranicenje.prijavaBlokirana(adresa, zahtjev.username())) {
            if (http.getResponse() != null) http.getResponse().setHeader("Retry-After", "900");
            throw new ApiException(HttpStatus.TOO_MANY_REQUESTS, "Previše neuspjelih pokušaja. Pokušajte kasnije.");
        }
        log.info("Zahtjev za prijavu");
        Optional<Korisnik> korisnik = pronadi(zahtjev.username())
                .filter(k -> hasher.provjeri(zahtjev.password(), k.passwordHash()));
        if (korisnik.isEmpty()) {
            if (ogranicenje != null && adresa != null) ogranicenje.neuspjelaPrijava(adresa, zahtjev.username());
            String cistiUsername = zahtjev.username().replaceAll("[\r\n]", "_");
            log.warn("Neuspjela prijava za korisnika: {}", cistiUsername);
            return Optional.empty();
        }
        if (ogranicenje != null && adresa != null) ogranicenje.uspjesnaPrijava(adresa, zahtjev.username());
        korisnik.filter(k -> k.passwordHash().matches("[0-9a-f]{64}")
                && zahtjev.password().getBytes(java.nio.charset.StandardCharsets.UTF_8).length <= 72).ifPresent(k ->
                jdbc.update("UPDATE korisnici SET password_hash = ? WHERE id = ? AND password_hash = ?",
                        hasher.hash(zahtjev.password()), k.id(), k.passwordHash()));
        return korisnik;
    }

    public Korisnik registriraj(RegistracijaZahtjev zahtjev) {
        if (zahtjev == null || zahtjev.username() == null || !KORISNICKO_IME.matcher(zahtjev.username()).matches()) {
            throw ApiException.neispravno("Korisničko ime mora imati 3–50 znakova (slova, brojke, točka, crtica).");
        }
        provjeriLozinku(zahtjev.password());
        if (zahtjev.email() == null || !zahtjev.email().contains("@")) {
            throw ApiException.neispravno("Neispravna adresa e-pošte.");
        }
        if (pronadi(zahtjev.username()).isPresent()) {
            throw new ApiException(HttpStatus.CONFLICT, "Korisničko ime je zauzeto.");
        }
        try {
            kreiraj(zahtjev.username(), zahtjev.password(), zahtjev.email(), zahtjev.ime(), zahtjev.prezime(),
                    "KORISNIK", null);
        } catch (DuplicateKeyException e) {
            throw new ApiException(HttpStatus.CONFLICT, "Korisničko ime je zauzeto.");
        }
        log.info("Registriran novi korisnik {}", zahtjev.username());
        return pronadi(zahtjev.username()).orElseThrow();
    }

    public long kreiraj(String username, String lozinka, String email, String ime, String prezime, String uloga,
                        String oib) {
        return JdbcPomoc.umetni(jdbc,
                "INSERT INTO korisnici (username, password_hash, email, ime, prezime, uloga, oib) "
                        + "VALUES (?, ?, ?, ?, ?, ?, ?)",
                username, hasher.hash(lozinka), email, ime, prezime, uloga, oib);
    }

    @Transactional
    public void promijeniLozinku(String username, String novaLozinka) {
        provjeriLozinku(novaLozinka);
        jdbc.update("UPDATE korisnici SET password_hash = ? WHERE username = ?", hasher.hash(novaLozinka), username);
        jdbc.update("UPDATE reset_tokeni SET iskoristen = TRUE WHERE username = ? AND iskoristen = FALSE", username);
        if (tokeni != null) tokeni.opozoviKorisnika(username);
        log.info("Promijenjena lozinka");
    }

    public static void provjeriLozinku(String lozinka) {
        if (lozinka == null || lozinka.length() < MIN_DULJINA_LOZINKE || lozinka.getBytes(java.nio.charset.StandardCharsets.UTF_8).length > 72) {
            throw ApiException.neispravno("Lozinka mora imati najmanje " + MIN_DULJINA_LOZINKE + " znakova.");
        }
    }
}

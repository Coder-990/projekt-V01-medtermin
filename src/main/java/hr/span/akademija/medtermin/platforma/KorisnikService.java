package hr.span.akademija.medtermin.platforma;

import java.util.Optional;
import java.util.regex.Pattern;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Service;

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
        log.info("Zahtjev za prijavu: {}", zahtjev);
        return pronadi(zahtjev.username())
                .filter(k -> hasher.provjeri(zahtjev.password(), k.passwordHash()));
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

    public void promijeniLozinku(String username, String novaLozinka) {
        provjeriLozinku(novaLozinka);
        jdbc.update("UPDATE korisnici SET password_hash = ? WHERE username = ?", hasher.hash(novaLozinka), username);
        log.info("Promijenjena lozinka korisnika {}", username);
    }

    public static void provjeriLozinku(String lozinka) {
        if (lozinka == null || lozinka.length() < MIN_DULJINA_LOZINKE) {
            throw ApiException.neispravno("Lozinka mora imati najmanje " + MIN_DULJINA_LOZINKE + " znakova.");
        }
    }
}

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
    // M09: Zajednička pohrana omogućuje opoziv svih sesija nakon promjene lozinke.
    private TokenStore tokeni;

    @org.springframework.beans.factory.annotation.Autowired
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
        // Izvorni nalaz ispod opisuje stanje prije ispravka; objašnjenje promjene slijedi.
        // TODO(sec): M06 [rucni pregled; FindSecBugs CRLF_INJECTION_LOGS]: Record zahtjev ispisuje i password u cistom tekstu; ukloniti tajne iz logova i sprijeciti CR/LF injekciju. Izvor/detalji: alati/poslije/pregled-koda.md.
        // M06: Record zahtjeva sadrži lozinku i korisničko ime koje može sadržavati
        // kontrolne znakove. Zapisujemo samo događaj prijave bez tih vrijednosti;
        // time ne izlažemo lozinku i ne dopuštamo lažne retke u ovom logu.
        log.info("Zahtjev za prijavu");
        Optional<Korisnik> korisnik = pronadi(zahtjev.username())
                .filter(k -> hasher.provjeri(zahtjev.password(), k.passwordHash()));
        // M05: Tek nakon ispravne lozinke stari SHA-256 zamjenjujemo BCrypt zapisom.
        // Uvjet WHERE uključuje stari hash kako migracija ne bi prepisala lozinku
        // promijenjenu u međuvremenu. Lozinku dulju od 72 UTF-8 bajta ne migriramo
        // automatski jer BCrypt ima tu granicu; korisnik je mora promijeniti.
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

    @org.springframework.transaction.annotation.Transactional
    public void promijeniLozinku(String username, String novaLozinka) {
        provjeriLozinku(novaLozinka);
        jdbc.update("UPDATE korisnici SET password_hash = ? WHERE username = ?", hasher.hash(novaLozinka), username);
        // M03/M04: Promjena lozinke poništava i ostale ranije izdane reset tokene;
        // u istoj su transakciji s promjenom hasha, kako stari link ne bi ostao valjan.
        jdbc.update("UPDATE reset_tokeni SET iskoristen = TRUE WHERE username = ? AND iskoristen = FALSE", username);
        // M09: Promjena ili reset lozinke opoziva sve postojeće sesije korisnika.
        if (tokeni != null) tokeni.opozoviKorisnika(username);
        log.info("Promijenjena lozinka");
    }

    // M05: Ograničenje na 72 UTF-8 bajta sprječava da BCrypt prihvati samo početak duže lozinke.
    public static void provjeriLozinku(String lozinka) {
        if (lozinka == null || lozinka.length() < MIN_DULJINA_LOZINKE || lozinka.getBytes(java.nio.charset.StandardCharsets.UTF_8).length > 72) {
            throw ApiException.neispravno("Lozinka mora imati najmanje " + MIN_DULJINA_LOZINKE + " znakova.");
        }
    }
}

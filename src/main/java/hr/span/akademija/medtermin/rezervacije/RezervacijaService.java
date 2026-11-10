package hr.span.akademija.medtermin.rezervacije;

import hr.span.akademija.medtermin.platforma.ApiException;
import hr.span.akademija.medtermin.platforma.JdbcPomoc;
import hr.span.akademija.medtermin.platforma.PrijavljeniKorisnik;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.regex.Pattern;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class RezervacijaService {

    private static final Logger log = LoggerFactory.getLogger(RezervacijaService.class);
    private static final Pattern OIB = Pattern.compile("\\d{11}");

    public record NovaRezervacija(Long terminId, String napomena, String oib) {
    }

    public record Rezervacija(long id, long terminId, String pacijent, String lijecnik, LocalDateTime pocetak,
                              String napomena, LocalDateTime kreirano) {
    }

    private static final String SELECT = "SELECT r.id, r.termin_id, r.pacijent, r.napomena, r.kreirano, t.pocetak, "
            + "l.ime, l.prezime FROM rezervacije r JOIN termini t ON t.id = r.termin_id "
            + "JOIN lijecnici l ON l.id = t.lijecnik_id";

    private static final RowMapper<Rezervacija> MAPPER = (rs, i) -> new Rezervacija(
            rs.getLong("id"), rs.getLong("termin_id"), rs.getString("pacijent"),
            "dr. " + rs.getString("ime") + " " + rs.getString("prezime"),
            rs.getObject("pocetak", LocalDateTime.class), rs.getString("napomena"),
            rs.getObject("kreirano", LocalDateTime.class));

    private final JdbcTemplate jdbc;
    private final BolnickiSustavKlijent bis;

    public RezervacijaService(JdbcTemplate jdbc, BolnickiSustavKlijent bis) {
        this.jdbc = jdbc;
        this.bis = bis;
    }

    public Optional<Rezervacija> pronadi(long id) {
        return jdbc.query(SELECT + " WHERE r.id = ?", MAPPER, id).stream().findFirst();
    }

    public List<Rezervacija> rezervacijeKorisnika(String username) {
        return jdbc.query(SELECT + " WHERE r.pacijent = ? ORDER BY t.pocetak, r.id", MAPPER, username);
    }

    public long kreiraj(NovaRezervacija zahtjev, PrijavljeniKorisnik korisnik) {
        if (zahtjev == null || zahtjev.terminId() == null) {
            throw ApiException.neispravno("Termin je obavezan.");
        }
        if (zahtjev.oib() == null || !OIB.matcher(zahtjev.oib()).matches()) {
            throw ApiException.neispravno("OIB mora imati 11 znamenki.");
        }
        if (zahtjev.napomena() != null && zahtjev.napomena().length() > 1000) {
            throw ApiException.neispravno("Napomena smije imati najviše 1000 znakova.");
        }
        long terminId = zahtjev.terminId();
        Integer postoji = jdbc.queryForObject("SELECT COUNT(*) FROM termini WHERE id = ?", Integer.class, terminId);
        if (postoji == null || postoji == 0) {
            throw ApiException.nijePronadeno("Termin nije pronađen.");
        }
        log.info("Nova rezervacija: {} (korisnik {})", zahtjev, korisnik.username());
        int azurirano = jdbc.update(
                "UPDATE termini SET zauzeto = zauzeto + 1 WHERE id = ? AND zauzeto < kapacitet", terminId);
        if (azurirano == 0) {
            throw new ApiException(HttpStatus.CONFLICT, "Nema slobodnih mjesta.");
        }
        try {
            String oznaka = bis.upisiURaspored(terminId, korisnik.username());
            return spremi(zahtjev, korisnik, oznaka);
        } catch (RuntimeException e) {
            jdbc.update("UPDATE termini SET zauzeto = zauzeto - 1 WHERE id = ?", terminId);
            throw e;
        }
    }

    @Transactional
    public void obrisi(long id, PrijavljeniKorisnik korisnik) {
        Rezervacija rezervacija = pronadi(id)
                .filter(r -> korisnik.isAdmin() || r.pacijent().equals(korisnik.username()))
                .orElseThrow(() -> ApiException.nijePronadeno("Rezervacija nije pronađena."));
        jdbc.update("DELETE FROM rezervacije WHERE id = ?", rezervacija.id());
        jdbc.update("UPDATE termini SET zauzeto = zauzeto - 1 WHERE id = ? AND zauzeto > 0", rezervacija.terminId());
        log.info("Rezervacija {} je otkazana (korisnik {})", id, korisnik.username());
    }

    private long spremi(NovaRezervacija zahtjev, PrijavljeniKorisnik korisnik, String oznakaBis) {
        return JdbcPomoc.umetni(jdbc,
                "INSERT INTO rezervacije (termin_id, pacijent, napomena, oib, kreirano, oznaka_bis) "
                        + "VALUES (?, ?, ?, ?, ?, ?)",
                zahtjev.terminId(), korisnik.username(), zahtjev.napomena(), zahtjev.oib(), LocalDateTime.now(),
                oznakaBis);
    }
}

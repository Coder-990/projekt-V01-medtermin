package hr.span.akademija.medtermin;

import hr.span.akademija.medtermin.platforma.KorisnikService;
import hr.span.akademija.medtermin.platforma.Pohrana;
import java.time.LocalDateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

/**
 * Početni podaci (baza je u memoriji pa se puni pri svakom pokretanju). Lozinke se spremaju istom
 * metodom kao pri registraciji.
 */
@Component
public class PocetniPodaci implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(PocetniPodaci.class);

    private final JdbcTemplate jdbc;
    private final KorisnikService korisnici;
    private final Pohrana pohrana;

    public PocetniPodaci(JdbcTemplate jdbc, KorisnikService korisnici, Pohrana pohrana) {
        // TODO(sec): spotbugs EI_EXPOSE_REP2 [MALICIOUS_CODE; provjeriti nalaz]: new hr.span.akademija.medtermin.PocetniPodaci(JdbcTemplate, KorisnikService, Pohrana) may expose internal representation by storing an externally mutable object into PocetniPodaci.jdbc
        this.jdbc = jdbc;
        this.korisnici = korisnici;
        this.pohrana = pohrana;
    }

    @Override
    public void run(String... args) throws Exception {
        pohrana.pripremi();
        zapisiDatoteke();
        Integer postojeci = jdbc.queryForObject("SELECT COUNT(*) FROM korisnici", Integer.class);
        if (postojeci != null && postojeci > 0) {
            return;
        }
        korisnici.kreiraj("ana", "Ana12345!", "ana@primjer.hr", "Ana", "Anić", "KORISNIK", "48291736504");
        korisnici.kreiraj("marko", "Marko12345!", "marko@primjer.hr", "Marko", "Marić", "KORISNIK", "73019485267");
        korisnici.kreiraj("dr.horvat", "Ovlasteni12345!", "dr.horvat@primjer.hr", "Ivan", "Horvat", "LIJECNIK",
                "50617283940");
        korisnici.kreiraj("admin", "Admin12345!", "admin@primjer.hr", "Ivana", "Admin", "ADMIN", "61728394051");

        lijecnik("Ivan", "Horvat", "Kardiologija", "Ordinacija 1");
        lijecnik("Maja", "Kovačević", "Dermatologija", "Ordinacija 2");
        lijecnik("Tomislav", "Babić", "Ortopedija", "Ordinacija 3");
        lijecnik("Petra", "Novak", "Neurologija", "Ordinacija 4");
        lijecnik("Luka", "Jurić", "Interna medicina", "Ordinacija 5");
        lijecnik("Ema", "Vuković", "Oftalmologija", "Ordinacija 6");

        termin(1, "2026-12-01T08:00", 1, 1);
        termin(2, "2026-12-01T09:00", 1, 1);
        termin(1, "2026-12-01T09:00", 1, 0);
        termin(2, "2026-12-01T10:00", 1, 0);
        termin(3, "2026-12-02T08:00", 1, 0);
        termin(4, "2026-12-02T09:00", 1, 0);
        termin(5, "2026-12-02T10:00", 1, 0);
        termin(6, "2026-12-03T08:00", 1, 0);
        termin(1, "2026-12-03T09:00", 3, 0);
        termin(3, "2026-12-03T10:00", 1, 0);

        rezervacija(1, "ana", "Kontrolni pregled tlaka", "48291736504", "BIS-1-1000");
        rezervacija(2, "marko", "Bolovi u leđima", "73019485267", "BIS-2-1000");

        nalaz("ana", "Krvna slika", "nalaz-ana-krvna-slika.txt");
        nalaz("marko", "RTG kralježnice", "nalaz-marko-rtg.txt");
        log.info("Početni podaci su učitani");
    }

    private void zapisiDatoteke() throws Exception {
        pohrana.zapisiAkoNePostoji("nalaz-ana-krvna-slika.txt", """
                Poliklinika MedTermin – laboratorijski nalaz
                Pacijent: Ana Anić
                Pretraga: kompletna krvna slika
                Leukociti: 6,4 x10^9/L (referentno 3,4 – 9,7)
                Eritrociti: 4,62 x10^12/L (referentno 3,86 – 5,08)
                Hemoglobin: 138 g/L (referentno 119 – 157)
                Trombociti: 245 x10^9/L (referentno 158 – 424)
                Zaključak: nalaz unutar referentnih vrijednosti.
                """);
        pohrana.zapisiAkoNePostoji("nalaz-marko-rtg.txt", """
                Poliklinika MedTermin – radiološki nalaz
                Pacijent: Marko Marić
                Pretraga: RTG lumbalne kralježnice u dva smjera
                Nalaz: blago smanjena visina intervertebralnog prostora L4-L5, bez znakova prijeloma.
                Preporuka: fizikalna terapija i kontrola za tri mjeseca.
                """);
    }

    private void lijecnik(String ime, String prezime, String specijalizacija, String ordinacija) {
        jdbc.update("INSERT INTO lijecnici (ime, prezime, specijalizacija, ordinacija) VALUES (?, ?, ?, ?)",
                ime, prezime, specijalizacija, ordinacija);
    }

    private void termin(long lijecnikId, String pocetak, int kapacitet, int zauzeto) {
        jdbc.update("INSERT INTO termini (lijecnik_id, pocetak, kapacitet, zauzeto) VALUES (?, ?, ?, ?)",
                lijecnikId, LocalDateTime.parse(pocetak), kapacitet, zauzeto);
    }

    private void rezervacija(long terminId, String pacijent, String napomena, String oib, String oznaka) {
        jdbc.update("INSERT INTO rezervacije (termin_id, pacijent, napomena, oib, kreirano, oznaka_bis) "
                + "VALUES (?, ?, ?, ?, ?, ?)", terminId, pacijent, napomena, oib, LocalDateTime.now(), oznaka);
    }

    private void nalaz(String pacijent, String naziv, String datoteka) {
        jdbc.update("INSERT INTO nalazi (pacijent, naziv, datoteka) VALUES (?, ?, ?)", pacijent, naziv, datoteka);
    }
}

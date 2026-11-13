package hr.span.akademija.medtermin.platforma;

import java.sql.PreparedStatement;
import org.springframework.jdbc.core.ArgumentPreparedStatementSetter;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;

/** Pomoćne metode za JdbcTemplate. */
public final class JdbcPomoc {

    private JdbcPomoc() {
    }

    /** Izvršava INSERT i vraća generirani {@code id}. */
    public static long umetni(JdbcTemplate jdbc, String sql, Object... argumenti) {
        KeyHolder kljuc = new GeneratedKeyHolder();
        jdbc.update(con -> {
            // TODO(sec): spotbugs OBL_UNSATISFIED_OBLIGATION_EXCEPTION_EDGE [EXPERIMENTAL; provjeriti nalaz]: hr.span.akademija.medtermin.platforma.JdbcPomoc.lambda$umetni$0(String, Object[], Connection) may fail to clean up java.sql.Statement on checked exception
            // TODO(sec): T02 [FindSecBugs SQL_INJECTION_JDBC; provjera]: Pregledani pozivatelji koriste fiksni SQL i bind parametre; ne oznacavati potvrdenom injekcijom. Zadrzati taj ugovor i provjeravati nove pozive. Izvor/detalji: alati/poslije/pregled-koda.md.
            PreparedStatement ps = con.prepareStatement(sql, new String[] {"ID"});
            new ArgumentPreparedStatementSetter(argumenti).setValues(ps);
            return ps;
        }, kljuc);
        Number id = kljuc.getKey();
        if (id == null) {
            throw new IllegalStateException("Baza nije vratila generirani ključ.");
        }
        return id.longValue();
    }
}

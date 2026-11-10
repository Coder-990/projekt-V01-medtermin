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

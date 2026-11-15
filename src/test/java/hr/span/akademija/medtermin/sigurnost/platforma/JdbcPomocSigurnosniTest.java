package hr.span.akademija.medtermin.platforma;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.PreparedStatementCreator;
import org.springframework.jdbc.support.KeyHolder;

class JdbcPomocSigurnosniTest {
    @Test void zatvaraStatementAkoPostavljanjeParametaraNeUspije() throws Exception {
        JdbcTemplate jdbc = mock(JdbcTemplate.class);
        Connection veza = mock(Connection.class);
        PreparedStatement naredba = mock(PreparedStatement.class);
        when(veza.prepareStatement(anyString(), any(String[].class))).thenReturn(naredba);
        doThrow(new SQLException("neuspjelo vezivanje")).when(naredba).setString(eq(1), anyString());
        doAnswer(poziv -> {
            PreparedStatementCreator tvorac = poziv.getArgument(0);
            tvorac.createPreparedStatement(veza);
            return 1;
        }).when(jdbc).update(any(PreparedStatementCreator.class), any(KeyHolder.class));
        assertThatThrownBy(() -> JdbcPomoc.umetni(jdbc, "INSERT INTO test VALUES (?)", "vrijednost"))
                .hasMessageContaining("neuspjelo vezivanje");
        verify(naredba).close();
    }
    @Test void cuvaIzvornuGreskuKadIZatvaranjeNeUspije() throws Exception {
        JdbcTemplate jdbc = mock(JdbcTemplate.class);
        Connection veza = mock(Connection.class);
        PreparedStatement naredba = mock(PreparedStatement.class);
        when(veza.prepareStatement(anyString(), any(String[].class))).thenReturn(naredba);
        doThrow(new SQLException("vezivanje")).when(naredba).setString(eq(1), anyString());
        doThrow(new SQLException("zatvaranje")).when(naredba).close();
        doAnswer(poziv -> {
            PreparedStatementCreator tvorac = poziv.getArgument(0);
            tvorac.createPreparedStatement(veza);
            return 1;
        }).when(jdbc).update(any(PreparedStatementCreator.class), any(KeyHolder.class));
        assertThatThrownBy(() -> JdbcPomoc.umetni(jdbc, "INSERT INTO test VALUES (?)", "vrijednost"))
                .hasMessage("vezivanje")
                .satisfies(e -> {
                    org.assertj.core.api.Assertions.assertThat(e.getSuppressed()).hasSize(1);
                    org.assertj.core.api.Assertions.assertThat(e.getSuppressed()[0]).hasMessage("zatvaranje");
                });
    }
}

package hr.span.akademija.medtermin.nalazi;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import hr.span.akademija.medtermin.platforma.*;
import java.nio.file.*;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.jdbc.core.JdbcTemplate;

class PutanjeSigurnosniTest {
    @TempDir Path direktorij;
    final PrijavljeniKorisnik ana = new PrijavljeniKorisnik(1, "ana", "KORISNIK");

    @Test void odbijaNeprihvatljivePutanje() throws Exception {
        JdbcTemplate jdbc = mock(JdbcTemplate.class);
        Pohrana pohrana = mock(Pohrana.class);
        when(pohrana.dokumenti()).thenReturn(direktorij);
        when(jdbc.queryForObject(anyString(), eq(Integer.class), any(), any())).thenReturn(1);
        var controller = new NalazController(jdbc, pohrana);
        Files.createDirectory(direktorij.resolve("mapa"));
        for (String naziv : List.of("../izvan", direktorij.resolve("apsolutno").toString(), "mapa", ".", "..")) {
            assertThatThrownBy(() -> controller.preuzmi(naziv, ana)).isInstanceOf(ApiException.class);
        }
        when(jdbc.queryForObject(anyString(), eq(Integer.class), any(), any())).thenReturn(null);
        assertThatThrownBy(() -> controller.preuzmi("nepostoji", ana)).isInstanceOf(ApiException.class);
    }

    @Test void datotekaNestaneTijekomCitanja() throws Exception {
        JdbcTemplate jdbc = mock(JdbcTemplate.class);
        Pohrana pohrana = mock(Pohrana.class);
        when(pohrana.dokumenti()).thenReturn(direktorij);
        var nalaz = new NalazController.Nalaz(1, "ana", "nalaz", "nalaz.txt");
        when(jdbc.query(anyString(), any(org.springframework.jdbc.core.RowMapper.class), anyLong())).thenReturn(List.of(nalaz));
        @SuppressWarnings("unchecked") SecureDirectoryStream<Path> mapa = mock(SecureDirectoryStream.class);
        var pogled = mock(java.nio.file.attribute.BasicFileAttributeView.class);
        var atributi = mock(java.nio.file.attribute.BasicFileAttributes.class);
        when(mapa.getFileAttributeView(any(Path.class), eq(java.nio.file.attribute.BasicFileAttributeView.class), any(LinkOption.class))).thenReturn(pogled);
        when(pogled.readAttributes()).thenReturn(atributi);
        when(atributi.isRegularFile()).thenReturn(true);
        when(mapa.newByteChannel(any(Path.class), anySet(), any(java.nio.file.attribute.FileAttribute[].class)))
                .thenThrow(new NoSuchFileException("nalaz.txt"));
        try (var files = mockStatic(Files.class, CALLS_REAL_METHODS)) {
            files.when(() -> Files.newDirectoryStream(direktorij)).thenReturn(mapa);
            assertThatThrownBy(() -> new NalazController(jdbc, pohrana).nalaz(1, "json", ana))
                    .isInstanceOf(ApiException.class);
        }
    }

    @Test void zamjenaDatotekeSymlinkomTijekomCitanjaNeOtkrivaVanjskiSadrzaj() throws Exception {
        JdbcTemplate jdbc = mock(JdbcTemplate.class);
        Pohrana pohrana = mock(Pohrana.class);
        when(pohrana.dokumenti()).thenReturn(direktorij);
        when(jdbc.queryForObject(anyString(), eq(Integer.class), any(), any())).thenReturn(1);
        Path odobrena = direktorij.resolve("nalaz.txt");
        Files.writeString(odobrena, "odobreni nalaz");
        Path vanjska = Files.createTempFile("medtermin-izvan", ".txt");
        try {
            Files.writeString(vanjska, "TAJNI VANJSKI SADRZAJ");
            // Simulira zamjenu nakon provjere, neposredno prije starog readAllBytes poziva.
            try (var files = mockStatic(Files.class, CALLS_REAL_METHODS)) {
                files.when(() -> Files.readAllBytes(any(Path.class))).thenAnswer(poziv -> {
                    Files.delete(odobrena);
                    Files.createSymbolicLink(odobrena, vanjska);
                    return poziv.callRealMethod();
                });
                var odgovor = new NalazController(jdbc, pohrana).preuzmi("nalaz.txt", ana);
                assertThat(new String(odgovor.getBody(), java.nio.charset.StandardCharsets.UTF_8))
                        .isEqualTo("odobreni nalaz");
            }
        } finally {
            Files.deleteIfExists(vanjska);
        }
    }
    @Test void odbijaDatotecniSustavBezSigurnogOtvaranja() throws Exception {
        JdbcTemplate jdbc = mock(JdbcTemplate.class);
        Pohrana pohrana = mock(Pohrana.class);
        when(pohrana.dokumenti()).thenReturn(direktorij);
        when(jdbc.queryForObject(anyString(), eq(Integer.class), any(), any())).thenReturn(1);
        @SuppressWarnings("unchecked") DirectoryStream<Path> obicnaMapa = mock(DirectoryStream.class);
        try (var files = mockStatic(Files.class, CALLS_REAL_METHODS)) {
            files.when(() -> Files.newDirectoryStream(direktorij)).thenReturn(obicnaMapa);
            assertThatThrownBy(() -> new NalazController(jdbc, pohrana).preuzmi("nalaz.txt", ana))
                    .isInstanceOf(java.io.IOException.class).hasMessageContaining("ne podržava sigurno otvaranje");
        }
    }
}

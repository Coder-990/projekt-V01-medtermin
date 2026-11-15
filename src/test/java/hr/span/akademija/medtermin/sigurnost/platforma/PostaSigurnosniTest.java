package hr.span.akademija.medtermin.platforma;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;
import java.nio.file.*;
import java.nio.file.attribute.PosixFilePermission;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.core.env.Environment;

class PostaSigurnosniTest {
    @TempDir Path temp;
    private PostaService posta(Path mapa) {
        Pohrana p = mock(Pohrana.class);
        when(p.posta()).thenReturn(mapa);
        Environment env = mock(Environment.class);
        when(env.getProperty(anyString(), anyString())).thenReturn("8081");
        return new PostaService(p, new AppProperties("MedTermin", "dokumenti", mapa.toString(), null), env);
    }
    private final KorisnikService.Korisnik k = new KorisnikService.Korisnik(1, "ana", "hash", "ana@example.org", "Ana", "A", "KORISNIK", null);

    @Test void odbijaJavnoCitljivuMapuIPorukaJePrivatna() throws Exception {
        org.junit.jupiter.api.Assumptions.assumeTrue(Files.getFileStore(temp).supportsFileAttributeView("posix"));
        Path mapa = Files.createDirectory(temp.resolve("posta"));
        Files.setPosixFilePermissions(mapa, Set.of(PosixFilePermission.OWNER_READ, PosixFilePermission.OWNER_WRITE,
                PosixFilePermission.OWNER_EXECUTE, PosixFilePermission.GROUP_READ, PosixFilePermission.GROUP_EXECUTE));
        assertThatThrownBy(() -> posta(mapa).posaljiUputeZaLozinku(k, "tajni-token")).isInstanceOf(java.io.UncheckedIOException.class);
        Files.setPosixFilePermissions(mapa, Set.of(PosixFilePermission.OWNER_READ, PosixFilePermission.OWNER_WRITE, PosixFilePermission.OWNER_EXECUTE));
        posta(mapa).posaljiUputeZaLozinku(k, "tajni-token");
        try (var datoteke = Files.list(mapa)) {
            Path poruka = datoteke.findFirst().orElseThrow();
            assertThat(Files.readString(poruka)).contains("tajni-token");
            assertThat(Files.getPosixFilePermissions(poruka)).containsOnly(PosixFilePermission.OWNER_READ, PosixFilePermission.OWNER_WRITE);
        }
    }
    @Test void odbijaSymlinkIDozvoleZaOstaleIKorisnickoIme() throws Exception {
        org.junit.jupiter.api.Assumptions.assumeTrue(Files.getFileStore(temp).supportsFileAttributeView("posix"));
        Path mapa = Files.createDirectory(temp.resolve("privatno"));
        Files.setPosixFilePermissions(mapa, Set.of(PosixFilePermission.OWNER_READ, PosixFilePermission.OWNER_WRITE, PosixFilePermission.OWNER_EXECUTE));
        Path poveznica = temp.resolve("link");
        Files.createSymbolicLink(poveznica, mapa);
        assertThatThrownBy(() -> posta(poveznica).posaljiUputeZaLozinku(k, "token")).isInstanceOf(java.io.UncheckedIOException.class);
        Files.setPosixFilePermissions(mapa, Set.of(PosixFilePermission.OWNER_READ, PosixFilePermission.OWNER_WRITE, PosixFilePermission.OWNER_EXECUTE, PosixFilePermission.OTHERS_READ));
        assertThatThrownBy(() -> posta(mapa).posaljiUputeZaLozinku(k, "token")).isInstanceOf(java.io.UncheckedIOException.class);
        Files.setPosixFilePermissions(mapa, Set.of(PosixFilePermission.OWNER_READ, PosixFilePermission.OWNER_WRITE, PosixFilePermission.OWNER_EXECUTE));
        var los = new KorisnikService.Korisnik(1, "../zlo", "hash", "a@b", "A", "B", "KORISNIK", null);
        assertThatThrownBy(() -> posta(mapa).posaljiUputeZaLozinku(los, "token")).isInstanceOf(java.io.UncheckedIOException.class);
    }
    @Test void odbijaDatotecniSustavBezPosixDozvola() throws Exception {
        Path mapa = Files.createDirectory(temp.resolve("bez-posix"));
        var store = mock(FileStore.class);
        when(store.supportsFileAttributeView("posix")).thenReturn(false);
        try (var files = mockStatic(Files.class, CALLS_REAL_METHODS)) {
            files.when(() -> Files.getFileStore(mapa)).thenReturn(store);
            assertThatThrownBy(() -> posta(mapa).posaljiUputeZaLozinku(k, "token"))
                    .isInstanceOf(java.io.UncheckedIOException.class);
        }
    }
}

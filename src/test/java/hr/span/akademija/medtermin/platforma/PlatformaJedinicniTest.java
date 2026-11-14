package hr.span.akademija.medtermin.platforma;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import hr.span.akademija.medtermin.platforma.AppProperties.Cors;
import hr.span.akademija.medtermin.platforma.AuthController.NovaLozinka;
import hr.span.akademija.medtermin.platforma.AuthController.ResetZahtjev;
import hr.span.akademija.medtermin.platforma.KorisnikService.Korisnik;
import hr.span.akademija.medtermin.platforma.KorisnikService.PrijavaZahtjev;
import hr.span.akademija.medtermin.platforma.KorisnikService.RegistracijaZahtjev;
import jakarta.servlet.FilterChain;
import jakarta.servlet.http.Cookie;
import java.io.File;
import java.io.IOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.core.env.Environment;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.PreparedStatementCreator;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

class PlatformaJedinicniTest {

    @Test
    @DisplayName("KorisnikService rubni slučajevi za pronadi, prijava i registracija")
    void korisnikServiceRubniSlucajevi() {
        JdbcTemplate jdbc = mock(JdbcTemplate.class);
        LozinkaHasher hasher = new LozinkaHasher();
        KorisnikService service = new KorisnikService(jdbc, hasher);

        // pronadi(null) -> Optional.empty()
        assertThat(service.pronadi(null)).isEmpty();

        // prijava s neispravnim parametrima -> Optional.empty()
        assertThat(service.prijava(null)).isEmpty();
        assertThat(service.prijava(new PrijavaZahtjev(null, "Lozinka123!"))).isEmpty();
        assertThat(service.prijava(new PrijavaZahtjev("korisnik", null))).isEmpty();

        // registriraj s null zahtjevom ili null imenom
        assertThatThrownBy(() -> service.registriraj(null))
                .isInstanceOf(ApiException.class)
                .hasMessageContaining("Korisničko ime mora imati");

        assertThatThrownBy(() -> service.registriraj(new RegistracijaZahtjev(null, "Lozinka123!", "a@b.hr", "I", "P")))
                .isInstanceOf(ApiException.class)
                .hasMessageContaining("Korisničko ime mora imati");

        // registriraj s null emailom
        assertThatThrownBy(() -> service.registriraj(new RegistracijaZahtjev("validnoIme", "Lozinka123!", null, "I", "P")))
                .isInstanceOf(ApiException.class)
                .hasMessageContaining("Neispravna adresa e-pošte.");

        // provjeriLozinku(null)
        assertThatThrownBy(() -> KorisnikService.provjeriLozinku(null))
                .isInstanceOf(ApiException.class)
                .hasMessageContaining("Lozinka mora imati najmanje");

        // DuplicateKeyException u kreiraj metodi
        when(jdbc.query(anyString(), any(org.springframework.jdbc.core.RowMapper.class), anyString()))
                .thenReturn(List.of()); // pronadi() vraća empty
        doAnswer(inv -> {
            throw new DuplicateKeyException("Duplikat");
        }).when(jdbc).update(any(PreparedStatementCreator.class), any(KeyHolder.class));

        RegistracijaZahtjev zahtjev = new RegistracijaZahtjev("duplikatIme", "Lozinka123!", "d@b.hr", "I", "P");
        assertThatThrownBy(() -> service.registriraj(zahtjev))
                .isInstanceOf(ApiException.class)
                .matches(e -> ((ApiException) e).status() == HttpStatus.CONFLICT);
    }

    @Test
    @DisplayName("ResetLozinkeService rubni slučajevi za prazan token i nepoznat algoritam")
    void resetLozinkeRubniSlucajevi() throws Exception {
        JdbcTemplate jdbc = mock(JdbcTemplate.class);
        KorisnikService korisnici = mock(KorisnikService.class);
        PostaService posta = mock(PostaService.class);
        ResetLozinkeService reset = new ResetLozinkeService(jdbc, korisnici, posta);

        // postaviNovuLozinku s null ili praznim tokenom
        assertThat(reset.postaviNovuLozinku(null, "NovaLozinka123!")).isFalse();
        assertThat(reset.postaviNovuLozinku("   ", "NovaLozinka123!")).isFalse();

        // Poziv privatne metode sazetak s nepostojećim algoritmom
        Method sazetak = ResetLozinkeService.class.getDeclaredMethod("sazetak", String.class, String.class);
        sazetak.setAccessible(true);
        assertThatThrownBy(() -> {
            try {
                sazetak.invoke(null, "NEPOSTOJEĆI_ALGORITAM", "tekst");
            } catch (InvocationTargetException e) {
                throw e.getCause();
            }
        }).isInstanceOf(IllegalStateException.class).hasMessageContaining("NEPOSTOJEĆI_ALGORITAM nije dostupan");
    }

    @Test
    @DisplayName("LozinkaHasher provjeri s null hash-om vraća false")
    void lozinkaHasherNullHash() {
        LozinkaHasher hasher = new LozinkaHasher();
        assertThat(hasher.provjeri("Lozinka123!", null)).isFalse();
        assertThat(hasher.provjeri("Lozinka123!", "krivo")).isFalse();
        assertThat(hasher.provjeri("Lozinka123!", hasher.hash("Lozinka123!"))).isTrue();
    }

    @Test
    @DisplayName("LozinkaHasher baca IllegalStateException kad SHA-256 nije dostupan")
    void lozinkaHasherNedostupanSha() {
        java.security.Provider sun = java.security.Security.getProvider("SUN");
        try {
            java.security.Security.removeProvider("SUN");
            LozinkaHasher hasher = new LozinkaHasher();
            assertThatThrownBy(() -> hasher.hash("Lozinka123!"))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("SHA-256 nije dostupan");
        } finally {
            if (sun != null) {
                java.security.Security.insertProviderAt(sun, 1);
            }
        }
    }

    @Test
    @DisplayName("PocetniPodaci kad je postojeci null nastavlja s inicijalizacijom")
    void pocetniPodaciNullPostojeci() throws Exception {
        JdbcTemplate jdbc = mock(JdbcTemplate.class);
        Pohrana pohrana = mock(Pohrana.class);
        KorisnikService korisnici = mock(KorisnikService.class);
        when(jdbc.queryForObject(anyString(), org.mockito.ArgumentMatchers.eq(Integer.class))).thenReturn(null);

        hr.span.akademija.medtermin.PocetniPodaci pp = new hr.span.akademija.medtermin.PocetniPodaci(jdbc, korisnici, pohrana);
        pp.run();
        org.mockito.Mockito.verify(korisnici, org.mockito.Mockito.atLeastOnce())
                .kreiraj(any(), any(), any(), any(), any(), any(), any());
    }

    @Test
    @DisplayName("AppProperties dopusteniOrigini pokriva null i prazne unose")
    void appPropertiesOrigini() {
        AppProperties bezCorsa = new AppProperties("Naziv", "dir1", "dir2", null);
        assertThat(bezCorsa.dopusteniOrigini()).isEmpty();

        AppProperties corsBezOrigina = new AppProperties("Naziv", "dir1", "dir2", new Cors(null));
        assertThat(corsBezOrigina.dopusteniOrigini()).isEmpty();

        AppProperties corsSPraznim = new AppProperties("Naziv", "dir1", "dir2",
                new Cors(List.of("  ", " http://a.com ", "")));
        assertThat(corsSPraznim.dopusteniOrigini()).containsExactly("http://a.com");
    }

    @Test
    @DisplayName("JdbcPomoc privatni konstruktor i bacanje iznimke kad nema generiranog ključa")
    void jdbcPomocTest() throws Exception {
        Constructor<JdbcPomoc> ctor = JdbcPomoc.class.getDeclaredConstructor();
        ctor.setAccessible(true);
        assertThat(ctor.newInstance()).isNotNull();

        JdbcTemplate jdbc = mock(JdbcTemplate.class);
        // Kad jdbc.update ne postavi ključ u KeyHolder
        assertThatThrownBy(() -> JdbcPomoc.umetni(jdbc, "INSERT INTO tablica VALUES (1)"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Baza nije vratila generirani ključ.");
    }

    @Test
    @DisplayName("Html privatni konstruktor i escape s null vrijednosti")
    void htmlPomocTest() throws Exception {
        Constructor<Html> ctor = Html.class.getDeclaredConstructor();
        ctor.setAccessible(true);
        assertThat(ctor.newInstance()).isNotNull();

        assertThat(Html.escape(null)).isEqualTo("");
        assertThat(Html.escape("<script>")).isEqualTo("&lt;script&gt;");
    }

    @Test
    @DisplayName("PostaService baca UncheckedIOException ako stvaranje mape ili datoteke ne uspije")
    void postaServiceIOException(@TempDir Path tempDir) throws IOException {
        // Postavimo običnu datoteku na mjesto gdje bi trebala biti mapa 'posta', tako da createDirectories baci IOException
        Path datotekaUmjestoMape = tempDir.resolve("posta-kao-datoteka.txt");
        Files.writeString(datotekaUmjestoMape, "blokada");

        Pohrana pohrana = mock(Pohrana.class);
        when(pohrana.posta()).thenReturn(datotekaUmjestoMape.resolve("podmapa"));

        AppProperties postavke = new AppProperties("MedTermin", tempDir.toString(), tempDir.toString(), null);
        Environment env = mock(Environment.class);
        when(env.getProperty(anyString(), anyString())).thenReturn("8080");
        PostaService posta = new PostaService(pohrana, postavke, env);

        Korisnik k = new Korisnik(1L, "ana", "hash", "ana@primjer.hr", "Ana", "Anić", "KORISNIK", "48291736504");

        assertThatThrownBy(() -> posta.posaljiUputeZaLozinku(k, "token123"))
                .isInstanceOf(java.io.UncheckedIOException.class);
    }

    @Test
    @DisplayName("AuthFilter pokriva prazan Bearer token i kolačiće bez SESSION")
    void authFilterRubniSlucajevi() throws Exception {
        TokenStore tokeni = mock(TokenStore.class);
        AuthFilter filter = new AuthFilter(tokeni);

        MockHttpServletRequest req = new MockHttpServletRequest();
        MockHttpServletResponse res = new MockHttpServletResponse();
        FilterChain chain = mock(FilterChain.class);

        // Bearer s praznim sadržajem
        req.addHeader("Authorization", "Bearer    ");
        filter.doFilter(req, res, chain);
        assertThat(req.getAttribute(AuthFilter.ATRIBUT)).isNull();

        // Kolačić koji nije SESSION
        MockHttpServletRequest req2 = new MockHttpServletRequest();
        req2.setCookies(new Cookie("DRUGI_KOLACIC", "vrijednost"));
        filter.doFilter(req2, res, chain);
        assertThat(req2.getAttribute(AuthFilter.ATRIBUT)).isNull();
    }

    @Test
    @DisplayName("AuthController pokriva rubne slučajeve za resetZahtjev, reset i ja")
    void authControllerRubniSlucajevi() {
        KorisnikService korisnici = mock(KorisnikService.class);
        TokenStore tokeni = mock(TokenStore.class);
        ResetLozinkeService reset = mock(ResetLozinkeService.class);
        AuthController controller = new AuthController(korisnici, tokeni, reset);

        // resetZahtjev s null tijelom
        assertThat(controller.resetZahtjev(null).getBody().get("poruka"))
                .isEqualTo("Ako korisnik postoji, poslali smo upute na e-mail.");

        // reset s neuspjelim servisom
        when(reset.postaviNovuLozinku(any(), any())).thenReturn(false);
        ResponseEntity<java.util.Map<String, String>> odgNeuspjeli = controller.reset(new NovaLozinka("token", "lozinka"));
        assertThat(odgNeuspjeli.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(odgNeuspjeli.getBody().get("greska")).isEqualTo("Token nije valjan ili je istekao.");

        // reset s null zahtjevom
        ResponseEntity<java.util.Map<String, String>> odgNull = controller.reset(null);
        assertThat(odgNull.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);

        // ja s nepoznatim korisnikom -> 401
        when(korisnici.pronadi("nepoznat")).thenReturn(Optional.empty());
        assertThatThrownBy(() -> controller.ja(new PrijavljeniKorisnik(99L, "nepoznat", "KORISNIK")))
                .isInstanceOf(ApiException.class)
                .matches(e -> ((ApiException) e).status() == HttpStatus.UNAUTHORIZED);
    }
}

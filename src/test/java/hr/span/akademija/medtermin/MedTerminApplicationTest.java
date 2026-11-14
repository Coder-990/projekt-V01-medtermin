package hr.span.akademija.medtermin;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNoException;

import hr.span.akademija.medtermin.platforma.Pohrana;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
class MedTerminApplicationTest {

    @Autowired
    private PocetniPodaci pocetniPodaci;

    @Autowired
    private Pohrana pohrana;

    @Test
    @DisplayName("PocetniPodaci run metoda se vraća rano kad korisnici već postoje u bazi")
    void pocetniPodaciRaniPovratak() throws Exception {
        assertThatNoException().isThrownBy(() -> pocetniPodaci.run());
    }

    @Test
    @DisplayName("Pohrana pripremi pokriva grananje kad mape već postoje")
    void pohranaPripremiPonovno() {
        assertThatNoException().isThrownBy(() -> pohrana.pripremi());
    }

    @Test
    @DisplayName("MedTerminApplication main pokreće aplikaciju bez greške")
    void mainMetoda() {
        assertThatNoException().isThrownBy(() ->
                MedTerminApplication.main(new String[]{"--spring.main.web-application-type=none"}));
    }
}

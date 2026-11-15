package hr.span.akademija.medtermin.sigurnost;

import static org.assertj.core.api.Assertions.assertThat;

import hr.span.akademija.medtermin.ApiTestBaza;
import org.junit.jupiter.api.Test;

public class YamlDeserijalizacijaTest extends ApiTestBaza {
    public static boolean instancirana;

    public static class Proba {
        public Proba() { instancirana = true; }
    }

    @Test
    void korisnickiYamlNeStvaraProizvoljniObjekt() {
        instancirana = false;
        String token = prijava("ana", "Ana12345!");
        String yaml = "neocekivano: !!hr.span.akademija.medtermin.sigurnost.YamlDeserijalizacijaTest$Proba {}\n";
        Odgovor odgovor = posalji("POST", "/api/postavke", token, "application/yaml", yaml);
        assertThat(instancirana).isFalse();
        assertThat(odgovor.status()).isEqualTo(400);
    }
}

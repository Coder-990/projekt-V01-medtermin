package hr.span.akademija.medtermin.termini;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import hr.span.akademija.medtermin.platforma.ApiException;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.Test;

class UvozSigurnosniTest {
    @Test void odbijaDoctypeIVanjskeEntitete() {
        String xml = "<!DOCTYPE termini [<!ENTITY xxe SYSTEM \"file:///etc/passwd\">]>"
                + "<termini><termin><napomena>&xxe;</napomena></termin></termini>";
        assertThatThrownBy(() -> new UvozTermina().procitaj(xml.getBytes(StandardCharsets.UTF_8)))
                .isInstanceOf(ApiException.class);
    }
}

package hr.span.akademija.medtermin.nalazi;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class PdfDokumentTest {

    @Test
    @DisplayName("PdfDokument generira valjan PDF s posebnim znakovima, zagradama i ne-ASCII znakovima")
    void generiranjePdfDokumenta() {
        List<String> linije = new ArrayList<>();
        linije.add("Redak sa zagradama: (otvorena i zatvorena) i kosom crtom \\");
        linije.add("Hrvatski dijakritici: čćžšđ ČĆŽŠĐ – crtica — duga crtica");
        linije.add("Znakovi izvan ASCII raspona: snjegović \u2603, ćirilica \u0410\u0411, kontrolni \u0007");
        linije.add(null); // null redak za pokrivanje Normalizer grananja

        byte[] pdf = PdfDokument.izradi("Naslov: (test) \\ i dijakritici", linije);

        assertThat(pdf).isNotNull().isNotEmpty();
        String pdfTekst = new String(pdf, java.nio.charset.StandardCharsets.US_ASCII);
        assertThat(pdfTekst).startsWith("%PDF-1.4").contains("%%EOF");
    }
}

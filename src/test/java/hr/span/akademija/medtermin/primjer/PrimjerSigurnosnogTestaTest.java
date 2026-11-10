package hr.span.akademija.medtermin.primjer;

import static org.assertj.core.api.Assertions.assertThat;

import hr.span.akademija.medtermin.ApiTestBaza;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

/**
 * Primjer sigurnosnog testa: zaštićeni endpoint bez prijave mora vratiti 401.
 *
 * Po ovom uzoru napiši vlastite testove (npr. u paketu {@code hr.span.akademija.medtermin.sigurnost}):
 * test opisuje očekivano sigurno ponašanje i prolazi tek kad je aplikacija ispravljena.
 */
@Tag("primjer")
class PrimjerSigurnosnogTestaTest extends ApiTestBaza {

    @Test
    @DisplayName("Popis vlastitih rezervacija bez prijave vraća 401")
    void bezPrijaveNemaPristupa() {
        Odgovor odgovor = get("/api/rezervacije/moje", null);

        assertThat(odgovor.status()).isEqualTo(401);
        assertThat(odgovor.json().get("greska").asText()).isEqualTo("Potrebna je prijava.");
    }

    @Test
    @DisplayName("Izmišljeni token ne daje pristup")
    void izmisljeniTokenNeDajePristup() {
        Odgovor odgovor = get("/api/ja", "izmisljeni-token");

        assertThat(odgovor.status()).isEqualTo(401);
    }
}

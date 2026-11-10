package hr.span.akademija.medtermin.platforma;

import java.util.Arrays;

/** Korisnik prijavljen tokenom (zaglavlje Authorization ili kolačić SESSION). */
public record PrijavljeniKorisnik(long id, String username, String uloga) {

    public boolean isAdmin() {
        return "ADMIN".equals(uloga);
    }

    public boolean imaUlogu(String... uloge) {
        return Arrays.asList(uloge).contains(uloga);
    }
}

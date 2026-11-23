package hr.span.akademija.medtermin.platforma;

import java.security.SecureRandom;
import java.time.Duration;
import java.time.Clock;
import java.time.Instant;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;
import org.springframework.stereotype.Component;

@Component
public class TokenStore {
    private record Sesija(PrijavljeniKorisnik korisnik, Instant istjece) { }
    private final SecureRandom random = new SecureRandom();
    private final Map<String, Sesija> tokeni = new HashMap<>();
    // M09: Ranije se tokeni nisu automatski uklanjali ni opozivali. Svaka nova
    // sesija sada traje najviše osam sati, a pohrana dopušta najviše 10 000 tokena.
    // Istekle sesije čiste se pri izdavanju i odbijaju pri provjeri. Pohrana je
    // memorijska: ponovnim pokretanjem aplikacije sve se sesije gube.
    private static final Duration TRAJANJE = Duration.ofHours(8);
    private static final int MAKSIMUM = 10000;
    private final Clock clock;

    public TokenStore() { this(Clock.systemUTC()); }
    TokenStore(Clock clock) { this.clock = clock; }

    public synchronized String izdaj(PrijavljeniKorisnik korisnik) {
        Instant sada = clock.instant();
        // M09: Prije izdavanja čistimo istekle sesije kako ne bi zauzimale ograničenu pohranu.
        tokeni.values().removeIf(s -> !s.istjece().isAfter(sada));
        if (tokeni.size() >= MAKSIMUM) throw new IllegalStateException("Dosegnut je maksimalan broj sesija.");
        byte[] bajtovi = new byte[32];
        random.nextBytes(bajtovi);
        String token = Base64.getUrlEncoder().withoutPadding().encodeToString(bajtovi);
        tokeni.put(token, new Sesija(korisnik, sada.plus(TRAJANJE)));
        return token;
    }

    // M09: Istekli token više ne autentificira i uklanja se iz memorije.
    public synchronized PrijavljeniKorisnik korisnik(String token) {
        Sesija s = tokeni.get(token);
        if (s == null) return null;
        if (!s.istjece().isAfter(clock.instant())) { tokeni.remove(token); return null; }
        return s.korisnik();
    }

    // M09: Odjava opoziva jednu sesiju; promjena lozinke opoziva sve korisnikove sesije.
    public synchronized void opozovi(String token) { tokeni.remove(token); }
    public synchronized void opozoviKorisnika(String username) {
        tokeni.values().removeIf(s -> s.korisnik().username().equals(username));
    }
}

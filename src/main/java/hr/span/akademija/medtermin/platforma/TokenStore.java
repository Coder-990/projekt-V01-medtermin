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
    private static final Duration TRAJANJE = Duration.ofHours(8);
    private static final int MAKSIMUM = 10000;
    private final Clock clock;

    public TokenStore() { this(Clock.systemUTC()); }
    public TokenStore(Clock clock) { this.clock = clock; }

    public synchronized String izdaj(PrijavljeniKorisnik korisnik) {
        Instant sada = clock.instant();
        tokeni.values().removeIf(s -> !s.istjece().isAfter(sada));
        if (tokeni.size() >= MAKSIMUM) throw new IllegalStateException("Dosegnut je maksimalan broj sesija.");
        byte[] bajtovi = new byte[32];
        random.nextBytes(bajtovi);
        String token = Base64.getUrlEncoder().withoutPadding().encodeToString(bajtovi);
        tokeni.put(token, new Sesija(korisnik, sada.plus(TRAJANJE)));
        return token;
    }

    public synchronized PrijavljeniKorisnik korisnik(String token) {
        Sesija s = tokeni.get(token);
        if (s == null) return null;
        if (!s.istjece().isAfter(clock.instant())) { tokeni.remove(token); return null; }
        return s.korisnik();
    }

    public synchronized void opozovi(String token) { tokeni.remove(token); }
    public synchronized void opozoviKorisnika(String username) {
        tokeni.values().removeIf(s -> s.korisnik().username().equals(username));
    }
}

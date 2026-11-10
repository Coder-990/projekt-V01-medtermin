package hr.span.akademija.medtermin.platforma;

import java.security.SecureRandom;
import java.util.Base64;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.stereotype.Component;

/** Tokeni prijave čuvaju se u memoriji (token → korisnik). */
@Component
public class TokenStore {

    private final SecureRandom random = new SecureRandom();
    private final ConcurrentHashMap<String, PrijavljeniKorisnik> tokeni = new ConcurrentHashMap<>();

    public String izdaj(PrijavljeniKorisnik korisnik) {
        byte[] bajtovi = new byte[32];
        random.nextBytes(bajtovi);
        String token = Base64.getUrlEncoder().withoutPadding().encodeToString(bajtovi);
        tokeni.put(token, korisnik);
        return token;
    }

    public PrijavljeniKorisnik korisnik(String token) {
        return token == null ? null : tokeni.get(token);
    }
}

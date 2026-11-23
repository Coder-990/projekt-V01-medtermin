package hr.span.akademija.medtermin.platforma;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Component;

@Component
public class LozinkaHasher {
    // M05: Raniji SHA-256 bio je brz i bez soli, što olakšava pogađanje pri curenju baze.
    // BCrypt za svaki hash stvara sol i koristi faktor rada 12, pa dvije iste lozinke
    // dobivaju različite zapise. Provjera stare SHA-256 vrijednosti ostaje radi migracije.
    private final BCryptPasswordEncoder bcrypt = new BCryptPasswordEncoder(12);

    public String hash(String lozinka) {
        return bcrypt.encode(lozinka);
    }

    public boolean provjeri(String lozinka, String spremljeniHash) {
        if (lozinka == null || spremljeniHash == null) return false;
        // M05: Format od 64 heksadekadska znaka prepoznaje stare SHA-256 zapise.
        // Nakon uspješne provjere KorisnikService ih može zamijeniti BCrypt hashom;
        // sama provjera ne prepisuje bazu. Usporedba sažetaka koristi MessageDigest.isEqual.
        if (spremljeniHash.matches("[0-9a-f]{64}")) {
            try {
                byte[] hash = MessageDigest.getInstance("SHA-256").digest(lozinka.getBytes(StandardCharsets.UTF_8));
                return MessageDigest.isEqual(HexFormat.of().formatHex(hash).getBytes(StandardCharsets.US_ASCII),
                        spremljeniHash.getBytes(StandardCharsets.US_ASCII));
            } catch (NoSuchAlgorithmException e) {
                throw new IllegalStateException("SHA-256 nije dostupan", e);
            }
        }
        // M05: BCrypt ima granicu od 72 bajta; odbijanje sprečava podudaranje samo prefiksa.
        if (lozinka.getBytes(StandardCharsets.UTF_8).length > 72) return false;
        return bcrypt.matches(lozinka, spremljeniHash);
    }
}

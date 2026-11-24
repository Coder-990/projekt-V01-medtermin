package hr.span.akademija.medtermin.platforma;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Component;

@Component
public class LozinkaHasher {
    private final BCryptPasswordEncoder bcrypt = new BCryptPasswordEncoder(12);

    public String hash(String lozinka) {
        return bcrypt.encode(lozinka);
    }

    public boolean provjeri(String lozinka, String spremljeniHash) {
        if (lozinka == null || spremljeniHash == null) return false;
        if (spremljeniHash.matches("[0-9a-f]{64}")) {
            try {
                byte[] hash = MessageDigest.getInstance("SHA-256").digest(lozinka.getBytes(StandardCharsets.UTF_8));
                return MessageDigest.isEqual(HexFormat.of().formatHex(hash).getBytes(StandardCharsets.US_ASCII),
                        spremljeniHash.getBytes(StandardCharsets.US_ASCII));
            } catch (NoSuchAlgorithmException e) {
                throw new IllegalStateException("SHA-256 nije dostupan", e);
            }
        }
        if (lozinka.getBytes(StandardCharsets.UTF_8).length > 72) return false;
        return bcrypt.matches(lozinka, spremljeniHash);
    }
}

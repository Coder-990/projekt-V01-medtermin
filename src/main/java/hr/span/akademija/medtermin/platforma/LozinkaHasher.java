package hr.span.akademija.medtermin.platforma;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import org.springframework.stereotype.Component;

/** Izračun i provjera sažetka lozinke (registracija, prijava, promjena lozinke, početni podaci). */
@Component
public class LozinkaHasher {

    public String hash(String lozinka) {
        try {
            MessageDigest sha = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(sha.digest(lozinka.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 nije dostupan", e);
        }
    }

    public boolean provjeri(String lozinka, String spremljeniHash) {
        if (spremljeniHash == null) {
            return false;
        }
        return MessageDigest.isEqual(hash(lozinka).getBytes(StandardCharsets.UTF_8),
                spremljeniHash.getBytes(StandardCharsets.UTF_8));
    }
}

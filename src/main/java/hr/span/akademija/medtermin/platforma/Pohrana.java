package hr.span.akademija.medtermin.platforma;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import org.springframework.stereotype.Component;

/** Mape s dokumentima i poslanim porukama ({@code app.storage-dir}, {@code app.mailbox-dir}). */
@Component
public class Pohrana {

    private final Path dokumenti;
    private final Path posta;

    public Pohrana(AppProperties postavke) {
        this.dokumenti = Path.of(postavke.storageDir());
        this.posta = Path.of(postavke.mailboxDir());
    }

    public Path dokumenti() {
        return dokumenti;
    }

    public Path posta() {
        return posta;
    }

    public void pripremi() throws IOException {
        Files.createDirectories(dokumenti);
        Files.createDirectories(posta);
    }

    public void zapisiAkoNePostoji(String naziv, String sadrzaj) throws IOException {
        Path datoteka = dokumenti.resolve(naziv);
        if (!Files.exists(datoteka)) {
            Files.writeString(datoteka, sadrzaj, StandardCharsets.UTF_8);
        }
    }
}

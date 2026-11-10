package hr.span.akademija.medtermin.platforma;

import java.util.List;
import org.springframework.boot.context.properties.ConfigurationProperties;

/** Postavke aplikacije iz {@code application.properties} (prefiks {@code app}). */
@ConfigurationProperties(prefix = "app")
public record AppProperties(String naziv, String storageDir, String mailboxDir, Cors cors) {

    public record Cors(List<String> allowedOrigins) {
    }

    public List<String> dopusteniOrigini() {
        if (cors == null || cors.allowedOrigins() == null) {
            return List.of();
        }
        return cors.allowedOrigins().stream().map(String::trim).filter(o -> !o.isEmpty()).toList();
    }
}

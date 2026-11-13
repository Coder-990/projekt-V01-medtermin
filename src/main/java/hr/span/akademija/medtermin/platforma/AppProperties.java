package hr.span.akademija.medtermin.platforma;

import java.util.List;
import org.springframework.boot.context.properties.ConfigurationProperties;

/** Postavke aplikacije iz {@code application.properties} (prefiks {@code app}). */
@ConfigurationProperties(prefix = "app")
public record AppProperties(String naziv, String storageDir, String mailboxDir, Cors cors) {

    // TODO(sec): spotbugs EI_EXPOSE_REP [MALICIOUS_CODE; provjeriti nalaz]: hr.span.akademija.medtermin.platforma.AppProperties$Cors.allowedOrigins() may expose internal representation by returning AppProperties$Cors.allowedOrigins
    // TODO(sec): spotbugs EI_EXPOSE_REP2 [MALICIOUS_CODE; provjeriti nalaz]: new hr.span.akademija.medtermin.platforma.AppProperties$Cors(List) may expose internal representation by storing an externally mutable object into AppProperties$Cors.allowedOrigins
    public record Cors(List<String> allowedOrigins) {
    }

    public List<String> dopusteniOrigini() {
        if (cors == null || cors.allowedOrigins() == null) {
            return List.of();
        }
        return cors.allowedOrigins().stream().map(String::trim).filter(o -> !o.isEmpty()).toList();
    }
}

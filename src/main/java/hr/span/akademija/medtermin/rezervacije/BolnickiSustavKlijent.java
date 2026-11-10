package hr.span.akademija.medtermin.rezervacije;

import java.time.Duration;
import java.util.concurrent.atomic.AtomicLong;
import org.springframework.stereotype.Component;

/**
 * Klijent bolničkog informacijskog sustava (BIS) koji upisuje pacijenta u raspored ordinacije.
 * U lokalnom okruženju BIS nije dostupan pa klijent nakon uobičajenog vremena odziva vraća
 * lokalno generiranu oznaku upisa.
 */
@Component
public class BolnickiSustavKlijent {

    private static final Duration VRIJEME_ODZIVA = Duration.ofMillis(150);

    private final AtomicLong brojac = new AtomicLong(1000);

    public String upisiURaspored(long terminId, String pacijent) {
        try {
            Thread.sleep(VRIJEME_ODZIVA);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Poziv prema BIS-u je prekinut", e);
        }
        return "BIS-" + terminId + "-" + brojac.incrementAndGet();
    }
}

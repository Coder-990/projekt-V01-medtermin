package hr.span.akademija.medtermin.platforma;

import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.TypeMismatchException;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.ErrorResponse;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/** Jedinstven format grešaka: {@code { "greska": "<poruka>" }}. */
@RestControllerAdvice
public class GreskeHandler {

    private static final Logger log = LoggerFactory.getLogger(GreskeHandler.class);

    @ExceptionHandler(ApiException.class)
    public ResponseEntity<Map<String, String>> api(ApiException ex) {
        return greska(ex.status(), ex.getMessage());
    }

    @ExceptionHandler({HttpMessageNotReadableException.class, TypeMismatchException.class})
    public ResponseEntity<Map<String, String>> neispravanZahtjev(Exception ex) {
        return greska(HttpStatus.BAD_REQUEST, "Neispravan zahtjev.");
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, String>> ostalo(Exception ex) {
        if (ex instanceof ErrorResponse odgovor) {
            HttpStatusCode status = odgovor.getStatusCode();
            return greska(status, poruka(status));
        }
        log.error("Neobrađena greška", ex);
        return greska(HttpStatus.INTERNAL_SERVER_ERROR, "Došlo je do pogreške.");
    }

    public static ResponseEntity<Map<String, String>> greska(HttpStatusCode status, String poruka) {
        return ResponseEntity.status(status)
                .contentType(MediaType.APPLICATION_JSON)
                .body(Map.of("greska", poruka));
    }

    private static String poruka(HttpStatusCode status) {
        return switch (status.value()) {
            case 404 -> "Resurs nije pronađen.";
            case 405 -> "Metoda nije podržana.";
            case 406 -> "Traženi format odgovora nije podržan.";
            case 415 -> "Vrsta sadržaja nije podržana.";
            default -> status.is5xxServerError() ? "Došlo je do pogreške." : "Neispravan zahtjev.";
        };
    }
}

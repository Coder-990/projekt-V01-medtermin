package hr.span.akademija.medtermin.platforma;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.ErrorResponseException;

class GreskeHandlerTest {

    private final GreskeHandler handler = new GreskeHandler();

    @Test
    @DisplayName("ApiException vraća status i poruku iz iznimke")
    void apiException() {
        ApiException ex = new ApiException(HttpStatus.NOT_FOUND, "Korisnik nije pronađen.");
        ResponseEntity<Map<String, String>> odgovor = handler.api(ex);

        assertThat(odgovor.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(odgovor.getBody()).containsEntry("greska", "Korisnik nije pronađen.");
    }

    @Test
    @DisplayName("ApiException pomoćne metode za statuse")
    void apiExceptionPomocneMetode() {
        ApiException notFound = ApiException.nijePronadeno("Nema");
        assertThat(notFound.status()).isEqualTo(HttpStatus.NOT_FOUND);

        ApiException badReq = ApiException.neispravno("Krivo");
        assertThat(badReq.status()).isEqualTo(HttpStatus.BAD_REQUEST);

        ApiException forbidden = ApiException.zabranjeno();
        assertThat(forbidden.status()).isEqualTo(HttpStatus.FORBIDDEN);
    }

    @Test
    @DisplayName("neispravanZahtjev vraća 400 i poruku Neispravan zahtjev.")
    void neispravanZahtjev() {
        ResponseEntity<Map<String, String>> odgovor = handler.neispravanZahtjev(
                new HttpMessageNotReadableException("Greška pri čitanju"));

        assertThat(odgovor.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(odgovor.getBody()).containsEntry("greska", "Neispravan zahtjev.");
    }

    @Test
    @DisplayName("ostalo s ErrorResponse mapira standardne HTTP statuse")
    void ostaloSErrorResponse() {
        // 404
        ErrorResponseException ex404 = new ErrorResponseException(HttpStatus.NOT_FOUND);
        ResponseEntity<Map<String, String>> odg404 = handler.ostalo(ex404);
        assertThat(odg404.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(odg404.getBody()).containsEntry("greska", "Resurs nije pronađen.");

        // 405
        ErrorResponseException ex405 = new ErrorResponseException(HttpStatus.METHOD_NOT_ALLOWED);
        ResponseEntity<Map<String, String>> odg405 = handler.ostalo(ex405);
        assertThat(odg405.getStatusCode()).isEqualTo(HttpStatus.METHOD_NOT_ALLOWED);
        assertThat(odg405.getBody()).containsEntry("greska", "Metoda nije podržana.");

        // 406
        ErrorResponseException ex406 = new ErrorResponseException(HttpStatus.NOT_ACCEPTABLE);
        ResponseEntity<Map<String, String>> odg406 = handler.ostalo(ex406);
        assertThat(odg406.getStatusCode()).isEqualTo(HttpStatus.NOT_ACCEPTABLE);
        assertThat(odg406.getBody()).containsEntry("greska", "Traženi format odgovora nije podržan.");

        // 415
        ErrorResponseException ex415 = new ErrorResponseException(HttpStatus.UNSUPPORTED_MEDIA_TYPE);
        ResponseEntity<Map<String, String>> odg415 = handler.ostalo(ex415);
        assertThat(odg415.getStatusCode()).isEqualTo(HttpStatus.UNSUPPORTED_MEDIA_TYPE);
        assertThat(odg415.getBody()).containsEntry("greska", "Vrsta sadržaja nije podržana.");

        // 500
        ErrorResponseException ex500 = new ErrorResponseException(HttpStatus.INTERNAL_SERVER_ERROR);
        ResponseEntity<Map<String, String>> odg500 = handler.ostalo(ex500);
        assertThat(odg500.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        assertThat(odg500.getBody()).containsEntry("greska", "Došlo je do pogreške.");

        // default 4xx (npr. 400 ili 422)
        ErrorResponseException ex422 = new ErrorResponseException(HttpStatus.UNPROCESSABLE_ENTITY);
        ResponseEntity<Map<String, String>> odg422 = handler.ostalo(ex422);
        assertThat(odg422.getStatusCode()).isEqualTo(HttpStatus.UNPROCESSABLE_ENTITY);
        assertThat(odg422.getBody()).containsEntry("greska", "Neispravan zahtjev.");
    }

    @Test
    @DisplayName("ostalo s općenitom iznimkom vraća 500 Došlo je do pogreške.")
    void ostaloSOpcenitomIznimkom() {
        ResponseEntity<Map<String, String>> odgovor = handler.ostalo(new RuntimeException("Neočekivani kvar"));

        assertThat(odgovor.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        assertThat(odgovor.getBody()).containsEntry("greska", "Došlo je do pogreške.");
    }
}

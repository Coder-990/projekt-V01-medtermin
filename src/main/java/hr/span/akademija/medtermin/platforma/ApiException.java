package hr.span.akademija.medtermin.platforma;

import org.springframework.http.HttpStatus;

/** Greška koja se klijentu vraća kao {@code { "greska": "..." }} sa zadanim statusom. */
public class ApiException extends RuntimeException {

    private final HttpStatus status;

    public ApiException(HttpStatus status, String poruka) {
        super(poruka);
        this.status = status;
    }

    public HttpStatus status() {
        return status;
    }

    public static ApiException nijePronadeno(String poruka) {
        return new ApiException(HttpStatus.NOT_FOUND, poruka);
    }

    public static ApiException neispravno(String poruka) {
        return new ApiException(HttpStatus.BAD_REQUEST, poruka);
    }

    public static ApiException zabranjeno() {
        return new ApiException(HttpStatus.FORBIDDEN, "Nemate ovlasti za ovu radnju.");
    }
}

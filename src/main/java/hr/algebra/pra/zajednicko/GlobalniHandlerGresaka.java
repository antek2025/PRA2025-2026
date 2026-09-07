package hr.algebra.pra.zajednicko;

import java.util.HashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

/**
 * Hvata iznimke iz svih REST kontrolera i pretvara ih u ApiGreska JSON odgovor.
 * Zahvaljujuci ovome kontroleri ne moraju imati try/catch blokove, nego samo
 * bacaju iznimku i ovdje se odredi ispravan HTTP status i poruka.
 */
@RestControllerAdvice
public class GlobalniHandlerGresaka {

    private static final Logger log = LoggerFactory.getLogger(GlobalniHandlerGresaka.class);

    /** Neispravan ulazni JSON - vraca mapu polje -> poruka da frontend moze oznaciti polja. */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiGreska> validacija(MethodArgumentNotValidException ex) {
        Map<String, String> greskePolja = new HashMap<>();
        for (FieldError greska : ex.getBindingResult().getFieldErrors()) {
            greskePolja.put(greska.getField(), greska.getDefaultMessage());
        }
        ApiGreska tijelo = ApiGreska.validacija(
                HttpStatus.BAD_REQUEST.value(),
                "Podaci nisu ispravno popunjeni.",
                greskePolja);
        return ResponseEntity.badRequest().body(tijelo);
    }

    @ExceptionHandler(NijePronadenoIznimka.class)
    public ResponseEntity<ApiGreska> nijePronadeno(NijePronadenoIznimka ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(ApiGreska.od(HttpStatus.NOT_FOUND.value(), ex.getMessage()));
    }

    @ExceptionHandler(PoslovnaIznimka.class)
    public ResponseEntity<ApiGreska> poslovnoPravilo(PoslovnaIznimka ex) {
        return ResponseEntity.badRequest()
                .body(ApiGreska.od(HttpStatus.BAD_REQUEST.value(), ex.getMessage()));
    }

    @ExceptionHandler(NijePrijavljenIznimka.class)
    public ResponseEntity<ApiGreska> nijePrijavljen(NijePrijavljenIznimka ex) {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(ApiGreska.od(HttpStatus.UNAUTHORIZED.value(), ex.getMessage()));
    }

    @ExceptionHandler(ZabranjenoIznimka.class)
    public ResponseEntity<ApiGreska> zabranjeno(ZabranjenoIznimka ex) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(ApiGreska.od(HttpStatus.FORBIDDEN.value(), ex.getMessage()));
    }

    /*
     * ------------------------------------------------------------------------
     * Greske koje javlja sam Spring, prije nego se uopce dodje do nase logike.
     *
     * Ove handlere je nuzno napisati zato sto ispod stoji @ExceptionHandler(Exception.class),
     * koji bi ih inace sve pokupio i pretvorio u HTTP 500. A nijedna od njih nije
     * greska posluzitelja nego greska u zahtjevu, pa mora vratiti 4xx.
     * ------------------------------------------------------------------------
     */

    /**
     * Trazena datoteka ili adresa ne postoji -> 404.
     *
     * Najcesci slucaj je /favicon.ico, koji preglednik trazi sam od sebe na svakoj
     * stranici. To je posve normalno i ne smije zavrsiti kao greska posluzitelja
     * ni kao stack trace u logu, zato se ovdje samo tiho vraca 404.
     */
    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<ApiGreska> nepostojecaPutanja(NoResourceFoundException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(ApiGreska.od(HttpStatus.NOT_FOUND.value(),
                        "Tražena adresa ne postoji."));
    }

    /**
     * Parametar u putanji nije ocekivanog tipa, npr. /api/kolegiji/abc gdje se
     * ocekuje broj -> 400, jer je korisnik poslao neispravan zahtjev.
     */
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ApiGreska> neispravanTipParametra(MethodArgumentTypeMismatchException ex) {
        return ResponseEntity.badRequest()
                .body(ApiGreska.od(HttpStatus.BAD_REQUEST.value(),
                        "Vrijednost \"" + ex.getValue() + "\" nije ispravna za parametar "
                                + ex.getName() + "."));
    }

    /** Tijelo zahtjeva nije ispravan JSON ili nedostaje -> 400. */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ApiGreska> neispravnoTijeloZahtjeva(HttpMessageNotReadableException ex) {
        return ResponseEntity.badRequest()
                .body(ApiGreska.od(HttpStatus.BAD_REQUEST.value(),
                        "Zahtjev nije u ispravnom JSON formatu."));
    }

    /** Ruta postoji, ali ne podrzava koristenu HTTP metodu -> 405. */
    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<ApiGreska> nepodrzanaMetoda(HttpRequestMethodNotSupportedException ex) {
        return ResponseEntity.status(HttpStatus.METHOD_NOT_ALLOWED)
                .body(ApiGreska.od(HttpStatus.METHOD_NOT_ALLOWED.value(),
                        "Metoda " + ex.getMethod() + " nije podržana na ovoj adresi."));
    }

    /**
     * Sve sto nismo predvidjeli. Poruku iznimke namjerno ne saljemo korisniku
     * (moze sadrzavati detalje o bazi), nego je samo logiramo.
     *
     * Spring uvijek bira NAJUZI handler koji odgovara iznimci, pa ovaj ostaje
     * rezerviran za stvarne, neocekivane greske.
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiGreska> neocekivanaGreska(Exception ex) {
        log.error("Neocekivana greska na posluzitelju", ex);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiGreska.od(HttpStatus.INTERNAL_SERVER_ERROR.value(),
                        "Dogodila se neočekivana greška na poslužitelju."));
    }
}

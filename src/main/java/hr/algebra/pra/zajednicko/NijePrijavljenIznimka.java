package hr.algebra.pra.zajednicko;

/**
 * Korisnik nije prijavljen (nema sesije ili je istekla) -> HTTP 401.
 */
public class NijePrijavljenIznimka extends RuntimeException {

    public NijePrijavljenIznimka(String poruka) {
        super(poruka);
    }
}

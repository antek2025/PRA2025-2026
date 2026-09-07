package hr.algebra.pra.zajednicko;

/**
 * Baca se kada trazeni zapis (korisnik, kolegij, obavijest) ne postoji u bazi.
 * GlobalniHandlerGresaka je pretvara u HTTP 404.
 */
public class NijePronadenoIznimka extends RuntimeException {

    public NijePronadenoIznimka(String poruka) {
        super(poruka);
    }

    public static NijePronadenoIznimka za(String entitet, Long id) {
        return new NijePronadenoIznimka(entitet + " s ID-em " + id + " ne postoji.");
    }
}

package hr.algebra.pra.zajednicko;

/**
 * Korisnik je prijavljen, ali nema pravo na trazenu akciju
 * (npr. predavac pokusava obrisati kolegij) -> HTTP 403.
 */
public class ZabranjenoIznimka extends RuntimeException {

    public ZabranjenoIznimka(String poruka) {
        super(poruka);
    }
}

package hr.algebra.pra.zajednicko;

/**
 * Krsenje nekog poslovnog pravila - npr. email koji se vec koristi ili
 * pokusaj brisanja predavaca koji jos ima obavijesti.
 * GlobalniHandlerGresaka je pretvara u HTTP 400.
 */
public class PoslovnaIznimka extends RuntimeException {

    public PoslovnaIznimka(String poruka) {
        super(poruka);
    }
}

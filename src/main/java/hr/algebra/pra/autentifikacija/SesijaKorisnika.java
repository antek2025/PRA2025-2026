package hr.algebra.pra.autentifikacija;

import org.springframework.stereotype.Component;

import hr.algebra.pra.autentifikacija.dto.PrijavljeniKorisnik;
import hr.algebra.pra.zajednicko.NijePrijavljenIznimka;
import jakarta.servlet.http.HttpSession;

/**
 * Male pomocne metode za rad sa sesijom, na jednom mjestu da se naziv atributa
 * ne prepisuje po kontrolerima.
 *
 * Prijava se pamti tako da se PrijavljeniKorisnik spremi u HttpSession. Preglednik
 * uz svaki zahtjev salje session cookie (INFOEDUKA_SESSION) pa posluzitelj zna
 * tko je prijavljen. Odjava = brisanje sesije.
 */
@Component
public class SesijaKorisnika {

    /** Naziv atributa u sesiji pod kojim cuvamo prijavljenog korisnika. */
    static final String ATRIBUT = "prijavljeniKorisnik";

    public void prijavi(HttpSession sesija, PrijavljeniKorisnik korisnik) {
        sesija.setAttribute(ATRIBUT, korisnik);
    }

    public void odjavi(HttpSession sesija) {
        sesija.invalidate();
    }

    /** Vraca prijavljenog korisnika ili baca 401 ako sesije nema / istekla je. */
    public PrijavljeniKorisnik dohvati(HttpSession sesija) {
        PrijavljeniKorisnik korisnik = procitaj(sesija);
        if (korisnik == null) {
            throw new NijePrijavljenIznimka("Niste prijavljeni ili je sesija istekla.");
        }
        return korisnik;
    }

    /** Vraca prijavljenog korisnika ili null - koristi interceptor i /api/auth/ja. */
    public PrijavljeniKorisnik procitaj(HttpSession sesija) {
        if (sesija == null) {
            return null;
        }
        return (PrijavljeniKorisnik) sesija.getAttribute(ATRIBUT);
    }

    /**
     * Nakon sto korisnik promijeni vlastite podatke ili lozinku, osvjezimo i sesiju
     * da se u zaglavlju odmah vidi novo ime.
     */
    public void osvjezi(HttpSession sesija, PrijavljeniKorisnik korisnik) {
        if (sesija != null && sesija.getAttribute(ATRIBUT) != null) {
            sesija.setAttribute(ATRIBUT, korisnik);
        }
    }
}

package hr.algebra.pra.autentifikacija.dto;

import java.io.Serializable;

import hr.algebra.pra.korisnik.Korisnik;
import hr.algebra.pra.korisnik.Uloga;

/**
 * Podaci o prijavljenom korisniku koji se spremaju u HttpSession i saljemo ih
 * frontendu nakon prijave.
 *
 * Namjerno je "lagan" (bez lozinke i bez JPA entiteta) jer se sprema u sesiju -
 * spremanje cijelog entiteta u sesiju bi znacilo da radimo s odspojenim (detached)
 * objektom i da podaci u sesiji zastare cim se korisnik promijeni u bazi.
 *
 * Serializable je zbog toga sto Tomcat kod restarta moze serijalizirati sesije.
 */
public record PrijavljeniKorisnik(
        Long id,
        String ime,
        String prezime,
        String punoIme,
        String email,
        Uloga uloga,
        String nazivUloge
) implements Serializable {

    public static PrijavljeniKorisnik od(Korisnik korisnik) {
        return new PrijavljeniKorisnik(
                korisnik.getId(),
                korisnik.getIme(),
                korisnik.getPrezime(),
                korisnik.punoIme(),
                korisnik.getEmail(),
                korisnik.getUloga(),
                korisnik.getUloga().getNaziv());
    }

    public boolean jeAdmin() {
        return uloga == Uloga.ADMIN;
    }
}

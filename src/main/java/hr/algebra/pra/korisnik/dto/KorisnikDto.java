package hr.algebra.pra.korisnik.dto;

import java.time.LocalDateTime;

import hr.algebra.pra.korisnik.Korisnik;
import hr.algebra.pra.korisnik.Uloga;

/**
 * Podaci o korisniku koje saljemo frontendu.
 * Namjerno NE sadrzi lozinkaHash - entitet nikad ne ide direktno van.
 */
public record KorisnikDto(
        Long id,
        String ime,
        String prezime,
        String punoIme,
        String email,
        Uloga uloga,
        String nazivUloge,
        boolean aktivan,
        LocalDateTime datumKreiranja
) {

    public static KorisnikDto od(Korisnik korisnik) {
        return new KorisnikDto(
                korisnik.getId(),
                korisnik.getIme(),
                korisnik.getPrezime(),
                korisnik.punoIme(),
                korisnik.getEmail(),
                korisnik.getUloga(),
                korisnik.getUloga().getNaziv(),
                korisnik.isAktivan(),
                korisnik.getDatumKreiranja());
    }
}

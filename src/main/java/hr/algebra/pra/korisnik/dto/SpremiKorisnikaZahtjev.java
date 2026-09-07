package hr.algebra.pra.korisnik.dto;

import hr.algebra.pra.korisnik.Uloga;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * Tijelo zahtjeva za kreiranje i uredjivanje korisnika.
 *
 * Kod uredjivanja se lozinka salje prazna ako je korisnik ne zeli mijenjati,
 * zato ovdje nema @NotBlank nego se duljina provjerava tek ako je nesto upisano
 * (provjera je u KorisnikServis-u).
 */
public record SpremiKorisnikaZahtjev(

        @NotBlank(message = "Ime je obavezno.")
        @Size(max = 50, message = "Ime može imati najviše 50 znakova.")
        String ime,

        @NotBlank(message = "Prezime je obavezno.")
        @Size(max = 50, message = "Prezime može imati najviše 50 znakova.")
        String prezime,

        @NotBlank(message = "E-mail je obavezan.")
        @Email(message = "E-mail nije u ispravnom formatu.")
        @Size(max = 120, message = "E-mail može imati najviše 120 znakova.")
        String email,

        String lozinka,

        @NotNull(message = "Uloga je obavezna.")
        Uloga uloga,

        boolean aktivan
) {
}

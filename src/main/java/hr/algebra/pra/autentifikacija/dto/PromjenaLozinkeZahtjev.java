package hr.algebra.pra.autentifikacija.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Promjena vlastite lozinke iz aplikacije. Trazi se i stara lozinka da netko
 * tko dodje do tudje otvorene sesije ne moze samo tako preuzeti racun.
 */
public record PromjenaLozinkeZahtjev(

        @NotBlank(message = "Stara lozinka je obavezna.")
        String staraLozinka,

        @NotBlank(message = "Nova lozinka je obavezna.")
        @Size(min = 6, message = "Nova lozinka mora imati barem 6 znakova.")
        String novaLozinka
) {
}

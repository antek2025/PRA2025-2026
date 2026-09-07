package hr.algebra.pra.autentifikacija.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * Podaci koje korisnik upisuje na ekranu za prijavu.
 */
public record PrijavaZahtjev(

        @NotBlank(message = "E-mail je obavezan.")
        String email,

        @NotBlank(message = "Lozinka je obavezna.")
        String lozinka
) {
}

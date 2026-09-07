package hr.algebra.pra.obavijest.dto;

import java.time.LocalDate;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * Tijelo zahtjeva kod dodavanja i uredjivanja obavijesti.
 * Odnos datuma (istek ne smije biti prije objave) provjerava se u servisu jer
 * standardne anotacije ne mogu usporediti dva polja medjusobno.
 */
public record SpremiObavijestZahtjev(

        @NotBlank(message = "Naslov obavijesti je obavezan.")
        @Size(max = 150, message = "Naslov može imati najviše 150 znakova.")
        String naslov,

        @NotBlank(message = "Opis obavijesti je obavezan.")
        @Size(max = 2000, message = "Opis može imati najviše 2000 znakova.")
        String opis,

        @NotNull(message = "Datum objave je obavezan.")
        LocalDate datumObjave,

        @NotNull(message = "Datum isteka je obavezan.")
        LocalDate datumIsteka,

        @NotNull(message = "Kolegij je obavezan.")
        Long kolegijId
) {
}

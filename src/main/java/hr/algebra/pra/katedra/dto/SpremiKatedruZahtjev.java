package hr.algebra.pra.katedra.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record SpremiKatedruZahtjev(

        @NotBlank(message = "Kratica katedre je obavezna.")
        @Size(max = 10, message = "Kratica može imati najviše 10 znakova.")
        String kratica,

        @NotBlank(message = "Naziv katedre je obavezan.")
        @Size(max = 100, message = "Naziv može imati najviše 100 znakova.")
        String naziv,

        @Size(max = 500, message = "Opis može imati najviše 500 znakova.")
        String opis
) {
}

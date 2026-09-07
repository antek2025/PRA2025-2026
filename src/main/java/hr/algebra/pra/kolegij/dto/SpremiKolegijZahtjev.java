package hr.algebra.pra.kolegij.dto;

import java.util.List;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * Tijelo zahtjeva kod dodavanja i uredjivanja kolegija.
 * "idPredavaca" je popis ID-eva korisnika koji predaju na kolegiju;
 * ako je prazan, kolegij jednostavno nema dodijeljenog predavaca.
 */
public record SpremiKolegijZahtjev(

        @NotBlank(message = "Šifra kolegija je obavezna.")
        @Size(max = 20, message = "Šifra može imati najviše 20 znakova.")
        String sifra,

        @NotBlank(message = "Naziv kolegija je obavezan.")
        @Size(max = 120, message = "Naziv može imati najviše 120 znakova.")
        String naziv,

        @Size(max = 1000, message = "Opis može imati najviše 1000 znakova.")
        String opis,

        @NotNull(message = "ECTS bodovi su obavezni.")
        @Min(value = 1, message = "ECTS mora biti barem 1.")
        @Max(value = 30, message = "ECTS ne može biti veći od 30.")
        Integer ects,

        @NotNull(message = "Semestar je obavezan.")
        @Min(value = 1, message = "Semestar mora biti između 1 i 6.")
        @Max(value = 6, message = "Semestar mora biti između 1 i 6.")
        Integer semestar,

        boolean aktivan,

        /** Neobavezno - kolegij smije biti bez katedre. */
        Long katedraId,

        List<Long> idPredavaca
) {
}

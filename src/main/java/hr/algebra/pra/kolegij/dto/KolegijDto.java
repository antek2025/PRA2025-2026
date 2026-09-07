package hr.algebra.pra.kolegij.dto;

import java.util.List;

import hr.algebra.pra.kolegij.Kolegij;
import hr.algebra.pra.korisnik.dto.KorisnikDto;

/**
 * Kolegij onako kako ga vidi frontend, zajedno s popisom predavaca i katedrom.
 *
 * @param godinaStudija izracunato iz semestra (vidi Kolegij.godinaStudija) -
 *                      salje se gotovo da frontend ne mora ponavljati isti racun
 * @param katedraId     null ako kolegij jos nije rasporedjen ni na jednu katedru
 */
public record KolegijDto(
        Long id,
        String sifra,
        String naziv,
        String opis,
        Integer ects,
        Integer semestar,
        Integer godinaStudija,
        Long katedraId,
        String katedraKratica,
        String katedraNaziv,
        boolean aktivan,
        List<KorisnikDto> predavaci
) {

    public static KolegijDto od(Kolegij kolegij) {
        List<KorisnikDto> predavaci = kolegij.getPredavaci()
                .stream()
                .map(KorisnikDto::od)
                .sorted((a, b) -> a.prezime().compareToIgnoreCase(b.prezime()))
                .toList();

        boolean imaKatedru = kolegij.getKatedra() != null;

        return new KolegijDto(
                kolegij.getId(),
                kolegij.getSifra(),
                kolegij.getNaziv(),
                kolegij.getOpis(),
                kolegij.getEcts(),
                kolegij.getSemestar(),
                kolegij.godinaStudija(),
                imaKatedru ? kolegij.getKatedra().getId() : null,
                imaKatedru ? kolegij.getKatedra().getKratica() : null,
                imaKatedru ? kolegij.getKatedra().getNaziv() : null,
                kolegij.isAktivan(),
                predavaci);
    }
}

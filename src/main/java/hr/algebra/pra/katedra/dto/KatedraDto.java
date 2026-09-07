package hr.algebra.pra.katedra.dto;

import hr.algebra.pra.katedra.Katedra;

/**
 * @param brojKolegija koliko kolegija pripada katedri - racuna se u servisu
 *                     da administrator na popisu odmah vidi je li katedra u upotrebi
 */
public record KatedraDto(
        Long id,
        String kratica,
        String naziv,
        String opis,
        long brojKolegija
) {

    public static KatedraDto od(Katedra katedra, long brojKolegija) {
        return new KatedraDto(
                katedra.getId(),
                katedra.getKratica(),
                katedra.getNaziv(),
                katedra.getOpis(),
                brojKolegija);
    }
}

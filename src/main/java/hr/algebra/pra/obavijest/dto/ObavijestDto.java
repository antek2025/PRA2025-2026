package hr.algebra.pra.obavijest.dto;

import java.time.LocalDate;
import java.time.LocalDateTime;

import hr.algebra.pra.obavijest.Obavijest;

/**
 * Obavijest pripremljena za prikaz. Osim vlastitih polja nosi i naziv kolegija
 * i ime autora kako frontend ne bi morao raditi dodatne pozive za svaki redak.
 *
 * @param aktivna     je li obavijest danas vidljiva (izmedju objave i isteka)
 * @param smijeUrediti smije li trenutno prijavljeni korisnik urediti/obrisati ovu obavijest;
 *                     racuna se na backendu da frontend ne mora ponavljati ista pravila
 *
 * Polja kolegijGodina i kolegijKatedra* su preslikana s kolegija da se obavijesti
 * mogu filtrirati po godini i katedri bez dodatnog poziva na posluzitelj.
 */
public record ObavijestDto(
        Long id,
        String naslov,
        String opis,
        LocalDate datumObjave,
        LocalDate datumIsteka,
        boolean aktivna,
        Long kolegijId,
        String kolegijSifra,
        String kolegijNaziv,
        Integer kolegijGodina,
        Long kolegijKatedraId,
        String kolegijKatedraKratica,
        Long autorId,
        String autorPunoIme,
        LocalDateTime datumKreiranja,
        boolean smijeUrediti
) {

    public static ObavijestDto od(Obavijest obavijest, LocalDate danas, boolean smijeUrediti) {
        var kolegij = obavijest.getKolegij();
        boolean imaKatedru = kolegij.getKatedra() != null;

        return new ObavijestDto(
                obavijest.getId(),
                obavijest.getNaslov(),
                obavijest.getOpis(),
                obavijest.getDatumObjave(),
                obavijest.getDatumIsteka(),
                obavijest.jeAktivna(danas),
                kolegij.getId(),
                kolegij.getSifra(),
                kolegij.getNaziv(),
                kolegij.godinaStudija(),
                imaKatedru ? kolegij.getKatedra().getId() : null,
                imaKatedru ? kolegij.getKatedra().getKratica() : null,
                obavijest.getAutor().getId(),
                obavijest.getAutor().punoIme(),
                obavijest.getDatumKreiranja(),
                smijeUrediti);
    }
}

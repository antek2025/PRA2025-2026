package hr.algebra.pra.obavijest;

import java.time.LocalDate;
import java.time.LocalDateTime;

import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

import hr.algebra.pra.kolegij.Kolegij;
import hr.algebra.pra.korisnik.Korisnik;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

/**
 * Obavijest vezana uz tocno jedan kolegij.
 *
 * Autor je korisnik koji ju je objavio - cuvamo ga da se zna tko je sto napisao
 * i da predavac moze uredjivati svoje obavijesti.
 */
@Entity
@Table(name = "obavijest")
public class Obavijest {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 150)
    private String naslov;

    @Column(nullable = false, length = 2000)
    private String opis;

    @Column(name = "datum_objave", nullable = false)
    private LocalDate datumObjave;

    @Column(name = "datum_isteka", nullable = false)
    private LocalDate datumIsteka;

    /*
     * LAZY jer kod ispisa obavijesti ne trebamo uvijek cijeli kolegij, a JPA ga
     * ionako ucita kad ga stvarno dohvatimo unutar transakcije.
     *
     * @OnDelete govori Hibernateu da u bazi napravi strani kljuc s ON DELETE CASCADE,
     * pa se brisanjem kolegija automatski brisu i njegove obavijesti. Bez toga bi
     * baza javila gresku zbog stranog kljuca.
     */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "kolegij_id", nullable = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    private Kolegij kolegij;

    /*
     * Autor se NE brise kaskadno - korisnika koji ima obavijesti uopce ne dopustamo
     * obrisati (vidi KorisnikServis.obrisi), nego ga se deaktivira.
     */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "autor_id", nullable = false)
    private Korisnik autor;

    @Column(name = "datum_kreiranja", nullable = false)
    private LocalDateTime datumKreiranja = LocalDateTime.now();

    protected Obavijest() {
        // JPA
    }

    public Obavijest(String naslov, String opis, LocalDate datumObjave, LocalDate datumIsteka,
                     Kolegij kolegij, Korisnik autor) {
        this.naslov = naslov;
        this.opis = opis;
        this.datumObjave = datumObjave;
        this.datumIsteka = datumIsteka;
        this.kolegij = kolegij;
        this.autor = autor;
        this.datumKreiranja = LocalDateTime.now();
    }

    /**
     * Je li obavijest trenutno vidljiva - danas je izmedju datuma objave i isteka.
     * Racuna se u letu jer ovisi o danasnjem datumu, pa nema smisla cuvati u bazi.
     */
    public boolean jeAktivna(LocalDate danas) {
        return !danas.isBefore(datumObjave) && !danas.isAfter(datumIsteka);
    }

    public Long getId() {
        return id;
    }

    public String getNaslov() {
        return naslov;
    }

    public void setNaslov(String naslov) {
        this.naslov = naslov;
    }

    public String getOpis() {
        return opis;
    }

    public void setOpis(String opis) {
        this.opis = opis;
    }

    public LocalDate getDatumObjave() {
        return datumObjave;
    }

    public void setDatumObjave(LocalDate datumObjave) {
        this.datumObjave = datumObjave;
    }

    public LocalDate getDatumIsteka() {
        return datumIsteka;
    }

    public void setDatumIsteka(LocalDate datumIsteka) {
        this.datumIsteka = datumIsteka;
    }

    public Kolegij getKolegij() {
        return kolegij;
    }

    public void setKolegij(Kolegij kolegij) {
        this.kolegij = kolegij;
    }

    public Korisnik getAutor() {
        return autor;
    }

    public LocalDateTime getDatumKreiranja() {
        return datumKreiranja;
    }
}

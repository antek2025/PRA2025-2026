package hr.algebra.pra.kolegij;

import java.util.LinkedHashSet;
import java.util.Set;

import hr.algebra.pra.katedra.Katedra;
import hr.algebra.pra.korisnik.Korisnik;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

/**
 * Kolegij koji se izvodi na ucilistu.
 *
 * Jedan kolegij moze imati vise predavaca, a jedan predavac moze predavati na
 * vise kolegija, pa je veza @ManyToMany preko spojne tablice "kolegij_predavac".
 * Kolegij je vlasnik veze (nema mappedBy), sto znaci da se dodjela predavaca
 * sprema kad se spremi kolegij - a to je i tijek u aplikaciji (admin uredjuje
 * kolegij i na njemu bira predavace).
 */
@Entity
@Table(name = "kolegij")
public class Kolegij {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Kratka oznaka kolegija, npr. "PRA" ili "SUPIT". Jedinstvena je. */
    @Column(nullable = false, unique = true, length = 20)
    private String sifra;

    @Column(nullable = false, length = 120)
    private String naziv;

    @Column(length = 1000)
    private String opis;

    @Column(nullable = false)
    private Integer ects;

    @Column(nullable = false)
    private Integer semestar;

    /*
     * Katedra pod koju kolegij spada. Smije biti prazna jer se novi kolegij
     * moze unijeti prije nego se odluci kojoj katedri pripada.
     *
     * EAGER iz istog razloga kao i predavaci - naziv katedre se prikazuje na
     * svakoj kartici kolegija, a katedri je svega nekoliko.
     */
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "katedra_id")
    private Katedra katedra;

    @Column(nullable = false)
    private boolean aktivan = true;

    /*
     * EAGER je ovdje namjerno: kolegij gotovo uvijek prikazujemo zajedno s
     * popisom predavaca, a broj predavaca po kolegiju je mali (par zapisa).
     * LinkedHashSet cuva redoslijed dodavanja kod prikaza.
     */
    @ManyToMany(fetch = FetchType.EAGER)
    @JoinTable(
            name = "kolegij_predavac",
            joinColumns = @JoinColumn(name = "kolegij_id"),
            inverseJoinColumns = @JoinColumn(name = "korisnik_id"))
    private Set<Korisnik> predavaci = new LinkedHashSet<>();

    protected Kolegij() {
        // JPA
    }

    public Kolegij(String sifra, String naziv, String opis, Integer ects, Integer semestar) {
        this.sifra = sifra;
        this.naziv = naziv;
        this.opis = opis;
        this.ects = ects;
        this.semestar = semestar;
        this.aktivan = true;
    }

    /**
     * Godina studija se NE cuva u bazi nego se racuna iz semestra.
     *
     * Razlog: godina i semestar nisu neovisni podaci - preddiplomski studij ima
     * 6 semestara, po dva na svakoj godini. Da su oba stupca u bazi, netko bi
     * prije ili kasnije spremio "semestar 5, godina 1" i podaci bi si
     * proturjecili. Ovako je semestar jedini izvor istine.
     *
     * semestar 1,2 -> 1. godina;  3,4 -> 2. godina;  5,6 -> 3. godina
     */
    public int godinaStudija() {
        return (semestar + 1) / 2;
    }

    /** Provjera koju koristimo kod obavijesti - smije li ovaj predavac objaviti na kolegiju. */
    public boolean imaPredavaca(Long korisnikId) {
        return predavaci.stream().anyMatch(p -> p.getId().equals(korisnikId));
    }

    public Long getId() {
        return id;
    }

    public String getSifra() {
        return sifra;
    }

    public void setSifra(String sifra) {
        this.sifra = sifra;
    }

    public String getNaziv() {
        return naziv;
    }

    public void setNaziv(String naziv) {
        this.naziv = naziv;
    }

    public String getOpis() {
        return opis;
    }

    public void setOpis(String opis) {
        this.opis = opis;
    }

    public Integer getEcts() {
        return ects;
    }

    public void setEcts(Integer ects) {
        this.ects = ects;
    }

    public Integer getSemestar() {
        return semestar;
    }

    public void setSemestar(Integer semestar) {
        this.semestar = semestar;
    }

    public Katedra getKatedra() {
        return katedra;
    }

    public void setKatedra(Katedra katedra) {
        this.katedra = katedra;
    }

    public boolean isAktivan() {
        return aktivan;
    }

    public void setAktivan(boolean aktivan) {
        this.aktivan = aktivan;
    }

    public Set<Korisnik> getPredavaci() {
        return predavaci;
    }

    /** Kopiramo u novi set jer nam kroz Set.of(...) moze doci nepromjenjiva kolekcija. */
    public void setPredavaci(Set<Korisnik> predavaci) {
        this.predavaci = new LinkedHashSet<>(predavaci);
    }
}

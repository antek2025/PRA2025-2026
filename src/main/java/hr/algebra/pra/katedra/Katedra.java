package hr.algebra.pra.katedra;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * Katedra - ustrojstvena jedinica ucilista pod koju spada skupina kolegija
 * (npr. "Programsko inzenjerstvo", "Racunalne mreze i sigurnost").
 *
 * Uvedena je zato da se kolegiji mogu grupirati i filtrirati po podrucju, a ne
 * samo po semestru. Veza je Katedra 1 : N Kolegij.
 */
@Entity
@Table(name = "katedra")
public class Katedra {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Kratka oznaka, npr. "PI". Prikazuje se na karticama kolegija. */
    @Column(nullable = false, unique = true, length = 10)
    private String kratica;

    @Column(nullable = false, unique = true, length = 100)
    private String naziv;

    @Column(length = 500)
    private String opis;

    protected Katedra() {
        // JPA
    }

    public Katedra(String kratica, String naziv, String opis) {
        this.kratica = kratica;
        this.naziv = naziv;
        this.opis = opis;
    }

    public Long getId() {
        return id;
    }

    public String getKratica() {
        return kratica;
    }

    public void setKratica(String kratica) {
        this.kratica = kratica;
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
}

package hr.algebra.pra.korisnik;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * Korisnik aplikacije - administrator ili predavac.
 * Lozinka se NIKAD ne cuva u citljivom obliku, nego samo kao BCrypt hash.
 */
@Entity
@Table(name = "korisnik")
public class Korisnik {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 50)
    private String ime;

    @Column(nullable = false, length = 50)
    private String prezime;

    /** Email je ujedno i korisnicko ime kod prijave, zato mora biti jedinstven. */
    @Column(nullable = false, unique = true, length = 120)
    private String email;

    @Column(name = "lozinka_hash", nullable = false, length = 100)
    private String lozinkaHash;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Uloga uloga;

    /** Deaktiviran korisnik ostaje u bazi (zbog povijesti obavijesti), ali se ne moze prijaviti. */
    @Column(nullable = false)
    private boolean aktivan = true;

    @Column(name = "datum_kreiranja", nullable = false)
    private LocalDateTime datumKreiranja = LocalDateTime.now();

    protected Korisnik() {
        // JPA trazi prazan konstruktor
    }

    public Korisnik(String ime, String prezime, String email, String lozinkaHash, Uloga uloga) {
        this.ime = ime;
        this.prezime = prezime;
        this.email = email;
        this.lozinkaHash = lozinkaHash;
        this.uloga = uloga;
        this.aktivan = true;
        this.datumKreiranja = LocalDateTime.now();
    }

    public String punoIme() {
        return ime + " " + prezime;
    }

    public boolean jeAdmin() {
        return uloga == Uloga.ADMIN;
    }

    public Long getId() {
        return id;
    }

    public String getIme() {
        return ime;
    }

    public void setIme(String ime) {
        this.ime = ime;
    }

    public String getPrezime() {
        return prezime;
    }

    public void setPrezime(String prezime) {
        this.prezime = prezime;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getLozinkaHash() {
        return lozinkaHash;
    }

    public void setLozinkaHash(String lozinkaHash) {
        this.lozinkaHash = lozinkaHash;
    }

    public Uloga getUloga() {
        return uloga;
    }

    public void setUloga(Uloga uloga) {
        this.uloga = uloga;
    }

    public boolean isAktivan() {
        return aktivan;
    }

    public void setAktivan(boolean aktivan) {
        this.aktivan = aktivan;
    }

    public LocalDateTime getDatumKreiranja() {
        return datumKreiranja;
    }
}

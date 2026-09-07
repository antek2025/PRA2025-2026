package hr.algebra.pra.korisnik;

/**
 * Vrste korisnika u sustavu. Prema zahtjevu postoje samo dvije i fiksne su,
 * pa su modelirane kao enum, a ne kao tablica u bazi.
 */
public enum Uloga {

    /** Vidi i uredjuje sve - predavace, kolegije i sve obavijesti. */
    ADMIN("Administrator"),

    /** Vidi samo kolegije na kojima predaje i obavijesti tih kolegija. */
    PREDAVAC("Predavač");

    private final String naziv;

    Uloga(String naziv) {
        this.naziv = naziv;
    }

    public String getNaziv() {
        return naziv;
    }
}

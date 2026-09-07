package hr.algebra.pra.korisnik;

import java.util.List;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import hr.algebra.pra.kolegij.Kolegij;
import hr.algebra.pra.kolegij.KolegijRepozitorij;
import hr.algebra.pra.korisnik.dto.KorisnikDto;
import hr.algebra.pra.korisnik.dto.SpremiKorisnikaZahtjev;
import hr.algebra.pra.obavijest.ObavijestRepozitorij;
import hr.algebra.pra.zajednicko.NijePronadenoIznimka;
import hr.algebra.pra.zajednicko.PoslovnaIznimka;

/**
 * Poslovna logika vezana uz korisnike (administratore i predavace).
 *
 * Sva pravila koja se ticu podataka (jedinstven email, minimalna duljina lozinke,
 * uvjeti za brisanje) su ovdje, a ne u kontroleru - kontroler samo prima zahtjev
 * i vraca odgovor.
 */
@Service
@Transactional
public class KorisnikServis {

    /** Minimalna duljina lozinke - dovoljno za projekt, ali da nije trivijalno. */
    private static final int MIN_DULJINA_LOZINKE = 6;

    private final KorisnikRepozitorij korisnikRepozitorij;
    private final KolegijRepozitorij kolegijRepozitorij;
    private final ObavijestRepozitorij obavijestRepozitorij;
    private final PasswordEncoder lozinkaEnkoder;

    public KorisnikServis(KorisnikRepozitorij korisnikRepozitorij,
                          KolegijRepozitorij kolegijRepozitorij,
                          ObavijestRepozitorij obavijestRepozitorij,
                          PasswordEncoder lozinkaEnkoder) {
        this.korisnikRepozitorij = korisnikRepozitorij;
        this.kolegijRepozitorij = kolegijRepozitorij;
        this.obavijestRepozitorij = obavijestRepozitorij;
        this.lozinkaEnkoder = lozinkaEnkoder;
    }

    @Transactional(readOnly = true)
    public List<KorisnikDto> dohvatiSve() {
        return korisnikRepozitorij.findAllByOrderByPrezimeAscImeAsc()
                .stream()
                .map(KorisnikDto::od)
                .toList();
    }

    /** Koristi se kod dodjele predavaca na kolegij - admin bira s ovog popisa. */
    @Transactional(readOnly = true)
    public List<KorisnikDto> dohvatiPredavace() {
        return korisnikRepozitorij.findByUlogaOrderByPrezimeAscImeAsc(Uloga.PREDAVAC)
                .stream()
                .map(KorisnikDto::od)
                .toList();
    }

    @Transactional(readOnly = true)
    public KorisnikDto dohvati(Long id) {
        return KorisnikDto.od(dohvatiEntitet(id));
    }

    public Korisnik dohvatiEntitet(Long id) {
        return korisnikRepozitorij.findById(id)
                .orElseThrow(() -> NijePronadenoIznimka.za("Korisnik", id));
    }

    public KorisnikDto kreiraj(SpremiKorisnikaZahtjev zahtjev) {
        String email = ocistiEmail(zahtjev.email());
        if (korisnikRepozitorij.existsByEmailIgnoreCase(email)) {
            throw new PoslovnaIznimka("Korisnik s e-mailom " + email + " već postoji.");
        }
        provjeriLozinku(zahtjev.lozinka(), true);

        Korisnik korisnik = new Korisnik(
                zahtjev.ime().trim(),
                zahtjev.prezime().trim(),
                email,
                lozinkaEnkoder.encode(zahtjev.lozinka()),
                zahtjev.uloga());
        korisnik.setAktivan(zahtjev.aktivan());

        return KorisnikDto.od(korisnikRepozitorij.save(korisnik));
    }

    public KorisnikDto azuriraj(Long id, SpremiKorisnikaZahtjev zahtjev) {
        Korisnik korisnik = dohvatiEntitet(id);
        String email = ocistiEmail(zahtjev.email());

        // Email smije ostati isti, ali ne smije "preuzeti" tudji.
        boolean emailSePromijenio = !korisnik.getEmail().equalsIgnoreCase(email);
        if (emailSePromijenio && korisnikRepozitorij.existsByEmailIgnoreCase(email)) {
            throw new PoslovnaIznimka("Korisnik s e-mailom " + email + " već postoji.");
        }

        korisnik.setIme(zahtjev.ime().trim());
        korisnik.setPrezime(zahtjev.prezime().trim());
        korisnik.setEmail(email);
        korisnik.setUloga(zahtjev.uloga());
        korisnik.setAktivan(zahtjev.aktivan());

        // Prazno polje lozinke kod uredjivanja znaci "ostavi staru lozinku".
        if (zahtjev.lozinka() != null && !zahtjev.lozinka().isBlank()) {
            provjeriLozinku(zahtjev.lozinka(), false);
            korisnik.setLozinkaHash(lozinkaEnkoder.encode(zahtjev.lozinka()));
        }

        return KorisnikDto.od(korisnikRepozitorij.save(korisnik));
    }

    /**
     * Brisanje korisnika. Prije brisanja ga treba maknuti sa svih kolegija jer
     * inace baza javi gresku zbog stranog kljuca u tablici kolegij_predavac.
     *
     * Ako je korisnik napisao neku obavijest, brisanje se ne dopusta - obavijest
     * bi ostala bez autora. U tom slucaju se korisnik moze deaktivirati.
     */
    public void obrisi(Long id, Long idPrijavljenog) {
        Korisnik korisnik = dohvatiEntitet(id);

        if (korisnik.getId().equals(idPrijavljenog)) {
            throw new PoslovnaIznimka("Ne možete obrisati vlastiti korisnički račun.");
        }

        long brojObavijesti = obavijestRepozitorij.countByAutorId(id);
        if (brojObavijesti > 0) {
            throw new PoslovnaIznimka("Korisnik je autor " + brojObavijesti
                    + " obavijesti pa se ne može obrisati. Umjesto brisanja ga deaktivirajte.");
        }

        List<Kolegij> kolegiji = kolegijRepozitorij.findByPredavaciIdOrderByNazivAsc(id);
        for (Kolegij kolegij : kolegiji) {
            kolegij.getPredavaci().remove(korisnik);
        }
        kolegijRepozitorij.saveAll(kolegiji);

        korisnikRepozitorij.delete(korisnik);
    }

    private void provjeriLozinku(String lozinka, boolean obavezna) {
        if (lozinka == null || lozinka.isBlank()) {
            if (obavezna) {
                throw new PoslovnaIznimka("Lozinka je obavezna.");
            }
            return;
        }
        if (lozinka.length() < MIN_DULJINA_LOZINKE) {
            throw new PoslovnaIznimka("Lozinka mora imati barem " + MIN_DULJINA_LOZINKE + " znakova.");
        }
    }

    private String ocistiEmail(String email) {
        return email.trim().toLowerCase();
    }
}

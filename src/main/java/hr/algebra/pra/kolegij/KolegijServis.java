package hr.algebra.pra.kolegij;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import hr.algebra.pra.autentifikacija.dto.PrijavljeniKorisnik;
import hr.algebra.pra.katedra.Katedra;
import hr.algebra.pra.katedra.KatedraServis;
import hr.algebra.pra.kolegij.dto.KolegijDto;
import hr.algebra.pra.kolegij.dto.SpremiKolegijZahtjev;
import hr.algebra.pra.korisnik.Korisnik;
import hr.algebra.pra.korisnik.KorisnikRepozitorij;
import hr.algebra.pra.korisnik.Uloga;
import hr.algebra.pra.zajednicko.NijePronadenoIznimka;
import hr.algebra.pra.zajednicko.PoslovnaIznimka;
import hr.algebra.pra.zajednicko.ZabranjenoIznimka;

/**
 * Poslovna logika za kolegije.
 *
 * Kljucno pravilo iz zahtjeva: administrator radi sa svim kolegijima, a predavac
 * vidi samo one na kojima predaje i ne smije ih dodavati, mijenjati ni brisati.
 * Zato skoro svaka metoda prima i podatke o prijavljenom korisniku.
 */
@Service
@Transactional
public class KolegijServis {

    private final KolegijRepozitorij kolegijRepozitorij;
    private final KorisnikRepozitorij korisnikRepozitorij;
    private final KatedraServis katedraServis;

    public KolegijServis(KolegijRepozitorij kolegijRepozitorij,
                         KorisnikRepozitorij korisnikRepozitorij,
                         KatedraServis katedraServis) {
        this.kolegijRepozitorij = kolegijRepozitorij;
        this.korisnikRepozitorij = korisnikRepozitorij;
        this.katedraServis = katedraServis;
    }

    /**
     * Popis kolegija prilagodjen ulozi:
     * admin dobiva sve, predavac samo svoje.
     */
    @Transactional(readOnly = true)
    public List<KolegijDto> dohvatiZaKorisnika(PrijavljeniKorisnik prijavljeni) {
        List<Kolegij> kolegiji = prijavljeni.jeAdmin()
                ? kolegijRepozitorij.findAllByOrderByNazivAsc()
                : kolegijRepozitorij.findByPredavaciIdOrderByNazivAsc(prijavljeni.id());

        return kolegiji.stream().map(KolegijDto::od).toList();
    }

    @Transactional(readOnly = true)
    public KolegijDto dohvati(Long id, PrijavljeniKorisnik prijavljeni) {
        Kolegij kolegij = dohvatiEntitet(id);
        provjeriSmijeVidjeti(kolegij, prijavljeni);
        return KolegijDto.od(kolegij);
    }

    public Kolegij dohvatiEntitet(Long id) {
        return kolegijRepozitorij.findById(id)
                .orElseThrow(() -> NijePronadenoIznimka.za("Kolegij", id));
    }

    public KolegijDto kreiraj(SpremiKolegijZahtjev zahtjev, PrijavljeniKorisnik prijavljeni) {
        samoAdmin(prijavljeni, "dodavati kolegije");

        String sifra = zahtjev.sifra().trim().toUpperCase();
        if (kolegijRepozitorij.existsBySifraIgnoreCase(sifra)) {
            throw new PoslovnaIznimka("Kolegij sa šifrom " + sifra + " već postoji.");
        }

        Kolegij kolegij = new Kolegij(
                sifra,
                zahtjev.naziv().trim(),
                ocistiOpis(zahtjev.opis()),
                zahtjev.ects(),
                zahtjev.semestar());
        kolegij.setAktivan(zahtjev.aktivan());
        kolegij.setKatedra(ucitajKatedru(zahtjev.katedraId()));
        kolegij.setPredavaci(ucitajPredavace(zahtjev.idPredavaca()));

        return KolegijDto.od(kolegijRepozitorij.save(kolegij));
    }

    public KolegijDto azuriraj(Long id, SpremiKolegijZahtjev zahtjev, PrijavljeniKorisnik prijavljeni) {
        samoAdmin(prijavljeni, "uredjivati kolegije");

        Kolegij kolegij = dohvatiEntitet(id);
        String sifra = zahtjev.sifra().trim().toUpperCase();

        boolean sifraSePromijenila = !kolegij.getSifra().equalsIgnoreCase(sifra);
        if (sifraSePromijenila && kolegijRepozitorij.existsBySifraIgnoreCase(sifra)) {
            throw new PoslovnaIznimka("Kolegij sa šifrom " + sifra + " već postoji.");
        }

        kolegij.setSifra(sifra);
        kolegij.setNaziv(zahtjev.naziv().trim());
        kolegij.setOpis(ocistiOpis(zahtjev.opis()));
        kolegij.setEcts(zahtjev.ects());
        kolegij.setSemestar(zahtjev.semestar());
        kolegij.setAktivan(zahtjev.aktivan());
        kolegij.setKatedra(ucitajKatedru(zahtjev.katedraId()));
        kolegij.setPredavaci(ucitajPredavace(zahtjev.idPredavaca()));

        return KolegijDto.od(kolegijRepozitorij.save(kolegij));
    }

    /**
     * Brisanje kolegija. Obavijesti tog kolegija se brisu zajedno s njim -
     * to je rijeseno na razini baze preko "ON DELETE CASCADE" koji Hibernate
     * generira iz @OnDelete anotacije na Obavijest.kolegij.
     */
    public void obrisi(Long id, PrijavljeniKorisnik prijavljeni) {
        samoAdmin(prijavljeni, "brisati kolegije");

        Kolegij kolegij = dohvatiEntitet(id);
        kolegij.getPredavaci().clear();
        kolegijRepozitorij.delete(kolegij);
    }

    /**
     * Predavac smije raditi s kolegijem samo ako je na njega dodijeljen.
     * Koristi se i iz ObavijestServis-a prije objave obavijesti.
     */
    public void provjeriSmijeVidjeti(Kolegij kolegij, PrijavljeniKorisnik prijavljeni) {
        if (prijavljeni.jeAdmin()) {
            return;
        }
        if (!kolegij.imaPredavaca(prijavljeni.id())) {
            throw new ZabranjenoIznimka("Nemate pristup kolegiju " + kolegij.getNaziv()
                    + " jer na njemu ne predajete.");
        }
    }

    /** null znaci "kolegij bez katedre", sto je dopusteno stanje. */
    private Katedra ucitajKatedru(Long katedraId) {
        return katedraId == null ? null : katedraServis.dohvatiEntitet(katedraId);
    }

    private Set<Korisnik> ucitajPredavace(List<Long> idPredavaca) {
        Set<Korisnik> predavaci = new LinkedHashSet<>();
        if (idPredavaca == null || idPredavaca.isEmpty()) {
            return predavaci;
        }

        for (Long idPredavaca1 : idPredavaca) {
            Korisnik korisnik = korisnikRepozitorij.findById(idPredavaca1)
                    .orElseThrow(() -> NijePronadenoIznimka.za("Korisnik", idPredavaca1));

            // Administrator moze biti dodijeljen kao predavac (npr. voditelj kolegija),
            // ali ovdje to ne dopustamo da popis predavaca ostane jasan.
            if (korisnik.getUloga() != Uloga.PREDAVAC) {
                throw new PoslovnaIznimka("Korisnik " + korisnik.punoIme()
                        + " nije predavač pa se ne može dodijeliti na kolegij.");
            }
            predavaci.add(korisnik);
        }
        return predavaci;
    }

    private void samoAdmin(PrijavljeniKorisnik prijavljeni, String akcija) {
        if (!prijavljeni.jeAdmin()) {
            throw new ZabranjenoIznimka("Samo administrator može " + akcija + ".");
        }
    }

    private String ocistiOpis(String opis) {
        return (opis == null || opis.isBlank()) ? null : opis.trim();
    }
}

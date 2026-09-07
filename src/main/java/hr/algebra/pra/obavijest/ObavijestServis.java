package hr.algebra.pra.obavijest;

import java.time.LocalDate;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import hr.algebra.pra.autentifikacija.dto.PrijavljeniKorisnik;
import hr.algebra.pra.kolegij.Kolegij;
import hr.algebra.pra.kolegij.KolegijRepozitorij;
import hr.algebra.pra.kolegij.KolegijServis;
import hr.algebra.pra.korisnik.Korisnik;
import hr.algebra.pra.korisnik.KorisnikServis;
import hr.algebra.pra.obavijest.dto.ObavijestDto;
import hr.algebra.pra.obavijest.dto.SpremiObavijestZahtjev;
import hr.algebra.pra.zajednicko.NijePronadenoIznimka;
import hr.algebra.pra.zajednicko.PoslovnaIznimka;
import hr.algebra.pra.zajednicko.ZabranjenoIznimka;

/**
 * Poslovna logika za obavijesti.
 *
 * Pravila iz zahtjeva:
 *  - obavijest je uvijek vezana na tocno jedan kolegij,
 *  - administrator moze objaviti/urediti/obrisati obavijest na bilo kojem kolegiju,
 *  - predavac to moze samo na kolegijima na kojima predaje.
 *
 * PRETPOSTAVKA: predavac smije urediti i tudju obavijest na svom kolegiju.
 * Zahtjev kaze "predavac samo za svoje (kolegije)", dakle ogranicenje je po
 * kolegiju, a ne po autoru.
 */
@Service
@Transactional
public class ObavijestServis {

    private final ObavijestRepozitorij obavijestRepozitorij;
    private final KolegijRepozitorij kolegijRepozitorij;
    private final KolegijServis kolegijServis;
    private final KorisnikServis korisnikServis;

    public ObavijestServis(ObavijestRepozitorij obavijestRepozitorij,
                           KolegijRepozitorij kolegijRepozitorij,
                           KolegijServis kolegijServis,
                           KorisnikServis korisnikServis) {
        this.obavijestRepozitorij = obavijestRepozitorij;
        this.kolegijRepozitorij = kolegijRepozitorij;
        this.kolegijServis = kolegijServis;
        this.korisnikServis = korisnikServis;
    }

    /**
     * Popis obavijesti za prijavljenog korisnika.
     *
     * @param kolegijId ako je poslan, vraca se samo obavijesti tog kolegija (filter u suceljima)
     */
    @Transactional(readOnly = true)
    public List<ObavijestDto> dohvatiZaKorisnika(PrijavljeniKorisnik prijavljeni, Long kolegijId) {
        LocalDate danas = LocalDate.now();

        if (kolegijId != null) {
            Kolegij kolegij = kolegijServis.dohvatiEntitet(kolegijId);
            kolegijServis.provjeriSmijeVidjeti(kolegij, prijavljeni);
            return obavijestRepozitorij.findByKolegijIdOrderByDatumObjaveDescIdDesc(kolegijId)
                    .stream()
                    .map(o -> ObavijestDto.od(o, danas, smijeUrediti(o, prijavljeni)))
                    .toList();
        }

        List<Obavijest> obavijesti;
        if (prijavljeni.jeAdmin()) {
            obavijesti = obavijestRepozitorij.findAllByOrderByDatumObjaveDescIdDesc();
        } else {
            List<Long> mojiKolegiji = kolegijRepozitorij
                    .findByPredavaciIdOrderByNazivAsc(prijavljeni.id())
                    .stream()
                    .map(Kolegij::getId)
                    .toList();

            // Predavac bez dodijeljenog kolegija nema sto vidjeti - preskacemo upit.
            obavijesti = mojiKolegiji.isEmpty()
                    ? List.of()
                    : obavijestRepozitorij.findByKolegijIdInOrderByDatumObjaveDescIdDesc(mojiKolegiji);
        }

        return obavijesti.stream()
                .map(o -> ObavijestDto.od(o, danas, smijeUrediti(o, prijavljeni)))
                .toList();
    }

    @Transactional(readOnly = true)
    public ObavijestDto dohvati(Long id, PrijavljeniKorisnik prijavljeni) {
        Obavijest obavijest = dohvatiEntitet(id);
        kolegijServis.provjeriSmijeVidjeti(obavijest.getKolegij(), prijavljeni);
        return ObavijestDto.od(obavijest, LocalDate.now(), smijeUrediti(obavijest, prijavljeni));
    }

    public ObavijestDto kreiraj(SpremiObavijestZahtjev zahtjev, PrijavljeniKorisnik prijavljeni) {
        provjeriDatume(zahtjev);

        Kolegij kolegij = kolegijServis.dohvatiEntitet(zahtjev.kolegijId());
        provjeriSmijeObjaviti(kolegij, prijavljeni);

        Korisnik autor = korisnikServis.dohvatiEntitet(prijavljeni.id());

        Obavijest obavijest = new Obavijest(
                zahtjev.naslov().trim(),
                zahtjev.opis().trim(),
                zahtjev.datumObjave(),
                zahtjev.datumIsteka(),
                kolegij,
                autor);

        Obavijest spremljena = obavijestRepozitorij.save(obavijest);
        return ObavijestDto.od(spremljena, LocalDate.now(), true);
    }

    public ObavijestDto azuriraj(Long id, SpremiObavijestZahtjev zahtjev, PrijavljeniKorisnik prijavljeni) {
        provjeriDatume(zahtjev);

        Obavijest obavijest = dohvatiEntitet(id);
        provjeriSmijeUrediti(obavijest, prijavljeni);

        // Obavijest se moze premjestiti na drugi kolegij, ali samo na onaj
        // na kojem korisnik i inace smije objavljivati.
        Kolegij noviKolegij = kolegijServis.dohvatiEntitet(zahtjev.kolegijId());
        provjeriSmijeObjaviti(noviKolegij, prijavljeni);

        obavijest.setNaslov(zahtjev.naslov().trim());
        obavijest.setOpis(zahtjev.opis().trim());
        obavijest.setDatumObjave(zahtjev.datumObjave());
        obavijest.setDatumIsteka(zahtjev.datumIsteka());
        obavijest.setKolegij(noviKolegij);

        Obavijest spremljena = obavijestRepozitorij.save(obavijest);
        return ObavijestDto.od(spremljena, LocalDate.now(), true);
    }

    public void obrisi(Long id, PrijavljeniKorisnik prijavljeni) {
        Obavijest obavijest = dohvatiEntitet(id);
        provjeriSmijeUrediti(obavijest, prijavljeni);
        obavijestRepozitorij.delete(obavijest);
    }

    public Obavijest dohvatiEntitet(Long id) {
        return obavijestRepozitorij.findById(id)
                .orElseThrow(() -> NijePronadenoIznimka.za("Obavijest", id));
    }

    private boolean smijeUrediti(Obavijest obavijest, PrijavljeniKorisnik prijavljeni) {
        return prijavljeni.jeAdmin() || obavijest.getKolegij().imaPredavaca(prijavljeni.id());
    }

    private void provjeriSmijeUrediti(Obavijest obavijest, PrijavljeniKorisnik prijavljeni) {
        if (!smijeUrediti(obavijest, prijavljeni)) {
            throw new ZabranjenoIznimka(
                    "Obavijest možete uređivati samo na kolegijima na kojima predajete.");
        }
    }

    private void provjeriSmijeObjaviti(Kolegij kolegij, PrijavljeniKorisnik prijavljeni) {
        if (prijavljeni.jeAdmin()) {
            return;
        }
        if (!kolegij.imaPredavaca(prijavljeni.id())) {
            throw new ZabranjenoIznimka("Na kolegiju " + kolegij.getNaziv()
                    + " ne predajete pa na njemu ne možete objaviti obavijest.");
        }
    }

    private void provjeriDatume(SpremiObavijestZahtjev zahtjev) {
        if (zahtjev.datumIsteka().isBefore(zahtjev.datumObjave())) {
            throw new PoslovnaIznimka("Datum isteka ne može biti prije datuma objave.");
        }
    }
}

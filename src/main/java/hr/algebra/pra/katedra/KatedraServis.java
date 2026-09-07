package hr.algebra.pra.katedra;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import hr.algebra.pra.autentifikacija.dto.PrijavljeniKorisnik;
import hr.algebra.pra.katedra.dto.KatedraDto;
import hr.algebra.pra.katedra.dto.SpremiKatedruZahtjev;
import hr.algebra.pra.kolegij.KolegijRepozitorij;
import hr.algebra.pra.zajednicko.NijePronadenoIznimka;
import hr.algebra.pra.zajednicko.PoslovnaIznimka;
import hr.algebra.pra.zajednicko.ZabranjenoIznimka;

/**
 * Poslovna logika za katedre.
 *
 * Katedre su sifrarnik - mijenja ih samo administrator, a citaju ih svi jer se
 * koriste kao filter na popisu kolegija i obavijesti.
 */
@Service
@Transactional
public class KatedraServis {

    private final KatedraRepozitorij katedraRepozitorij;
    private final KolegijRepozitorij kolegijRepozitorij;

    public KatedraServis(KatedraRepozitorij katedraRepozitorij,
                         KolegijRepozitorij kolegijRepozitorij) {
        this.katedraRepozitorij = katedraRepozitorij;
        this.kolegijRepozitorij = kolegijRepozitorij;
    }

    @Transactional(readOnly = true)
    public List<KatedraDto> dohvatiSve() {
        return katedraRepozitorij.findAllByOrderByNazivAsc()
                .stream()
                .map(k -> KatedraDto.od(k, kolegijRepozitorij.countByKatedraId(k.getId())))
                .toList();
    }

    @Transactional(readOnly = true)
    public KatedraDto dohvati(Long id) {
        Katedra katedra = dohvatiEntitet(id);
        return KatedraDto.od(katedra, kolegijRepozitorij.countByKatedraId(id));
    }

    public Katedra dohvatiEntitet(Long id) {
        return katedraRepozitorij.findById(id)
                .orElseThrow(() -> NijePronadenoIznimka.za("Katedra", id));
    }

    public KatedraDto kreiraj(SpremiKatedruZahtjev zahtjev, PrijavljeniKorisnik prijavljeni) {
        samoAdmin(prijavljeni, "dodavati katedre");

        String kratica = zahtjev.kratica().trim().toUpperCase();
        String naziv = zahtjev.naziv().trim();

        if (katedraRepozitorij.existsByKraticaIgnoreCase(kratica)) {
            throw new PoslovnaIznimka("Katedra s kraticom " + kratica + " već postoji.");
        }
        if (katedraRepozitorij.existsByNazivIgnoreCase(naziv)) {
            throw new PoslovnaIznimka("Katedra pod nazivom " + naziv + " već postoji.");
        }

        Katedra katedra = new Katedra(kratica, naziv, ocistiOpis(zahtjev.opis()));
        return KatedraDto.od(katedraRepozitorij.save(katedra), 0);
    }

    public KatedraDto azuriraj(Long id, SpremiKatedruZahtjev zahtjev, PrijavljeniKorisnik prijavljeni) {
        samoAdmin(prijavljeni, "uređivati katedre");

        Katedra katedra = dohvatiEntitet(id);
        String kratica = zahtjev.kratica().trim().toUpperCase();
        String naziv = zahtjev.naziv().trim();

        // Kao i kod kolegija - katedra smije zadrzati svoju kraticu i naziv.
        if (!katedra.getKratica().equalsIgnoreCase(kratica)
                && katedraRepozitorij.existsByKraticaIgnoreCase(kratica)) {
            throw new PoslovnaIznimka("Katedra s kraticom " + kratica + " već postoji.");
        }
        if (!katedra.getNaziv().equalsIgnoreCase(naziv)
                && katedraRepozitorij.existsByNazivIgnoreCase(naziv)) {
            throw new PoslovnaIznimka("Katedra pod nazivom " + naziv + " već postoji.");
        }

        katedra.setKratica(kratica);
        katedra.setNaziv(naziv);
        katedra.setOpis(ocistiOpis(zahtjev.opis()));

        Katedra spremljena = katedraRepozitorij.save(katedra);
        return KatedraDto.od(spremljena, kolegijRepozitorij.countByKatedraId(id));
    }

    /**
     * Brisanje katedre se ne dopusta dok na njoj ima kolegija.
     *
     * Isti pristup kao kod brisanja korisnika koji je autor obavijesti: radije
     * jasna poruka nego tiho odspajanje dvadesetak kolegija koje bi korisnik
     * primijetio tek kasnije.
     */
    public void obrisi(Long id, PrijavljeniKorisnik prijavljeni) {
        samoAdmin(prijavljeni, "brisati katedre");

        Katedra katedra = dohvatiEntitet(id);
        long brojKolegija = kolegijRepozitorij.countByKatedraId(id);
        if (brojKolegija > 0) {
            throw new PoslovnaIznimka("Katedra " + katedra.getNaziv() + " ima " + brojKolegija
                    + " kolegija pa se ne može obrisati. Prvo ih premjestite na drugu katedru.");
        }

        katedraRepozitorij.delete(katedra);
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

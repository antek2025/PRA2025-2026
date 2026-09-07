package hr.algebra.pra.korisnik;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import hr.algebra.pra.autentifikacija.SesijaKorisnika;
import hr.algebra.pra.autentifikacija.dto.PrijavljeniKorisnik;
import hr.algebra.pra.korisnik.dto.KorisnikDto;
import hr.algebra.pra.korisnik.dto.SpremiKorisnikaZahtjev;
import hr.algebra.pra.zajednicko.ZabranjenoIznimka;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;

/**
 * REST rute za upravljanje korisnicima (predavacima i administratorima).
 *
 * Cijeli ovaj kontroler je dostupan samo administratoru - jedina iznimka je
 * GET /api/korisnici/predavaci koji treba i predavac kad gleda kolegij.
 *
 * Kontroler namjerno ne sadrzi poslovnu logiku: samo procita sesiju, pozove
 * servis i vrati rezultat.
 */
@RestController
@RequestMapping("/api/korisnici")
public class KorisnikKontroler {

    private final KorisnikServis korisnikServis;
    private final SesijaKorisnika sesijaKorisnika;

    public KorisnikKontroler(KorisnikServis korisnikServis, SesijaKorisnika sesijaKorisnika) {
        this.korisnikServis = korisnikServis;
        this.sesijaKorisnika = sesijaKorisnika;
    }

    @GetMapping
    public List<KorisnikDto> dohvatiSve(HttpSession sesija) {
        samoAdmin(sesija);
        return korisnikServis.dohvatiSve();
    }

    /** Popis predavaca za padajuci izbornik kod dodjele na kolegij. */
    @GetMapping("/predavaci")
    public List<KorisnikDto> dohvatiPredavace(HttpSession sesija) {
        sesijaKorisnika.dohvati(sesija);
        return korisnikServis.dohvatiPredavace();
    }

    @GetMapping("/{id}")
    public KorisnikDto dohvati(@PathVariable Long id, HttpSession sesija) {
        samoAdmin(sesija);
        return korisnikServis.dohvati(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public KorisnikDto kreiraj(@Valid @RequestBody SpremiKorisnikaZahtjev zahtjev, HttpSession sesija) {
        samoAdmin(sesija);
        return korisnikServis.kreiraj(zahtjev);
    }

    @PutMapping("/{id}")
    public KorisnikDto azuriraj(@PathVariable Long id,
                                @Valid @RequestBody SpremiKorisnikaZahtjev zahtjev,
                                HttpSession sesija) {
        samoAdmin(sesija);
        return korisnikServis.azuriraj(id, zahtjev);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> obrisi(@PathVariable Long id, HttpSession sesija) {
        PrijavljeniKorisnik prijavljeni = samoAdmin(sesija);
        korisnikServis.obrisi(id, prijavljeni.id());
        return ResponseEntity.noContent().build();
    }

    private PrijavljeniKorisnik samoAdmin(HttpSession sesija) {
        PrijavljeniKorisnik prijavljeni = sesijaKorisnika.dohvati(sesija);
        if (!prijavljeni.jeAdmin()) {
            throw new ZabranjenoIznimka("Samo administrator može upravljati korisnicima.");
        }
        return prijavljeni;
    }
}

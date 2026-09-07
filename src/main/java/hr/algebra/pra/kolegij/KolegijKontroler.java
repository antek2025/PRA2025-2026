package hr.algebra.pra.kolegij;

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
import hr.algebra.pra.kolegij.dto.KolegijDto;
import hr.algebra.pra.kolegij.dto.SpremiKolegijZahtjev;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;

/**
 * REST rute za kolegije.
 *
 * GET rute su dostupne svim prijavljenima (servis sam suzi popis predavacu na
 * njegove kolegije), a POST/PUT/DELETE odbija servis ako korisnik nije admin.
 */
@RestController
@RequestMapping("/api/kolegiji")
public class KolegijKontroler {

    private final KolegijServis kolegijServis;
    private final SesijaKorisnika sesijaKorisnika;

    public KolegijKontroler(KolegijServis kolegijServis, SesijaKorisnika sesijaKorisnika) {
        this.kolegijServis = kolegijServis;
        this.sesijaKorisnika = sesijaKorisnika;
    }

    @GetMapping
    public List<KolegijDto> dohvatiSve(HttpSession sesija) {
        return kolegijServis.dohvatiZaKorisnika(sesijaKorisnika.dohvati(sesija));
    }

    @GetMapping("/{id}")
    public KolegijDto dohvati(@PathVariable Long id, HttpSession sesija) {
        return kolegijServis.dohvati(id, sesijaKorisnika.dohvati(sesija));
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public KolegijDto kreiraj(@Valid @RequestBody SpremiKolegijZahtjev zahtjev, HttpSession sesija) {
        PrijavljeniKorisnik prijavljeni = sesijaKorisnika.dohvati(sesija);
        return kolegijServis.kreiraj(zahtjev, prijavljeni);
    }

    @PutMapping("/{id}")
    public KolegijDto azuriraj(@PathVariable Long id,
                               @Valid @RequestBody SpremiKolegijZahtjev zahtjev,
                               HttpSession sesija) {
        PrijavljeniKorisnik prijavljeni = sesijaKorisnika.dohvati(sesija);
        return kolegijServis.azuriraj(id, zahtjev, prijavljeni);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> obrisi(@PathVariable Long id, HttpSession sesija) {
        kolegijServis.obrisi(id, sesijaKorisnika.dohvati(sesija));
        return ResponseEntity.noContent().build();
    }
}

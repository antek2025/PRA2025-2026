package hr.algebra.pra.katedra;

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
import hr.algebra.pra.katedra.dto.KatedraDto;
import hr.algebra.pra.katedra.dto.SpremiKatedruZahtjev;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;

/**
 * REST rute za katedre.
 *
 * GET je dostupan svim prijavljenima jer i predavac treba popis katedri za
 * filter na kolegijima i obavijestima. Izmjene odbija servis ako korisnik
 * nije administrator.
 */
@RestController
@RequestMapping("/api/katedre")
public class KatedraKontroler {

    private final KatedraServis katedraServis;
    private final SesijaKorisnika sesijaKorisnika;

    public KatedraKontroler(KatedraServis katedraServis, SesijaKorisnika sesijaKorisnika) {
        this.katedraServis = katedraServis;
        this.sesijaKorisnika = sesijaKorisnika;
    }

    @GetMapping
    public List<KatedraDto> dohvatiSve(HttpSession sesija) {
        sesijaKorisnika.dohvati(sesija);
        return katedraServis.dohvatiSve();
    }

    @GetMapping("/{id}")
    public KatedraDto dohvati(@PathVariable Long id, HttpSession sesija) {
        sesijaKorisnika.dohvati(sesija);
        return katedraServis.dohvati(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public KatedraDto kreiraj(@Valid @RequestBody SpremiKatedruZahtjev zahtjev, HttpSession sesija) {
        return katedraServis.kreiraj(zahtjev, sesijaKorisnika.dohvati(sesija));
    }

    @PutMapping("/{id}")
    public KatedraDto azuriraj(@PathVariable Long id,
                               @Valid @RequestBody SpremiKatedruZahtjev zahtjev,
                               HttpSession sesija) {
        return katedraServis.azuriraj(id, zahtjev, sesijaKorisnika.dohvati(sesija));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> obrisi(@PathVariable Long id, HttpSession sesija) {
        katedraServis.obrisi(id, sesijaKorisnika.dohvati(sesija));
        return ResponseEntity.noContent().build();
    }
}

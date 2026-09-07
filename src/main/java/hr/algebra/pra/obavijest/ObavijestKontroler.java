package hr.algebra.pra.obavijest;

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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import hr.algebra.pra.autentifikacija.SesijaKorisnika;
import hr.algebra.pra.autentifikacija.dto.PrijavljeniKorisnik;
import hr.algebra.pra.obavijest.dto.ObavijestDto;
import hr.algebra.pra.obavijest.dto.SpremiObavijestZahtjev;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;

/**
 * REST rute za obavijesti.
 *
 * Svi prijavljeni korisnici mogu dodavati obavijesti - razlika je samo u tome
 * na kojim kolegijima, a to provjerava ObavijestServis.
 */
@RestController
@RequestMapping("/api/obavijesti")
public class ObavijestKontroler {

    private final ObavijestServis obavijestServis;
    private final SesijaKorisnika sesijaKorisnika;

    public ObavijestKontroler(ObavijestServis obavijestServis, SesijaKorisnika sesijaKorisnika) {
        this.obavijestServis = obavijestServis;
        this.sesijaKorisnika = sesijaKorisnika;
    }

    /**
     * @param kolegijId neobavezan filter - ako je poslan, vracaju se samo obavijesti tog kolegija
     */
    @GetMapping
    public List<ObavijestDto> dohvatiSve(@RequestParam(required = false) Long kolegijId,
                                         HttpSession sesija) {
        PrijavljeniKorisnik prijavljeni = sesijaKorisnika.dohvati(sesija);
        return obavijestServis.dohvatiZaKorisnika(prijavljeni, kolegijId);
    }

    @GetMapping("/{id}")
    public ObavijestDto dohvati(@PathVariable Long id, HttpSession sesija) {
        return obavijestServis.dohvati(id, sesijaKorisnika.dohvati(sesija));
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ObavijestDto kreiraj(@Valid @RequestBody SpremiObavijestZahtjev zahtjev, HttpSession sesija) {
        PrijavljeniKorisnik prijavljeni = sesijaKorisnika.dohvati(sesija);
        return obavijestServis.kreiraj(zahtjev, prijavljeni);
    }

    @PutMapping("/{id}")
    public ObavijestDto azuriraj(@PathVariable Long id,
                                 @Valid @RequestBody SpremiObavijestZahtjev zahtjev,
                                 HttpSession sesija) {
        PrijavljeniKorisnik prijavljeni = sesijaKorisnika.dohvati(sesija);
        return obavijestServis.azuriraj(id, zahtjev, prijavljeni);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> obrisi(@PathVariable Long id, HttpSession sesija) {
        obavijestServis.obrisi(id, sesijaKorisnika.dohvati(sesija));
        return ResponseEntity.noContent().build();
    }
}

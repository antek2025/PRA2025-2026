package hr.algebra.pra.autentifikacija;

import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import hr.algebra.pra.autentifikacija.dto.PrijavaZahtjev;
import hr.algebra.pra.autentifikacija.dto.PrijavljeniKorisnik;
import hr.algebra.pra.autentifikacija.dto.PromjenaLozinkeZahtjev;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;

/**
 * Prijava, odjava i podaci o trenutno prijavljenom korisniku.
 *
 * Ove rute su izuzete iz AuthInterceptor-a (osim promjene lozinke) jer im se
 * pristupa prije nego sto sesija uopce postoji.
 */
@RestController
@RequestMapping("/api/auth")
public class AuthKontroler {

    private final AuthServis authServis;
    private final SesijaKorisnika sesijaKorisnika;

    public AuthKontroler(AuthServis authServis, SesijaKorisnika sesijaKorisnika) {
        this.authServis = authServis;
        this.sesijaKorisnika = sesijaKorisnika;
    }

    @PostMapping("/prijava")
    public PrijavljeniKorisnik prijava(@Valid @RequestBody PrijavaZahtjev zahtjev,
                                       HttpServletRequest httpZahtjev) {
        PrijavljeniKorisnik korisnik = authServis.prijava(zahtjev);

        // Stara sesija (ako postoji) se ponistava i radi nova - time se sprjecava
        // "session fixation", tj. da napadac unaprijed podmetne ID sesije.
        HttpSession stara = httpZahtjev.getSession(false);
        if (stara != null) {
            stara.invalidate();
        }
        sesijaKorisnika.prijavi(httpZahtjev.getSession(true), korisnik);

        return korisnik;
    }

    @PostMapping("/odjava")
    public ResponseEntity<Void> odjava(HttpSession sesija) {
        if (sesija != null) {
            sesijaKorisnika.odjavi(sesija);
        }
        return ResponseEntity.noContent().build();
    }

    /**
     * Frontend ovo zove pri ucitavanju svake stranice da provjeri je li korisnik
     * jos prijavljen i da si popuni zaglavlje. Ako nije, vraca 200 s prijavljen=false
     * (a ne 401) jer ovo nije greska nego normalno stanje na ekranu za prijavu.
     */
    @GetMapping("/ja")
    public Map<String, Object> trenutniKorisnik(HttpSession sesija) {
        PrijavljeniKorisnik korisnik = sesijaKorisnika.procitaj(sesija);
        if (korisnik == null) {
            return Map.of("prijavljen", false);
        }
        return Map.of("prijavljen", true, "korisnik", korisnik);
    }

    @PostMapping("/promjena-lozinke")
    public PrijavljeniKorisnik promjenaLozinke(@Valid @RequestBody PromjenaLozinkeZahtjev zahtjev,
                                               HttpSession sesija) {
        PrijavljeniKorisnik prijavljeni = sesijaKorisnika.dohvati(sesija);
        PrijavljeniKorisnik osvjezen = authServis.promijeniLozinku(prijavljeni.id(), zahtjev);
        sesijaKorisnika.osvjezi(sesija, osvjezen);
        return osvjezen;
    }
}

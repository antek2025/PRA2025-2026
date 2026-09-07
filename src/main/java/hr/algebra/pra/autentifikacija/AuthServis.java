package hr.algebra.pra.autentifikacija;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import hr.algebra.pra.autentifikacija.dto.PrijavaZahtjev;
import hr.algebra.pra.autentifikacija.dto.PrijavljeniKorisnik;
import hr.algebra.pra.autentifikacija.dto.PromjenaLozinkeZahtjev;
import hr.algebra.pra.korisnik.Korisnik;
import hr.algebra.pra.korisnik.KorisnikRepozitorij;
import hr.algebra.pra.zajednicko.NijePrijavljenIznimka;
import hr.algebra.pra.zajednicko.PoslovnaIznimka;

/**
 * Provjera prijave i promjena lozinke.
 *
 * Lozinke se usporedjuju preko BCrypta - u bazi je samo hash, a PasswordEncoder
 * sam iz hasha procita sol i broj rundi pa hash lozinke koju je korisnik upisao.
 */
@Service
@Transactional
public class AuthServis {

    private final KorisnikRepozitorij korisnikRepozitorij;
    private final PasswordEncoder lozinkaEnkoder;

    public AuthServis(KorisnikRepozitorij korisnikRepozitorij, PasswordEncoder lozinkaEnkoder) {
        this.korisnikRepozitorij = korisnikRepozitorij;
        this.lozinkaEnkoder = lozinkaEnkoder;
    }

    /**
     * Provjerava email i lozinku.
     *
     * Namjerno vracamo istu poruku i kad email ne postoji i kad je lozinka kriva -
     * inace bi netko mogao "pogadjanjem" doznati koje email adrese postoje u sustavu.
     */
    @Transactional(readOnly = true)
    public PrijavljeniKorisnik prijava(PrijavaZahtjev zahtjev) {
        Korisnik korisnik = korisnikRepozitorij
                .findByEmailIgnoreCase(zahtjev.email().trim())
                .orElseThrow(() -> new NijePrijavljenIznimka("Neispravan e-mail ili lozinka."));

        if (!lozinkaEnkoder.matches(zahtjev.lozinka(), korisnik.getLozinkaHash())) {
            throw new NijePrijavljenIznimka("Neispravan e-mail ili lozinka.");
        }

        if (!korisnik.isAktivan()) {
            throw new NijePrijavljenIznimka(
                    "Korisnički račun je deaktiviran. Javite se administratoru.");
        }

        return PrijavljeniKorisnik.od(korisnik);
    }

    public PrijavljeniKorisnik promijeniLozinku(Long korisnikId, PromjenaLozinkeZahtjev zahtjev) {
        Korisnik korisnik = korisnikRepozitorij.findById(korisnikId)
                .orElseThrow(() -> new NijePrijavljenIznimka("Korisnik više ne postoji."));

        if (!lozinkaEnkoder.matches(zahtjev.staraLozinka(), korisnik.getLozinkaHash())) {
            throw new PoslovnaIznimka("Stara lozinka nije ispravna.");
        }
        if (zahtjev.staraLozinka().equals(zahtjev.novaLozinka())) {
            throw new PoslovnaIznimka("Nova lozinka mora biti različita od stare.");
        }

        korisnik.setLozinkaHash(lozinkaEnkoder.encode(zahtjev.novaLozinka()));
        korisnikRepozitorij.save(korisnik);

        return PrijavljeniKorisnik.od(korisnik);
    }
}

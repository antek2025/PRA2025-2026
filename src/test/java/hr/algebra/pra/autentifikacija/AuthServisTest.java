package hr.algebra.pra.autentifikacija;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import hr.algebra.pra.autentifikacija.dto.PrijavaZahtjev;
import hr.algebra.pra.autentifikacija.dto.PrijavljeniKorisnik;
import hr.algebra.pra.autentifikacija.dto.PromjenaLozinkeZahtjev;
import hr.algebra.pra.korisnik.Korisnik;
import hr.algebra.pra.korisnik.KorisnikRepozitorij;
import hr.algebra.pra.korisnik.Uloga;
import hr.algebra.pra.zajednicko.NijePrijavljenIznimka;
import hr.algebra.pra.zajednicko.PoslovnaIznimka;

/**
 * Testovi prijave i promjene lozinke.
 *
 * Repozitorij i enkoder su mockovi pa test ne treba bazu ni podignut Spring
 * kontekst - provjerava se samo logika u AuthServis-u.
 */
@ExtendWith(MockitoExtension.class)
class AuthServisTest {

    @Mock
    private KorisnikRepozitorij korisnikRepozitorij;

    @Mock
    private PasswordEncoder lozinkaEnkoder;

    @InjectMocks
    private AuthServis authServis;

    private Korisnik predavac;

    @BeforeEach
    void pripremi() {
        predavac = new Korisnik("Ivan", "Horvat", "ivan.horvat@algebra.hr",
                "hash-stare-lozinke", Uloga.PREDAVAC);
    }

    @Test
    @DisplayName("Prijava s ispravnim podacima vraca prijavljenog korisnika")
    void prijavaSIspravnimPodacima() {
        when(korisnikRepozitorij.findByEmailIgnoreCase("ivan.horvat@algebra.hr"))
                .thenReturn(Optional.of(predavac));
        when(lozinkaEnkoder.matches("tajna123", "hash-stare-lozinke")).thenReturn(true);

        PrijavljeniKorisnik rezultat = authServis.prijava(
                new PrijavaZahtjev("ivan.horvat@algebra.hr", "tajna123"));

        assertThat(rezultat.email()).isEqualTo("ivan.horvat@algebra.hr");
        assertThat(rezultat.uloga()).isEqualTo(Uloga.PREDAVAC);
        assertThat(rezultat.jeAdmin()).isFalse();
    }

    @Test
    @DisplayName("Prijava s krivom lozinkom baca iznimku")
    void prijavaSKrivomLozinkom() {
        when(korisnikRepozitorij.findByEmailIgnoreCase(anyString()))
                .thenReturn(Optional.of(predavac));
        when(lozinkaEnkoder.matches(anyString(), anyString())).thenReturn(false);

        assertThatThrownBy(() -> authServis.prijava(
                new PrijavaZahtjev("ivan.horvat@algebra.hr", "krivo")))
                .isInstanceOf(NijePrijavljenIznimka.class)
                .hasMessageContaining("Neispravan e-mail ili lozinka");
    }

    @Test
    @DisplayName("Nepostojeci email daje istu poruku kao i kriva lozinka")
    void prijavaSNepostojecimEmailom() {
        when(korisnikRepozitorij.findByEmailIgnoreCase(anyString()))
                .thenReturn(Optional.empty());

        // Poruka je namjerno ista da se ne moze doznati koji emailovi postoje.
        assertThatThrownBy(() -> authServis.prijava(
                new PrijavaZahtjev("netko@algebra.hr", "bilosto")))
                .isInstanceOf(NijePrijavljenIznimka.class)
                .hasMessageContaining("Neispravan e-mail ili lozinka");
    }

    @Test
    @DisplayName("Deaktivirani korisnik se ne moze prijaviti")
    void prijavaDeaktiviranogKorisnika() {
        predavac.setAktivan(false);
        when(korisnikRepozitorij.findByEmailIgnoreCase(anyString()))
                .thenReturn(Optional.of(predavac));
        when(lozinkaEnkoder.matches(anyString(), anyString())).thenReturn(true);

        assertThatThrownBy(() -> authServis.prijava(
                new PrijavaZahtjev("ivan.horvat@algebra.hr", "tajna123")))
                .isInstanceOf(NijePrijavljenIznimka.class)
                .hasMessageContaining("deaktiviran");
    }

    @Test
    @DisplayName("Promjena lozinke sprema novi hash")
    void promjenaLozinke() {
        when(korisnikRepozitorij.findById(1L)).thenReturn(Optional.of(predavac));
        when(lozinkaEnkoder.matches("staraLozinka", "hash-stare-lozinke")).thenReturn(true);
        when(lozinkaEnkoder.encode("novaLozinka")).thenReturn("hash-nove-lozinke");

        authServis.promijeniLozinku(1L, new PromjenaLozinkeZahtjev("staraLozinka", "novaLozinka"));

        assertThat(predavac.getLozinkaHash()).isEqualTo("hash-nove-lozinke");
        verify(korisnikRepozitorij).save(predavac);
    }

    @Test
    @DisplayName("Promjena lozinke s krivom starom lozinkom se odbija")
    void promjenaLozinkeSKrivomStarom() {
        when(korisnikRepozitorij.findById(1L)).thenReturn(Optional.of(predavac));
        when(lozinkaEnkoder.matches(anyString(), anyString())).thenReturn(false);

        assertThatThrownBy(() -> authServis.promijeniLozinku(1L,
                new PromjenaLozinkeZahtjev("krivaStara", "novaLozinka")))
                .isInstanceOf(PoslovnaIznimka.class)
                .hasMessageContaining("Stara lozinka nije ispravna");

        verify(korisnikRepozitorij, never()).save(any());
    }

    @Test
    @DisplayName("Nova lozinka ne smije biti ista kao stara")
    void novaLozinkaIstaKaoStara() {
        when(korisnikRepozitorij.findById(1L)).thenReturn(Optional.of(predavac));
        when(lozinkaEnkoder.matches("istaLozinka", "hash-stare-lozinke")).thenReturn(true);

        assertThatThrownBy(() -> authServis.promijeniLozinku(1L,
                new PromjenaLozinkeZahtjev("istaLozinka", "istaLozinka")))
                .isInstanceOf(PoslovnaIznimka.class)
                .hasMessageContaining("različita");

        verify(korisnikRepozitorij, never()).save(any());
    }
}

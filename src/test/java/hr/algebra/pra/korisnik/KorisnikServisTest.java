package hr.algebra.pra.korisnik;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import hr.algebra.pra.kolegij.Kolegij;
import hr.algebra.pra.kolegij.KolegijRepozitorij;
import hr.algebra.pra.korisnik.dto.KorisnikDto;
import hr.algebra.pra.korisnik.dto.SpremiKorisnikaZahtjev;
import hr.algebra.pra.obavijest.ObavijestRepozitorij;
import hr.algebra.pra.zajednicko.PoslovnaIznimka;

/**
 * Testovi pravila kod unosa, uredjivanja i brisanja korisnika.
 */
@ExtendWith(MockitoExtension.class)
class KorisnikServisTest {

    @Mock
    private KorisnikRepozitorij korisnikRepozitorij;

    @Mock
    private KolegijRepozitorij kolegijRepozitorij;

    @Mock
    private ObavijestRepozitorij obavijestRepozitorij;

    @Mock
    private PasswordEncoder lozinkaEnkoder;

    @InjectMocks
    private KorisnikServis korisnikServis;

    private Korisnik postojeci;

    @BeforeEach
    void pripremi() {
        postojeci = new Korisnik("Ivan", "Horvat", "ivan.horvat@algebra.hr",
                "stari-hash", Uloga.PREDAVAC);
        ReflectionTestUtils.setField(postojeci, "id", 2L);
    }

    private SpremiKorisnikaZahtjev zahtjev(String email, String lozinka) {
        return new SpremiKorisnikaZahtjev("Ivan", "Horvat", email, lozinka, Uloga.PREDAVAC, true);
    }

    /* --- Kreiranje ---------------------------------------------------------- */

    @Test
    @DisplayName("Email se sprema malim slovima i bez razmaka")
    void emailSeNormalizira() {
        when(korisnikRepozitorij.existsByEmailIgnoreCase("novi@algebra.hr")).thenReturn(false);
        when(lozinkaEnkoder.encode(anyString())).thenReturn("novi-hash");
        when(korisnikRepozitorij.save(any(Korisnik.class))).thenAnswer(p -> p.getArgument(0));

        KorisnikDto rezultat = korisnikServis.kreiraj(zahtjev("  NOVI@Algebra.hr  ", "lozinka123"));

        assertThat(rezultat.email()).isEqualTo("novi@algebra.hr");
    }

    @Test
    @DisplayName("Lozinka se sprema kao hash, nikad kao obican tekst")
    void lozinkaSeHashira() {
        when(korisnikRepozitorij.existsByEmailIgnoreCase(anyString())).thenReturn(false);
        when(lozinkaEnkoder.encode("lozinka123")).thenReturn("bcrypt-hash");
        when(korisnikRepozitorij.save(any(Korisnik.class))).thenAnswer(p -> p.getArgument(0));

        korisnikServis.kreiraj(zahtjev("novi@algebra.hr", "lozinka123"));

        verify(lozinkaEnkoder).encode("lozinka123");
    }

    @Test
    @DisplayName("Duplikat emaila se odbija")
    void duplikatEmailaSeOdbija() {
        when(korisnikRepozitorij.existsByEmailIgnoreCase("ivan.horvat@algebra.hr")).thenReturn(true);

        assertThatThrownBy(() -> korisnikServis.kreiraj(
                zahtjev("ivan.horvat@algebra.hr", "lozinka123")))
                .isInstanceOf(PoslovnaIznimka.class)
                .hasMessageContaining("već postoji");

        verify(korisnikRepozitorij, never()).save(any());
    }

    @Test
    @DisplayName("Lozinka je obavezna kod novog korisnika")
    void lozinkaJeObaveznaKodNovog() {
        when(korisnikRepozitorij.existsByEmailIgnoreCase(anyString())).thenReturn(false);

        assertThatThrownBy(() -> korisnikServis.kreiraj(zahtjev("novi@algebra.hr", "")))
                .isInstanceOf(PoslovnaIznimka.class)
                .hasMessageContaining("Lozinka je obavezna");
    }

    @Test
    @DisplayName("Prekratka lozinka se odbija")
    void prekratkaLozinkaSeOdbija() {
        when(korisnikRepozitorij.existsByEmailIgnoreCase(anyString())).thenReturn(false);

        assertThatThrownBy(() -> korisnikServis.kreiraj(zahtjev("novi@algebra.hr", "12345")))
                .isInstanceOf(PoslovnaIznimka.class)
                .hasMessageContaining("barem 6 znakova");
    }

    /* --- Azuriranje ---------------------------------------------------------- */

    @Test
    @DisplayName("Prazna lozinka kod uredjivanja ostavlja staru")
    void praznaLozinkaKodUredjivanjaOstavljaStaru() {
        when(korisnikRepozitorij.findById(2L)).thenReturn(Optional.of(postojeci));
        when(korisnikRepozitorij.save(any(Korisnik.class))).thenAnswer(p -> p.getArgument(0));

        korisnikServis.azuriraj(2L, zahtjev("ivan.horvat@algebra.hr", ""));

        assertThat(postojeci.getLozinkaHash()).isEqualTo("stari-hash");
        verify(lozinkaEnkoder, never()).encode(anyString());
    }

    @Test
    @DisplayName("Upisana lozinka kod uredjivanja mijenja hash")
    void novaLozinkaKodUredjivanjaMijenjaHash() {
        when(korisnikRepozitorij.findById(2L)).thenReturn(Optional.of(postojeci));
        when(lozinkaEnkoder.encode("novaLozinka")).thenReturn("novi-hash");
        when(korisnikRepozitorij.save(any(Korisnik.class))).thenAnswer(p -> p.getArgument(0));

        korisnikServis.azuriraj(2L, zahtjev("ivan.horvat@algebra.hr", "novaLozinka"));

        assertThat(postojeci.getLozinkaHash()).isEqualTo("novi-hash");
    }

    @Test
    @DisplayName("Korisnik moze zadrzati svoj email")
    void istiEmailKodUredjivanjaNijeDuplikat() {
        when(korisnikRepozitorij.findById(2L)).thenReturn(Optional.of(postojeci));
        when(korisnikRepozitorij.save(any(Korisnik.class))).thenAnswer(p -> p.getArgument(0));

        KorisnikDto rezultat = korisnikServis.azuriraj(2L, zahtjev("IVAN.HORVAT@algebra.hr", ""));

        assertThat(rezultat.email()).isEqualTo("ivan.horvat@algebra.hr");
        verify(korisnikRepozitorij, never()).existsByEmailIgnoreCase(anyString());
    }

    /* --- Brisanje -------------------------------------------------------------- */

    @Test
    @DisplayName("Korisnik ne moze obrisati sam sebe")
    void korisnikNeMozeObrisatiSebe() {
        when(korisnikRepozitorij.findById(2L)).thenReturn(Optional.of(postojeci));

        assertThatThrownBy(() -> korisnikServis.obrisi(2L, 2L))
                .isInstanceOf(PoslovnaIznimka.class)
                .hasMessageContaining("vlastiti");

        verify(korisnikRepozitorij, never()).delete(any());
    }

    @Test
    @DisplayName("Korisnik s obavijestima se ne moze obrisati")
    void korisnikSObavijestimaSeNeMozeObrisati() {
        when(korisnikRepozitorij.findById(2L)).thenReturn(Optional.of(postojeci));
        when(obavijestRepozitorij.countByAutorId(2L)).thenReturn(3L);

        assertThatThrownBy(() -> korisnikServis.obrisi(2L, 1L))
                .isInstanceOf(PoslovnaIznimka.class)
                .hasMessageContaining("deaktivirajte");

        verify(korisnikRepozitorij, never()).delete(any());
    }

    @Test
    @DisplayName("Brisanjem korisnika on se skida sa svih kolegija")
    void brisanjeSkidaKorisnikaSKolegija() {
        Kolegij kolegij = new Kolegij("PRA", "Projektni razvoj aplikacija", "Opis", 6, 4);
        kolegij.setPredavaci(Set.of(postojeci));

        when(korisnikRepozitorij.findById(2L)).thenReturn(Optional.of(postojeci));
        when(obavijestRepozitorij.countByAutorId(2L)).thenReturn(0L);
        when(kolegijRepozitorij.findByPredavaciIdOrderByNazivAsc(2L)).thenReturn(List.of(kolegij));

        korisnikServis.obrisi(2L, 1L);

        assertThat(kolegij.getPredavaci()).isEmpty();
        verify(kolegijRepozitorij).saveAll(List.of(kolegij));
        verify(korisnikRepozitorij).delete(postojeci);
    }

    /* --- Dohvat ----------------------------------------------------------------- */

    @Test
    @DisplayName("Popis predavaca ne sadrzi administratore")
    void popisPredavacaTraziSamoPredavace() {
        when(korisnikRepozitorij.findByUlogaOrderByPrezimeAscImeAsc(Uloga.PREDAVAC))
                .thenReturn(List.of(postojeci));

        List<KorisnikDto> rezultat = korisnikServis.dohvatiPredavace();

        assertThat(rezultat).hasSize(1);
        assertThat(rezultat.getFirst().uloga()).isEqualTo(Uloga.PREDAVAC);
        verify(korisnikRepozitorij).findByUlogaOrderByPrezimeAscImeAsc(Uloga.PREDAVAC);
    }

    @Test
    @DisplayName("KorisnikDto ne izlaze hash lozinke")
    void dtoNeSadrziLozinku() {
        KorisnikDto dto = KorisnikDto.od(postojeci);

        assertThat(dto.toString()).doesNotContain("stari-hash");
        assertThat(dto.punoIme()).isEqualTo("Ivan Horvat");
    }
}

package hr.algebra.pra.katedra;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import hr.algebra.pra.autentifikacija.dto.PrijavljeniKorisnik;
import hr.algebra.pra.katedra.dto.KatedraDto;
import hr.algebra.pra.katedra.dto.SpremiKatedruZahtjev;
import hr.algebra.pra.kolegij.KolegijRepozitorij;
import hr.algebra.pra.korisnik.Uloga;
import hr.algebra.pra.zajednicko.PoslovnaIznimka;
import hr.algebra.pra.zajednicko.ZabranjenoIznimka;

/**
 * Testovi za katedre - sifrarnik koji uredjuje samo administrator.
 */
@ExtendWith(MockitoExtension.class)
class KatedraServisTest {

    @Mock
    private KatedraRepozitorij katedraRepozitorij;

    @Mock
    private KolegijRepozitorij kolegijRepozitorij;

    @InjectMocks
    private KatedraServis katedraServis;

    private PrijavljeniKorisnik admin;
    private PrijavljeniKorisnik predavac;
    private Katedra postojeca;

    @BeforeEach
    void pripremi() {
        admin = new PrijavljeniKorisnik(1L, "Ana", "Adminović", "Ana Adminović",
                "admin@algebra.hr", Uloga.ADMIN, "Administrator");
        predavac = new PrijavljeniKorisnik(2L, "Ivan", "Horvat", "Ivan Horvat",
                "ivan.horvat@algebra.hr", Uloga.PREDAVAC, "Predavač");

        postojeca = new Katedra("PI", "Programsko inženjerstvo", "Opis katedre");
        ReflectionTestUtils.setField(postojeca, "id", 3L);
    }

    private SpremiKatedruZahtjev zahtjev(String kratica, String naziv) {
        return new SpremiKatedruZahtjev(kratica, naziv, "Opis");
    }

    @Test
    @DisplayName("Predavac ne moze dodati katedru")
    void predavacNeMozeDodatiKatedru() {
        assertThatThrownBy(() -> katedraServis.kreiraj(zahtjev("NOV", "Nova katedra"), predavac))
                .isInstanceOf(ZabranjenoIznimka.class)
                .hasMessageContaining("Samo administrator");

        verify(katedraRepozitorij, never()).save(any());
    }

    @Test
    @DisplayName("Kratica katedre se sprema velikim slovima")
    void kraticaSePretvaraUVelikaSlova() {
        when(katedraRepozitorij.existsByKraticaIgnoreCase("NOV")).thenReturn(false);
        when(katedraRepozitorij.existsByNazivIgnoreCase(anyString())).thenReturn(false);
        when(katedraRepozitorij.save(any(Katedra.class))).thenAnswer(p -> p.getArgument(0));

        KatedraDto rezultat = katedraServis.kreiraj(zahtjev("nov", "Nova katedra"), admin);

        assertThat(rezultat.kratica()).isEqualTo("NOV");
        assertThat(rezultat.brojKolegija()).isZero();
    }

    @Test
    @DisplayName("Duplikat kratice se odbija")
    void duplikatKraticeSeOdbija() {
        when(katedraRepozitorij.existsByKraticaIgnoreCase("PI")).thenReturn(true);

        assertThatThrownBy(() -> katedraServis.kreiraj(zahtjev("PI", "Nešto drugo"), admin))
                .isInstanceOf(PoslovnaIznimka.class)
                .hasMessageContaining("već postoji");

        verify(katedraRepozitorij, never()).save(any());
    }

    @Test
    @DisplayName("Duplikat naziva se odbija")
    void duplikatNazivaSeOdbija() {
        when(katedraRepozitorij.existsByKraticaIgnoreCase(anyString())).thenReturn(false);
        when(katedraRepozitorij.existsByNazivIgnoreCase("Programsko inženjerstvo")).thenReturn(true);

        assertThatThrownBy(() -> katedraServis.kreiraj(
                zahtjev("XX", "Programsko inženjerstvo"), admin))
                .isInstanceOf(PoslovnaIznimka.class)
                .hasMessageContaining("već postoji");
    }

    @Test
    @DisplayName("Katedra zadrzava svoju kraticu i naziv kod uredjivanja")
    void istaKraticaKodUredjivanjaNijeDuplikat() {
        when(katedraRepozitorij.findById(3L)).thenReturn(Optional.of(postojeca));
        when(katedraRepozitorij.save(any(Katedra.class))).thenAnswer(p -> p.getArgument(0));
        when(kolegijRepozitorij.countByKatedraId(3L)).thenReturn(7L);

        KatedraDto rezultat = katedraServis.azuriraj(3L,
                new SpremiKatedruZahtjev("PI", "Programsko inženjerstvo", "Novi opis"), admin);

        assertThat(rezultat.opis()).isEqualTo("Novi opis");
        assertThat(rezultat.brojKolegija()).isEqualTo(7);
        verify(katedraRepozitorij, never()).existsByKraticaIgnoreCase(anyString());
        verify(katedraRepozitorij, never()).existsByNazivIgnoreCase(anyString());
    }

    @Test
    @DisplayName("Katedra s kolegijima se ne moze obrisati")
    void katedraSKolegijimaSeNeMozeObrisati() {
        when(katedraRepozitorij.findById(3L)).thenReturn(Optional.of(postojeca));
        when(kolegijRepozitorij.countByKatedraId(3L)).thenReturn(5L);

        assertThatThrownBy(() -> katedraServis.obrisi(3L, admin))
                .isInstanceOf(PoslovnaIznimka.class)
                .hasMessageContaining("premjestite");

        verify(katedraRepozitorij, never()).delete(any());
    }

    @Test
    @DisplayName("Prazna katedra se moze obrisati")
    void praznaKatedraSeMozeObrisati() {
        when(katedraRepozitorij.findById(3L)).thenReturn(Optional.of(postojeca));
        when(kolegijRepozitorij.countByKatedraId(3L)).thenReturn(0L);

        katedraServis.obrisi(3L, admin);

        verify(katedraRepozitorij).delete(postojeca);
    }

    @Test
    @DisplayName("Popis katedri nosi broj kolegija po katedri")
    void popisNosiBrojKolegija() {
        when(katedraRepozitorij.findAllByOrderByNazivAsc()).thenReturn(List.of(postojeca));
        when(kolegijRepozitorij.countByKatedraId(3L)).thenReturn(4L);

        List<KatedraDto> rezultat = katedraServis.dohvatiSve();

        assertThat(rezultat).hasSize(1);
        assertThat(rezultat.getFirst().brojKolegija()).isEqualTo(4);
    }
}

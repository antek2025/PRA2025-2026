package hr.algebra.pra.obavijest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import hr.algebra.pra.autentifikacija.dto.PrijavljeniKorisnik;
import hr.algebra.pra.kolegij.Kolegij;
import hr.algebra.pra.kolegij.KolegijRepozitorij;
import hr.algebra.pra.kolegij.KolegijServis;
import hr.algebra.pra.korisnik.Korisnik;
import hr.algebra.pra.korisnik.KorisnikServis;
import hr.algebra.pra.korisnik.Uloga;
import hr.algebra.pra.obavijest.dto.ObavijestDto;
import hr.algebra.pra.obavijest.dto.SpremiObavijestZahtjev;
import hr.algebra.pra.zajednicko.PoslovnaIznimka;
import hr.algebra.pra.zajednicko.ZabranjenoIznimka;

/**
 * Testovi pravila objavljivanja obavijesti.
 *
 * Provjerava se ono sto je u zahtjevu izricito napisano: administrator moze
 * objaviti na svaki kolegij, a predavac samo na kolegije na kojima predaje.
 */
@ExtendWith(MockitoExtension.class)
class ObavijestServisTest {

    @Mock
    private ObavijestRepozitorij obavijestRepozitorij;

    @Mock
    private KolegijRepozitorij kolegijRepozitorij;

    @Mock
    private KolegijServis kolegijServis;

    @Mock
    private KorisnikServis korisnikServis;

    @InjectMocks
    private ObavijestServis obavijestServis;

    private PrijavljeniKorisnik admin;
    private PrijavljeniKorisnik predavac;
    private Korisnik entitetPredavaca;
    private Kolegij mojKolegij;
    private Kolegij tudjiKolegij;

    @BeforeEach
    void pripremi() {
        admin = new PrijavljeniKorisnik(1L, "Ana", "Adminović", "Ana Adminović",
                "admin@algebra.hr", Uloga.ADMIN, "Administrator");
        predavac = new PrijavljeniKorisnik(2L, "Ivan", "Horvat", "Ivan Horvat",
                "ivan.horvat@algebra.hr", Uloga.PREDAVAC, "Predavač");

        entitetPredavaca = new Korisnik("Ivan", "Horvat", "ivan.horvat@algebra.hr",
                "hash", Uloga.PREDAVAC);
        ReflectionTestUtils.setField(entitetPredavaca, "id", 2L);

        mojKolegij = new Kolegij("PRA", "Projektni razvoj aplikacija", "Opis", 6, 4);
        ReflectionTestUtils.setField(mojKolegij, "id", 10L);
        mojKolegij.setPredavaci(Set.of(entitetPredavaca));

        tudjiKolegij = new Kolegij("BAZ", "Baze podataka", "Opis", 5, 2);
        ReflectionTestUtils.setField(tudjiKolegij, "id", 11L);
    }

    private SpremiObavijestZahtjev zahtjev(Long kolegijId, LocalDate objava, LocalDate istek) {
        return new SpremiObavijestZahtjev("Naslov", "Sadrzaj obavijesti", objava, istek, kolegijId);
    }

    /* --- Validacija datuma -------------------------------------------------- */

    @Test
    @DisplayName("Datum isteka ne smije biti prije datuma objave")
    void istekPrijeObjaveSeOdbija() {
        LocalDate danas = LocalDate.now();

        assertThatThrownBy(() -> obavijestServis.kreiraj(
                zahtjev(10L, danas, danas.minusDays(1)), admin))
                .isInstanceOf(PoslovnaIznimka.class)
                .hasMessageContaining("Datum isteka");

        verify(obavijestRepozitorij, never()).save(any());
    }

    @Test
    @DisplayName("Objava i istek na isti dan su dozvoljeni")
    void istiDatumObjaveIIstekaJeDozvoljen() {
        LocalDate danas = LocalDate.now();
        when(kolegijServis.dohvatiEntitet(10L)).thenReturn(mojKolegij);
        when(korisnikServis.dohvatiEntitet(1L)).thenReturn(entitetPredavaca);
        when(obavijestRepozitorij.save(any(Obavijest.class))).thenAnswer(p -> p.getArgument(0));

        ObavijestDto rezultat = obavijestServis.kreiraj(zahtjev(10L, danas, danas), admin);

        assertThat(rezultat.aktivna()).isTrue();
    }

    /* --- Prava objavljivanja ------------------------------------------------- */

    @Test
    @DisplayName("Predavac ne moze objaviti na kolegiju na kojem ne predaje")
    void predavacNeMozeObjavitiNaTudjemKolegiju() {
        LocalDate danas = LocalDate.now();
        when(kolegijServis.dohvatiEntitet(11L)).thenReturn(tudjiKolegij);

        assertThatThrownBy(() -> obavijestServis.kreiraj(
                zahtjev(11L, danas, danas.plusDays(7)), predavac))
                .isInstanceOf(ZabranjenoIznimka.class)
                .hasMessageContaining("Baze podataka");

        verify(obavijestRepozitorij, never()).save(any());
    }

    @Test
    @DisplayName("Predavac moze objaviti na svom kolegiju")
    void predavacMozeObjavitiNaSvomKolegiju() {
        LocalDate danas = LocalDate.now();
        when(kolegijServis.dohvatiEntitet(10L)).thenReturn(mojKolegij);
        when(korisnikServis.dohvatiEntitet(2L)).thenReturn(entitetPredavaca);
        when(obavijestRepozitorij.save(any(Obavijest.class))).thenAnswer(p -> p.getArgument(0));

        ObavijestDto rezultat = obavijestServis.kreiraj(
                zahtjev(10L, danas, danas.plusDays(7)), predavac);

        assertThat(rezultat.kolegijSifra()).isEqualTo("PRA");
        assertThat(rezultat.autorPunoIme()).isEqualTo("Ivan Horvat");
    }

    @Test
    @DisplayName("Administrator moze objaviti na bilo kojem kolegiju")
    void adminMozeObjavitiSvugdje() {
        LocalDate danas = LocalDate.now();
        when(kolegijServis.dohvatiEntitet(11L)).thenReturn(tudjiKolegij);
        when(korisnikServis.dohvatiEntitet(1L)).thenReturn(entitetPredavaca);
        when(obavijestRepozitorij.save(any(Obavijest.class))).thenAnswer(p -> p.getArgument(0));

        ObavijestDto rezultat = obavijestServis.kreiraj(
                zahtjev(11L, danas, danas.plusDays(3)), admin);

        assertThat(rezultat.kolegijSifra()).isEqualTo("BAZ");
    }

    /* --- Brisanje ------------------------------------------------------------ */

    @Test
    @DisplayName("Predavac ne moze obrisati obavijest s tudjeg kolegija")
    void predavacNeMozeObrisatiTudjuObavijest() {
        Obavijest obavijest = new Obavijest("Naslov", "Opis",
                LocalDate.now(), LocalDate.now().plusDays(5), tudjiKolegij, entitetPredavaca);
        when(obavijestRepozitorij.findById(5L)).thenReturn(java.util.Optional.of(obavijest));

        assertThatThrownBy(() -> obavijestServis.obrisi(5L, predavac))
                .isInstanceOf(ZabranjenoIznimka.class);

        verify(obavijestRepozitorij, never()).delete(any());
    }

    @Test
    @DisplayName("Predavac moze obrisati obavijest sa svog kolegija")
    void predavacBriseObavijestSaSvogKolegija() {
        Obavijest obavijest = new Obavijest("Naslov", "Opis",
                LocalDate.now(), LocalDate.now().plusDays(5), mojKolegij, entitetPredavaca);
        when(obavijestRepozitorij.findById(5L)).thenReturn(java.util.Optional.of(obavijest));

        obavijestServis.obrisi(5L, predavac);

        verify(obavijestRepozitorij).delete(obavijest);
    }

    /* --- Popis obavijesti ----------------------------------------------------- */

    @Test
    @DisplayName("Predavac bez kolegija dobiva prazan popis bez upita u bazu")
    void predavacBezKolegijaDobivaPrazanPopis() {
        when(kolegijRepozitorij.findByPredavaciIdOrderByNazivAsc(2L)).thenReturn(List.of());

        List<ObavijestDto> rezultat = obavijestServis.dohvatiZaKorisnika(predavac, null);

        assertThat(rezultat).isEmpty();
        // Nema smisla raditi "WHERE kolegij_id IN ()" pa upit preskacemo.
        verify(obavijestRepozitorij, never())
                .findByKolegijIdInOrderByDatumObjaveDescIdDesc(anyList());
    }

    @Test
    @DisplayName("Administrator u popisu dobiva sve obavijesti i smije ih uredjivati")
    void adminDobivaSveObavijesti() {
        Obavijest obavijest = new Obavijest("Naslov", "Opis",
                LocalDate.now(), LocalDate.now().plusDays(5), tudjiKolegij, entitetPredavaca);
        when(obavijestRepozitorij.findAllByOrderByDatumObjaveDescIdDesc())
                .thenReturn(List.of(obavijest));

        List<ObavijestDto> rezultat = obavijestServis.dohvatiZaKorisnika(admin, null);

        assertThat(rezultat).hasSize(1);
        assertThat(rezultat.getFirst().smijeUrediti()).isTrue();
    }

    @Test
    @DisplayName("Istekla obavijest nije oznacena kao aktivna")
    void isteklaObavijestNijeAktivna() {
        LocalDate danas = LocalDate.now();
        Obavijest istekla = new Obavijest("Stara", "Opis",
                danas.minusDays(20), danas.minusDays(5), mojKolegij, entitetPredavaca);
        when(obavijestRepozitorij.findAllByOrderByDatumObjaveDescIdDesc())
                .thenReturn(List.of(istekla));

        List<ObavijestDto> rezultat = obavijestServis.dohvatiZaKorisnika(admin, null);

        assertThat(rezultat.getFirst().aktivna()).isFalse();
    }
}

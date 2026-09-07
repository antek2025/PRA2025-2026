package hr.algebra.pra.kolegij;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
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
import hr.algebra.pra.katedra.Katedra;
import hr.algebra.pra.katedra.KatedraServis;
import hr.algebra.pra.kolegij.dto.KolegijDto;
import hr.algebra.pra.kolegij.dto.SpremiKolegijZahtjev;
import hr.algebra.pra.korisnik.Korisnik;
import hr.algebra.pra.korisnik.KorisnikRepozitorij;
import hr.algebra.pra.korisnik.Uloga;
import hr.algebra.pra.zajednicko.PoslovnaIznimka;
import hr.algebra.pra.zajednicko.ZabranjenoIznimka;

/**
 * Testovi pravila pristupa kolegijima - to je najvazniji dio zahtjeva:
 * administrator radi sa svim kolegijima, a predavac vidi samo svoje.
 */
@ExtendWith(MockitoExtension.class)
class KolegijServisTest {

    @Mock
    private KolegijRepozitorij kolegijRepozitorij;

    @Mock
    private KorisnikRepozitorij korisnikRepozitorij;

    @Mock
    private KatedraServis katedraServis;

    @InjectMocks
    private KolegijServis kolegijServis;

    private PrijavljeniKorisnik admin;
    private PrijavljeniKorisnik predavac;
    private Korisnik entitetPredavaca;
    private Kolegij kolegijPredavaca;

    @BeforeEach
    void pripremi() {
        admin = new PrijavljeniKorisnik(1L, "Ana", "Adminović", "Ana Adminović",
                "admin@algebra.hr", Uloga.ADMIN, "Administrator");
        predavac = new PrijavljeniKorisnik(2L, "Ivan", "Horvat", "Ivan Horvat",
                "ivan.horvat@algebra.hr", Uloga.PREDAVAC, "Predavač");

        entitetPredavaca = new Korisnik("Ivan", "Horvat", "ivan.horvat@algebra.hr",
                "hash", Uloga.PREDAVAC);
        // ID postavlja baza, pa ga u testu moramo upisati preko refleksije.
        ReflectionTestUtils.setField(entitetPredavaca, "id", 2L);

        kolegijPredavaca = new Kolegij("PRA", "Projektni razvoj aplikacija", "Opis", 6, 4);
        ReflectionTestUtils.setField(kolegijPredavaca, "id", 10L);
        kolegijPredavaca.setPredavaci(java.util.Set.of(entitetPredavaca));
    }

    private SpremiKolegijZahtjev zahtjev(String sifra, List<Long> idPredavaca) {
        return zahtjev(sifra, idPredavaca, null);
    }

    private SpremiKolegijZahtjev zahtjev(String sifra, List<Long> idPredavaca, Long katedraId) {
        return new SpremiKolegijZahtjev(sifra, "Novi kolegij", "Opis", 5, 3, true,
                katedraId, idPredavaca);
    }

    /* --- Dohvat popisa ---------------------------------------------------- */

    @Test
    @DisplayName("Administrator dobiva popis svih kolegija")
    void adminDohvacaSveKolegije() {
        when(kolegijRepozitorij.findAllByOrderByNazivAsc())
                .thenReturn(List.of(kolegijPredavaca));

        List<KolegijDto> rezultat = kolegijServis.dohvatiZaKorisnika(admin);

        assertThat(rezultat).hasSize(1);
        verify(kolegijRepozitorij).findAllByOrderByNazivAsc();
        verify(kolegijRepozitorij, never()).findByPredavaciIdOrderByNazivAsc(any());
    }

    @Test
    @DisplayName("Predavac dobiva samo kolegije na kojima predaje")
    void predavacDohvacaSamoSvojeKolegije() {
        when(kolegijRepozitorij.findByPredavaciIdOrderByNazivAsc(2L))
                .thenReturn(List.of(kolegijPredavaca));

        List<KolegijDto> rezultat = kolegijServis.dohvatiZaKorisnika(predavac);

        assertThat(rezultat).hasSize(1);
        assertThat(rezultat.getFirst().sifra()).isEqualTo("PRA");
        verify(kolegijRepozitorij, never()).findAllByOrderByNazivAsc();
    }

    /* --- Pravo pristupa pojedinom kolegiju --------------------------------- */

    @Test
    @DisplayName("Predavac ne smije otvoriti kolegij na kojem ne predaje")
    void predavacNemaPristupTudjemKolegiju() {
        Kolegij tudjiKolegij = new Kolegij("BAZ", "Baze podataka", "Opis", 5, 2);
        ReflectionTestUtils.setField(tudjiKolegij, "id", 11L);

        assertThatThrownBy(() -> kolegijServis.provjeriSmijeVidjeti(tudjiKolegij, predavac))
                .isInstanceOf(ZabranjenoIznimka.class)
                .hasMessageContaining("Baze podataka");
    }

    @Test
    @DisplayName("Administrator ima pristup svakom kolegiju")
    void adminImaPristupSvemu() {
        Kolegij tudjiKolegij = new Kolegij("BAZ", "Baze podataka", "Opis", 5, 2);

        assertThatCode(() -> kolegijServis.provjeriSmijeVidjeti(tudjiKolegij, admin))
                .doesNotThrowAnyException();
    }

    /* --- Kreiranje --------------------------------------------------------- */

    @Test
    @DisplayName("Predavac ne moze dodati kolegij")
    void predavacNeMozeDodatiKolegij() {
        assertThatThrownBy(() -> kolegijServis.kreiraj(zahtjev("NOV", List.of()), predavac))
                .isInstanceOf(ZabranjenoIznimka.class)
                .hasMessageContaining("Samo administrator");

        verify(kolegijRepozitorij, never()).save(any());
    }

    @Test
    @DisplayName("Sifra kolegija se sprema velikim slovima")
    void sifraSePretvaraUVelikaSlova() {
        when(kolegijRepozitorij.existsBySifraIgnoreCase("NOV")).thenReturn(false);
        when(kolegijRepozitorij.save(any(Kolegij.class))).thenAnswer(poziv -> poziv.getArgument(0));

        KolegijDto rezultat = kolegijServis.kreiraj(zahtjev("nov", List.of()), admin);

        assertThat(rezultat.sifra()).isEqualTo("NOV");
    }

    @Test
    @DisplayName("Duplikat sifre se odbija")
    void duplikatSifreSeOdbija() {
        when(kolegijRepozitorij.existsBySifraIgnoreCase("PRA")).thenReturn(true);

        assertThatThrownBy(() -> kolegijServis.kreiraj(zahtjev("PRA", List.of()), admin))
                .isInstanceOf(PoslovnaIznimka.class)
                .hasMessageContaining("već postoji");

        verify(kolegijRepozitorij, never()).save(any());
    }

    @Test
    @DisplayName("Na kolegij se ne moze dodijeliti administrator kao predavac")
    void adminSeNeMozeDodijelitiKaoPredavac() {
        Korisnik drugiAdmin = new Korisnik("Ana", "Adminović", "admin@algebra.hr",
                "hash", Uloga.ADMIN);
        ReflectionTestUtils.setField(drugiAdmin, "id", 1L);

        when(kolegijRepozitorij.existsBySifraIgnoreCase(anyString())).thenReturn(false);
        when(korisnikRepozitorij.findById(1L)).thenReturn(Optional.of(drugiAdmin));

        assertThatThrownBy(() -> kolegijServis.kreiraj(zahtjev("NOV", List.of(1L)), admin))
                .isInstanceOf(PoslovnaIznimka.class)
                .hasMessageContaining("nije predavač");
    }

    /* --- Katedra i godina studija -------------------------------------------- */

    @Test
    @DisplayName("Kolegij se moze dodijeliti katedri")
    void kolegijSeDodjeljujeKatedri() {
        Katedra katedra = new Katedra("PI", "Programsko inženjerstvo", "Opis");
        ReflectionTestUtils.setField(katedra, "id", 3L);

        when(kolegijRepozitorij.existsBySifraIgnoreCase(anyString())).thenReturn(false);
        when(katedraServis.dohvatiEntitet(3L)).thenReturn(katedra);
        when(kolegijRepozitorij.save(any(Kolegij.class))).thenAnswer(poziv -> poziv.getArgument(0));

        KolegijDto rezultat = kolegijServis.kreiraj(zahtjev("NOV", List.of(), 3L), admin);

        assertThat(rezultat.katedraId()).isEqualTo(3L);
        assertThat(rezultat.katedraKratica()).isEqualTo("PI");
    }

    @Test
    @DisplayName("Kolegij smije biti bez katedre")
    void kolegijSmijeBitiBezKatedre() {
        when(kolegijRepozitorij.existsBySifraIgnoreCase(anyString())).thenReturn(false);
        when(kolegijRepozitorij.save(any(Kolegij.class))).thenAnswer(poziv -> poziv.getArgument(0));

        KolegijDto rezultat = kolegijServis.kreiraj(zahtjev("NOV", List.of(), null), admin);

        assertThat(rezultat.katedraId()).isNull();
        assertThat(rezultat.katedraKratica()).isNull();
        // Katedra se ni ne pokusava dohvatiti kad ID nije poslan
        verify(katedraServis, never()).dohvatiEntitet(any());
    }

    @Test
    @DisplayName("Godina studija se racuna iz semestra")
    void godinaStudijaSeRacunaIzSemestra() {
        // 1,2 -> 1. godina | 3,4 -> 2. godina | 5,6 -> 3. godina
        assertThat(new Kolegij("A", "A", null, 5, 1).godinaStudija()).isEqualTo(1);
        assertThat(new Kolegij("B", "B", null, 5, 2).godinaStudija()).isEqualTo(1);
        assertThat(new Kolegij("C", "C", null, 5, 3).godinaStudija()).isEqualTo(2);
        assertThat(new Kolegij("D", "D", null, 5, 4).godinaStudija()).isEqualTo(2);
        assertThat(new Kolegij("E", "E", null, 5, 5).godinaStudija()).isEqualTo(3);
        assertThat(new Kolegij("F", "F", null, 5, 6).godinaStudija()).isEqualTo(3);
    }

    /* --- Azuriranje i brisanje ---------------------------------------------- */

    @Test
    @DisplayName("Kolegij zadrzava svoju sifru kod uredjivanja")
    void istaSifraKodUredjivanjaNijeDuplikat() {
        when(kolegijRepozitorij.findById(10L)).thenReturn(Optional.of(kolegijPredavaca));
        when(kolegijRepozitorij.save(any(Kolegij.class))).thenAnswer(poziv -> poziv.getArgument(0));

        SpremiKolegijZahtjev zahtjev = new SpremiKolegijZahtjev(
                "PRA", "Novi naziv", "Novi opis", 6, 4, true, null, List.of());

        KolegijDto rezultat = kolegijServis.azuriraj(10L, zahtjev, admin);

        assertThat(rezultat.naziv()).isEqualTo("Novi naziv");
        // existsBySifraIgnoreCase se ne smije ni pozvati jer se sifra nije promijenila
        verify(kolegijRepozitorij, never()).existsBySifraIgnoreCase(anyString());
    }

    @Test
    @DisplayName("Predavac ne moze obrisati kolegij")
    void predavacNeMozeObrisatiKolegij() {
        assertThatThrownBy(() -> kolegijServis.obrisi(10L, predavac))
                .isInstanceOf(ZabranjenoIznimka.class);

        verify(kolegijRepozitorij, never()).delete(any());
    }

    @Test
    @DisplayName("Administrator brise kolegij")
    void adminBriseKolegij() {
        when(kolegijRepozitorij.findById(10L)).thenReturn(Optional.of(kolegijPredavaca));

        kolegijServis.obrisi(10L, admin);

        verify(kolegijRepozitorij).delete(kolegijPredavaca);
        assertThat(kolegijPredavaca.getPredavaci()).isEmpty();
    }
}

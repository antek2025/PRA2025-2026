package hr.algebra.pra.konfiguracija;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.annotation.Transactional;

import hr.algebra.pra.katedra.Katedra;
import hr.algebra.pra.katedra.KatedraRepozitorij;
import hr.algebra.pra.kolegij.Kolegij;
import hr.algebra.pra.kolegij.KolegijRepozitorij;
import hr.algebra.pra.korisnik.Korisnik;
import hr.algebra.pra.korisnik.KorisnikRepozitorij;
import hr.algebra.pra.korisnik.Uloga;
import hr.algebra.pra.obavijest.Obavijest;
import hr.algebra.pra.obavijest.ObavijestRepozitorij;

/**
 * Puni bazu pocetnim podacima pri prvom pokretanju.
 *
 * Zasto uopce postoji: prema zahtjevu "vrste korisnika i administratora potrebno
 * je definirati u sustavu" - dakle admin racun se ne registrira kroz aplikaciju
 * nego mora vec postojati. Ostali demo podaci su tu da se aplikacija moze odmah
 * pokazati bez rucnog unosa.
 *
 * Podaci su namjerno razvedeni (5 katedri, 18 kolegija kroz 3 godine studija,
 * 9 predavaca, 20-ak obavijesti) jer se na malom uzorku ne vidi ima li
 * filtriranje po godini i katedri uopce smisla.
 *
 * Pokrece se samo ako je tablica korisnika prazna, pa ponovno pokretanje
 * aplikacije ne moze prebrisati podatke koje je netko u medjuvremenu unio.
 */
@Configuration
public class PocetniPodaci {

    private static final Logger log = LoggerFactory.getLogger(PocetniPodaci.class);

    @Bean
    public ApplicationRunner ucitajPocetnePodatke(
            KorisnikRepozitorij korisnikRepozitorij,
            KatedraRepozitorij katedraRepozitorij,
            KolegijRepozitorij kolegijRepozitorij,
            ObavijestRepozitorij obavijestRepozitorij,
            PasswordEncoder lozinkaEnkoder,
            @Value("${infoeduka.ucitaj-pocetne-podatke:true}") boolean ucitaj,
            @Value("${infoeduka.admin.email}") String adminEmail,
            @Value("${infoeduka.admin.lozinka}") String adminLozinka) {

        return new Punjac(korisnikRepozitorij, katedraRepozitorij, kolegijRepozitorij,
                obavijestRepozitorij, lozinkaEnkoder, ucitaj, adminEmail, adminLozinka);
    }

    /**
     * Izdvojeno u zasebnu klasu (umjesto lambde) da se moze staviti @Transactional -
     * inace bi svako spremanje islo u vlastitoj transakciji.
     */
    static class Punjac implements ApplicationRunner {

        private final KorisnikRepozitorij korisnikRepozitorij;
        private final KatedraRepozitorij katedraRepozitorij;
        private final KolegijRepozitorij kolegijRepozitorij;
        private final ObavijestRepozitorij obavijestRepozitorij;
        private final PasswordEncoder lozinkaEnkoder;
        private final boolean ucitaj;
        private final String adminEmail;
        private final String adminLozinka;

        Punjac(KorisnikRepozitorij korisnikRepozitorij,
               KatedraRepozitorij katedraRepozitorij,
               KolegijRepozitorij kolegijRepozitorij,
               ObavijestRepozitorij obavijestRepozitorij,
               PasswordEncoder lozinkaEnkoder,
               boolean ucitaj,
               String adminEmail,
               String adminLozinka) {
            this.korisnikRepozitorij = korisnikRepozitorij;
            this.katedraRepozitorij = katedraRepozitorij;
            this.kolegijRepozitorij = kolegijRepozitorij;
            this.obavijestRepozitorij = obavijestRepozitorij;
            this.lozinkaEnkoder = lozinkaEnkoder;
            this.ucitaj = ucitaj;
            this.adminEmail = adminEmail;
            this.adminLozinka = adminLozinka;
        }

        @Override
        @Transactional
        public void run(ApplicationArguments args) {
            if (!ucitaj) {
                log.info("Ucitavanje pocetnih podataka je iskljuceno.");
                return;
            }
            if (korisnikRepozitorij.count() > 0) {
                log.info("Baza vec sadrzi korisnike, pocetni podaci se preskacu.");
                return;
            }

            log.info("Baza je prazna - ucitavam pocetne podatke.");

            // --- korisnici ----------------------------------------------------
            Korisnik admin = new Korisnik("Ana", "Adminović", adminEmail,
                    lozinkaEnkoder.encode(adminLozinka), Uloga.ADMIN);

            Korisnik horvat = predavac("Ivan", "Horvat", "ivan.horvat@algebra.hr");
            Korisnik kovacevic = predavac("Marija", "Kovačević", "marija.kovacevic@algebra.hr");
            Korisnik novak = predavac("Petar", "Novak", "petar.novak@algebra.hr");
            Korisnik juric = predavac("Iva", "Jurić", "iva.juric@algebra.hr");
            Korisnik babic = predavac("Tomislav", "Babić", "tomislav.babic@algebra.hr");
            Korisnik maric = predavac("Ana", "Marić", "ana.maric@algebra.hr");
            Korisnik vukovic = predavac("Luka", "Vuković", "luka.vukovic@algebra.hr");
            Korisnik peric = predavac("Nikolina", "Perić", "nikolina.peric@algebra.hr");
            Korisnik knezevic = predavac("Domagoj", "Knežević", "domagoj.knezevic@algebra.hr");

            korisnikRepozitorij.saveAll(List.of(admin, horvat, kovacevic, novak, juric,
                    babic, maric, vukovic, peric, knezevic));

            // --- katedre -------------------------------------------------------
            Katedra pi = new Katedra("PI", "Programsko inženjerstvo",
                    "Razvoj programske podrške, arhitektura aplikacija i inženjerske prakse.");
            Katedra bis = new Katedra("BIS", "Baze podataka i informacijski sustavi",
                    "Modeliranje podataka, baze podataka i poslovni informacijski sustavi.");
            Katedra rms = new Katedra("RMS", "Računalne mreže i sigurnost",
                    "Mrežne tehnologije, operacijski sustavi i informacijska sigurnost.");
            Katedra md = new Katedra("MD", "Multimedija i dizajn",
                    "Korisnička sučelja, korisničko iskustvo i multimedijski sadržaji.");
            Katedra mtd = new Katedra("MTD", "Matematika i temeljne discipline",
                    "Matematičke i teorijske osnove računarstva.");

            katedraRepozitorij.saveAll(List.of(pi, bis, rms, md, mtd));

            // --- kolegiji ------------------------------------------------------
            // 1. godina (semestri 1 i 2)
            Kolegij mat1 = kolegij("MAT1", "Matematika 1",
                    "Diferencijalni i integralni račun, linearna algebra.", 6, 1, mtd, maric);
            Kolegij uvp = kolegij("UVP", "Uvod u programiranje",
                    "Osnove algoritama, tipovi podataka, petlje i funkcije.", 6, 1, pi, horvat, babic);
            Kolegij osn = kolegij("OSN", "Osnove računalnih sustava",
                    "Arhitektura računala, operacijski sustavi i rad u naredbenom retku.", 5, 1, rms, vukovic);
            Kolegij mat2 = kolegij("MAT2", "Matematika 2",
                    "Vjerojatnost, statistika i diskretne strukture.", 5, 2, mtd, maric);
            Kolegij oop = kolegij("OOP", "Objektno orijentirano programiranje",
                    "Klase, nasljeđivanje, polimorfizam i oblikovni obrasci.", 6, 2, pi, horvat);
            Kolegij baz = kolegij("BAZ", "Baze podataka",
                    "Relacijski model, SQL, normalizacija i transakcije.", 5, 2, bis, novak);

            // 2. godina (semestri 3 i 4)
            Kolegij web = kolegij("WEB", "Web programiranje",
                    "HTML, CSS, JavaScript i izrada dinamičkih web stranica.", 5, 3, pi, kovacevic, juric);
            Kolegij mre = kolegij("MRE", "Računalne mreže",
                    "TCP/IP model, usmjeravanje i osnove mrežne sigurnosti.", 4, 3, rms, vukovic);
            Kolegij diz = kolegij("DIZ", "Dizajn korisničkog sučelja",
                    "Načela upotrebljivosti, izrada okvira i prototipova sučelja.", 4, 3, md, peric);
            Kolegij pra = kolegij("PRA", "Projektni razvoj aplikacija",
                    "Izrada aplikativnog rješenja kroz faze prikupljanja zahtjeva, "
                            + "izrade prototipa, planiranja, testiranja i razvoja.", 6, 4, pi, horvat, kovacevic);
            Kolegij nap = kolegij("NAP", "Napredne baze podataka",
                    "Optimizacija upita, indeksiranje i nerelacijske baze podataka.", 5, 4, bis, novak, knezevic);
            Kolegij sig = kolegij("SIG", "Sigurnost informacijskih sustava",
                    "Kriptografija, upravljanje pristupom i sigurnosne ranjivosti.", 5, 4, rms, vukovic);

            // 3. godina (semestri 5 i 6)
            Kolegij mob = kolegij("MOB", "Razvoj mobilnih aplikacija",
                    "Izrada Android aplikacija i rad s mobilnim API-jima.", 6, 5, pi, babic);
            Kolegij pos = kolegij("POS", "Poslovni informacijski sustavi",
                    "ERP i CRM sustavi, poslovni procesi i izvještavanje.", 5, 5, bis, knezevic);
            Kolegij mul = kolegij("MUL", "Multimedijski sustavi",
                    "Obrada slike, zvuka i videa te formati zapisa.", 4, 5, md, peric);
            Kolegij obl = kolegij("OBL", "Računarstvo u oblaku",
                    "Virtualizacija, kontejneri i postavljanje aplikacija u oblak.", 5, 6, rms, vukovic, babic);
            Kolegij str = kolegij("STR", "Strojno učenje",
                    "Nadzirano i nenadzirano učenje, evaluacija modela.", 6, 6, mtd, maric, juric);
            Kolegij zav = kolegij("ZAV", "Završni rad",
                    "Samostalna izrada i obrana završnog rada.", 8, 6, pi, horvat);

            // Namjerno neaktivni - da se vidi i filter po statusu
            mul.setAktivan(false);
            osn.setAktivan(false);

            // Namjerno bez katedre i bez predavaca - da se vidi i taj slucaj
            Kolegij izb = new Kolegij("IZB", "Izborni seminar",
                    "Sadržaj se određuje na početku semestra.", 3, 5);

            List<Kolegij> sviKolegiji = List.of(mat1, uvp, osn, mat2, oop, baz,
                    web, mre, diz, pra, nap, sig, mob, pos, mul, obl, str, zav, izb);
            kolegijRepozitorij.saveAll(sviKolegiji);

            // --- obavijesti -----------------------------------------------------
            LocalDate danas = LocalDate.now();
            List<Obavijest> obavijesti = new ArrayList<>();

            // Trenutno vrijede
            obavijesti.add(new Obavijest("Prvo predavanje",
                    "Prvo predavanje održat će se u dvorani A3. Molim studente da ponesu "
                            + "prijenosno računalo s instaliranim razvojnim okruženjem.",
                    danas.minusDays(3), danas.plusDays(14), pra, horvat));
            obavijesti.add(new Obavijest("Rok za predaju projektnog zadatka",
                    "Projektni zadatak predaje se najkasnije do kraja mjeseca. Predaja ide "
                            + "preko repozitorija, a dokumentacija u PDF formatu.",
                    danas.minusDays(1), danas.plusDays(28), pra, kovacevic));
            obavijesti.add(new Obavijest("Otkazane vježbe",
                    "Vježbe u petak su otkazane zbog održavanja stručnog skupa. "
                            + "Nadoknada će biti dogovorena naknadno.",
                    danas.minusDays(1), danas.plusDays(7), baz, novak));
            obavijesti.add(new Obavijest("Konzultacije",
                    "Konzultacije se ovaj tjedan pomiču na srijedu od 10 do 12 sati.",
                    danas, danas.plusDays(5), oop, horvat));
            obavijesti.add(new Obavijest("Podjela u grupe za laboratorijske vježbe",
                    "Popis grupa objavljen je na oglasnoj ploči. Zamjene su moguće "
                            + "isključivo uz dogovor s asistentom.",
                    danas.minusDays(2), danas.plusDays(10), mre, vukovic));
            obavijesti.add(new Obavijest("Materijali za prvi kolokvij",
                    "Materijali i zadaci za vježbu objavljeni su na sustavu za e-učenje.",
                    danas.minusDays(5), danas.plusDays(12), mat1, maric));
            obavijesti.add(new Obavijest("Obavezna prijava teme završnog rada",
                    "Studenti su dužni prijaviti temu završnog rada do kraja mjeseca. "
                            + "Popis raspoloživih tema nalazi se u prilogu kolegija.",
                    danas.minusDays(6), danas.plusDays(20), zav, horvat));
            obavijesti.add(new Obavijest("Instalacija razvojne okoline",
                    "Prije prvih vježbi instalirajte Android Studio i emulator uređaja.",
                    danas.minusDays(4), danas.plusDays(9), mob, babic));
            obavijesti.add(new Obavijest("Gostujuće predavanje",
                    "U utorak nam gostuje stručnjak iz industrije s temom sigurnosti "
                            + "web aplikacija. Dolazak se preporučuje svim studentima.",
                    danas, danas.plusDays(6), sig, vukovic));
            obavijesti.add(new Obavijest("Predaja prve laboratorijske vježbe",
                    "Prva laboratorijska vježba predaje se do nedjelje u ponoć.",
                    danas.minusDays(1), danas.plusDays(4), web, kovacevic));
            obavijesti.add(new Obavijest("Izmjena rasporeda",
                    "Predavanja se od sljedećeg tjedna održavaju u dvorani B2.",
                    danas.minusDays(2), danas.plusDays(15), nap, novak));
            obavijesti.add(new Obavijest("Prijedlozi tema za seminar",
                    "Prijedloge tema za seminarski rad šaljite e-mailom do petka.",
                    danas, danas.plusDays(11), diz, peric));

            // Buduce - jos nisu na snazi
            obavijesti.add(new Obavijest("Termin prvog kolokvija",
                    "Prvi kolokvij održat će se u dvorani A1. Ponesite osobnu iskaznicu.",
                    danas.plusDays(7), danas.plusDays(30), oop, horvat));
            obavijesti.add(new Obavijest("Radionica o kontejnerima",
                    "Dodatna radionica o Dockeru održat će se sljedeći mjesec.",
                    danas.plusDays(10), danas.plusDays(40), obl, vukovic));
            obavijesti.add(new Obavijest("Najava terenske nastave",
                    "Planira se posjet podatkovnom centru. Prijave kreću za dva tjedna.",
                    danas.plusDays(14), danas.plusDays(45), pos, knezevic));

            // Istekle - ostaju u sustavu radi povijesti
            obavijesti.add(new Obavijest("Materijali za drugi kolokvij",
                    "Materijali za drugi kolokvij objavljeni su na sustavu za e-učenje.",
                    danas.minusDays(30), danas.minusDays(9), web, juric));
            obavijesti.add(new Obavijest("Rezultati prvog kolokvija",
                    "Rezultati su objavljeni. Uvid u radove moguć je na konzultacijama.",
                    danas.minusDays(25), danas.minusDays(5), mat2, maric));
            obavijesti.add(new Obavijest("Nadoknada predavanja",
                    "Nadoknada propuštenog predavanja održana je u subotu.",
                    danas.minusDays(20), danas.minusDays(12), sig, vukovic));
            obavijesti.add(new Obavijest("Upute za izradu seminara",
                    "Upute i predložak za seminarski rad bili su dostupni do kraja roka.",
                    danas.minusDays(40), danas.minusDays(2), mul, peric));
            obavijesti.add(new Obavijest("Anketa o kvaliteti nastave",
                    "Anketa je zatvorena. Hvala svima koji su sudjelovali.",
                    danas.minusDays(18), danas.minusDays(3), str, juric));

            obavijestRepozitorij.saveAll(obavijesti);

            log.info("Pocetni podaci ucitani: {} korisnika, {} katedri, {} kolegija, {} obavijesti.",
                    korisnikRepozitorij.count(), katedraRepozitorij.count(),
                    kolegijRepozitorij.count(), obavijestRepozitorij.count());
            log.info("Admin prijava: {} / {}", adminEmail, adminLozinka);
        }

        /** Svi demo predavaci imaju istu lozinku da se aplikacija lakse testira. */
        private Korisnik predavac(String ime, String prezime, String email) {
            return new Korisnik(ime, prezime, email,
                    lozinkaEnkoder.encode("predavac123"), Uloga.PREDAVAC);
        }

        /** Kratica da se 18 kolegija ne pise u 18 blokova od pet redaka. */
        private Kolegij kolegij(String sifra, String naziv, String opis, int ects, int semestar,
                                Katedra katedra, Korisnik... predavaci) {
            Kolegij kolegij = new Kolegij(sifra, naziv, opis, ects, semestar);
            kolegij.setKatedra(katedra);
            kolegij.setPredavaci(new LinkedHashSet<>(Set.of(predavaci)));
            return kolegij;
        }
    }
}

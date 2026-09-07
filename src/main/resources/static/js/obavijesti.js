/* ==========================================================================
   obavijesti.js - popis obavijesti te unos, uređivanje i brisanje.
   --------------------------------------------------------------------------
   Obavijesti smiju dodavati svi prijavljeni korisnici: administrator na bilo
   kojem kolegiju, a predavač samo na kolegijima na kojima predaje. Zbog toga
   se padajući izbornik kolegija puni iz /api/kolegiji - backend tamo već vrati
   samo kolegije koje korisnik smije koristiti.

   Za svaku obavijest backend šalje i polje "smijeUrediti" pa se gumbi crtaju
   prema tome, umjesto da se pravila ponavljaju u JavaScriptu.
   ========================================================================== */

document.addEventListener("DOMContentLoaded", async function () {

    const korisnik = await Zajednicko.pokreniStranicu("obavijesti.html");
    if (!korisnik) {
        return;
    }

    let sveObavijesti = [];
    let mojiKolegiji = [];
    let sveKatedre = [];

    const forma = document.getElementById("formaObavijesti");
    const poljePretrage = document.getElementById("pretraga");
    const filterKatedre = document.getElementById("filterKatedra");
    const filterGodine = document.getElementById("filterGodina");
    const filterKolegija = document.getElementById("filterKolegij");
    const filterStatusa = document.getElementById("filterStatus");

    if (!Zajednicko.jeAdmin()) {
        document.getElementById("podnaslov").textContent =
            "Obavijesti kolegija na kojima predajete";
    }

    /* --- Povezivanje događaja ---------------------------------------------- */

    document.getElementById("gumbNova").addEventListener("click", function () {
        otvoriFormu(null);
    });
    document.getElementById("gumbZatvori").addEventListener("click", zatvoriFormu);
    document.getElementById("gumbOdustani").addEventListener("click", zatvoriFormu);
    Zajednicko.poveziZatvaranjeModala("modalObavijest");

    poljePretrage.addEventListener("input", nacrtajPopis);
    filterKolegija.addEventListener("change", nacrtajPopis);
    filterStatusa.addEventListener("change", nacrtajPopis);

    // Katedra i godina sužavaju popis kolegija u trećem izborniku.
    filterKatedre.addEventListener("change", function () {
        napuniFilterKolegija();
        nacrtajPopis();
    });
    filterGodine.addEventListener("change", function () {
        napuniFilterKolegija();
        nacrtajPopis();
    });

    forma.addEventListener("submit", spremiObavijest);

    ucitajPodatke();

    /* --- Dohvat podataka ---------------------------------------------------- */

    async function ucitajPodatke() {
        try {
            const [obavijesti, kolegiji, katedre] = await Promise.all([
                Api.obavijesti(null),
                Api.kolegiji(),
                Api.katedre()
            ]);

            sveObavijesti = obavijesti;
            mojiKolegiji = kolegiji;
            sveKatedre = katedre;

            napuniFilterKatedri();
            napuniFilterKolegija();
            nacrtajPopis();
        } catch (greska) {
            if (greska.status === 401) {
                window.location.href = "index.html";
                return;
            }
            Zajednicko.prikaziGresku("porukaGreske", greska.message);
        }
    }

    /**
     * Filter po katedri. Nudi samo katedre koje se stvarno pojavljuju među
     * kolegijima koje korisnik vidi - predavaču nema smisla nuditi katedru
     * na kojoj nema nijedan svoj kolegij.
     */
    function napuniFilterKatedri() {
        const prisutne = new Set();
        mojiKolegiji.forEach(function (kolegij) {
            if (kolegij.katedraId !== null) {
                prisutne.add(kolegij.katedraId);
            }
        });

        let html = '<option value="">Sve katedre</option>';
        sveKatedre.forEach(function (katedra) {
            if (prisutne.has(katedra.id)) {
                html += '<option value="' + katedra.id + '">' +
                    Zajednicko.escapeHtml(katedra.kratica + " — " + katedra.naziv) + "</option>";
            }
        });
        filterKatedre.innerHTML = html;
    }

    /**
     * Filter po kolegiju - OVDJE je kaskada.
     *
     * Popis kolegija se sužava prema odabranoj katedri i godini studija, pa se
     * ne nude kombinacije koje bi ionako dale prazan rezultat. Ako trenutno
     * odabrani kolegij ispadne iz suženog popisa, odabir se vraća na "Svi".
     */
    function napuniFilterKolegija() {
        const prethodni = filterKolegija.value;
        const katedraId = filterKatedre.value;
        const godina = filterGodine.value;

        const odgovarajuci = mojiKolegiji.filter(function (kolegij) {
            if (katedraId !== "" && String(kolegij.katedraId) !== katedraId) {
                return false;
            }
            if (godina !== "" && String(kolegij.godinaStudija) !== godina) {
                return false;
            }
            return true;
        });

        let html = '<option value="">Svi kolegiji</option>';
        odgovarajuci.forEach(function (kolegij) {
            html += '<option value="' + kolegij.id + '">' +
                Zajednicko.escapeHtml(kolegij.sifra + " - " + kolegij.naziv) + "</option>";
        });
        filterKolegija.innerHTML = html;

        const josPostoji = odgovarajuci.some(function (kolegij) {
            return String(kolegij.id) === prethodni;
        });
        filterKolegija.value = josPostoji ? prethodni : "";
    }

    /* --- Prikaz -------------------------------------------------------------- */

    function nacrtajPopis() {
        const spremnik = document.getElementById("popisObavijesti");
        const obavijesti = primijeniFiltere();

        document.getElementById("brojRezultata").textContent =
            obavijesti.length === sveObavijesti.length
                ? "Prikazano svih " + sveObavijesti.length + " obavijesti."
                : "Prikazano " + obavijesti.length + " od " + sveObavijesti.length + " obavijesti.";

        if (obavijesti.length === 0) {
            spremnik.innerHTML =
                '<div class="kartica"><div class="prazno">' +
                "<strong>Nema obavijesti za prikaz</strong>" +
                (sveObavijesti.length === 0
                    ? "Dodajte prvu obavijest klikom na gumb gore desno."
                    : "Nijedna obavijest ne odgovara zadanim filterima.") +
                "</div></div>";
            return;
        }

        // Obavijesti su kartice u mreži - kao "Vijesti" u pravoj Infoeduci.
        let html = "";
        obavijesti.forEach(function (obavijest) {
            html += nacrtajKarticu(obavijest);
        });
        spremnik.innerHTML = '<div class="mreza-obavijesti">' + html + "</div>";

        spremnik.querySelectorAll("[data-uredi]").forEach(function (gumb) {
            gumb.addEventListener("click", function () {
                otvoriFormu(Number(gumb.getAttribute("data-uredi")));
            });
        });
        spremnik.querySelectorAll("[data-obrisi]").forEach(function (gumb) {
            gumb.addEventListener("click", function () {
                obrisiObavijest(Number(gumb.getAttribute("data-obrisi")));
            });
        });
    }

    function nacrtajKarticu(obavijest) {
        const status = odrediStatus(obavijest);

        // Gumbe crtamo samo ako backend kaže da korisnik smije uređivati.
        let akcije = "";
        if (obavijest.smijeUrediti) {
            akcije =
                '<div class="kartica-obavijest-akcije">' +
                '<button type="button" class="gumb gumb-sporedni gumb-mali" data-uredi="' +
                obavijest.id + '">Uredi</button>' +
                '<button type="button" class="gumb gumb-opasni gumb-mali" data-obrisi="' +
                obavijest.id + '">Obriši</button>' +
                "</div>";
        }

        const katedra = obavijest.kolegijKatedraKratica
            ? Zajednicko.escapeHtml(obavijest.kolegijKatedraKratica) + " &middot; "
            : "";

        return (
            '<article class="kartica-obavijest">' +
            '  <div class="kartica-obavijest-vrh">' +
            '    <div class="kartica-obavijest-oznake">' +
            '      <span class="oznaka oznaka-primarna" title="' +
            Zajednicko.escapeHtml(obavijest.kolegijNaziv) + '">' +
            Zajednicko.escapeHtml(obavijest.kolegijSifra) + "</span>" +
            "      <span>" + katedra + obavijest.kolegijGodina + ". godina</span>" +
            "    </div>" +
            '    <span class="oznaka ' + status.klasa + '">' + status.tekst + "</span>" +
            "  </div>" +
            "  <h3>" + Zajednicko.escapeHtml(obavijest.naslov) + "</h3>" +
            '  <p class="kartica-obavijest-tekst">' + Zajednicko.escapeHtml(obavijest.opis) + "</p>" +
            '  <div class="kartica-obavijest-podnozje">' +
            "    <span><strong>" + Zajednicko.escapeHtml(obavijest.autorPunoIme) + "</strong><br>" +
            Zajednicko.formatirajDatum(obavijest.datumObjave) + " &ndash; " +
            Zajednicko.formatirajDatum(obavijest.datumIsteka) + "</span>" +
            akcije +
            "  </div>" +
            "</article>"
        );
    }

    /**
     * Backend šalje samo "aktivna" (danas je između objave i isteka), pa se
     * razlika između istekle i buduće obavijesti računa ovdje iz datuma.
     */
    function odrediStatus(obavijest) {
        if (obavijest.aktivna) {
            return { klasa: "oznaka-uspjeh", tekst: "Vrijedi" };
        }
        const danas = Zajednicko.danasZaInput();
        if (obavijest.datumObjave > danas) {
            return { klasa: "oznaka-upozorenje", tekst: "Buduća" };
        }
        return { klasa: "oznaka-neutralna", tekst: "Istekla" };
    }

    function primijeniFiltere() {
        const pojam = poljePretrage.value.trim().toLowerCase();
        const katedraId = filterKatedre.value;
        const godina = filterGodine.value;
        const kolegijId = filterKolegija.value;
        const status = filterStatusa.value;
        const danas = Zajednicko.danasZaInput();

        return sveObavijesti.filter(function (obavijest) {
            if (pojam !== "") {
                const tekst = (obavijest.naslov + " " + obavijest.opis).toLowerCase();
                if (tekst.indexOf(pojam) === -1) {
                    return false;
                }
            }
            // Katedra i godina dolaze s kolegija na koji je obavijest vezana -
            // backend ih šalje u ObavijestDto pa ih ne treba tražiti po popisu kolegija.
            if (katedraId !== "" && String(obavijest.kolegijKatedraId) !== katedraId) {
                return false;
            }
            if (godina !== "" && String(obavijest.kolegijGodina) !== godina) {
                return false;
            }
            if (kolegijId !== "" && String(obavijest.kolegijId) !== kolegijId) {
                return false;
            }
            if (status === "aktivna" && !obavijest.aktivna) {
                return false;
            }
            if (status === "istekla" && (obavijest.aktivna || obavijest.datumObjave > danas)) {
                return false;
            }
            if (status === "buduca" && obavijest.datumObjave <= danas) {
                return false;
            }
            return true;
        });
    }

    /* --- Forma ---------------------------------------------------------------- */

    function otvoriFormu(id) {
        Zajednicko.ocistiGreskeForme(forma);
        Zajednicko.sakrijGresku("greskaForme");
        forma.reset();

        // Bez kolegija se obavijest ne može objaviti jer je veza obavezna.
        if (mojiKolegiji.length === 0) {
            Zajednicko.prikaziToast(
                "Nema kolegija na koji biste mogli objaviti obavijest.", "greska");
            return;
        }

        napuniIzbornikKolegija();

        const obavijest = id === null ? null : sveObavijesti.find(function (o) {
            return o.id === id;
        });

        document.getElementById("naslovModala").textContent =
            obavijest ? "Uređivanje obavijesti" : "Nova obavijest";
        document.getElementById("obavijestId").value = obavijest ? obavijest.id : "";
        document.getElementById("naslov").value = obavijest ? obavijest.naslov : "";
        document.getElementById("opis").value = obavijest ? obavijest.opis : "";

        // Kod nove obavijesti predlažemo današnji datum i istek za tjedan dana -
        // to je najčešći slučaj pa korisnik ne mora ništa mijenjati.
        document.getElementById("datumObjave").value =
            obavijest ? obavijest.datumObjave : Zajednicko.danasZaInput();
        document.getElementById("datumIsteka").value =
            obavijest ? obavijest.datumIsteka : Zajednicko.datumZaInput(7);
        document.getElementById("kolegijId").value =
            obavijest ? obavijest.kolegijId : mojiKolegiji[0].id;

        Zajednicko.otvoriModal("modalObavijest");
        document.getElementById("naslov").focus();
    }

    function napuniIzbornikKolegija() {
        let html = "";
        mojiKolegiji.forEach(function (kolegij) {
            html += '<option value="' + kolegij.id + '">' +
                Zajednicko.escapeHtml(kolegij.sifra + " - " + kolegij.naziv) + "</option>";
        });
        document.getElementById("kolegijId").innerHTML = html;

        document.getElementById("pomocKolegij").textContent = Zajednicko.jeAdmin()
            ? "Obavijest je uvijek vezana uz jedan kolegij."
            : "Prikazani su samo kolegiji na kojima predajete.";
    }

    function zatvoriFormu() {
        Zajednicko.zatvoriModal("modalObavijest");
    }

    async function spremiObavijest(dogadaj) {
        dogadaj.preventDefault();

        Zajednicko.ocistiGreskeForme(forma);
        Zajednicko.sakrijGresku("greskaForme");

        const id = document.getElementById("obavijestId").value;
        const datumObjave = document.getElementById("datumObjave").value;
        const datumIsteka = document.getElementById("datumIsteka").value;

        // Isto pravilo provjerava i backend, ovdje je samo da korisnik odmah vidi grešku.
        if (datumObjave !== "" && datumIsteka !== "" && datumIsteka < datumObjave) {
            Zajednicko.prikaziGresku("greskaForme",
                "Datum isteka ne može biti prije datuma objave.");
            return;
        }

        const podaci = {
            naslov: document.getElementById("naslov").value.trim(),
            opis: document.getElementById("opis").value.trim(),
            datumObjave: datumObjave,
            datumIsteka: datumIsteka,
            kolegijId: Number(document.getElementById("kolegijId").value)
        };

        const gumb = document.getElementById("gumbSpremi");
        gumb.disabled = true;

        try {
            await Api.spremiObavijest(id === "" ? null : Number(id), podaci);
            zatvoriFormu();
            Zajednicko.prikaziToast(
                id === "" ? "Obavijest je objavljena." : "Obavijest je spremljena.", "uspjeh");
            await ucitajPodatke();
        } catch (greska) {
            Zajednicko.prikaziGresku("greskaForme", greska.message);
            Zajednicko.prikaziGreskeForme(forma, greska.greskePolja);
        } finally {
            gumb.disabled = false;
        }
    }

    /* --- Brisanje ------------------------------------------------------------- */

    async function obrisiObavijest(id) {
        const obavijest = sveObavijesti.find(function (o) {
            return o.id === id;
        });
        if (!obavijest) {
            return;
        }

        if (!window.confirm("Obrisati obavijest \"" + obavijest.naslov + "\"?")) {
            return;
        }

        try {
            await Api.obrisiObavijest(id);
            Zajednicko.prikaziToast("Obavijest je obrisana.", "uspjeh");
            await ucitajPodatke();
        } catch (greska) {
            Zajednicko.prikaziToast(greska.message, "greska");
        }
    }
});

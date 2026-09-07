/* ==========================================================================
   katedre.js - šifrarnik katedri (ustrojstvenih jedinica učilišta).
   --------------------------------------------------------------------------
   Stranica je dostupna samo administratoru. Katedre se koriste kao filter na
   stranicama Kolegiji i Obavijesti, pa ih predavač čita, ali ne uređuje.

   Ovdje nema filtera jer je katedri malo (nekoliko) - cijeli popis stane
   na jedan ekran.
   ========================================================================== */

document.addEventListener("DOMContentLoaded", async function () {

    const korisnik = await Zajednicko.pokreniStranicu("katedre.html");
    if (!korisnik) {
        return;
    }

    // Predavač ovdje nema što raditi - kao i na stranici Korisnici.
    if (!Zajednicko.jeAdmin()) {
        window.location.href = "nadzorna-ploca.html";
        return;
    }

    let sveKatedre = [];

    const forma = document.getElementById("formaKatedre");

    /* --- Povezivanje događaja ---------------------------------------------- */

    document.getElementById("gumbNova").addEventListener("click", function () {
        otvoriFormu(null);
    });
    document.getElementById("gumbZatvori").addEventListener("click", zatvoriFormu);
    document.getElementById("gumbOdustani").addEventListener("click", zatvoriFormu);
    Zajednicko.poveziZatvaranjeModala("modalKatedra");

    forma.addEventListener("submit", spremiKatedru);

    ucitajKatedre();

    /* --- Dohvat podataka ---------------------------------------------------- */

    async function ucitajKatedre() {
        try {
            sveKatedre = await Api.katedre();
            nacrtajPopis();
        } catch (greska) {
            if (greska.status === 401) {
                window.location.href = "index.html";
                return;
            }
            Zajednicko.prikaziGresku("porukaGreske", greska.message);
        }
    }

    /* --- Prikaz -------------------------------------------------------------- */

    function nacrtajPopis() {
        const spremnik = document.getElementById("popisKatedri");

        if (sveKatedre.length === 0) {
            spremnik.innerHTML =
                '<div class="kartica"><div class="prazno">' +
                "<strong>Nema katedri</strong>" +
                "Dodajte prvu katedru klikom na gumb gore desno.</div></div>";
            return;
        }

        let redci = "";
        sveKatedre.forEach(function (katedra) {
            redci += nacrtajRedak(katedra);
        });

        spremnik.innerHTML =
            '<div class="tablica-omot"><table>' +
            "<thead><tr>" +
            "  <th>Kratica</th><th>Naziv</th><th>Opis</th><th>Kolegija</th><th></th>" +
            "</tr></thead>" +
            "<tbody>" + redci + "</tbody>" +
            "</table></div>";

        spremnik.querySelectorAll("[data-uredi]").forEach(function (gumb) {
            gumb.addEventListener("click", function () {
                otvoriFormu(Number(gumb.getAttribute("data-uredi")));
            });
        });
        spremnik.querySelectorAll("[data-obrisi]").forEach(function (gumb) {
            gumb.addEventListener("click", function () {
                obrisiKatedru(Number(gumb.getAttribute("data-obrisi")));
            });
        });
    }

    function nacrtajRedak(katedra) {
        /*
         * Katedra s kolegijima se ne može obrisati (backend to odbija), pa se
         * gumb ni ne crta - korisniku je jasnije nego da klikne pa dobije grešku.
         */
        const gumbBrisanja = katedra.brojKolegija === 0
            ? '<button type="button" class="gumb gumb-opasni gumb-mali" data-obrisi="' +
              katedra.id + '">Obriši</button>'
            : "";

        const oznakaBroja = katedra.brojKolegija === 0
            ? '<span class="oznaka oznaka-neutralna">nema kolegija</span>'
            : '<span class="oznaka oznaka-primarna">' + katedra.brojKolegija + "</span>";

        return (
            "<tr>" +
            '  <td><span class="oznaka oznaka-primarna">' +
            Zajednicko.escapeHtml(katedra.kratica) + "</span></td>" +
            "  <td><strong>" + Zajednicko.escapeHtml(katedra.naziv) + "</strong></td>" +
            '  <td><span style="color:var(--boja-tekst-blijedi);font-size:13px">' +
            Zajednicko.escapeHtml(Zajednicko.skrati(katedra.opis, 90)) + "</span></td>" +
            "  <td>" + oznakaBroja + "</td>" +
            '  <td class="akcije">' +
            '    <button type="button" class="gumb gumb-sporedni gumb-mali" data-uredi="' +
            katedra.id + '">Uredi</button>' + gumbBrisanja +
            "  </td>" +
            "</tr>"
        );
    }

    /* --- Forma ---------------------------------------------------------------- */

    function otvoriFormu(id) {
        Zajednicko.ocistiGreskeForme(forma);
        Zajednicko.sakrijGresku("greskaForme");
        forma.reset();

        const katedra = id === null ? null : sveKatedre.find(function (k) {
            return k.id === id;
        });

        document.getElementById("naslovModala").textContent =
            katedra ? "Uređivanje katedre" : "Nova katedra";
        document.getElementById("katedraId").value = katedra ? katedra.id : "";
        document.getElementById("kratica").value = katedra ? katedra.kratica : "";
        document.getElementById("naziv").value = katedra ? katedra.naziv : "";
        document.getElementById("opis").value = (katedra && katedra.opis) ? katedra.opis : "";

        Zajednicko.otvoriModal("modalKatedra");
        document.getElementById("kratica").focus();
    }

    function zatvoriFormu() {
        Zajednicko.zatvoriModal("modalKatedra");
    }

    async function spremiKatedru(dogadaj) {
        dogadaj.preventDefault();

        Zajednicko.ocistiGreskeForme(forma);
        Zajednicko.sakrijGresku("greskaForme");

        const id = document.getElementById("katedraId").value;

        const podaci = {
            kratica: document.getElementById("kratica").value.trim(),
            naziv: document.getElementById("naziv").value.trim(),
            opis: document.getElementById("opis").value.trim()
        };

        const gumb = document.getElementById("gumbSpremi");
        gumb.disabled = true;

        try {
            await Api.spremiKatedru(id === "" ? null : Number(id), podaci);
            zatvoriFormu();
            Zajednicko.prikaziToast(
                id === "" ? "Katedra je dodana." : "Katedra je spremljena.", "uspjeh");
            await ucitajKatedre();
        } catch (greska) {
            Zajednicko.prikaziGresku("greskaForme", greska.message);
            Zajednicko.prikaziGreskeForme(forma, greska.greskePolja);
        } finally {
            gumb.disabled = false;
        }
    }

    /* --- Brisanje -------------------------------------------------------------- */

    async function obrisiKatedru(id) {
        const katedra = sveKatedre.find(function (k) {
            return k.id === id;
        });
        if (!katedra) {
            return;
        }

        if (!window.confirm("Obrisati katedru \"" + katedra.naziv + "\"?")) {
            return;
        }

        try {
            await Api.obrisiKatedru(id);
            Zajednicko.prikaziToast("Katedra je obrisana.", "uspjeh");
            await ucitajKatedre();
        } catch (greska) {
            Zajednicko.prikaziToast(greska.message, "greska");
        }
    }
});

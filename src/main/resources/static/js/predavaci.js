/* ==========================================================================
   predavaci.js - upravljanje korisnicima (predavačima i administratorima).
   --------------------------------------------------------------------------
   Ova stranica je dostupna samo administratoru. Predavač je uopće ne vidi u
   izborniku, a i da ručno upiše adresu, backend bi vratio 403 - provjera na
   frontendu je samo radi lijepšeg sučelja, prava zaštita je na poslužitelju.
   ========================================================================== */

document.addEventListener("DOMContentLoaded", async function () {

    const korisnik = await Zajednicko.pokreniStranicu("predavaci.html");
    if (!korisnik) {
        return;
    }

    // Predavač ovdje nema što raditi - odmah ga vraćamo na nadzornu ploču.
    if (!Zajednicko.jeAdmin()) {
        window.location.href = "nadzorna-ploca.html";
        return;
    }

    let sviKorisnici = [];

    const forma = document.getElementById("formaKorisnika");
    const poljePretrage = document.getElementById("pretraga");
    const filterUloge = document.getElementById("filterUloga");
    const filterStatusa = document.getElementById("filterStatus");

    /* --- Povezivanje događaja ---------------------------------------------- */

    document.getElementById("gumbNovi").addEventListener("click", function () {
        otvoriFormu(null);
    });
    document.getElementById("gumbZatvori").addEventListener("click", zatvoriFormu);
    document.getElementById("gumbOdustani").addEventListener("click", zatvoriFormu);
    Zajednicko.poveziZatvaranjeModala("modalKorisnik");

    poljePretrage.addEventListener("input", nacrtajPopis);
    filterUloge.addEventListener("change", nacrtajPopis);
    filterStatusa.addEventListener("change", nacrtajPopis);

    forma.addEventListener("submit", spremiKorisnika);

    ucitajKorisnike();

    /* --- Dohvat podataka ---------------------------------------------------- */

    async function ucitajKorisnike() {
        try {
            sviKorisnici = await Api.korisnici();
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
        const spremnik = document.getElementById("popisKorisnika");
        const korisnici = primijeniFiltere();

        if (korisnici.length === 0) {
            spremnik.innerHTML =
                '<div class="kartica"><div class="prazno">' +
                "<strong>Nema korisnika za prikaz</strong>" +
                "Nijedan korisnik ne odgovara zadanim filterima.</div></div>";
            return;
        }

        let redci = "";
        korisnici.forEach(function (k) {
            redci += nacrtajRedak(k);
        });

        spremnik.innerHTML =
            '<div class="tablica-omot"><table>' +
            "<thead><tr>" +
            "  <th>Ime i prezime</th><th>E-mail</th><th>Uloga</th>" +
            "  <th>Status</th><th>Dodan</th><th></th>" +
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
                obrisiKorisnika(Number(gumb.getAttribute("data-obrisi")));
            });
        });
    }

    function nacrtajRedak(k) {
        const oznakaUloge = k.uloga === "ADMIN"
            ? '<span class="oznaka oznaka-primarna">Administrator</span>'
            : '<span class="oznaka oznaka-neutralna">Predavač</span>';

        const oznakaStatusa = k.aktivan
            ? '<span class="oznaka oznaka-uspjeh">Aktivan</span>'
            : '<span class="oznaka oznaka-upozorenje">Deaktiviran</span>';

        // Vlastiti račun se ne može obrisati, pa se taj gumb ni ne crta.
        const jeJa = k.id === Zajednicko.korisnik.id;
        const gumbBrisanja = jeJa
            ? ""
            : '<button type="button" class="gumb gumb-opasni gumb-mali" data-obrisi="' +
              k.id + '">Obriši</button>';

        const oznakaJa = jeJa
            ? ' <span class="oznaka oznaka-neutralna">Vi</span>'
            : "";

        return (
            "<tr>" +
            "  <td><strong>" + Zajednicko.escapeHtml(k.punoIme) + "</strong>" + oznakaJa + "</td>" +
            "  <td>" + Zajednicko.escapeHtml(k.email) + "</td>" +
            "  <td>" + oznakaUloge + "</td>" +
            "  <td>" + oznakaStatusa + "</td>" +
            "  <td>" + Zajednicko.formatirajDatum(k.datumKreiranja) + "</td>" +
            '  <td class="akcije">' +
            '    <button type="button" class="gumb gumb-sporedni gumb-mali" data-uredi="' +
            k.id + '">Uredi</button>' + gumbBrisanja +
            "  </td>" +
            "</tr>"
        );
    }

    function primijeniFiltere() {
        const pojam = poljePretrage.value.trim().toLowerCase();
        const uloga = filterUloge.value;
        const status = filterStatusa.value;

        return sviKorisnici.filter(function (k) {
            if (pojam !== "") {
                const tekst = (k.punoIme + " " + k.email).toLowerCase();
                if (tekst.indexOf(pojam) === -1) {
                    return false;
                }
            }
            if (uloga !== "" && k.uloga !== uloga) {
                return false;
            }
            if (status === "aktivan" && !k.aktivan) {
                return false;
            }
            if (status === "neaktivan" && k.aktivan) {
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

        const k = id === null ? null : sviKorisnici.find(function (x) {
            return x.id === id;
        });

        document.getElementById("naslovModala").textContent =
            k ? "Uređivanje korisnika" : "Novi korisnik";
        document.getElementById("korisnikId").value = k ? k.id : "";
        document.getElementById("ime").value = k ? k.ime : "";
        document.getElementById("prezime").value = k ? k.prezime : "";
        document.getElementById("email").value = k ? k.email : "";
        document.getElementById("uloga").value = k ? k.uloga : "PREDAVAC";
        document.getElementById("aktivan").checked = k ? k.aktivan : true;
        document.getElementById("lozinka").value = "";

        /*
         * Kod novog korisnika lozinka je obavezna, a kod uređivanja se prazno
         * polje tumači kao "ostavi postojeću lozinku" - zato se mijenja i
         * zvjezdica uz labelu i tekst pomoći.
         */
        document.getElementById("oznakaLozinke").textContent = k ? "" : "*";
        document.getElementById("pomocLozinke").textContent = k
            ? "Ostavite prazno ako ne želite mijenjati lozinku."
            : "Najmanje 6 znakova.";

        Zajednicko.otvoriModal("modalKorisnik");
        document.getElementById("ime").focus();
    }

    function zatvoriFormu() {
        Zajednicko.zatvoriModal("modalKorisnik");
    }

    async function spremiKorisnika(dogadaj) {
        dogadaj.preventDefault();

        Zajednicko.ocistiGreskeForme(forma);
        Zajednicko.sakrijGresku("greskaForme");

        const id = document.getElementById("korisnikId").value;

        const podaci = {
            ime: document.getElementById("ime").value.trim(),
            prezime: document.getElementById("prezime").value.trim(),
            email: document.getElementById("email").value.trim(),
            lozinka: document.getElementById("lozinka").value,
            uloga: document.getElementById("uloga").value,
            aktivan: document.getElementById("aktivan").checked
        };

        const gumb = document.getElementById("gumbSpremi");
        gumb.disabled = true;

        try {
            await Api.spremiKorisnika(id === "" ? null : Number(id), podaci);
            zatvoriFormu();
            Zajednicko.prikaziToast(
                id === "" ? "Korisnik je dodan." : "Podaci su spremljeni.", "uspjeh");
            await ucitajKorisnike();
        } catch (greska) {
            Zajednicko.prikaziGresku("greskaForme", greska.message);
            Zajednicko.prikaziGreskeForme(forma, greska.greskePolja);
        } finally {
            gumb.disabled = false;
        }
    }

    /* --- Brisanje ------------------------------------------------------------- */

    async function obrisiKorisnika(id) {
        const k = sviKorisnici.find(function (x) {
            return x.id === id;
        });
        if (!k) {
            return;
        }

        const potvrda = window.confirm(
            "Obrisati korisnika " + k.punoIme + "?\n\n" +
            "Korisnik će biti uklonjen sa svih kolegija na kojima predaje.");

        if (!potvrda) {
            return;
        }

        try {
            await Api.obrisiKorisnika(id);
            Zajednicko.prikaziToast("Korisnik je obrisan.", "uspjeh");
            await ucitajKorisnike();
        } catch (greska) {
            // Najčešći slučaj: korisnik je autor obavijesti pa se ne smije obrisati.
            Zajednicko.prikaziToast(greska.message, "greska");
        }
    }
});

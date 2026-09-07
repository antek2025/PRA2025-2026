/* ==========================================================================
   kolegiji.js - popis kolegija te unos, uređivanje i brisanje.
   --------------------------------------------------------------------------
   Administrator radi sa svim kolegijima, predavač samo gleda svoje. Popis
   koji stigne s backenda je već filtriran prema ulozi, pa se ovdje samo
   sakriva ono što predavač ne smije koristiti (gumbi za unos i brisanje).
   ========================================================================== */

document.addEventListener("DOMContentLoaded", async function () {

    const korisnik = await Zajednicko.pokreniStranicu("kolegiji.html");
    if (!korisnik) {
        return;
    }

    /* Svi kolegiji dohvaćeni s poslužitelja. Filteri rade nad ovim popisom,
       pa se kod svake promjene filtera ne mora ponovno ići na poslužitelj. */
    let sviKolegiji = [];
    let sviPredavaci = [];
    let sveKatedre = [];

    const forma = document.getElementById("formaKolegija");
    const poljePretrage = document.getElementById("pretraga");
    const filterKatedre = document.getElementById("filterKatedra");
    const filterGodine = document.getElementById("filterGodina");
    const filterSemestra = document.getElementById("filterSemestar");
    const filterStatusa = document.getElementById("filterStatus");

    if (Zajednicko.jeAdmin()) {
        document.getElementById("gumbNovi").classList.remove("skriveno");
    } else {
        document.getElementById("naslov").textContent = "Moji kolegiji";
        document.getElementById("podnaslov").textContent =
            "Kolegiji na kojima predajete";
    }

    /* --- Povezivanje događaja --------------------------------------------- */

    document.getElementById("gumbNovi").addEventListener("click", function () {
        otvoriFormu(null);
    });
    document.getElementById("gumbZatvori").addEventListener("click", zatvoriFormu);
    document.getElementById("gumbOdustani").addEventListener("click", zatvoriFormu);
    Zajednicko.poveziZatvaranjeModala("modalKolegij");

    poljePretrage.addEventListener("input", nacrtajPopis);
    filterKatedre.addEventListener("change", nacrtajPopis);
    filterSemestra.addEventListener("change", nacrtajPopis);
    filterStatusa.addEventListener("change", nacrtajPopis);

    // Godina i semestar su povezani: odabir godine suzava izbor semestra
    // na ona dva koja toj godini pripadaju.
    filterGodine.addEventListener("change", function () {
        osvjeziIzborSemestra();
        nacrtajPopis();
    });

    forma.addEventListener("submit", spremiKolegij);

    ucitajKolegije();

    /* --- Dohvat podataka --------------------------------------------------- */

    async function ucitajKolegije() {
        try {
            // Katedre i kolegiji su neovisni pozivi pa idu paralelno.
            const [kolegiji, katedre] = await Promise.all([Api.kolegiji(), Api.katedre()]);
            sviKolegiji = kolegiji;
            sveKatedre = katedre;

            napuniIzbornikeKatedri();
            osvjeziIzborSemestra();
            nacrtajPopis();
        } catch (greska) {
            if (greska.status === 401) {
                window.location.href = "index.html";
                return;
            }
            Zajednicko.prikaziGresku("porukaGreske", greska.message);
        }
    }

    /* --- Prikaz ------------------------------------------------------------ */

    function nacrtajPopis() {
        const spremnik = document.getElementById("popisKolegija");
        const kolegiji = primijeniFiltere();

        document.getElementById("brojRezultata").textContent =
            kolegiji.length === sviKolegiji.length
                ? "Prikazano svih " + sviKolegiji.length + " kolegija."
                : "Prikazano " + kolegiji.length + " od " + sviKolegiji.length + " kolegija.";

        if (kolegiji.length === 0) {
            spremnik.innerHTML =
                '<div class="kartica"><div class="prazno">' +
                "<strong>Nema kolegija za prikaz</strong>" +
                (sviKolegiji.length === 0
                    ? (Zajednicko.jeAdmin()
                        ? "Dodajte prvi kolegij klikom na gumb gore desno."
                        : "Niste dodijeljeni ni na jedan kolegij.")
                    : "Nijedan kolegij ne odgovara zadanim filterima.") +
                "</div></div>";
            return;
        }

        /*
         * Kolegiji se grupiraju po godini studija i semestru, s plavim naslovom
         * iznad svake grupe - kao "Predmeti" u pravoj Infoeduci, gdje su predmeti
         * grupirani po semestru. Grupe idu redom od 1. do 6. semestra.
         */
        const grupe = new Map();
        kolegiji.forEach(function (kolegij) {
            const kljuc = kolegij.semestar;
            if (!grupe.has(kljuc)) {
                grupe.set(kljuc, []);
            }
            grupe.get(kljuc).push(kolegij);
        });

        let html = "";
        Array.from(grupe.keys()).sort(function (a, b) { return a - b; }).forEach(function (semestar) {
            const uGrupi = grupe.get(semestar);
            const godina = uGrupi[0].godinaStudija;
            html += '<section class="grupa-kolegija">' +
                '<h2 class="naslov-sekcije">' + godina + ". godina, " + semestar + ". semestar</h2>";
            uGrupi.forEach(function (kolegij) {
                html += nacrtajRedak(kolegij);
            });
            html += "</section>";
        });
        spremnik.innerHTML = html;

        // Gumbi se crtaju zajedno s redcima pa se događaji vežu tek sada.
        spremnik.querySelectorAll("[data-uredi]").forEach(function (gumb) {
            gumb.addEventListener("click", function () {
                otvoriFormu(Number(gumb.getAttribute("data-uredi")));
            });
        });
        spremnik.querySelectorAll("[data-obrisi]").forEach(function (gumb) {
            gumb.addEventListener("click", function () {
                obrisiKolegij(Number(gumb.getAttribute("data-obrisi")));
            });
        });
    }

    /** Jedan kolegij kao vodoravni redak s ikonom mape. */
    function nacrtajRedak(kolegij) {
        const oznakaStatusa = kolegij.aktivan
            ? '<span class="oznaka oznaka-uspjeh">Aktivan</span>'
            : '<span class="oznaka oznaka-neutralna">Neaktivan</span>';

        const predavaci = kolegij.predavaci.length === 0
            ? "Nema dodijeljenog predavača"
            : kolegij.predavaci.map(function (p) {
                return Zajednicko.escapeHtml(p.punoIme);
            }).join(", ");

        const katedra = kolegij.katedraNaziv
            ? Zajednicko.escapeHtml(kolegij.katedraKratica + " — " + kolegij.katedraNaziv)
            : "katedra nije određena";

        // Predavač vidi redak, ali bez gumba za uređivanje i brisanje.
        let gumbi = "";
        if (Zajednicko.jeAdmin()) {
            gumbi =
                '<button type="button" class="gumb gumb-sporedni gumb-mali" data-uredi="' +
                kolegij.id + '">Uredi</button>' +
                '<button type="button" class="gumb gumb-opasni gumb-mali" data-obrisi="' +
                kolegij.id + '">Obriši</button>';
        }

        return (
            '<article class="red-kolegija' + (kolegij.aktivan ? "" : " neaktivan") + '">' +
            '  <svg class="ikona-mape" viewBox="0 0 24 24" aria-hidden="true">' +
            '    <path d="M3 7a2 2 0 0 1 2-2h4l2 2h8a2 2 0 0 1 2 2v9a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2z"/>' +
            "  </svg>" +
            '  <div class="red-kolegija-tijelo">' +
            '    <div class="red-kolegija-naziv">' +
            '      <span class="oznaka oznaka-primarna">' + Zajednicko.escapeHtml(kolegij.sifra) + "</span>" +
            "      " + Zajednicko.escapeHtml(kolegij.naziv) +
            "    </div>" +
            '    <div class="red-kolegija-meta">' +
            "      <strong>" + kolegij.ects + " ECTS</strong> &middot; " + katedra +
            "      &middot; Predavači: " + predavaci +
            "    </div>" +
            (kolegij.opis
                ? '    <div class="red-kolegija-opis">' +
                  Zajednicko.escapeHtml(Zajednicko.skrati(kolegij.opis, 140)) + "</div>"
                : "") +
            "  </div>" +
            '  <div class="red-kolegija-akcije">' + oznakaStatusa + gumbi + "</div>" +
            "</article>"
        );
    }

    /**
     * Pretraga po nazivu/šifri te filteri po katedri, godini, semestru i statusu.
     * Svi se filteri primjenjuju zajedno (logičko I).
     */
    function primijeniFiltere() {
        const pojam = poljePretrage.value.trim().toLowerCase();
        const katedraId = filterKatedre.value;
        const godina = filterGodine.value;
        const semestar = filterSemestra.value;
        const status = filterStatusa.value;

        return sviKolegiji.filter(function (kolegij) {
            if (pojam !== "") {
                const tekst = (kolegij.naziv + " " + kolegij.sifra).toLowerCase();
                if (tekst.indexOf(pojam) === -1) {
                    return false;
                }
            }
            if (katedraId !== "" && String(kolegij.katedraId) !== katedraId) {
                return false;
            }
            if (godina !== "" && String(kolegij.godinaStudija) !== godina) {
                return false;
            }
            if (semestar !== "" && String(kolegij.semestar) !== semestar) {
                return false;
            }
            if (status === "aktivan" && !kolegij.aktivan) {
                return false;
            }
            if (status === "neaktivan" && kolegij.aktivan) {
                return false;
            }
            return true;
        });
    }

    /* --- Kaskadni filteri ---------------------------------------------------- */

    /** Puni izbornik katedri u filteru i u formi za unos kolegija. */
    function napuniIzbornikeKatedri() {
        let opcije = "";
        sveKatedre.forEach(function (katedra) {
            opcije += '<option value="' + katedra.id + '">' +
                Zajednicko.escapeHtml(katedra.kratica + " — " + katedra.naziv) + "</option>";
        });

        filterKatedre.innerHTML = '<option value="">Sve katedre</option>' + opcije;
        document.getElementById("katedraId").innerHTML =
            '<option value="">— bez katedre —</option>' + opcije;
    }

    /**
     * Sužava izbor semestra na semestre odabrane godine studija.
     *
     * Ovo je "povezani" dio filtera: bez toga bi korisnik mogao odabrati
     * 2. godinu i 5. semestar, što je kombinacija koja ne postoji, pa bi
     * popis uvijek ispao prazan.
     */
    function osvjeziIzborSemestra() {
        const godina = filterGodine.value;
        const prethodni = filterSemestra.value;

        let semestri;
        if (godina === "") {
            semestri = [1, 2, 3, 4, 5, 6];
        } else {
            semestri = Zajednicko.semestriGodine(godina);
        }

        let html = '<option value="">Svi semestri</option>';
        semestri.forEach(function (broj) {
            html += '<option value="' + broj + '">' + broj + ". semestar</option>";
        });
        filterSemestra.innerHTML = html;

        // Zadržimo prethodni odabir ako i dalje postoji u suženom popisu.
        filterSemestra.value = semestri.indexOf(Number(prethodni)) !== -1 ? prethodni : "";
    }

    /* --- Forma -------------------------------------------------------------- */

    /**
     * Otvara modal. Ako je id null, radi se o novom kolegiju,
     * inače se polja popune postojećim podacima.
     */
    async function otvoriFormu(id) {
        Zajednicko.ocistiGreskeForme(forma);
        Zajednicko.sakrijGresku("greskaForme");
        forma.reset();

        // Popis predavača treba i za novi i za postojeći kolegij,
        // a dohvaća se tek kod prvog otvaranja forme.
        if (sviPredavaci.length === 0) {
            try {
                sviPredavaci = await Api.predavaci();
            } catch (greska) {
                Zajednicko.prikaziToast("Nije moguće dohvatiti popis predavača.", "greska");
            }
        }

        const kolegij = id === null ? null : sviKolegiji.find(function (k) {
            return k.id === id;
        });

        document.getElementById("naslovModala").textContent =
            kolegij ? "Uređivanje kolegija" : "Novi kolegij";
        document.getElementById("kolegijId").value = kolegij ? kolegij.id : "";
        document.getElementById("sifra").value = kolegij ? kolegij.sifra : "";
        document.getElementById("naziv").value = kolegij ? kolegij.naziv : "";
        document.getElementById("opis").value = (kolegij && kolegij.opis) ? kolegij.opis : "";
        document.getElementById("ects").value = kolegij ? kolegij.ects : 5;
        document.getElementById("semestar").value = kolegij ? kolegij.semestar : 1;
        document.getElementById("aktivan").checked = kolegij ? kolegij.aktivan : true;
        document.getElementById("katedraId").value =
            (kolegij && kolegij.katedraId) ? kolegij.katedraId : "";

        const odabrani = kolegij ? kolegij.predavaci.map(function (p) {
            return p.id;
        }) : [];
        nacrtajPopisPredavaca(odabrani);

        Zajednicko.otvoriModal("modalKolegij");
        document.getElementById("sifra").focus();
    }

    function nacrtajPopisPredavaca(odabraniIdovi) {
        const spremnik = document.getElementById("popisPredavaca");

        if (sviPredavaci.length === 0) {
            spremnik.innerHTML =
                '<p style="color:var(--boja-tekst-blijedi);font-size:13px">' +
                "U sustavu još nema predavača.</p>";
            return;
        }

        let html = "";
        sviPredavaci.forEach(function (predavac) {
            const oznacen = odabraniIdovi.indexOf(predavac.id) !== -1 ? " checked" : "";
            const neaktivan = predavac.aktivan ? "" : " (neaktivan)";
            html +=
                '<div class="stavka">' +
                '  <input type="checkbox" id="predavac_' + predavac.id + '" value="' +
                predavac.id + '"' + oznacen + ">" +
                '  <label for="predavac_' + predavac.id + '">' +
                Zajednicko.escapeHtml(predavac.punoIme + neaktivan) + "</label>" +
                "</div>";
        });
        spremnik.innerHTML = html;
    }

    function zatvoriFormu() {
        Zajednicko.zatvoriModal("modalKolegij");
    }

    async function spremiKolegij(dogadaj) {
        dogadaj.preventDefault();

        Zajednicko.ocistiGreskeForme(forma);
        Zajednicko.sakrijGresku("greskaForme");

        const id = document.getElementById("kolegijId").value;

        const idPredavaca = [];
        document.querySelectorAll("#popisPredavaca input:checked").forEach(function (okvir) {
            idPredavaca.push(Number(okvir.value));
        });

        const odabranaKatedra = document.getElementById("katedraId").value;

        const podaci = {
            sifra: document.getElementById("sifra").value.trim(),
            naziv: document.getElementById("naziv").value.trim(),
            opis: document.getElementById("opis").value.trim(),
            ects: Number(document.getElementById("ects").value),
            semestar: Number(document.getElementById("semestar").value),
            aktivan: document.getElementById("aktivan").checked,
            katedraId: odabranaKatedra === "" ? null : Number(odabranaKatedra),
            idPredavaca: idPredavaca
        };

        const gumb = document.getElementById("gumbSpremi");
        gumb.disabled = true;

        try {
            await Api.spremiKolegij(id === "" ? null : Number(id), podaci);
            zatvoriFormu();
            Zajednicko.prikaziToast(id === "" ? "Kolegij je dodan." : "Kolegij je spremljen.", "uspjeh");
            await ucitajKolegije();
        } catch (greska) {
            Zajednicko.prikaziGresku("greskaForme", greska.message);
            Zajednicko.prikaziGreskeForme(forma, greska.greskePolja);
        } finally {
            gumb.disabled = false;
        }
    }

    /* --- Brisanje ------------------------------------------------------------ */

    async function obrisiKolegij(id) {
        const kolegij = sviKolegiji.find(function (k) {
            return k.id === id;
        });
        if (!kolegij) {
            return;
        }

        // Brisanje kolegija povlači i njegove obavijesti, pa upozoravamo na to.
        const potvrda = window.confirm(
            "Obrisati kolegij \"" + kolegij.naziv + "\"?\n\n" +
            "Zajedno s kolegijem obrisat će se i sve njegove obavijesti. " +
            "Ova radnja se ne može poništiti.");

        if (!potvrda) {
            return;
        }

        try {
            await Api.obrisiKolegij(id);
            Zajednicko.prikaziToast("Kolegij je obrisan.", "uspjeh");
            await ucitajKolegije();
        } catch (greska) {
            Zajednicko.prikaziToast(greska.message, "greska");
        }
    }
});

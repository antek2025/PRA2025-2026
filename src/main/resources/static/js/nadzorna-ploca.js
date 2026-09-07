/* ==========================================================================
   nadzorna-ploca.js - početni ekran nakon prijave.
   --------------------------------------------------------------------------
   Prikazuje kratki pregled: par brojeva, aktualne obavijesti i popis kolegija.
   Podaci se dohvaćaju s istih ruta kao i na ostalim stranicama - backend sam
   suzi popis prema ulozi, pa se ovdje ne mora ništa dodatno filtrirati.
   ========================================================================== */

document.addEventListener("DOMContentLoaded", async function () {

    const korisnik = await Zajednicko.pokreniStranicu("nadzorna-ploca.html");
    if (!korisnik) {
        return;
    }

    document.getElementById("pozdrav").textContent =
        "Dobrodošli, " + korisnik.punoIme + " (" + korisnik.nazivUloge + ")";

    // Predavaču pišemo "Moji kolegiji" jer i vidi samo svoje.
    document.getElementById("naslovKolegija").textContent =
        Zajednicko.jeAdmin() ? "Svi kolegiji" : "Moji kolegiji";

    ucitajPodatke();

    async function ucitajPodatke() {
        try {
            /*
             * Promise.all pokreće oba zahtjeva odjednom umjesto jedan pa drugi.
             * Kad su neovisni, nema razloga da drugi čeka prvi.
             */
            const [kolegiji, obavijesti] = await Promise.all([
                Api.kolegiji(),
                Api.obavijesti(null)
            ]);

            nacrtajStatistike(kolegiji, obavijesti);
            nacrtajObavijesti(obavijesti);
            nacrtajKolegije(kolegiji);
        } catch (greska) {
            if (greska.status === 401) {
                window.location.href = "index.html";
                return;
            }
            Zajednicko.prikaziGresku("porukaGreske", greska.message);
        }
    }

    function nacrtajStatistike(kolegiji, obavijesti) {
        const aktivneObavijesti = obavijesti.filter(function (o) {
            return o.aktivna;
        }).length;

        const aktivniKolegiji = kolegiji.filter(function (k) {
            return k.aktivan;
        }).length;

        const stavke = [
            { broj: kolegiji.length, opis: Zajednicko.jeAdmin() ? "Kolegija ukupno" : "Mojih kolegija" },
            { broj: aktivniKolegiji, opis: "Aktivnih kolegija" },
            { broj: obavijesti.length, opis: "Obavijesti ukupno" },
            { broj: aktivneObavijesti, opis: "Trenutno vrijedi" }
        ];

        let html = "";
        stavke.forEach(function (stavka) {
            html +=
                '<div class="statistika">' +
                '  <div class="statistika-broj">' + stavka.broj + "</div>" +
                '  <div class="statistika-opis">' + stavka.opis + "</div>" +
                "</div>";
        });

        document.getElementById("statistike").innerHTML = html;
    }

    function nacrtajObavijesti(obavijesti) {
        const spremnik = document.getElementById("popisObavijesti");

        // Na nadzornoj ploči prikazujemo samo obavijesti koje danas vrijede,
        // i to najviše pet - detaljan popis je na stranici Obavijesti.
        const aktualne = obavijesti.filter(function (o) {
            return o.aktivna;
        }).slice(0, 5);

        if (aktualne.length === 0) {
            spremnik.innerHTML =
                '<div class="prazno"><strong>Nema aktualnih obavijesti</strong>' +
                "Trenutno nema obavijesti koje vrijede na današnji dan.</div>";
            return;
        }

        let redci = "";
        aktualne.forEach(function (obavijest) {
            redci +=
                "<tr>" +
                "  <td><strong>" + Zajednicko.escapeHtml(obavijest.naslov) + "</strong><br>" +
                '    <span style="color:var(--boja-tekst-blijedi);font-size:13px">' +
                Zajednicko.escapeHtml(Zajednicko.skrati(obavijest.opis, 90)) + "</span></td>" +
                '  <td><span class="oznaka oznaka-primarna">' +
                Zajednicko.escapeHtml(obavijest.kolegijSifra) + "</span></td>" +
                "  <td>" + Zajednicko.escapeHtml(obavijest.autorPunoIme) + "</td>" +
                "  <td>" + Zajednicko.formatirajDatum(obavijest.datumIsteka) + "</td>" +
                "</tr>";
        });

        spremnik.innerHTML =
            '<div class="tablica-omot">' +
            "<table>" +
            "  <thead><tr><th>Obavijest</th><th>Kolegij</th><th>Autor</th><th>Vrijedi do</th></tr></thead>" +
            "  <tbody>" + redci + "</tbody>" +
            "</table></div>" +
            '<p style="margin-top:12px"><a href="obavijesti.html">Prikaži sve obavijesti &rarr;</a></p>';
    }

    function nacrtajKolegije(kolegiji) {
        const spremnik = document.getElementById("popisKolegija");

        if (kolegiji.length === 0) {
            spremnik.innerHTML =
                '<div class="prazno"><strong>Nema kolegija</strong>' +
                (Zajednicko.jeAdmin()
                    ? "Dodajte prvi kolegij na stranici Kolegiji."
                    : "Niste dodijeljeni ni na jedan kolegij. Javite se administratoru.") +
                "</div>";
            return;
        }

        let redci = "";
        kolegiji.forEach(function (kolegij) {
            const imenaPredavaca = kolegij.predavaci.map(function (p) {
                return p.punoIme;
            }).join(", ");

            const oznakaStatusa = kolegij.aktivan
                ? '<span class="oznaka oznaka-uspjeh">Aktivan</span>'
                : '<span class="oznaka oznaka-neutralna">Neaktivan</span>';

            redci +=
                "<tr>" +
                '  <td><span class="oznaka oznaka-primarna">' +
                Zajednicko.escapeHtml(kolegij.sifra) + "</span></td>" +
                "  <td><strong>" + Zajednicko.escapeHtml(kolegij.naziv) + "</strong></td>" +
                "  <td>" + kolegij.semestar + ".</td>" +
                "  <td>" + kolegij.ects + "</td>" +
                "  <td>" + (imenaPredavaca
                    ? Zajednicko.escapeHtml(imenaPredavaca)
                    : '<span style="color:var(--boja-tekst-blijedi)">Nije dodijeljen</span>') + "</td>" +
                "  <td>" + oznakaStatusa + "</td>" +
                "</tr>";
        });

        spremnik.innerHTML =
            '<div class="tablica-omot">' +
            "<table>" +
            "  <thead><tr><th>Šifra</th><th>Naziv</th><th>Sem.</th><th>ECTS</th>" +
            "      <th>Predavači</th><th>Status</th></tr></thead>" +
            "  <tbody>" + redci + "</tbody>" +
            "</table></div>";
    }
});

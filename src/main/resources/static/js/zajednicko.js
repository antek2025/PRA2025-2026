/* ==========================================================================
   zajednicko.js - stvari koje trebaju svakoj stranici osim prijave.
   --------------------------------------------------------------------------
   Ovdje je:
   - provjera je li korisnik prijavljen i crtanje zaglavlja s izbornikom,
   - pomoćne funkcije za datume, escapanje teksta i kratke poruke (toast),
   - otvaranje/zatvaranje modalnih prozora i prikaz grešaka u formama.

   Zaglavlje se crta iz JavaScripta, a ne kopira u svaki HTML, da se izbornik
   mijenja na jednom mjestu i da se stavke mogu sakriti ovisno o ulozi.
   ========================================================================== */

const Zajednicko = {

    /** Popunjava se u pokreniStranicu() pa ga stranice mogu čitati. */
    korisnik: null,

    /* --- Pokretanje stranice --------------------------------------------- */

    /**
     * Poziva se na početku svake zaštićene stranice.
     * Ako korisnik nije prijavljen, vraća ga na ekran za prijavu i vraća null.
     *
     * @param {string} aktivnaStranica naziv datoteke, npr. "kolegiji.html"
     * @returns {Promise<object|null>} podaci o prijavljenom korisniku
     */
    async pokreniStranicu(aktivnaStranica) {
        let odgovor;
        try {
            odgovor = await Api.trenutniKorisnik();
        } catch (greska) {
            document.body.innerHTML =
                '<div class="poruka poruka-greska" style="margin:24px">' +
                this.escapeHtml(greska.message) + "</div>";
            return null;
        }

        if (!odgovor || !odgovor.prijavljen) {
            window.location.href = "index.html";
            return null;
        }

        this.korisnik = odgovor.korisnik;
        this.nacrtajZaglavlje(aktivnaStranica);
        return this.korisnik;
    },

    /**
     * Crta zaglavlje (tamna traka) u <div id="zaglavlje"> i bočnu navigaciju
     * u <nav id="bocnaTraka">. Oba elementa ima svaka zaštićena stranica.
     *
     * Ikone su mali inline SVG-ovi (obrisi, bez ispune) da se boje automatski
     * preuzmu iz CSS-a preko "currentColor" - aktivna stavka je plava, ostale sive.
     */
    nacrtajZaglavlje(aktivnaStranica) {
        const zaglavlje = document.getElementById("zaglavlje");
        const bocna = document.getElementById("bocnaTraka");
        if (!zaglavlje) {
            return;
        }

        const jeAdmin = this.korisnik.uloga === "ADMIN";

        // Predavač ne vidi "Katedre" ni "Predavači" jer nema pravo upravljati njima.
        const stavke = [
            { datoteka: "nadzorna-ploca.html", naziv: "Nadzorna ploča", samoAdmin: false,
              ikona: '<path d="M3 11l9-8 9 8"/><path d="M5 10v10h14V10"/><path d="M10 20v-6h4v6"/>' },
            { datoteka: "kolegiji.html", naziv: "Kolegiji", samoAdmin: false,
              ikona: '<path d="M4 4h12a2 2 0 0 1 2 2v14H6a2 2 0 0 0-2 2z"/><path d="M4 18a2 2 0 0 1 2-2h12"/>' },
            { datoteka: "obavijesti.html", naziv: "Obavijesti", samoAdmin: false,
              ikona: '<path d="M6 9a6 6 0 0 1 12 0v5l2 3H4l2-3z"/><path d="M10 20a2 2 0 0 0 4 0"/>' },
            { datoteka: "katedre.html", naziv: "Katedre", samoAdmin: true,
              ikona: '<path d="M3 21h18"/><path d="M5 21V10l7-5 7 5v11"/><path d="M9 21v-6h6v6"/>' },
            { datoteka: "predavaci.html", naziv: "Predavači", samoAdmin: true,
              ikona: '<circle cx="9" cy="8" r="3.5"/><path d="M2.5 20a6.5 6.5 0 0 1 13 0"/><path d="M16 5a3 3 0 0 1 0 6"/><path d="M18 14a5 5 0 0 1 3.5 6"/>' },
            { datoteka: "profil.html", naziv: "Moj profil", samoAdmin: false,
              ikona: '<circle cx="12" cy="8" r="4"/><path d="M4 21a8 8 0 0 1 16 0"/>' }
        ];

        let navigacija = "";
        stavke.forEach(function (stavka) {
            if (stavka.samoAdmin && !jeAdmin) {
                return;
            }
            const aktivna = stavka.datoteka === aktivnaStranica ? " aktivna" : "";
            navigacija +=
                '<li><a href="' + stavka.datoteka + '" class="bocna-stavka' + aktivna + '">' +
                '<svg viewBox="0 0 24 24" aria-hidden="true">' + stavka.ikona + "</svg>" +
                "<span>" + stavka.naziv + "</span></a></li>";
        });

        zaglavlje.innerHTML =
            '<header class="zaglavlje">' +
            '  <div class="zaglavlje-sadrzaj">' +
            '    <a href="nadzorna-ploca.html" class="logo">' +
            '      <span class="logo-znak">i</span>' +
            '      <span>Infoeduka<span class="logo-podnaslov">Visoko učilište Algebra</span></span>' +
            "    </a>" +
            '    <div class="korisnik-blok">' +
            "      <div>" +
            '        <div class="korisnik-ime">' + this.escapeHtml(this.korisnik.punoIme) + "</div>" +
            '        <div class="korisnik-uloga">' + this.escapeHtml(this.korisnik.nazivUloge) + "</div>" +
            "      </div>" +
            '      <button type="button" class="gumb-odjava" id="gumbOdjava">Odjava</button>' +
            "    </div>" +
            "  </div>" +
            "</header>";

        if (bocna) {
            bocna.innerHTML = "<ul>" + navigacija + "</ul>";
        }

        document.getElementById("gumbOdjava").addEventListener("click", async function () {
            try {
                await Api.odjava();
            } catch (greska) {
                // Ako je sesija u međuvremenu istekla, korisnik je ionako odjavljen.
            }
            window.location.href = "index.html";
        });
    },

    /** Je li prijavljeni korisnik administrator. */
    jeAdmin() {
        return this.korisnik !== null && this.korisnik.uloga === "ADMIN";
    },

    /* --- Pomoćne funkcije ------------------------------------------------- */

    /**
     * Pretvara posebne znakove u HTML entitete.
     * Koristi se SVUGDJE gdje se podatak iz baze ubacuje u innerHTML - bez toga
     * bi netko mogao u naslov obavijesti upisati <script> i taj bi se izvršio.
     */
    escapeHtml(tekst) {
        if (tekst === null || tekst === undefined) {
            return "";
        }
        return String(tekst)
            .replace(/&/g, "&amp;")
            .replace(/</g, "&lt;")
            .replace(/>/g, "&gt;")
            .replace(/"/g, "&quot;")
            .replace(/'/g, "&#39;");
    },

    /** "2026-08-28" -> "28.08.2026." */
    formatirajDatum(isoDatum) {
        if (!isoDatum) {
            return "-";
        }
        const dijelovi = String(isoDatum).substring(0, 10).split("-");
        if (dijelovi.length !== 3) {
            return isoDatum;
        }
        return dijelovi[2] + "." + dijelovi[1] + "." + dijelovi[0] + ".";
    },

    /** Današnji datum u obliku koji traži <input type="date">. */
    danasZaInput() {
        const danas = new Date();
        const mjesec = String(danas.getMonth() + 1).padStart(2, "0");
        const dan = String(danas.getDate()).padStart(2, "0");
        return danas.getFullYear() + "-" + mjesec + "-" + dan;
    },

    /** Datum pomaknut za zadani broj dana, u obliku za <input type="date">. */
    datumZaInput(pomakDana) {
        const datum = new Date();
        datum.setDate(datum.getDate() + pomakDana);
        const mjesec = String(datum.getMonth() + 1).padStart(2, "0");
        const dan = String(datum.getDate()).padStart(2, "0");
        return datum.getFullYear() + "-" + mjesec + "-" + dan;
    },

    /**
     * Semestri koji pripadaju zadanoj godini studija.
     * 1. godina -> [1,2], 2. -> [3,4], 3. -> [5,6].
     * Koristi se za kaskadne filtere (odabir godine sužava izbor semestra).
     */
    semestriGodine(godina) {
        const g = Number(godina);
        return [g * 2 - 1, g * 2];
    },

    /** Skraćuje dugačak tekst da ne razvuče tablicu. */
    skrati(tekst, duljina) {
        if (!tekst) {
            return "";
        }
        return tekst.length > duljina ? tekst.substring(0, duljina) + "..." : tekst;
    },

    /* --- Poruke ----------------------------------------------------------- */

    /**
     * Kratka poruka u donjem desnom kutu koja sama nestane nakon 3 sekunde.
     *
     * @param {string} poruka
     * @param {string} tip "uspjeh" ili "greska"
     */
    prikaziToast(poruka, tip) {
        const stari = document.querySelector(".obavijest-toast");
        if (stari) {
            stari.remove();
        }

        const element = document.createElement("div");
        element.className = "obavijest-toast " + (tip || "uspjeh");
        element.textContent = poruka;
        document.body.appendChild(element);

        setTimeout(function () {
            element.remove();
        }, 3000);
    },

    /** Prikazuje poruku greške unutar zadanog elementa. */
    prikaziGresku(idElementa, poruka) {
        const element = document.getElementById(idElementa);
        if (!element) {
            return;
        }
        element.textContent = poruka;
        element.classList.remove("skriveno");
    },

    sakrijGresku(idElementa) {
        const element = document.getElementById(idElementa);
        if (element) {
            element.classList.add("skriveno");
            element.textContent = "";
        }
    },

    /* --- Forme ------------------------------------------------------------ */

    /**
     * Označava polja koja backend nije prihvatio.
     * Backend u ApiGreska.greskePolja vraća mapu naziv polja -> poruka, a nazivi
     * polja u formi su namjerno isti kao u DTO-u pa se mogu spojiti po imenu.
     */
    prikaziGreskeForme(forma, greskePolja) {
        this.ocistiGreskeForme(forma);
        if (!greskePolja) {
            return;
        }

        Object.keys(greskePolja).forEach(function (naziv) {
            const polje = forma.querySelector('[name="' + naziv + '"]');
            if (!polje) {
                return;
            }
            polje.classList.add("neispravno");

            const poruka = document.createElement("span");
            poruka.className = "greska-polja";
            poruka.textContent = greskePolja[naziv];
            polje.parentNode.appendChild(poruka);
        });
    },

    ocistiGreskeForme(forma) {
        forma.querySelectorAll(".neispravno").forEach(function (polje) {
            polje.classList.remove("neispravno");
        });
        forma.querySelectorAll(".greska-polja").forEach(function (poruka) {
            poruka.remove();
        });
    },

    /* --- Modalni prozor ---------------------------------------------------- */

    otvoriModal(idModala) {
        document.getElementById(idModala).classList.remove("skriveno");
    },

    zatvoriModal(idModala) {
        document.getElementById(idModala).classList.add("skriveno");
    },

    /**
     * Zatvaranje modala klikom na tamnu pozadinu ili tipkom Escape.
     * Klik unutar samog modala ne smije ga zatvoriti, zato provjera
     * event.target === pozadina.
     */
    poveziZatvaranjeModala(idModala) {
        const pozadina = document.getElementById(idModala);
        const self = this;

        pozadina.addEventListener("click", function (dogadaj) {
            if (dogadaj.target === pozadina) {
                self.zatvoriModal(idModala);
            }
        });

        document.addEventListener("keydown", function (dogadaj) {
            if (dogadaj.key === "Escape" && !pozadina.classList.contains("skriveno")) {
                self.zatvoriModal(idModala);
            }
        });
    }
};

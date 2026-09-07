/* ==========================================================================
   api.js - jedno mjesto preko kojeg cijeli frontend razgovara s backendom.
   --------------------------------------------------------------------------
   Zašto postoji:
   - da se fetch, zaglavlja i pretvaranje odgovora u JSON ne prepisuju na
     svakoj stranici,
   - da se greške s backenda uvijek obrade na isti način,
   - da se istek sesije (HTTP 401) na jednom mjestu pretvori u povratak
     na ekran za prijavu.

   Datoteka se učitava PRIJE ostalih skripti jer se svi ostali JS-evi
   oslanjaju na globalni objekt "Api".
   ========================================================================== */

/**
 * Greška koju vrati backend. Osim poruke nosi i HTTP status te mapu
 * greškaPolja (naziv polja -> poruka) koju koristimo za označavanje polja u formi.
 */
class ApiGreska extends Error {
    constructor(poruka, status, greskePolja) {
        super(poruka);
        this.name = "ApiGreska";
        this.status = status;
        this.greskePolja = greskePolja || null;
    }
}

const Api = {

    /** Osnovna putanja API-ja. Frontend i backend su na istom poslužitelju. */
    OSNOVNA_PUTANJA: "/api",

    /**
     * Šalje zahtjev i vraća pročitani JSON (ili null ako odgovor nema tijelo).
     *
     * @param {string} putanja npr. "/kolegiji"
     * @param {object} opcije  { metoda, tijelo }
     */
    async zahtjev(putanja, opcije = {}) {
        const postavke = {
            method: opcije.metoda || "GET",
            headers: {},
            // Bez ovoga preglednik ne bi slao cookie sesije pa bi svaki
            // zahtjev izgledao kao da korisnik nije prijavljen.
            credentials: "same-origin"
        };

        if (opcije.tijelo !== undefined) {
            postavke.headers["Content-Type"] = "application/json";
            postavke.body = JSON.stringify(opcije.tijelo);
        }

        let odgovor;
        try {
            odgovor = await fetch(this.OSNOVNA_PUTANJA + putanja, postavke);
        } catch (greska) {
            // Ovdje završi samo pad mreže - poslužitelj nije upaljen ili nema veze.
            throw new ApiGreska("Nije moguće doći do poslužitelja. Provjerite je li aplikacija pokrenuta.", 0, null);
        }

        // 204 No Content (npr. nakon brisanja) nema tijelo pa ga ne pokušavamo čitati.
        if (odgovor.status === 204) {
            return null;
        }

        let podaci = null;
        const tipSadrzaja = odgovor.headers.get("Content-Type") || "";
        if (tipSadrzaja.includes("application/json")) {
            podaci = await odgovor.json();
        }

        if (!odgovor.ok) {
            const poruka = (podaci && podaci.poruka)
                ? podaci.poruka
                : "Greška " + odgovor.status + " prilikom komunikacije s poslužiteljem.";
            throw new ApiGreska(poruka, odgovor.status, podaci ? podaci.greskePolja : null);
        }

        return podaci;
    },

    get(putanja) {
        return this.zahtjev(putanja, { metoda: "GET" });
    },

    post(putanja, tijelo) {
        return this.zahtjev(putanja, { metoda: "POST", tijelo: tijelo });
    },

    put(putanja, tijelo) {
        return this.zahtjev(putanja, { metoda: "PUT", tijelo: tijelo });
    },

    obrisi(putanja) {
        return this.zahtjev(putanja, { metoda: "DELETE" });
    },

    /* --- Prijava / odjava ------------------------------------------------ */

    prijava(email, lozinka) {
        return this.post("/auth/prijava", { email: email, lozinka: lozinka });
    },

    odjava() {
        return this.post("/auth/odjava");
    },

    /** Vraća { prijavljen: false } ili { prijavljen: true, korisnik: {...} }. */
    trenutniKorisnik() {
        return this.get("/auth/ja");
    },

    promjenaLozinke(staraLozinka, novaLozinka) {
        return this.post("/auth/promjena-lozinke", {
            staraLozinka: staraLozinka,
            novaLozinka: novaLozinka
        });
    },

    /* --- Kolegiji -------------------------------------------------------- */

    kolegiji() {
        return this.get("/kolegiji");
    },

    kolegij(id) {
        return this.get("/kolegiji/" + id);
    },

    spremiKolegij(id, podaci) {
        return id ? this.put("/kolegiji/" + id, podaci) : this.post("/kolegiji", podaci);
    },

    obrisiKolegij(id) {
        return this.obrisi("/kolegiji/" + id);
    },

    /* --- Katedre --------------------------------------------------------- */

    katedre() {
        return this.get("/katedre");
    },

    spremiKatedru(id, podaci) {
        return id ? this.put("/katedre/" + id, podaci) : this.post("/katedre", podaci);
    },

    obrisiKatedru(id) {
        return this.obrisi("/katedre/" + id);
    },

    /* --- Korisnici ------------------------------------------------------- */

    korisnici() {
        return this.get("/korisnici");
    },

    predavaci() {
        return this.get("/korisnici/predavaci");
    },

    spremiKorisnika(id, podaci) {
        return id ? this.put("/korisnici/" + id, podaci) : this.post("/korisnici", podaci);
    },

    obrisiKorisnika(id) {
        return this.obrisi("/korisnici/" + id);
    },

    /* --- Obavijesti ------------------------------------------------------ */

    /** @param {number|null} kolegijId neobavezan filter po kolegiju */
    obavijesti(kolegijId) {
        return this.get(kolegijId ? "/obavijesti?kolegijId=" + kolegijId : "/obavijesti");
    },

    spremiObavijest(id, podaci) {
        return id ? this.put("/obavijesti/" + id, podaci) : this.post("/obavijesti", podaci);
    },

    obrisiObavijest(id) {
        return this.obrisi("/obavijesti/" + id);
    }
};

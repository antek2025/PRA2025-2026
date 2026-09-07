/* ==========================================================================
   profil.js - pregled vlastitih podataka i promjena lozinke.
   --------------------------------------------------------------------------
   Podaci o korisniku se ne dohvaćaju posebno - već ih ima Zajednicko.korisnik
   jer ih je pokreniStranicu() pročitao s /api/auth/ja.
   ========================================================================== */

document.addEventListener("DOMContentLoaded", async function () {

    const korisnik = await Zajednicko.pokreniStranicu("profil.html");
    if (!korisnik) {
        return;
    }

    document.getElementById("prikazImePrezime").textContent = korisnik.punoIme;
    document.getElementById("prikazEmail").textContent = korisnik.email;
    document.getElementById("prikazUloga").textContent = korisnik.nazivUloge;

    const forma = document.getElementById("formaLozinke");

    forma.addEventListener("submit", async function (dogadaj) {
        dogadaj.preventDefault();

        Zajednicko.sakrijGresku("greskaForme");
        Zajednicko.sakrijGresku("porukaUspjeha");
        Zajednicko.ocistiGreskeForme(forma);

        const stara = document.getElementById("staraLozinka").value;
        const nova = document.getElementById("novaLozinka").value;
        const potvrda = document.getElementById("potvrdaLozinke").value;

        if (stara === "" || nova === "") {
            Zajednicko.prikaziGresku("greskaForme", "Popunite sva polja.");
            return;
        }

        // Potvrda se provjerava samo ovdje - backend je ni ne prima
        // jer je to stvar sučelja, a ne poslovnog pravila.
        if (nova !== potvrda) {
            Zajednicko.prikaziGresku("greskaForme", "Nova lozinka i potvrda se ne podudaraju.");
            return;
        }

        if (nova.length < 6) {
            Zajednicko.prikaziGresku("greskaForme", "Nova lozinka mora imati barem 6 znakova.");
            return;
        }

        const gumb = document.getElementById("gumbSpremi");
        gumb.disabled = true;

        try {
            await Api.promjenaLozinke(stara, nova);
            forma.reset();
            Zajednicko.prikaziGresku("porukaUspjeha", "Lozinka je uspješno promijenjena.");
            Zajednicko.prikaziToast("Lozinka je promijenjena.", "uspjeh");
        } catch (greska) {
            Zajednicko.prikaziGresku("greskaForme", greska.message);
            Zajednicko.prikaziGreskeForme(forma, greska.greskePolja);
        } finally {
            gumb.disabled = false;
        }
    });
});

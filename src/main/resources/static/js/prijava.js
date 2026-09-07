/* ==========================================================================
   prijava.js - logika ekrana za prijavu (index.html)
   ========================================================================== */

document.addEventListener("DOMContentLoaded", function () {

    const forma = document.getElementById("formaPrijave");
    const poljeEmail = document.getElementById("email");
    const poljeLozinke = document.getElementById("lozinka");
    const gumb = document.getElementById("gumbPrijava");

    // Ako korisnik već ima sesiju (npr. otvorio je karticu pa se vratio),
    // nema smisla da ponovno upisuje podatke - odmah ga šaljemo dalje.
    provjeriPostojecuPrijavu();

    async function provjeriPostojecuPrijavu() {
        try {
            const odgovor = await Api.trenutniKorisnik();
            if (odgovor && odgovor.prijavljen) {
                window.location.href = "nadzorna-ploca.html";
            }
        } catch (greska) {
            // Poslužitelj nije dostupan - korisnik će grešku vidjeti kad pokuša prijavu.
        }
    }

    forma.addEventListener("submit", async function (dogadaj) {
        // Bez ovoga bi preglednik napravio klasičan POST i osvježio stranicu.
        dogadaj.preventDefault();

        Zajednicko.sakrijGresku("porukaGreske");
        Zajednicko.ocistiGreskeForme(forma);

        const email = poljeEmail.value.trim();
        const lozinka = poljeLozinke.value;

        // Provjera na klijentu je samo da korisnik brže dobije povratnu informaciju -
        // backend svejedno provjerava sve ispočetka.
        if (email === "" || lozinka === "") {
            Zajednicko.prikaziGresku("porukaGreske", "Unesite e-mail adresu i lozinku.");
            return;
        }

        gumb.disabled = true;
        gumb.textContent = "Prijava u tijeku...";

        try {
            await Api.prijava(email, lozinka);
            window.location.href = "nadzorna-ploca.html";
        } catch (greska) {
            Zajednicko.prikaziGresku("porukaGreske", greska.message);
            Zajednicko.prikaziGreskeForme(forma, greska.greskePolja);
            poljeLozinke.value = "";
            poljeLozinke.focus();
        } finally {
            gumb.disabled = false;
            gumb.textContent = "Prijava";
        }
    });
});

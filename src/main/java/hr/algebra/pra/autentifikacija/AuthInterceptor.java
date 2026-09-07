package hr.algebra.pra.autentifikacija;

import java.time.LocalDateTime;

import tools.jackson.databind.ObjectMapper;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import hr.algebra.pra.zajednicko.ApiGreska;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * Zaustavlja svaki zahtjev prema /api/** ako korisnik nije prijavljen.
 *
 * Ovo je "prva brana" - provjerava samo POSTOJI li prijava. Provjera prava
 * (smije li bas ovaj korisnik bas ovu akciju) je u servisima jer ovisi o podacima,
 * npr. o tome predaje li korisnik na kolegiju.
 *
 * Iznimke su /api/auth/prijava i /api/auth/ja - njima se pristupa upravo zato
 * da bi se korisnik prijavio, odnosno da frontend provjeri ima li vec sesiju.
 */
@Component
public class AuthInterceptor implements HandlerInterceptor {

    private final SesijaKorisnika sesijaKorisnika;
    private final ObjectMapper objectMapper;

    public AuthInterceptor(SesijaKorisnika sesijaKorisnika, ObjectMapper objectMapper) {
        this.sesijaKorisnika = sesijaKorisnika;
        this.objectMapper = objectMapper;
    }

    @Override
    public boolean preHandle(HttpServletRequest zahtjev, HttpServletResponse odgovor, Object handler)
            throws Exception {

        // CORS preflight zahtjeve preglednik salje bez cookieja pa ih propustamo.
        if ("OPTIONS".equalsIgnoreCase(zahtjev.getMethod())) {
            return true;
        }

        boolean prijavljen = sesijaKorisnika.procitaj(zahtjev.getSession(false)) != null;
        if (prijavljen) {
            return true;
        }

        // Rucno pisemo 401 jer interceptor radi prije kontrolera, pa ga
        // GlobalniHandlerGresaka ne bi uhvatio.
        ApiGreska greska = new ApiGreska(
                HttpStatus.UNAUTHORIZED.value(),
                "Niste prijavljeni ili je sesija istekla.",
                null,
                LocalDateTime.now());

        odgovor.setStatus(HttpStatus.UNAUTHORIZED.value());
        odgovor.setContentType(MediaType.APPLICATION_JSON_VALUE);
        odgovor.setCharacterEncoding("UTF-8");
        objectMapper.writeValue(odgovor.getWriter(), greska);
        return false;
    }
}

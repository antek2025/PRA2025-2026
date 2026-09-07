package hr.algebra.pra.konfiguracija;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import hr.algebra.pra.autentifikacija.AuthInterceptor;

/**
 * Registracija AuthInterceptor-a.
 *
 * Interceptor pokrivamo SAMO na /api/**. HTML, CSS i JS datoteke su slobodno
 * dostupne jer u njima nema podataka - cim neka stranica zatrazi podatke,
 * ide preko /api/** i tu se provjerava prijava. Zbog toga korisnik koji nije
 * prijavljen moze otvoriti npr. kolegiji.html, ali ce dobiti 401 na dohvat
 * podataka i JavaScript ga vraca na ekran za prijavu.
 *
 * Izuzeti su:
 *  - /api/auth/prijava  (jos nema sesije)
 *  - /api/auth/odjava   (radi i ako je sesija u medjuvremenu istekla)
 *  - /api/auth/ja       (frontend njime provjerava postoji li uopce sesija)
 */
@Configuration
public class WebKonfiguracija implements WebMvcConfigurer {

    private final AuthInterceptor authInterceptor;

    public WebKonfiguracija(AuthInterceptor authInterceptor) {
        this.authInterceptor = authInterceptor;
    }

    @Override
    public void addInterceptors(InterceptorRegistry registar) {
        registar.addInterceptor(authInterceptor)
                .addPathPatterns("/api/**")
                .excludePathPatterns(
                        "/api/auth/prijava",
                        "/api/auth/odjava",
                        "/api/auth/ja");
    }
}

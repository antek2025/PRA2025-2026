package hr.algebra.pra.konfiguracija;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

/**
 * Jedina "sigurnosna" konfiguracija koja nam treba - alat za hashiranje lozinki.
 *
 * Ne koristimo cijeli Spring Security starter jer bi nam odmah ukljucio svoj
 * filter chain, login formu i CSRF zastitu, pa bismo vise vremena potrosili na
 * gasenje toga nego na sam projekt. Prijavu radimo sami preko HttpSession-a
 * (vidi AuthKontroler i AuthInterceptor), a odavde uzimamo samo BCrypt.
 *
 * BCrypt sam generira nasumicnu sol za svaku lozinku i sprema je unutar hasha,
 * pa dvije iste lozinke u bazi izgledaju razlicito.
 */
@Configuration
public class SigurnosnaKonfiguracija {

    @Bean
    public PasswordEncoder lozinkaEnkoder() {
        return new BCryptPasswordEncoder();
    }
}

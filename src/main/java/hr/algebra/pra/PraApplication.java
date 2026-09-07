package hr.algebra.pra;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Ulazna tocka aplikacije Infoeduka.
 *
 * Aplikacija je monolit - jedan Spring Boot proces posluzuje i REST API
 * (/api/**) i staticki frontend (HTML/CSS/JS iz src/main/resources/static).
 * Zbog toga frontend i backend dijele istu adresu i port, pa nema problema
 * s CORS-om ni s cookiejem sesije.
 */
@SpringBootApplication
public class PraApplication {

    public static void main(String[] args) {
        SpringApplication.run(PraApplication.class, args);
    }

}

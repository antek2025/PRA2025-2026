package hr.algebra.pra;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

/**
 * Smoke test - provjerava da se cijeli Spring kontekst uspjesno podigne, tj.
 * da su svi beanovi (repozitoriji, servisi, kontroleri, interceptor) ispravno
 * povezani.
 *
 * VAZNO: ovaj test se stvarno spaja na PostgreSQL bazu "pra_projekt" iz
 * application.properties, pa baza mora biti pokrenuta. Ostali testovi
 * (*ServisTest) koriste Mockito i ne trebaju bazu.
 *
 * Pokretanje samo unit testova bez baze:
 *   mvnw test -Dtest="*ServisTest"
 */
@SpringBootTest
class PraApplicationTests {

    @Test
    void contextLoads() {
        // Ako se kontekst ne moze podici, test pada sam od sebe.
    }

}

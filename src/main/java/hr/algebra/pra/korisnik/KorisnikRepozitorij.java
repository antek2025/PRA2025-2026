package hr.algebra.pra.korisnik;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Spring Data sam generira implementaciju iz naziva metoda,
 * pa ovdje nema rucno pisanog SQL-a.
 */
public interface KorisnikRepozitorij extends JpaRepository<Korisnik, Long> {

    Optional<Korisnik> findByEmailIgnoreCase(String email);

    boolean existsByEmailIgnoreCase(String email);

    List<Korisnik> findAllByOrderByPrezimeAscImeAsc();

    List<Korisnik> findByUlogaOrderByPrezimeAscImeAsc(Uloga uloga);
}

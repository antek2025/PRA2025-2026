package hr.algebra.pra.kolegij;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

public interface KolegijRepozitorij extends JpaRepository<Kolegij, Long> {

    boolean existsBySifraIgnoreCase(String sifra);

    Optional<Kolegij> findBySifraIgnoreCase(String sifra);

    List<Kolegij> findAllByOrderByNazivAsc();

    /**
     * Kolegiji na kojima predaje zadani korisnik.
     * "PredavaciId" Spring Data prevodi u JOIN preko spojne tablice kolegij_predavac.
     */
    List<Kolegij> findByPredavaciIdOrderByNazivAsc(Long predavacId);

    /** Koristi KatedraServis - koliko kolegija visi na katedri (prije brisanja). */
    long countByKatedraId(Long katedraId);
}

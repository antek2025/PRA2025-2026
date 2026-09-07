package hr.algebra.pra.katedra;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

public interface KatedraRepozitorij extends JpaRepository<Katedra, Long> {

    boolean existsByKraticaIgnoreCase(String kratica);

    boolean existsByNazivIgnoreCase(String naziv);

    List<Katedra> findAllByOrderByNazivAsc();
}

package hr.algebra.pra.obavijest;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

public interface ObavijestRepozitorij extends JpaRepository<Obavijest, Long> {

    /** Koristi se prije brisanja korisnika - ako je autor necega, ne brisemo ga. */
    long countByAutorId(Long autorId);

    List<Obavijest> findAllByOrderByDatumObjaveDescIdDesc();

    List<Obavijest> findByKolegijIdOrderByDatumObjaveDescIdDesc(Long kolegijId);

    /** Obavijesti svih kolegija na kojima predavac predaje. */
    List<Obavijest> findByKolegijIdInOrderByDatumObjaveDescIdDesc(List<Long> kolegijIdovi);
}

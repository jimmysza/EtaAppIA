package maineta.eta.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import maineta.eta.entity.Idioma;


@Repository 
public interface IdiomaRepository extends JpaRepository<Idioma, Long> {
    Optional<Idioma> findByNombre(String nombre);
}

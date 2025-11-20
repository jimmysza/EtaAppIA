package maineta.eta.repository;

import maineta.eta.entity.Idioma;
import maineta.eta.entity.Rol;
import org.springframework.data.jpa.repository.JpaRepository;

import org.springframework.stereotype.Repository;

import java.util.Optional;


@Repository 
public interface IdiomaRepository extends JpaRepository<Idioma, Long> {
    Optional<Idioma> findByNombre(String nombre);
}

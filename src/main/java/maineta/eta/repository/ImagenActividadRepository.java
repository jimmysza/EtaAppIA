package maineta.eta.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import maineta.eta.entity.ImagenActividad;

@Repository
public interface ImagenActividadRepository extends JpaRepository<ImagenActividad, Long> {

    List<ImagenActividad> findByActividad_IdActividad(Long idActividad);

    void deleteByActividad_IdActividad(Long idActividad);
}

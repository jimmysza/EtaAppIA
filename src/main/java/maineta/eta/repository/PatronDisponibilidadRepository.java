package maineta.eta.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import maineta.eta.entity.PatronDisponibilidad;

@Repository
public interface PatronDisponibilidadRepository extends JpaRepository<PatronDisponibilidad, Long> {

    List<PatronDisponibilidad> findByActividad_IdActividad(Long idActividad);

    List<PatronDisponibilidad> findByActividad_IdActividadAndEstado(Long idActividad, String estado);
}

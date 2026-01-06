package maineta.eta.repository;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.transaction.annotation.Transactional;

import maineta.eta.entity.Comentario;

public interface ComentarioRepository extends JpaRepository<Comentario, Long> {
    List<Comentario> findByActividad_IdActividad(Long idActividad);
    Page<Comentario> findByActividad_IdActividad(Long idActividad, Pageable pageable);

    @Modifying
    @Transactional
    @Query("DELETE FROM Comentario c WHERE c.actividad.id = :actividadId")
    void deleteByActividadId(Long actividadId);
}

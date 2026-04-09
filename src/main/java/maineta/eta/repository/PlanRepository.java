package maineta.eta.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import maineta.eta.entity.Plan;

public interface PlanRepository extends JpaRepository<Plan, Long> {
    
    // Top 5 planes públicos más recientes para la vista pública
    List<Plan> findTop5ByPublicoTrueOrderByFechaCreacionDesc();

    // Planes creados por un cliente específico
    List<Plan> findByClienteCreadorIdAndPublicoTrue(Long idCliente);

    // Planes creados por un colaborador específico
    List<Plan> findByColaboradorCreadorIdColaboradorAndPublicoTrue(Long idColaborador);

    // Incrementar vistas sin SELECT previo
    @Modifying
    @Query("UPDATE Plan p SET p.vistas = p.vistas + 1 WHERE p.id = :id")
    void incrementarVistas(@Param("id") Long id);
}

package maineta.eta.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import maineta.eta.entity.PlanActividad;

public interface PlanActividadRepository extends JpaRepository<PlanActividad, Long> {
    // Los métodos básicos de JPA son suficientes
    // Las consultas se harán a través de Plan.getActividades()
}

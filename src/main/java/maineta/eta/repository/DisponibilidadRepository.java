package maineta.eta.repository;

import java.time.LocalDate;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import maineta.eta.entity.Disponibilidad;

@Repository
public interface DisponibilidadRepository extends JpaRepository<Disponibilidad, Long> {

    List<Disponibilidad> findByActividadIdActividad(Long idActividad);

    @org.springframework.data.jpa.repository.Modifying
    @org.springframework.transaction.annotation.Transactional
    void deleteByActividad_IdActividad(Long idActividad);

    // Para el calendario: obtener disponibilidades de una actividad en un rango de fechas
    List<Disponibilidad> findByActividad_IdActividadAndFechaBetween(Long idActividad, LocalDate fechaInicio, LocalDate fechaFin);

    // Para detalle de un día: obtener disponibilidades de una actividad en una fecha específica
    List<Disponibilidad> findByActividad_IdActividadAndFechaOrderByHoraInicioAsc(Long idActividad, LocalDate fecha);

    // Verificar si ya existe una disponibilidad generada por un patrón para una fecha
    boolean existsByPatron_IdPatronAndFecha(Long idPatron, LocalDate fecha);

    // Queries para KPIs

    @org.springframework.data.jpa.repository.Query("""
        SELECT COUNT(d)
        FROM Disponibilidad d
        WHERE d.actividad.colaborador.idColaborador = :idColaborador
        AND d.estado = :estado
        AND d.fecha >= :inicio
        AND d.fecha <= :fin
    """)
    Long contarDisponibilidadesPorColaboradorYEstado(
        @org.springframework.data.repository.query.Param("idColaborador") Long idColaborador,
        @org.springframework.data.repository.query.Param("inicio") LocalDate inicio,
        @org.springframework.data.repository.query.Param("fin") LocalDate fin,
        @org.springframework.data.repository.query.Param("estado") String estado
    );
}

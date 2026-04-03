package maineta.eta.repository;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import maineta.eta.entity.Actividad;
import maineta.eta.entity.Categoria;

public interface ActividadRepository extends JpaRepository<Actividad, Long>, JpaSpecificationExecutor<Actividad> {

    // SELECT * FROM actividad WHERE LOWER(titulo) LIKE LOWER('%<valor>%');
    Page<Actividad> findByTituloContainingIgnoreCase(String titulo, Pageable pageable);

    // SELECT * FROM actividad WHERE id_colaborador = ?;
    Page<Actividad> findByColaborador_IdColaborador(Long idColaborador, Pageable pageable);

        List<Actividad> findByColaborador_IdColaboradorOrderByCreatedAtDesc(Long idColaborador);

        Optional<Actividad> findFirstByColaborador_IdColaboradorOrderByCreatedAtDesc(Long idColaborador);

    // combinacion de las dos anteriores
    Page<Actividad> findByColaborador_IdColaboradorAndTituloContainingIgnoreCase(Long idColaborador, String titulo,
            Pageable pageable);

    // SELECT COUNT(*) FROM actividad WHERE id_categoria = ?
    int countByCategoria_IdCategoria(Long idCategoria);

    // buscar activiidad por categoria
    Page<Actividad> findByCategoria_IdCategoria(Long idCategoria, Pageable pageable);

    // buscar actividad por categoria del nonmbre
    Page<Actividad> findByCategoria_NombreContainingIgnoreCase(String nombreCategoria, Pageable pageable);

    Actividad findByCategoria_NombreContainingIgnoreCase(String nombreCategoria);

    @Query("""
                SELECT a.categoria.idCategoria, COUNT(a)
                FROM Actividad a
                WHERE a.categoria.idCategoria IN :categoriaIds
                GROUP BY a.categoria.idCategoria
            """)
    List<Object[]> contarActividadesPorCategorias(
            @Param("categoriaIds") List<Long> categoriaIds);

    // Queries para personalización del home

    /**
     * Obtiene las actividades con más tendencia (vistas recientes)
     */
    List<Actividad> findTop10ByOrderByTotalTendenciaDesc();

    /**
     * Obtiene las actividades con más vistas totales
     */
    List<Actividad> findTop10ByOrderByTotalVistasDesc();

    /**
     * Obtiene las actividades más reservadas
     */
    @Query("SELECT a FROM Actividad a LEFT JOIN a.reservas r GROUP BY a ORDER BY COUNT(r) DESC")
    List<Actividad> findTop10MasReservadas(Pageable pageable);

    /**
     * Obtiene actividades personalizadas para el cliente basadas en sus categorías preferidas
     */
    @Query("SELECT a FROM Actividad a WHERE a.categoria IN :categorias ORDER BY a.calificacion DESC, a.createdAt DESC")
    List<Actividad> findByCategoriaInOrderByCalificacionDesc(
        @Param("categorias") Set<Categoria> categorias, 
        Pageable pageable
    );

    // Queries para KPIs

    @Query("SELECT COUNT(a) FROM Actividad a WHERE a.colaborador.idColaborador = :idColaborador")
    Long contarActividadesPorColaborador(@Param("idColaborador") Long idColaborador);

    @Query("""
        SELECT a.idActividad, a.titulo,
               (SELECT COUNT(r) FROM Reserva r WHERE r.actividad.idActividad = a.idActividad 
                AND r.fechaReserva >= :inicioReserva AND r.fechaReserva <= :finReserva) as reservas,
               (SELECT COALESCE(AVG(c.calificacion), 0) FROM Comentario c WHERE c.actividad.idActividad = a.idActividad) as calificacion,
               (SELECT COALESCE(AVG((d.cuposTotales - d.cuposDisponibles) * 100.0 / d.cuposTotales), 0) 
                FROM Disponibilidad d WHERE d.actividad.idActividad = a.idActividad 
                AND d.fecha >= :inicioDisp AND d.fecha <= :finDisp AND d.estado != 'CANCELADO') as ocupacion,
               (SELECT COUNT(f) FROM Favorito f WHERE f.actividad.idActividad = a.idActividad) as favoritos,
               a.totalVistas
        FROM Actividad a
        WHERE a.colaborador.idColaborador = :idColaborador
        ORDER BY reservas DESC
    """)
    List<Object[]> obtenerActividadesConKpis(
        @Param("idColaborador") Long idColaborador,
        @Param("inicioReserva") LocalDateTime inicioReserva,
        @Param("finReserva") LocalDateTime finReserva,
        @Param("inicioDisp") LocalDate inicioDisp,
        @Param("finDisp") LocalDate finDisp
    );
}

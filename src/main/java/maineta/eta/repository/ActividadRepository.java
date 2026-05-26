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

    // =========================================================================
    // QUERIES PARA HOME / SECCIONES DESTACADAS
    // =========================================================================

    /**
     * [LÍNEA 60] Actividades más populares por cantidad de favoritos.
     * Diferente a "más vistas": aquí el criterio es intención de guardar, no solo
     * curiosidad.
     * Ordenadas por número de clientes que las marcaron como favoritas (DESC).
     */
    @Query("""
                SELECT a FROM Actividad a
                LEFT JOIN a.favoritos f
                GROUP BY a.idActividad
                ORDER BY COUNT(f) DESC
            """)
    List<Actividad> findTop10MasGuardadasEnFavoritos(Pageable pageable);

    /**
     * [LÍNEA 65] Actividades con más vistas totales (campo desnormalizado
     * totalVistas).
     * Sin cambios — ya es distinto del resto.
     */
    List<Actividad> findTop10ByOrderByTotalVistasDesc();

    /**
     * Actividades mejor calificadas por promedio de estrellas (campo calificacion).
     */
    List<Actividad> findTop10ByOrderByCalificacionDesc();

    /**
     * Actividades con mejor rendimiento: más reservas CONFIRMADAS o COMPLETADAS.
     * A diferencia de findTop10MasReservadas (todas las reservas), esta filtra
     * solo reservas exitosas para medir rendimiento real del colaborador.
     */
    @Query("""
                SELECT a FROM Actividad a
                LEFT JOIN a.reservas r
                WHERE r.estado IN ('Confirmada', 'Hecho')
                GROUP BY a.idActividad
                ORDER BY COUNT(r) DESC
            """)
    List<Actividad> findTop10MejorRendimiento(Pageable pageable);

    /**
     * Actividades de la semana: las más reservadas en los últimos 7 días.
     * Ideal para mostrar una sección "Tendencia esta semana" que se actualiza sola.
     *
     * @param inicioSemana LocalDateTime de inicio del rango (hace 7 días)
     * @param finSemana    LocalDateTime de fin del rango (ahora)
     */
    @Query("""
                SELECT a FROM Actividad a
                LEFT JOIN a.reservas r
                WHERE r.fechaReserva >= :inicioSemana
                  AND r.fechaReserva <= :finSemana
                GROUP BY a.idActividad
                ORDER BY COUNT(r) DESC
            """)
    List<Actividad> findActividadesDeLaSemana(
            @Param("inicioSemana") LocalDateTime inicioSemana,
            @Param("finSemana") LocalDateTime finSemana,
            Pageable pageable);

    /**
     * Actividad aleatoria para el widget "¿No sabes qué hacer?".
     * Usa FUNCTION('RAND') compatible con MySQL en modo strict (GROUP BY
     * correctness).
     */
    @Query("SELECT a FROM Actividad a ORDER BY FUNCTION('RAND')")
    List<Actividad> findActividadAleatoria(Pageable pageable);

    /**
     * Todas las reservas (cualquier estado), paginado — para listados globales.
     */
    @Query("SELECT a FROM Actividad a LEFT JOIN a.reservas r GROUP BY a.idActividad ORDER BY COUNT(r) DESC")
    List<Actividad> findTop10MasReservadas(Pageable pageable);

    // =========================================================================
    // QUERIES DE PERSONALIZACIÓN
    // =========================================================================

    /**
     * Obtiene actividades similares por categoría e idioma excluyendo la actual
     */
    List<Actividad> findTop3ByCategoria_IdCategoriaAndIdioma_IdIdiomaAndIdActividadNotOrderByCalificacionDesc(
            Long idCategoria, Long idIdioma, Long idActividad);

    /**
     * Actividades para el cliente basadas en sus categorías preferidas
     */
    @Query("SELECT a FROM Actividad a WHERE a.categoria IN :categorias ORDER BY a.calificacion DESC, a.createdAt DESC")
    List<Actividad> findByCategoriaInOrderByCalificacionDesc(
            @Param("categorias") Set<Categoria> categorias,
            Pageable pageable);

    // =========================================================================
    // QUERIES PAGINADAS PARA VISTAS COMPLETAS
    // =========================================================================

    Page<Actividad> findAllByOrderByTotalTendenciaDesc(Pageable pageable);

    List<Actividad> findTop10ByOrderByTotalTendenciaDesc();

    Page<Actividad> findAllByOrderByTotalVistasDesc(Pageable pageable);

    @Query("SELECT a FROM Actividad a LEFT JOIN a.reservas r GROUP BY a.idActividad ORDER BY COUNT(r) DESC")
    Page<Actividad> findAllMasReservadas(Pageable pageable);

    @Query("SELECT a FROM Actividad a WHERE a.categoria IN :categorias ORDER BY a.calificacion DESC, a.createdAt DESC")
    Page<Actividad> findByCategoriaInOrderByCalificacionDescPaged(
            @Param("categorias") Set<Categoria> categorias,
            Pageable pageable);

    // =========================================================================
    // QUERIES PARA KPIs DEL COLABORADOR
    // =========================================================================

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
            @Param("finDisp") LocalDate finDisp);

    /**
     * Busca actividades dentro de un bounding box geográfico.
     */
    List<Actividad> findByLatitudBetweenAndLongitudBetween(
            Double latMin,
            Double latMax,
            Double lonMin,
            Double lonMax);
}
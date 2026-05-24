package maineta.eta.repository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import maineta.eta.entity.Cliente;
import maineta.eta.entity.Disponibilidad;
import maineta.eta.entity.EstadoPagoColaborador;
import maineta.eta.entity.EstadoReembolso;
import maineta.eta.entity.Reserva;

@Repository
public interface ReservaRepository extends JpaRepository<Reserva, Long> {

    List<Reserva> findByCliente(Cliente cliente);
    List<Reserva> findByActividad_IdActividad(Long idActividad);
    List<Reserva> findByActividad_Colaborador_IdColaboradorOrderByFechaReservaDesc(Long idColaborador);
    @Query("SELECT r FROM Reserva r JOIN FETCH r.actividad")
    List<Reserva> findAllConActividad();
    List<Reserva> findByCliente_IdAndActividad_IdActividad(Long idCliente, Long idActividad);
    Optional<Reserva> findByIdReservaAndCliente_Usuario_Email(Long idReserva, String email);
    Optional<Reserva> findByIdReservaAndActividad_Colaborador_Usuario_Email(Long idReserva, String email);
    Optional<Reserva> findByidReserva(Long idReserva);
    Optional<Reserva> findByRefPayco(String refPayco);
    Optional<Reserva> findByRefWompi(String refWompi);
    
    // Para cancelaciones por colaborador
    List<Reserva> findByDisponibilidadAndEstado(Disponibilidad disponibilidad, String estado);

    // Queries para KPIs

    @Query("""
        SELECT SUM(r.cantidad * r.precioColaborador)
        FROM Reserva r
        WHERE r.actividad.colaborador.idColaborador = :idColaborador
        AND r.estado = :estado
        AND r.fechaReserva >= :inicio
        AND r.fechaReserva <= :fin
    """)
    BigDecimal calcularIngresoBrutoPorColaborador(
        @Param("idColaborador") Long idColaborador,
        @Param("inicio") LocalDateTime inicio,
        @Param("fin") LocalDateTime fin,
        @Param("estado") String estado
    );

    @Query("""
        SELECT COUNT(r)
        FROM Reserva r
        WHERE r.actividad.colaborador.idColaborador = :idColaborador
        AND r.estado != 'Cancelada'
        AND r.fechaReserva >= :inicio
        AND r.fechaReserva <= :fin
    """)
    Long contarReservasPorColaboradorYPeriodo(
        @Param("idColaborador") Long idColaborador,
        @Param("inicio") LocalDateTime inicio,
        @Param("fin") LocalDateTime fin
    );

    @Query("""
        SELECT COUNT(r)
        FROM Reserva r
        WHERE r.actividad.colaborador.idColaborador = :idColaborador
        AND r.estado = :estado
        AND r.fechaReserva >= :inicio
        AND r.fechaReserva <= :fin
    """)
    Long contarReservasPorColaboradorYEstado(
        @Param("idColaborador") Long idColaborador,
        @Param("inicio") LocalDateTime inicio,
        @Param("fin") LocalDateTime fin,
        @Param("estado") String estado
    );

    @Query("""
        SELECT COUNT(r)
        FROM Reserva r
        WHERE r.actividad.colaborador.idColaborador = :idColaborador
        AND r.fechaReserva >= :inicio
        AND r.fechaReserva <= :fin
    """)
    Long contarTodasReservasPorColaborador(
        @Param("idColaborador") Long idColaborador,
        @Param("inicio") LocalDateTime inicio,
        @Param("fin") LocalDateTime fin
    );

    @Query("""
        SELECT COUNT(DISTINCT r.cliente.id)
        FROM Reserva r
        WHERE r.actividad.colaborador.idColaborador = :idColaborador
        AND r.fechaReserva >= :inicio
        AND r.fechaReserva <= :fin
    """)
    Long contarClientesUnicosPorColaborador(
        @Param("idColaborador") Long idColaborador,
        @Param("inicio") LocalDateTime inicio,
        @Param("fin") LocalDateTime fin
    );

    @Query("""
        SELECT COUNT(DISTINCT r.cliente.id)
        FROM Reserva r
        WHERE r.actividad.colaborador.idColaborador = :idColaborador
        AND r.estado = 'Hecho'
        AND r.cliente.id IN (
            SELECT r2.cliente.id
            FROM Reserva r2
            WHERE r2.actividad.colaborador.idColaborador = :idColaborador
            AND r2.estado = 'Hecho'
            GROUP BY r2.cliente.id
            HAVING COUNT(r2) > 1
        )
    """)
    Long contarClientesRecurrentesPorColaborador(
        @Param("idColaborador") Long idColaborador
    );

    /**
     * Busca reservas cuya disponibilidad sea en la fecha especificada
     * y que estén en estado PENDIENTE o CONFIRMADA.
     * Usado para enviar recordatorios 24 horas antes.
     */
    @Query("""
        SELECT r
        FROM Reserva r
        WHERE r.disponibilidad.fecha = :fecha
        AND (r.estado = 'PENDIENTE' OR r.estado = 'CONFIRMADA' OR r.estado = 'Confirmada' OR r.estado = 'Pendiente')
    """)
    List<Reserva> findReservasPorFechaDisponibilidad(@Param("fecha") java.time.LocalDate fecha);
    
    // Métodos de paginación para administración
    Page<Reserva> findByEstadoPagoColaborador(EstadoPagoColaborador estadoPagoColaborador, Pageable pageable);
    Page<Reserva> findByEstadoReembolso(EstadoReembolso estadoReembolso, Pageable pageable);
    Page<Reserva> findByEstado(String estado, Pageable pageable);
}

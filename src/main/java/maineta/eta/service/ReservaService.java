package maineta.eta.service;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import maineta.eta.entity.Actividad;
import maineta.eta.entity.Cliente;
import maineta.eta.entity.Disponibilidad;
import maineta.eta.entity.Reserva;

public interface ReservaService {
 
    Reserva hacerReserva(Cliente cliente, Actividad actividad, 
                        Disponibilidad disponibilidad, int cantidad) throws Exception;
    Optional<Reserva> ObtenerReservaPorId(Long idReserva);
    void EliminarReservacion(Reserva reserva);
    Long ContadorReservas();
    List<Reserva> getReservasCliente(Cliente cliente);
    List<Reserva> getReservasColaborador(Long idColaborador);
    List<Reserva> getReservasPorIdActividad(Long idActividad);
    Reserva guardarReserva(Reserva reserva);
    public boolean existeReservaRealizada(Long idCliente, Long idActividad);
    
    /**
     * Crea una reserva desde el proceso de pago de ePayco.
     * Incluye el refPayco para idempotencia.
     */
    Reserva crearReservaDesdeEpayco(Long idDisponibilidad, Long idCliente, 
                                    Long idActividad, int cantidad, String refPayco) throws Exception;
    
    /**
     * Crea una reserva desde el proceso de pago de Wompi.
     * Incluye la reference y wompiTransactionId para idempotencia y trazabilidad.
     */
    Reserva crearReservaDesdeWompi(Long idDisponibilidad, Long idCliente, 
                                   Long idActividad, int cantidad, String reference, 
                                   String wompiTransactionId) throws Exception;
    
    // Métodos para administración de pagos y reembolsos
    Page<Reserva> obtenerReservasConPagoPendiente(Pageable pageable);
    Page<Reserva> obtenerReservasConReembolsoPendiente(Pageable pageable);
    Page<Reserva> obtenerTodasReservas(Pageable pageable);
    Page<Reserva> obtenerPorEstado(String estado, Pageable pageable);
    Optional<Reserva> obtenerPorId(Long idReserva);
    BigDecimal calcularTotalComisionesGanadas();
}

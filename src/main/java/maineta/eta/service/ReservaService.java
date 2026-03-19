package maineta.eta.service;

import java.util.List;
import java.util.Optional;

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
    
}
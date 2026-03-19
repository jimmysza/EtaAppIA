package maineta.eta.service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import maineta.eta.entity.Actividad;
import maineta.eta.entity.Cliente;
import maineta.eta.entity.Disponibilidad;
import maineta.eta.entity.Reserva;
import maineta.eta.repository.ReservaRepository;

@Service
public class ReservaServiceImpl implements ReservaService {

    private final ReservaRepository reservaRepository;
    private final ActividadService actividadService;

    public ReservaServiceImpl(ReservaRepository reservaRepository, ActividadService actividadService) {
        this.reservaRepository = reservaRepository;
        this.actividadService = actividadService;
    }

    @Override
    public Long ContadorReservas() {
        return reservaRepository.count();
    }

    @Override
    @Transactional
    public Reserva hacerReserva(Cliente cliente, Actividad actividad,
            Disponibilidad disponibilidad, int cantidad) throws Exception {

        // Verificar cupos
        if (disponibilidad.getCuposDisponibles() < cantidad) {
            throw new Exception("No hay suficientes cupos disponibles. Solo quedan: " + disponibilidad.getCuposDisponibles());

        }

        // Verificar disponibilidad
        if (!disponibilidad.getActividad().getIdActividad().equals(actividad.getIdActividad())) {
            throw new Exception("La disponibilidad no pertenece a esta actividad");
        }

        // Crear reserva
        Reserva reserva = new Reserva();
        reserva.setCliente(cliente);
        reserva.setActividad(actividad);
        reserva.setDisponibilidad(disponibilidad);
        reserva.setCantidad(cantidad);
        reserva.setFechaReserva(LocalDateTime.now());

        // Reducir cupos
        disponibilidad.setCuposDisponibles(disponibilidad.getCuposDisponibles() - cantidad);
        actividadService.agregarActividad(actividad);

        // Guardar reserva
        return reservaRepository.save(reserva);
    }
    
    @Override
    public Reserva guardarReserva(Reserva reserva){
        return reservaRepository.save(reserva);
    }

    @Override
    public Optional<Reserva> ObtenerReservaPorId(Long idReserva){
        return reservaRepository.findByidReserva(idReserva);
    }

    @Override
    public void EliminarReservacion(Reserva reserva) {
        reservaRepository.delete(reserva);
    }

    @Override
    public boolean existeReservaRealizada(Long idCliente, Long idActividad) {
        List<Reserva> reservas = reservaRepository
                .findByCliente_IdAndActividad_IdActividad(idCliente, idActividad);

        // Verificar si ALGUNA reserva tiene estado "Hecho"
        return reservas.stream()
                .anyMatch(r -> "Hecho".equalsIgnoreCase(r.getEstado()));
    }


    @Override
    public List<Reserva> getReservasCliente(Cliente cliente) {
        // ✅ Usar el repositorio en lugar de la lista en memoria
        return reservaRepository.findByCliente(cliente);
    }

    @Override
    public List<Reserva> getReservasColaborador(Long idColaborador) {
        return reservaRepository.findByActividad_Colaborador_IdColaboradorOrderByFechaReservaDesc(idColaborador);
    }

    @Override
    public List<Reserva> getReservasPorIdActividad(Long idActividad){
        return reservaRepository.findByActividad_IdActividad(idActividad);
    }
}

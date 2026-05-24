package maineta.eta.service;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import maineta.eta.dto.CalendarioDiaDTO;
import maineta.eta.dto.DisponibilidadDetalleDTO;
import maineta.eta.entity.Disponibilidad;

public interface DisponibilidadService {

    Optional<Disponibilidad> obtenerPorId(Long id);
    Optional<Disponibilidad> obtenerDisponibilidadPorId(Long id);
    Disponibilidad guardarDisponibilidad(Disponibilidad disponibilidad);
    List<Disponibilidad> obtenerPorActividad(Long idActividad);
    Long ContadorDisponibilidades();

    /**
     * Retorna los datos del calendario mensual para una actividad.
     * Cada CalendarioDiaDTO tiene la fecha, total de horarios y si hay cupos.
     */
    List<CalendarioDiaDTO> obtenerCalendarioMensual(Long idActividad, int anio, int mes);

    /**
     * Retorna las disponibilidades de un día específico como DTOs.
     */
    List<DisponibilidadDetalleDTO> obtenerDetallePorFecha(Long idActividad, LocalDate fecha);

    /**
     * Editar cupos totales de una disponibilidad (recalcula cuposDisponibles).
     */
    Disponibilidad editarCupos(Long idDisponibilidad, int nuevosCuposTotales);

    /**
     * Cambiar el estado de una disponibilidad (DISPONIBLE, CANCELADO, COMPLETADO).
     */
    Disponibilidad cambiarEstado(Long idDisponibilidad, String nuevoEstado);

    /**
     * Eliminar una disponibilidad por id.
     */
    void eliminarDisponibilidad(Long idDisponibilidad);
}

package maineta.eta.service;

import java.util.List;

import maineta.eta.dto.PatronDisponibilidadDTO;
import maineta.eta.entity.PatronDisponibilidad;

public interface PatronDisponibilidadService {

    PatronDisponibilidad guardar(PatronDisponibilidad patron);

    List<PatronDisponibilidad> obtenerPorActividad(Long idActividad);

    List<PatronDisponibilidad> obtenerActivosPorActividad(Long idActividad);

    /**
     * Crea un patrón y genera automáticamente las disponibilidades
     * para cada día que coincida dentro del rango [fechaInicio, fechaFin].
     */
    PatronDisponibilidad crearPatronYGenerar(PatronDisponibilidadDTO dto);

    /**
     * Genera disponibilidades faltantes desde un patrón existente.
     */
    int generarDesdePatron(Long idPatron);

    void eliminar(Long idPatron);
}

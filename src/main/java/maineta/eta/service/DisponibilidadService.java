package maineta.eta.service;

import java.util.List;
import java.util.Optional;

import maineta.eta.entity.Disponibilidad;

public interface DisponibilidadService {

    Optional<Disponibilidad> obtenerPorId(Long id);
    Disponibilidad guardarDisponibilidad(Disponibilidad disponibilidad);
    List<Disponibilidad> obtenerPorActividad(Long idActividad);
    Long ContadorDisponibilidades();
}

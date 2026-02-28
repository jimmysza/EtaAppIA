package maineta.eta.service;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import maineta.eta.dto.PatronDisponibilidadDTO;
import maineta.eta.entity.Actividad;
import maineta.eta.entity.Disponibilidad;
import maineta.eta.entity.PatronDisponibilidad;
import maineta.eta.repository.DisponibilidadRepository;
import maineta.eta.repository.PatronDisponibilidadRepository;

@Service
public class PatronDisponibilidadServiceImpl implements PatronDisponibilidadService {

    @Autowired
    private PatronDisponibilidadRepository patronRepo;

    @Autowired
    private DisponibilidadRepository disponibilidadRepo;

    @Autowired
    private ActividadService actividadService;

    @Override
    public PatronDisponibilidad guardar(PatronDisponibilidad patron) {
        return patronRepo.save(patron);
    }

    @Override
    public List<PatronDisponibilidad> obtenerPorActividad(Long idActividad) {
        return patronRepo.findByActividad_IdActividad(idActividad);
    }

    @Override
    public List<PatronDisponibilidad> obtenerActivosPorActividad(Long idActividad) {
        return patronRepo.findByActividad_IdActividadAndEstado(idActividad, "ACTIVO");
    }

    @Override
    @Transactional
    public PatronDisponibilidad crearPatronYGenerar(PatronDisponibilidadDTO dto) {
        Actividad actividad = actividadService.obtenerPorId(dto.getIdActividad());

        PatronDisponibilidad patron = new PatronDisponibilidad();
        patron.setActividad(actividad);
        patron.setHoraInicio(dto.getHoraInicio());
        patron.setHoraFin(dto.getHoraFin());
        patron.setDiasSemana(dto.getDiasSemana());
        patron.setCuposTotales(dto.getCuposTotales());
        patron.setFechaInicio(dto.getFechaInicio());
        patron.setFechaFin(dto.getFechaFin());
        patron.setEstado("ACTIVO");

        patron = patronRepo.save(patron);
        generarDisponibilidadesDesdePatron(patron);

        return patron;
    }

    @Override
    @Transactional
    public int generarDesdePatron(Long idPatron) {
        PatronDisponibilidad patron = patronRepo.findById(idPatron)
                .orElseThrow(() -> new RuntimeException("Patrón no encontrado"));
        return generarDisponibilidadesDesdePatron(patron);
    }

    @Override
    @Transactional
    public void eliminar(Long idPatron) {
        patronRepo.deleteById(idPatron);
    }

    /**
     * Genera disponibilidades individuales para cada día que coincida
     * con los días de semana del patrón dentro del rango de fechas.
     * Omite fechas que ya tengan una disponibilidad generada por este patrón.
     */
    private int generarDisponibilidadesDesdePatron(PatronDisponibilidad patron) {
        Set<DayOfWeek> dias = Arrays.stream(patron.getDiasSemana().split(","))
                .map(String::trim)
                .map(DayOfWeek::valueOf)
                .collect(Collectors.toSet());

        int count = 0;
        LocalDate cursor = patron.getFechaInicio();

        while (!cursor.isAfter(patron.getFechaFin())) {
            if (dias.contains(cursor.getDayOfWeek())) {
                // Solo crear si no existe ya para este patrón y fecha
                if (!disponibilidadRepo.existsByPatron_IdPatronAndFecha(patron.getIdPatron(), cursor)) {
                    Disponibilidad disp = new Disponibilidad();
                    disp.setFecha(cursor);
                    disp.setHoraInicio(patron.getHoraInicio());
                    disp.setHoraFin(patron.getHoraFin());
                    disp.setCuposTotales(patron.getCuposTotales());
                    disp.setCuposDisponibles(patron.getCuposTotales());
                    disp.setEstado("DISPONIBLE");
                    disp.setActividad(patron.getActividad());
                    disp.setPatron(patron);
                    disponibilidadRepo.save(disp);
                    count++;
                }
            }
            cursor = cursor.plusDays(1);
        }
        return count;
    }
}

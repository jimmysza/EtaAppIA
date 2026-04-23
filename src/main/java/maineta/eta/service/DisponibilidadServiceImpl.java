package maineta.eta.service;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import maineta.eta.dto.CalendarioDiaDTO;
import maineta.eta.dto.DisponibilidadDetalleDTO;
import maineta.eta.entity.Disponibilidad;
import maineta.eta.repository.DisponibilidadRepository;

@Service
public class DisponibilidadServiceImpl implements DisponibilidadService {
    
    @Autowired
    private DisponibilidadRepository disponibilidadRepository;
    
    @Override
    public Disponibilidad guardarDisponibilidad(Disponibilidad disponibilidad){
        return disponibilidadRepository.save(disponibilidad);
    }

    @Override
    public Optional<Disponibilidad> obtenerPorId(Long id) {
        return disponibilidadRepository.findById(id);
    }

    @Override
    public Optional<Disponibilidad> obtenerDisponibilidadPorId(Long id) {
        return disponibilidadRepository.findById(id);
    }

    @Override
    public Long ContadorDisponibilidades() {
        return disponibilidadRepository.count();
    }

    @Override
    public List<Disponibilidad> obtenerPorActividad(Long idActividad) {
        return disponibilidadRepository.findByActividadIdActividad(idActividad);
    }

    @Override
    public List<CalendarioDiaDTO> obtenerCalendarioMensual(Long idActividad, int anio, int mes) {
        YearMonth ym = YearMonth.of(anio, mes);
        LocalDate inicio = ym.atDay(1);
        LocalDate fin = ym.atEndOfMonth();

        List<Disponibilidad> disps = disponibilidadRepository
                .findByActividad_IdActividadAndFechaBetween(idActividad, inicio, fin);

        // Agrupar por fecha
        Map<LocalDate, List<Disponibilidad>> porFecha = disps.stream()
                .collect(Collectors.groupingBy(Disponibilidad::getFecha, LinkedHashMap::new, Collectors.toList()));

        List<CalendarioDiaDTO> resultado = new ArrayList<>();
        for (Map.Entry<LocalDate, List<Disponibilidad>> entry : porFecha.entrySet()) {
            boolean tieneCupos = entry.getValue().stream()
                    .anyMatch(d -> d.getCuposDisponibles() > 0 && "DISPONIBLE".equals(d.getEstado()));
            resultado.add(new CalendarioDiaDTO(entry.getKey(), entry.getValue().size(), tieneCupos));
        }
        return resultado;
    }

    @Override
    public List<DisponibilidadDetalleDTO> obtenerDetallePorFecha(Long idActividad, LocalDate fecha) {
        List<Disponibilidad> disps = disponibilidadRepository
                .findByActividad_IdActividadAndFechaOrderByHoraInicioAsc(idActividad, fecha);

        return disps.stream().map(d -> new DisponibilidadDetalleDTO(
                d.getIdDisponibilidad(),
                d.getHoraInicio(),
                d.getHoraFin(),
                d.getCuposDisponibles(),
                d.getCuposTotales(),
                d.getEstado(),
                null // La predicción se calcula en el controlador
        )).collect(Collectors.toList());
    }

    @Override
    @Transactional
    public Disponibilidad editarCupos(Long idDisponibilidad, int nuevosCuposTotales) {
        Disponibilidad disp = disponibilidadRepository.findById(idDisponibilidad)
                .orElseThrow(() -> new RuntimeException("Disponibilidad no encontrada"));

        int reservados = disp.getCuposTotales() - disp.getCuposDisponibles();
        if (nuevosCuposTotales < reservados) {
            throw new RuntimeException("No se puede reducir por debajo de los cupos ya reservados (" + reservados + ")");
        }

        disp.setCuposTotales(nuevosCuposTotales);
        disp.setCuposDisponibles(nuevosCuposTotales - reservados);
        return disponibilidadRepository.save(disp);
    }

    @Override
    @Transactional
    public Disponibilidad cambiarEstado(Long idDisponibilidad, String nuevoEstado) {
        Disponibilidad disp = disponibilidadRepository.findById(idDisponibilidad)
                .orElseThrow(() -> new RuntimeException("Disponibilidad no encontrada"));
        disp.setEstado(nuevoEstado);
        return disponibilidadRepository.save(disp);
    }
}
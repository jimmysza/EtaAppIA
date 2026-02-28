package maineta.eta.dto;

import java.time.LocalDate;
import java.time.LocalTime;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class PatronDisponibilidadDTO {
    private Long idActividad;
    private LocalTime horaInicio;
    private LocalTime horaFin;
    private String diasSemana; // "MONDAY,WEDNESDAY,FRIDAY"
    private int cuposTotales;
    private LocalDate fechaInicio;
    private LocalDate fechaFin;
}

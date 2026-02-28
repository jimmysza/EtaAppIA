package maineta.eta.dto;

import java.time.LocalDate;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class CalendarioDiaDTO {
    private LocalDate fecha;
    private int totalHorarios;
    private boolean tieneCupos; // true if at least one disponibilidad has cuposDisponibles > 0
}

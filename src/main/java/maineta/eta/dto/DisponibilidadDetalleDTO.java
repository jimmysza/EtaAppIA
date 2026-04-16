package maineta.eta.dto;

import java.time.LocalTime;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class DisponibilidadDetalleDTO {
    private Long idDisponibilidad;
    private LocalTime horaInicio;
    private LocalTime horaFin;
    private int cuposDisponibles;
    private int cuposTotales;
    private String estado;
    
    // Predicción de ocupación
    private PrediccionOcupacionDTO prediccion;
}

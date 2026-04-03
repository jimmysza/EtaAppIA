package maineta.eta.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class KpiActividadDTO {
    private Long idActividad;
    private String titulo;
    private int reservas;
    private double calificacion;
    private double tasaOcupacion;         // porcentaje
    private int vecesEnFavoritos;
    private int totalVistas;
    private double conversionVisitaReserva; // porcentaje
}

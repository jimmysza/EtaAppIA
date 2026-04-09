package maineta.eta.dto;

import java.math.BigDecimal;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class PlanActividadDTO {
    private Long idActividad;
    private String nombreActividad;
    private String slugActividad; // para construir /actividad/{slug}-{id}
    private String imagenActividad;
    private BigDecimal precioConsumidor; // calculado con UsuarioHelper
    private String categoriaActividad;
    private Double latitud; // para el mapa
    private Double longitud; // para el mapa
    private int orden;
    private String horaSugerida;
    private String notaPersonalizada;
}

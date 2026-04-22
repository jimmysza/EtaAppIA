package maineta.eta.dto;

import java.math.BigDecimal;

import lombok.Data;

@Data
public class ActividadCercanaDTO {
    private Long idActividad;
    private String titulo;
    private String slug;              // para construir la URL /actividad/{slug}-{id}
    private String imagen;            // imagen principal
    private BigDecimal precioConsumidor; // precio ya con comisión aplicada (via UsuarioHelper)
    private Integer calificacion;
    private String categoriaNombre;
    private String idiomaNombre;
    private Double latitud;
    private Double longitud;
    private Double distanciaKm;       // calculada en el servicio, no en BD
    private Integer totalComentarios;
}

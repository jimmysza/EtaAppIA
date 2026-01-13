package maineta.eta.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import lombok.Data;

@Data
public class ActividadDTO {

    private Long idActividad;
    private String titulo;
    private String descripcion;
    private int calificacion;
    private String ubicacion;
    private String imagen;

    private BigDecimal precio;            // Precio original
    private BigDecimal precioConsumidor;  // Precio + 18%

    private Long idCategoria;             // ID para usar en URLs
    private String nombreCategoria; 
    private Long idColaborador;  
    private Long idIdioma;
    private String nombreIdioma;
    private String codigoIdioma;
    private LocalDateTime createdAt;
         // ID del dueño
}

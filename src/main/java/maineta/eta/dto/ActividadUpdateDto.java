package maineta.eta.dto;

import java.math.BigDecimal;

import lombok.Data;

@Data
public class ActividadUpdateDto {

    private Long idActividad;
    private String titulo;
    private String descripcion;
    private String imagen;
    private String ubicacion;
    private Double latitud;
    private Double longitud;

    private int calificacion;
    private BigDecimal precio; // Precio original
    private BigDecimal precioConsumidor; // Precio + 18%

    private String condiciones;
    private String normas;
    private String incluye;

    private Long idCategoria;
    private Long idIdioma;
    private String nombreIdioma;

}

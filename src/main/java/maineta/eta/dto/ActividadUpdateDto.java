package maineta.eta.dto;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

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
    private List<String> preguntasFrecuentesPreguntas = new ArrayList<>();
    private List<String> preguntasFrecuentesRespuestas = new ArrayList<>();
    private Boolean actualizarPreguntasFrecuentes;

    private Long idCategoria;
    private Long idIdioma;
    private String nombreIdioma;

}

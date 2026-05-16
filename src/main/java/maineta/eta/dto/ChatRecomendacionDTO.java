package maineta.eta.dto;

import java.math.BigDecimal;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO que contiene la recomendación del chatbot con filtros estructurados
 * para redirigir al usuario a la página de resultados.
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class ChatRecomendacionDTO {
    
    private String respuesta; // Texto de la respuesta del bot
    private boolean tieneRecomendacion; // Si incluye una sugerencia de búsqueda
    private FiltrosRecomendadosDTO filtros; // Filtros para aplicar en /actividades/buscar
    
    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class FiltrosRecomendadosDTO {
        private String nombre; // Palabra clave para búsqueda
        private Long categoriaId; // ID de categoría recomendada
        private String categoriaNombre; // Nombre de la categoría (para mostrar)
        private Long idiomaId; // ID de idioma
        private String idiomaNombre; // Nombre del idioma
        private BigDecimal precioMin; // Precio mínimo
        private BigDecimal precioMax; // Precio máximo
        private String textoBoton; // Texto del botón de acción (ej: "Ver actividades acuáticas")
    }
    
    public static ChatRecomendacionDTO sinRecomendacion(String respuesta) {
        return new ChatRecomendacionDTO(respuesta, false, null);
    }
    
    public static ChatRecomendacionDTO conRecomendacion(String respuesta, FiltrosRecomendadosDTO filtros) {
        return new ChatRecomendacionDTO(respuesta, true, filtros);
    }
}

package maineta.eta.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO para la predicción de nivel de ocupación de una actividad.
 * 
 * Contiene los datos necesarios para realizar la predicción mediante
 * el modelo de Machine Learning integrado.
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class PrediccionOcupacionDTO {
    
    /**
     * Nivel de ocupación predicho: baja, media, alta, agotado
     */
    private String nivelOcupacion;
    
    /**
     * Confianza de la predicción (0.0 a 1.0)
     */
    private Double confianza;
    
    /**
     * Categoría de la actividad
     */
    private String categoria;
    
    /**
     * Rango de precio: bajo, medio, alto
     */
    private String rangoPrecio;
    
    /**
     * Rango de hora: mañana, tarde, noche
     */
    private String rangoHora;
    
    /**
     * Tipo de día: entre_semana, fin_semana
     */
    private String tipoDia;
    
    /**
     * Rango de cupos: bajo, medio, alto
     */
    private String rangoCupos;
}

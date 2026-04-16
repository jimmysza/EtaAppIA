package maineta.eta.service;

import maineta.eta.dto.PrediccionOcupacionDTO;

/**
 * Servicio para realizar predicciones de nivel de ocupación de actividades
 * utilizando el modelo de Machine Learning integrado.
 * 
 * El servicio carga datos de Actividad y Disponibilidad, realiza las
 * transformaciones necesarias y ejecuta la predicción con Weka.
 */
public interface PrediccionService {
    
    /**
     * Predice el nivel de ocupación para una disponibilidad específica.
     * 
     * @param idDisponibilidad ID de la disponibilidad a predecir
     * @return DTO con el nivel predicho (baja, media, alta, agotado) y confianza
     */
    PrediccionOcupacionDTO predecirOcupacion(Long idDisponibilidad);
    
    /**
     * Predice el nivel de ocupación basado en parámetros manuales.
     * 
     * @param categoria nombre de la categoría
     * @param rangoPrecio rango de precio (bajo, medio, alto)
     * @param rangoHora rango de hora (mañana, tarde, noche)
     * @param tipoDia tipo de día (entre_semana, fin_semana)
     * @param rangoCupos rango de cupos (bajo, medio, alto)
     * @return DTO con el nivel predicho y confianza
     */
    PrediccionOcupacionDTO predecirOcupacionManual(
        String categoria, 
        String rangoPrecio, 
        String rangoHora, 
        String tipoDia, 
        String rangoCupos
    );
}

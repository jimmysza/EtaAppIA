package maineta.eta.dto;

import java.util.List;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import maineta.eta.entity.DisponibilidadSemana;
import maineta.eta.entity.GrupoViaje;
import maineta.eta.entity.RangoPrecio;

/**
 * DTO para capturar las respuestas del onboarding del cliente
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class OnboardingForm {
    
    @NotNull(message = "Debes seleccionar al menos una categoría favorita")
    private List<Long> categoriasIds;

    @NotNull(message = "Debes seleccionar con quién sueles viajar")
    private GrupoViaje grupoViaje;

    @NotNull(message = "Debes seleccionar tu presupuesto preferido")
    private RangoPrecio rangoPrecio;

    @NotNull(message = "Debes seleccionar tu disponibilidad habitual")
    private DisponibilidadSemana disponibilidadSemana;
}

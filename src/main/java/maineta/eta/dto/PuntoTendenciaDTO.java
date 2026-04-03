package maineta.eta.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class PuntoTendenciaDTO {
    private String etiqueta;              // "Sem 1", "Sem 2"...
    private int reservas;
}

package maineta.eta.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class OcupacionDTO {
    private int disponible;               // cantidad de slots
    private int completado;
    private int cancelado;
}

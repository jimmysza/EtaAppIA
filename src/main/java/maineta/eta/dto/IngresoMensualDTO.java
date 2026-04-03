package maineta.eta.dto;

import java.math.BigDecimal;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class IngresoMensualDTO {
    private String mes;                   // "Ene", "Feb", etc.
    private BigDecimal ingresoBruto;
    private BigDecimal ingresoNeto;
}

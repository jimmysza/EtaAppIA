package maineta.eta.dto;

import java.math.BigDecimal;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class KpiResumenDTO {
    private BigDecimal ingresoBruto;
    private BigDecimal ingresoNeto;
    private int totalReservas;
    private double tasaCancelacion;       // porcentaje
    private double calificacionPromedio;
    private int actividadesPublicadas;
    private int clientesUnicos;
    private int clientesRecurrentes;
    
    // Variaciones (para badges)
    private Double variacionReservas;     // porcentaje vs período anterior
    private Double variacionIngresos;     // porcentaje vs período anterior
}

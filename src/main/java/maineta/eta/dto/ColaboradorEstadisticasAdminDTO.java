package maineta.eta.dto;

import java.math.BigDecimal;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ColaboradorEstadisticasAdminDTO {
    private Long idColaborador;
    private String nombre;
    private String email;
    private String nit;
    private int antiguedadMeses;
    
    private int totalActividades;
    private int popularidadGlobal;
    private double calificacionPromedio;
    
    private int totalReservas;
    private double tasaCumplimiento;
    private double tasaCancelacion;
    private int penalizacionesNoShow;
    private double tasaOcupacionPromedio;
    
    private BigDecimal ingresosGenerados;
    private BigDecimal comisionesEta;
}

package maineta.eta.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class EstadosReservaDTO {
    private int pendiente;
    private int confirmada;
    private int hecho;
    private int cancelada;
    private int noShowCliente;
    private int noShowColaborador;
    private double tasaConversionPendienteConfirmada;
}

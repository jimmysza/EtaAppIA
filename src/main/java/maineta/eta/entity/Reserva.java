package maineta.eta.entity;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Lob;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Data
@AllArgsConstructor
@NoArgsConstructor
@Table(name = "reserva")
public class Reserva {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long idReserva;

    @ManyToOne
    @JoinColumn(name = "id_disponibilidad")
    private Disponibilidad disponibilidad;

    @ManyToOne
    @JoinColumn(name = "id_cliente")    
    private Cliente cliente;

    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_actividad")
    private Actividad actividad;


    @Column(nullable = false)
    private String estado = "Pendiente";

    private int cantidad;

    private LocalDateTime fechaReserva;

    @Column(length = 100, unique = true)
    private String refPayco; // Referencia del pago en ePayco

    @Column(length = 100, unique = true)
    private String refWompi; // Referencia del pago en Wompi

    @Column(length = 100)
    private String wompiTransactionId; // ID de transacción en Wompi

    // Campos de precios y comisiones (RN-01, RN-02)
    @Column(precision = 10, scale = 2)
    private BigDecimal precioColaborador; // Precio base de la actividad

    @Column(precision = 10, scale = 2)
    private BigDecimal precioConsumidor; // Precio con comisión que pagó el cliente

    @Column(precision = 5, scale = 2)
    private BigDecimal comisionPorcentaje; // % de comisión aplicado (ej: 18.00)

    @Column(precision = 10, scale = 2)
    private BigDecimal comisionEta; // Monto que gana ETA (precioConsumidor - precioColaborador)

    // Campos de pago al colaborador
    @Enumerated(EnumType.STRING)
    @Column(length = 30)
    private EstadoPagoColaborador estadoPagoColaborador = EstadoPagoColaborador.PENDIENTE_PAGO;

    private LocalDateTime fechaPagoColaborador;

    // Campos de cancelación y reembolsos
    @Enumerated(EnumType.STRING)
    @Column(length = 50)
    private PoliticaCancelacion politicaAplicada; // Política vigente al momento de la reserva

    @Column(precision = 10, scale = 2)
    private BigDecimal montoReembolso; // Monto a reembolsar al cliente en caso de cancelación

    @Enumerated(EnumType.STRING)
    @Column(length = 30)
    private EstadoReembolso estadoReembolso = EstadoReembolso.SIN_REEMBOLSO;

    private LocalDateTime fechaReembolso;

    @Column(length = 20)
    private String canceladoPor; // CLIENTE, COLABORADOR, ADMIN

    @Lob
    private String motivoCancelacion;

    public Cliente getCliente() {
        return cliente;
    }

    public Actividad getActividad() {
        return actividad;
    }

    public int getCantidad() {
        return cantidad;
    }

    public LocalDateTime getFechaReserva() {
        return fechaReserva;
    }

    public BigDecimal getPrecioTotalSafe() {
        BigDecimal unitPrice = getPrecioConsumidorSafe();
        return unitPrice.multiply(new BigDecimal(Math.max(1, cantidad)));
    }

    public BigDecimal getPrecioConsumidorSafe() {
        if (precioConsumidor != null) {
            return precioConsumidor;
        }
        if (actividad != null && actividad.getPrecio() != null) {
            return actividad.getPrecio().multiply(new BigDecimal("1.18"));
        }
        return BigDecimal.ZERO;
    }

    public BigDecimal getPrecioColaboradorSafe() {
        return precioColaborador != null ? precioColaborador : BigDecimal.ZERO;
    }

    public BigDecimal getComisionEtaSafe() {
        return comisionEta != null ? comisionEta : BigDecimal.ZERO;
    }

}

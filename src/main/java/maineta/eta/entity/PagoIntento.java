package maineta.eta.entity;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Entidad que almacena los intentos de pago realizados a través de Wompi.
 * Permite mantener el contexto de la transacción (idDisponibilidad, idCliente, idActividad)
 * incluso cuando el webhook de Wompi llega antes que el redirect del navegador.
 */
@Entity
@Data
@AllArgsConstructor
@NoArgsConstructor
@Table(name = "pago_intento")
public class PagoIntento {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * Referencia única del pago en Wompi.
     * Formato: "ETA-{idDisponibilidad}-{idCliente}-{timestamp}"
     * Ejemplo: "ETA-123-456-1714567890123"
     */
    @Column(length = 100, unique = true, nullable = false)
    private String reference;

    /**
     * ID de la disponibilidad que se está reservando
     */
    @Column(nullable = false)
    private Long idDisponibilidad;

    /**
     * ID del cliente que realiza la reserva
     */
    @Column(nullable = false)
    private Long idCliente;

    /**
     * ID de la actividad que se está reservando
     */
    @Column(nullable = false)
    private Long idActividad;

    /**
     * Cantidad de cupos reservados
     */
    @Column(nullable = false)
    private Integer cantidad;

    /**
     * Monto total de la transacción en centavos
     * Ejemplo: $118.000 → 11800000 centavos
     */
    @Column(nullable = false)
    private Long amountInCents;

    /**
     * Estado del intento de pago
     * - PENDIENTE: El intento fue creado pero el pago aún no se ha procesado
     * - PROCESADO: El webhook de Wompi fue recibido y la reserva fue creada
     * - FALLIDO: El pago fue rechazado o falló
     */
    @Column(length = 20, nullable = false)
    private String estado = "PENDIENTE";

    /**
     * Fecha y hora en que se creó el intento de pago
     */
    @Column(nullable = false)
    private LocalDateTime createdAt;

    /**
     * ID de la transacción en Wompi (se actualiza cuando llega el webhook)
     */
    @Column(length = 100)
    private String wompiTransactionId;
}

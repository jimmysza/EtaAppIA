package maineta.eta.entity;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
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

}

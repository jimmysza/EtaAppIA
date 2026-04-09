package maineta.eta.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
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
import lombok.ToString;

@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "pregunta_frecuente_actividad")
public class PreguntaFrecuenteActividad {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long idPreguntaFrecuente;

    @Column(nullable = false, length = 255)
    private String pregunta;

    @Lob
    private String respuesta;

    @Column(nullable = false)
    private int ordenVisual;

    @ManyToOne
    @JoinColumn(name = "id_actividad", nullable = false)
    @ToString.Exclude
    private Actividad actividad;
}

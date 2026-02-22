package maineta.eta.entity;

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
import lombok.ToString;

@Entity
@Data
@AllArgsConstructor
@NoArgsConstructor
@Table(name = "imagen_actividad")
public class ImagenActividad {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long idImagen;

    private String nombre;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_actividad", nullable = false)
    @ToString.Exclude
    private Actividad actividad;

    public ImagenActividad(String nombre, Actividad actividad) {
        this.nombre = nombre;
        this.actividad = actividad;
    }
}

package maineta.eta.entity;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Lob;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;

@Entity
@Data
@AllArgsConstructor
@NoArgsConstructor
@Table(name = "planes")
public class Plan {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 200)
    private String titulo;

    @Lob
    private String descripcion;

    @Column(name = "imagen_portada", length = 255)
    private String imagenPortada;

    @Column(name = "duracion_estimada", length = 50)
    private String duracionEstimada; // Ej: "8 horas"

    @Column(length = 50)
    private String tipo; // Ej: "Cultural", "Aventura", "Gastronómico"

    // Creador polimórfico — solo uno de los dos será no-null
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_cliente_creador")
    @ToString.Exclude
    private Cliente clienteCreador;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_colaborador_creador")
    @ToString.Exclude
    private Colaborador colaboradorCreador;

    @OneToMany(mappedBy = "plan", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("orden ASC")
    @ToString.Exclude
    private List<PlanActividad> actividades = new ArrayList<>();

    @Column(name = "fecha_creacion", updatable = false)
    private LocalDateTime fechaCreacion = LocalDateTime.now();

    @Column(nullable = false)
    private boolean publico = true; // Si es visible en la vista pública

    @Column(nullable = false)
    private int vistas = 0;
}

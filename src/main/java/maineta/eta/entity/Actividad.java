package maineta.eta.entity;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
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
@Table(name = "actividad")
public class Actividad {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long idActividad;

    @Column(nullable = false, length = 200)
    private String titulo;

    @Lob
    private String descripcion;

    private int calificacion;

    private String ubicacion;

    @Column(name = "latitud")
    private Double latitud;

    @Column(name = "longitud")
    private Double longitud;

    @Lob
    private String normas;

    /* borrables */
    @Lob
    private String incluye;

    @ManyToOne
    @JoinColumn(name = "id_idioma", nullable = false)
    @ToString.Exclude
    private Idioma idioma;

    @Lob
    private String condiciones;

    /* private LocalDateTime fechaActividad; */
    private String imagen;

    @Column(updatable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(nullable = false    )
    private BigDecimal precio;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    private PoliticaCancelacion politicaCancelacion = PoliticaCancelacion.REEMBOLSO_TOTAL_SI_A_TIEMPO;

    private LocalDateTime updatedAt = LocalDateTime.now();




    @OneToMany(mappedBy = "actividad", cascade = CascadeType.ALL, orphanRemoval = true)
    @ToString.Exclude
    private List<Comentario> comentarios = new ArrayList<>();

    @ManyToOne
    @JoinColumn(name = "id_colaborador", nullable = false)
    @ToString.Exclude
    private Colaborador colaborador;

    @ManyToOne
    @ToString.Exclude
    @JoinColumn(name = "id_categoria", nullable = false)
    private Categoria categoria;



    @OneToMany(mappedBy = "actividad", cascade = CascadeType.ALL, orphanRemoval = true)
    @ToString.Exclude
    private List<Reserva> reservas;

    @OneToMany(mappedBy = "actividad", cascade = CascadeType.ALL, orphanRemoval = true)
    @ToString.Exclude
    private List<ImagenActividad> imagenes = new ArrayList<>();

    @OneToMany(mappedBy = "actividad", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("ordenVisual ASC")
    @ToString.Exclude
    private List<PreguntaFrecuenteActividad> preguntasFrecuentes = new ArrayList<>();

    // Contadores de vistas para personalización y tendencias
    @Column(name = "total_vistas", nullable = false)
    private int totalVistas = 0;

    @Column(name = "total_tendencia", nullable = false)
    private int totalTendencia = 0;

    public BigDecimal getPrecioConsumidorSafe() {
        if (precio == null) {
            return BigDecimal.ZERO;
        }
        return precio.multiply(new BigDecimal("1.18"));
    }

    public long getCantidadComentario() {
        return comentarios != null ? comentarios.size() : 0;
    }

}

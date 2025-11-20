package maineta.eta.entity;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Lob;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
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




}

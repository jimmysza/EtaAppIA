package maineta.eta.entity;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.Lob;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;

@Entity
@Data
@AllArgsConstructor
@NoArgsConstructor
@Table(name = "cliente")
public class Cliente {  

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToMany(mappedBy = "cliente")
    private List<Reserva> reservas;

    @OneToOne
    @JoinColumn(name = "id_usuario", nullable = false, unique = true)
    private Usuario usuario;

    @Column(nullable = false, unique = true)
    private Long cedula;

    private String direccion;

    @Lob
    private String preferencias;

    @OneToMany(mappedBy = "cliente", cascade = CascadeType.ALL, orphanRemoval = true)
    @ToString.Exclude
    private List<Comentario> comentarios = new ArrayList<>();

    @OneToMany(mappedBy = "cliente", cascade = CascadeType.ALL, orphanRemoval = true)
    @ToString.Exclude
    private List<Favorito> favoritos = new ArrayList<>();

    // Nuevos campos para onboarding y personalización
    @Column(name = "pais_origen", length = 80)
    private String paisOrigen;

    @Enumerated(EnumType.STRING)
    @Column(name = "grupo_viaje", length = 20)
    private GrupoViaje grupoViaje;

    @Enumerated(EnumType.STRING)
    @Column(name = "rango_precio", length = 20)
    private RangoPrecio rangoPrecio;

    @Enumerated(EnumType.STRING)
    @Column(name = "disponibilidad_semana", length = 20)
    private DisponibilidadSemana disponibilidadSemana;

    @Column(name = "onboarding_completado", nullable = false)
    private boolean onboardingCompletado = false;

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
        name = "cliente_categorias_preferidas",
        joinColumns = @JoinColumn(name = "cliente_id"),
        inverseJoinColumns = @JoinColumn(name = "categoria_id")
    )
    @ToString.Exclude
    private Set<Categoria> categoriasPreferidas = new HashSet<>();
}

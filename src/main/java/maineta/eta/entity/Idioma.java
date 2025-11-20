package maineta.eta.entity;

import java.util.ArrayList;
import java.util.List;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Entity
@ToString(exclude = {"actividad"})
@Table(name = "idioma")
public class Idioma {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long idIdioma;

    @Column(nullable = false, length = 10)
    private String codigo; // 'es', 'en', 'fr'

    @Column(nullable = false, length = 50)
    private String nombre; // 'Español', 'Inglés'

    @OneToMany(mappedBy = "idioma")
    private List<Actividad> actividades = new ArrayList<>();
}
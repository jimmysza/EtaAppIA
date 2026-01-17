package maineta.eta.repository;

import maineta.eta.entity.Actividad;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ActividadRepository extends JpaRepository<Actividad, Long> {

    //SELECT * FROM actividad WHERE LOWER(titulo) LIKE LOWER('%<valor>%');
    Page<Actividad> findByTituloContainingIgnoreCase(String titulo, Pageable pageable);
    //SELECT * FROM actividad WHERE id_colaborador = ?;
    Page<Actividad> findByColaborador_IdColaborador(Long idColaborador, Pageable pageable);
    ;   

    //SELECT COUNT(*) FROM actividad WHERE id_categoria = ?
    int countByCategoria_IdCategoria(Long idCategoria);

    //combinacion de las dos anteriores
    Page<Actividad> findByColaborador_IdColaboradorAndTituloContainingIgnoreCase(Long idColaborador, String titulo, Pageable pageable);
    // buscar activiidad por categoria
    Page<Actividad> findByCategoria_IdCategoria(Long idCategoria, Pageable pageable);
    //buscar actividad por categoria del nonmbre
    Page<Actividad> findByCategoria_NombreContainingIgnoreCase(String nombreCategoria, Pageable pageable);
    Actividad findByCategoria_NombreContainingIgnoreCase(String nombreCategoria);

}


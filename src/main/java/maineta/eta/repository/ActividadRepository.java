package maineta.eta.repository;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import maineta.eta.entity.Actividad;

public interface ActividadRepository extends JpaRepository<Actividad, Long>, JpaSpecificationExecutor<Actividad> {

    // SELECT * FROM actividad WHERE LOWER(titulo) LIKE LOWER('%<valor>%');
    Page<Actividad> findByTituloContainingIgnoreCase(String titulo, Pageable pageable);

    // SELECT * FROM actividad WHERE id_colaborador = ?;
    Page<Actividad> findByColaborador_IdColaborador(Long idColaborador, Pageable pageable);

    // combinacion de las dos anteriores
    Page<Actividad> findByColaborador_IdColaboradorAndTituloContainingIgnoreCase(Long idColaborador, String titulo,
            Pageable pageable);

    // SELECT COUNT(*) FROM actividad WHERE id_categoria = ?
    int countByCategoria_IdCategoria(Long idCategoria);

    // buscar activiidad por categoria
    Page<Actividad> findByCategoria_IdCategoria(Long idCategoria, Pageable pageable);

    // buscar actividad por categoria del nonmbre
    Page<Actividad> findByCategoria_NombreContainingIgnoreCase(String nombreCategoria, Pageable pageable);

    Actividad findByCategoria_NombreContainingIgnoreCase(String nombreCategoria);

    @Query("""
                SELECT a.categoria.idCategoria, COUNT(a)
                FROM Actividad a
                WHERE a.categoria.idCategoria IN :categoriaIds
                GROUP BY a.categoria.idCategoria
            """)
    List<Object[]> contarActividadesPorCategorias(
            @Param("categoriaIds") List<Long> categoriaIds);
}

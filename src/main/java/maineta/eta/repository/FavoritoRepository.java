package maineta.eta.repository;

import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import maineta.eta.entity.Actividad;
import maineta.eta.entity.Cliente;
import maineta.eta.entity.Favorito;

@Repository
public interface FavoritoRepository extends JpaRepository<Favorito, Long> {

    List<Favorito> findByCliente(Cliente cliente);

    Optional<Favorito> findByClienteAndActividad(Cliente cliente, Actividad actividad);

    boolean existsByClienteAndActividad(Cliente cliente, Actividad actividad);

    void deleteByClienteAndActividad(Cliente cliente, Actividad actividad);

    List<Favorito> findByClienteOrderByCreatedAtDesc(Cliente cliente);

    long countByCliente(Cliente cliente);

    @Query("SELECT f.actividad.idActividad FROM Favorito f WHERE f.cliente = :cliente")
    Set<Long> findActividadIdsByCliente(@Param("cliente") Cliente cliente);
}

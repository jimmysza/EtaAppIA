package maineta.eta.repository;

import maineta.eta.entity.Colaborador;
import maineta.eta.entity.Usuario;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ColaboradorRepository extends JpaRepository<Colaborador,Integer> {

    Optional<Colaborador> findById(Integer integer);
    Optional<Colaborador> findByNit(String nit);
    Optional<Colaborador> findByCorreoSeguridad(String correo);

    Optional<Colaborador> findByUsuario(Usuario usuario);
}

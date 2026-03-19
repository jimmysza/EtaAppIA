package maineta.eta.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import maineta.eta.entity.Cliente;
import maineta.eta.entity.Reserva;

@Repository
public interface ReservaRepository extends JpaRepository<Reserva, Long> {

    List<Reserva> findByCliente(Cliente cliente);
    List<Reserva> findByActividad_IdActividad(Long idActividad);
    List<Reserva> findByActividad_Colaborador_IdColaboradorOrderByFechaReservaDesc(Long idColaborador);
    List<Reserva> findByCliente_IdAndActividad_IdActividad(Long idCliente, Long idActividad);
    Optional<Reserva> findByIdReservaAndCliente_Usuario_Email(Long idReserva, String email);
    Optional<Reserva> findByIdReservaAndActividad_Colaborador_Usuario_Email(Long idReserva, String email);
    Optional<Reserva> findByidReserva(Long idReserva);

    


}

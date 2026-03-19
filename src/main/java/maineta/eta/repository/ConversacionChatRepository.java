package maineta.eta.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import maineta.eta.entity.ConversacionChat;

public interface ConversacionChatRepository extends JpaRepository<ConversacionChat, Long> {

    Optional<ConversacionChat> findByReserva_IdReserva(Long idReserva);

    List<ConversacionChat> findByCliente_IdOrderByUpdatedAtDesc(Long idCliente);

    List<ConversacionChat> findByColaborador_IdColaboradorOrderByUpdatedAtDesc(Long idColaborador);
}

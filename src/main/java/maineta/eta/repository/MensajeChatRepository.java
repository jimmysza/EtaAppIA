package maineta.eta.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import maineta.eta.entity.MensajeChat;

public interface MensajeChatRepository extends JpaRepository<MensajeChat, Long> {

    List<MensajeChat> findByConversacion_IdConversacionOrderByFechaEnvioAsc(Long idConversacion);
}

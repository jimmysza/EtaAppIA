package maineta.eta.service;

import java.util.List;

import org.springframework.lang.NonNull;

import maineta.eta.entity.ConversacionChat;
import maineta.eta.entity.MensajeChat;

public interface ChatService {

    List<ConversacionChat> listarConversacionesCliente(String emailUsuario);

    List<ConversacionChat> listarConversacionesColaborador(String emailUsuario);

    ConversacionChat obtenerOCrearConversacionDesdeReservaCliente(Long idReserva, String emailUsuario);

    ConversacionChat obtenerOCrearConversacionDesdeReservaColaborador(Long idReserva, String emailUsuario);

    List<MensajeChat> listarMensajes(@NonNull Long idConversacion, String emailUsuario);

    MensajeChat enviarMensaje(@NonNull Long idConversacion, String contenido, String emailUsuario);
}

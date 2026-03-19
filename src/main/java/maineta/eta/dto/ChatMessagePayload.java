package maineta.eta.dto;

import java.time.LocalDateTime;

import lombok.Data;
import maineta.eta.entity.MensajeChat;

@Data
public class ChatMessagePayload {
    private Long idMensaje;
    private Long conversacionId;
    private Long remitenteId;
    private String remitenteNombre;
    private String remitenteEmail;
    private String contenido;
    private LocalDateTime fechaEnvio;

    public static ChatMessagePayload from(MensajeChat mensaje) {
        ChatMessagePayload payload = new ChatMessagePayload();
        payload.setIdMensaje(mensaje.getIdMensaje());
        payload.setConversacionId(mensaje.getConversacion().getIdConversacion());
        payload.setRemitenteId(mensaje.getRemitente().getId());
        payload.setRemitenteNombre(mensaje.getRemitente().getNombre());
        payload.setRemitenteEmail(mensaje.getRemitente().getEmail());
        payload.setContenido(mensaje.getContenido());
        payload.setFechaEnvio(mensaje.getFechaEnvio());
        return payload;
    }
}

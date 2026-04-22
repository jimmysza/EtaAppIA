package maineta.eta.dto;

/**
 * DTO de response para el endpoint POST /chat/mensaje
 */
public class ChatMensajeResponseDTO {

    private String respuesta;
    private String error;

    public ChatMensajeResponseDTO() {}

    public static ChatMensajeResponseDTO ok(String respuesta) {
        ChatMensajeResponseDTO dto = new ChatMensajeResponseDTO();
        dto.respuesta = respuesta;
        return dto;
    }

    public static ChatMensajeResponseDTO error(String error) {
        ChatMensajeResponseDTO dto = new ChatMensajeResponseDTO();
        dto.error = error;
        return dto;
    }

    public String getRespuesta() { return respuesta; }
    public void setRespuesta(String respuesta) { this.respuesta = respuesta; }

    public String getError() { return error; }
    public void setError(String error) { this.error = error; }
}

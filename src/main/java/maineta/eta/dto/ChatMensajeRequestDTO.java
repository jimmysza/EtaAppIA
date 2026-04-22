package maineta.eta.dto;

import java.util.List;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * DTO de request para el endpoint POST /chat/mensaje
 */
public class ChatMensajeRequestDTO {

    @NotBlank(message = "El mensaje no puede estar vacío")
    @Size(max = 500, message = "El mensaje no puede superar 500 caracteres")
    private String mensaje;

    private List<MensajeDTO> historial;

    /** Opcional: ID de la actividad si el usuario está en detalle-actividad */
    private Long contextoActividad;

    public ChatMensajeRequestDTO() {}

    public String getMensaje() { return mensaje; }
    public void setMensaje(String mensaje) { this.mensaje = mensaje; }

    public List<MensajeDTO> getHistorial() { return historial; }
    public void setHistorial(List<MensajeDTO> historial) { this.historial = historial; }

    public Long getContextoActividad() { return contextoActividad; }
    public void setContextoActividad(Long contextoActividad) { this.contextoActividad = contextoActividad; }
}

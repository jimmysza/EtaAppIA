package maineta.eta.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * DTO para un mensaje individual del historial de conversación.
 * rol: "user" o "assistant"
 */
public class MensajeDTO {

    @NotBlank
    @Pattern(regexp = "user|assistant", message = "El rol debe ser 'user' o 'assistant'")
    private String rol;

    @NotBlank
    @Size(max = 2000)
    private String contenido;

    public MensajeDTO() {}

    public MensajeDTO(String rol, String contenido) {
        this.rol = rol;
        this.contenido = contenido;
    }

    public String getRol() { return rol; }
    public void setRol(String rol) { this.rol = rol; }

    public String getContenido() { return contenido; }
    public void setContenido(String contenido) { this.contenido = contenido; }
}

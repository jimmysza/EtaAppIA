package maineta.eta.dto;

import org.springframework.web.multipart.MultipartFile;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class CrearPlanFormDTO {
    
    @NotBlank(message = "El título es obligatorio")
    private String titulo;
    
    private String descripcion;
    
    private String duracionEstimada;
    
    private String tipo;
    
    private MultipartFile imagenPortada;
    
    // IDs de actividades en orden, con hora y nota — enviadas como JSON string
    private String actividadesJson; // se parsea en el servicio
}

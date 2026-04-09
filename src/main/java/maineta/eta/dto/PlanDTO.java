package maineta.eta.dto;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class PlanDTO {
    private Long id;
    private String titulo;
    private String descripcion;
    private String imagenPortada;
    private String duracionEstimada;
    private String tipo;
    private List<PlanActividadDTO> actividades = new ArrayList<>();
    private String nombreCreador;
    private String rolCreador; // "CLIENTE" o "COLABORADOR"
    private Long idCreador;
    private LocalDateTime fechaCreacion;
    private int vistas;
}

package maineta.eta.dto;

import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class ColaboradorPublicoDTO {

    private Long idColaborador;
    private String nombre;
    private long totalActividades;
    private long totalReservas;
    private long totalViajeros;
    private double promedioCalificacion;
    private String imagenPrincipal;
    private String actividadDestacada;
    private String iniciales;

    public ColaboradorPublicoDTO(
            Long idColaborador,
            String nombre,
            Long totalActividades,
            Long totalReservas,
            Long totalViajeros,
            Double promedioCalificacion) {
        this.idColaborador = idColaborador;
        this.nombre = nombre;
        this.totalActividades = totalActividades != null ? totalActividades : 0L;
        this.totalReservas = totalReservas != null ? totalReservas : 0L;
        this.totalViajeros = totalViajeros != null ? totalViajeros : 0L;
        this.promedioCalificacion = promedioCalificacion != null ? promedioCalificacion : 0.0;
    }
}
package maineta.eta.service;

import java.util.List;
import java.util.Map;

import org.springframework.data.domain.Page;

import maineta.eta.entity.Comentario;

public interface ComentarioService {
    void guardar(Comentario comentario);
    List<Comentario> listarPorActividad(Long idActividad);
    Page<Comentario> listarComentarioPorIdYPaginacion(Long idActividad, int page, int size);
    Map<Long, Integer> contarComentariosPorActividades(List<Long> ids);
    int calcularPromedioActividad(Long idActividad);
    Double calcularPromedioDecimal(Long idActividad);
    Map<Integer, Long> obtenerDistribucionEstrellas(Long idActividad);

}

package maineta.eta.service;

import maineta.eta.entity.Actividad;
import maineta.eta.entity.Comentario;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import java.util.Map;
import java.util.List;

public interface ComentarioService {
    void guardar(Comentario comentario);
    List<Comentario> listarPorActividad(Long idActividad);
    Page<Comentario> listarComentarioPorIdYPaginacion(Long idActividad, int page, int size);
    Map<Long, Integer> contarComentariosPorActividades(List<Long> ids);
}

package maineta.eta.service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import maineta.eta.entity.Comentario;
import maineta.eta.repository.ComentarioRepository;

@Service
public class ComentarioServiceImpl implements ComentarioService {

    private final ComentarioRepository comentarioRepository;

    @Autowired
    public ComentarioServiceImpl(ComentarioRepository comentarioRepository) {
        this.comentarioRepository = comentarioRepository;
    }

    @Override
    public Map<Long, Integer> contarComentariosPorActividades(List<Long> ids) {

        Map<Long, Integer> resultado = new HashMap<>();

        List<Object[]> data = comentarioRepository.contarComentariosPorActividades(ids);

        for (Object[] row : data) {
            Long idActividad = (Long) row[0];
            Long cantidad = (Long) row[1];
            resultado.put(idActividad, cantidad.intValue());
        }

        return resultado;
    }

    @Override
    public int calcularPromedioActividad(Long idActividad) {
        Double promedio = comentarioRepository.promedioCalificacionPorActividad(idActividad);
        return (int) Math.round(promedio);
    }

    @Override
    public Double calcularPromedioDecimal(Long idActividad) {
        return comentarioRepository.promedioCalificacionPorActividad(idActividad);
    }

    @Override
    public Map<Integer, Long> obtenerDistribucionEstrellas(Long idActividad) {
        Map<Integer, Long> distribucion = new HashMap<>();
        // Inicializar todas las estrellas en 0
        for (int i = 1; i <= 5; i++) {
            distribucion.put(i, 0L);
        }
        List<Object[]> data = comentarioRepository.distribucionCalificacionPorActividad(idActividad);
        for (Object[] row : data) {
            Integer calificacion = (Integer) row[0];
            Long cantidad = (Long) row[1];
            distribucion.put(calificacion, cantidad);
        }
        return distribucion;
    }

    @Override
    public void guardar(Comentario comentario) {
        comentarioRepository.save(comentario);
    }

    @Override
    public List<Comentario> listarPorActividad(Long idActividad) {
        return comentarioRepository.findByActividad_IdActividad(idActividad);
    }

    @Override
    public Page<Comentario> listarComentarioPorIdYPaginacion(Long idActividad, int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        return comentarioRepository.findByActividad_IdActividad(idActividad, pageable);
    }
}

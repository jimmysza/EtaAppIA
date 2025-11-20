package maineta.eta.service;

import maineta.eta.entity.Actividad;
import maineta.eta.entity.Comentario;
import maineta.eta.repository.ComentarioRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.beans.factory.annotation.Autowired;
import java.util.List;

@Service
public class ComentarioServiceImpl implements ComentarioService {

    private final ComentarioRepository comentarioRepository;

    @Autowired
    public ComentarioServiceImpl(ComentarioRepository comentarioRepository) {
        this.comentarioRepository = comentarioRepository;
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

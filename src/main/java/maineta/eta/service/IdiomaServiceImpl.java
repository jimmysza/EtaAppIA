package maineta.eta.service;

import maineta.eta.entity.Categoria;
import maineta.eta.entity.Idioma;
import maineta.eta.repository.IdiomaRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class IdiomaServiceImpl implements IdiomaService{

    @Autowired
    private final IdiomaRepository idiomaRepository;

    public IdiomaServiceImpl(IdiomaRepository idiomaRepository){
        this.idiomaRepository = idiomaRepository;
    }


    @Override
    public Idioma guardarIdioma(Idioma idioma) {
        return idiomaRepository.save(idioma);
    }

    @Override
    public List<Idioma> listarIdiomas() {
        return idiomaRepository.findAll();
    }

    @Override
    public Idioma obtenerIdioma(Long id) {
        return idiomaRepository.findById(id).orElse(null);
    }

    @Override
    public void eliminarIdioma(Long id) {
        idiomaRepository.deleteById(id);
    }

    @Override
    public Optional<Idioma> buscarPorNombre(String nombre) {
        return idiomaRepository.findByNombre(nombre);
    }

}

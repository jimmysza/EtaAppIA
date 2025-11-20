package maineta.eta.service;


import maineta.eta.entity.Idioma;

import java.util.Optional;

public interface IdiomaService {

    Idioma guardarIdioma(Idioma idioma);
    java.util.List<Idioma> listarIdiomas();
    Idioma obtenerIdioma(Long id);
    void eliminarIdioma(Long id);
    Optional<Idioma> buscarPorNombre(String nombre);
}

package maineta.eta.service;

import java.util.List;
import java.util.Set;

import maineta.eta.entity.Actividad;
import maineta.eta.entity.Cliente;
import maineta.eta.entity.Favorito;

public interface FavoritoService {

    Favorito agregarFavorito(Cliente cliente, Actividad actividad);

    void eliminarFavorito(Cliente cliente, Actividad actividad);

    boolean esFavorito(Cliente cliente, Actividad actividad);

    /**
     * Toggle: si ya es favorito lo elimina, si no lo agrega.
     * @return true si se agregó, false si se eliminó
     */
    boolean toggleFavorito(Cliente cliente, Actividad actividad);

    List<Favorito> obtenerFavoritosDeCliente(Cliente cliente);

    long contarFavoritos(Cliente cliente);

    Set<Long> obtenerIdsFavoritosDeCliente(Cliente cliente);
}

package maineta.eta.service;

import java.util.List;
import java.util.Set;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import maineta.eta.entity.Actividad;
import maineta.eta.entity.Cliente;
import maineta.eta.entity.Favorito;
import maineta.eta.repository.FavoritoRepository;

@Service
@Transactional
public class FavoritoServiceImpl implements FavoritoService {

    private final FavoritoRepository favoritoRepository;

    @Autowired
    public FavoritoServiceImpl(FavoritoRepository favoritoRepository) {
        this.favoritoRepository = favoritoRepository;
    }

    @Override
    public Favorito agregarFavorito(Cliente cliente, Actividad actividad) {
        // Verificar si ya existe
        if (favoritoRepository.existsByClienteAndActividad(cliente, actividad)) {
            throw new RuntimeException("La actividad ya está en favoritos");
        }
        Favorito favorito = new Favorito();
        favorito.setCliente(cliente);
        favorito.setActividad(actividad);
        return favoritoRepository.save(favorito);
    }

    @Override
    public void eliminarFavorito(Cliente cliente, Actividad actividad) {
        favoritoRepository.deleteByClienteAndActividad(cliente, actividad);
    }

    @Override
    public boolean esFavorito(Cliente cliente, Actividad actividad) {
        return favoritoRepository.existsByClienteAndActividad(cliente, actividad);
    }

    @Override
    public boolean toggleFavorito(Cliente cliente, Actividad actividad) {
        if (favoritoRepository.existsByClienteAndActividad(cliente, actividad)) {
            favoritoRepository.deleteByClienteAndActividad(cliente, actividad);
            return false; // Se eliminó
        } else {
            Favorito favorito = new Favorito();
            favorito.setCliente(cliente);
            favorito.setActividad(actividad);
            favoritoRepository.save(favorito);
            return true; // Se agregó
        }
    }

    @Override
    @Transactional(readOnly = true)
    public List<Favorito> obtenerFavoritosDeCliente(Cliente cliente) {
        return favoritoRepository.findByClienteOrderByCreatedAtDesc(cliente);
    }

    @Override
    @Transactional(readOnly = true)
    public long contarFavoritos(Cliente cliente) {
        return favoritoRepository.countByCliente(cliente);
    }

    @Override
    @Transactional(readOnly = true)
    public Set<Long> obtenerIdsFavoritosDeCliente(Cliente cliente) {
        return favoritoRepository.findActividadIdsByCliente(cliente);
    }
}

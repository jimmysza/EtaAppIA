package maineta.eta.service;

import java.util.List;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import maineta.eta.entity.Disponibilidad;
import maineta.eta.repository.DisponibilidadRepository;

@Service
public class DisponibilidadServiceImpl implements DisponibilidadService {
    
    @Autowired
    private DisponibilidadRepository disponibilidadRepository;
    
    @Override
    public Disponibilidad guardarDisponibilidad(Disponibilidad disponibilidad){
        return disponibilidadRepository.save(disponibilidad);
    }

    @Override
    public Optional<Disponibilidad> obtenerPorId(Long id) {
        return disponibilidadRepository.findById(id);
    }

    @Override
    public Long ContadorDisponibilidades() {
        return disponibilidadRepository.count();
    }

    @Override
    public List<Disponibilidad> obtenerPorActividad(Long idActividad) {
        return disponibilidadRepository.findByActividadIdActividad(idActividad);
    }
}
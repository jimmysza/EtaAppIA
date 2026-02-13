package maineta.eta.service;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import jakarta.persistence.EntityNotFoundException;
import maineta.eta.dto.ActividadUpdateDto;
import maineta.eta.entity.Actividad;
import maineta.eta.entity.Categoria;
import maineta.eta.entity.Idioma;
import maineta.eta.repository.ActividadRepository;
import maineta.eta.repository.CategoriaRepository;
import maineta.eta.repository.ComentarioRepository;
import maineta.eta.repository.IdiomaRepository;

/**
 * 🔹 Implementación de la interfaz ActividadService.
 * 
 * - Aquí se define el "cómo" funcionan los métodos declarados en la interfaz.
 * - Usa el repositorio (ActividadRepository) para interactuar con la base de
 * datos.
 * - Contiene toda la lógica de negocio relacionada con las actividades.
 */
@Service
public class ActividadServiceImpl implements ActividadService {

    // Inyección de dependencias: repositorio que conecta con la BD
    private final ActividadRepository actividadRepository;
    private final IUploadFileService uploadFileService;
    private final ComentarioRepository comentarioRepository;
    private final IdiomaRepository idiomaRepository;
    private final CategoriaRepository categoriaRepository;

    /**
     * Constructor con @Autowired para inyectar el repositorio automáticamente.
     */
    @Autowired
    public ActividadServiceImpl(IdiomaRepository idiomaRepository, IUploadFileService uploadFileService,
            CategoriaRepository categoriaRepository,
            ActividadRepository actividadRepository,
            ComentarioRepository comentarioRepository) {
        this.actividadRepository = actividadRepository;
        this.categoriaRepository = categoriaRepository;
        this.comentarioRepository = comentarioRepository;
        this.uploadFileService = uploadFileService;
        this.idiomaRepository = idiomaRepository;
    }

    public Map<Long, Long> contarActividadesPorCategorias(List<Long> categoriaIds) {

        List<Object[]> resultados = actividadRepository.contarActividadesPorCategorias(categoriaIds);

        Map<Long, Long> conteo = new HashMap<>();

        for (Object[] fila : resultados) {
            Long idCategoria = (Long) fila[0];
            Long cantidad = (Long) fila[1];
            conteo.put(idCategoria, cantidad);
        }

        return conteo;
    }

    @Override
    public int ContadorActividadesPorCategoria(Long idCategoria) {
        return actividadRepository.countByCategoria_IdCategoria(idCategoria);
    };

    @Override
    public Page<Actividad> ObtenerActividadesPorTitulo(String titulo, Pageable pageable) {
        return actividadRepository.findByTituloContainingIgnoreCase(titulo, pageable);
    }

    @Override
    public Actividad obtenerPorId(Long id) {
        if (id == null) {
            throw new IllegalArgumentException("El id de la actividad no puede ser null");
        }

        return actividadRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("No se encontró la actividad con ID: " + id));
    }

    /**
     * 🔹 Listar actividades con paginación para el dashboard principal (clientes).
     * Si se pasa un filtroNombre, busca por título (ignora mayúsculas/minúsculas).
     */
    @Override
    public Page<Actividad> getActividadesWithPaginationMain(int page, int size, String filtroNombre) {
        Pageable pageable = PageRequest.of(page, size); // Objeto que define página y tamaño

        if (filtroNombre != null && !filtroNombre.isEmpty()) {
            // Si hay filtro: buscar solo actividades cuyo título contenga el texto
            return actividadRepository.findByTituloContainingIgnoreCase(filtroNombre, pageable);
        }
        // Si no hay filtro: traer todas las actividades paginadas
        return actividadRepository.findAll(pageable);
    }

    @Override
    public Page<Actividad> buscarActividadPorIdCategoria(Long idCategoria, int page, int size) {
        Pageable pageable = PageRequest.of(page, size); // Objeto que define página y tamaño

        if (idCategoria != null) {
            return actividadRepository.findByCategoria_IdCategoria(idCategoria, pageable);
        }

        return actividadRepository.findAll(pageable);
    }

    @Override
    public Page<Actividad> buscarActividadesPorNombreDeCategoria(String nombreCategoria, int page, int size) {
        Pageable pageable = PageRequest.of(page, size);

        if (nombreCategoria != null && !nombreCategoria.isEmpty()) {
            return actividadRepository.findByCategoria_NombreContainingIgnoreCase(nombreCategoria, pageable);
        }

        return actividadRepository.findAll(pageable);
    }

    @Override
    public Actividad buscarActividadPorNombreDeCategoria(String nombreCategoria) {
        Actividad actividad = actividadRepository.findByCategoria_NombreContainingIgnoreCase(nombreCategoria);

        if (actividad != null) {
            return actividad;
        } else {
            throw new EntityNotFoundException("No se encontró actividad con la categoría: " + nombreCategoria);
        }
    }

    /**
     * 🔹 Listar actividades de un colaborador con filtros y paginación.
     * Permite filtrar tanto por el ID del colaborador como por el título de la
     * actividad.
     */
    @Override
    public Page<Actividad> getActividadesConPaginacionDeColaborador(int page, int size, Long idColaborador,
            String filtroTitulo) {
        Pageable pageable = PageRequest.of(page, size);

        // Caso 1: hay colaborador y también filtro por título
        if (idColaborador != null && filtroTitulo != null && !filtroTitulo.isEmpty()) {
            return actividadRepository.findByColaborador_IdColaboradorAndTituloContainingIgnoreCase(
                    idColaborador, filtroTitulo, pageable);
        }

        // Caso 2: solo hay colaborador
        if (idColaborador != null) {
            return actividadRepository.findByColaborador_IdColaborador(idColaborador, pageable);
        }

        // Caso 3: solo hay filtro por título
        if (filtroTitulo != null && !filtroTitulo.isEmpty()) {
            return actividadRepository.findByTituloContainingIgnoreCase(filtroTitulo, pageable);
        }

        // Caso 4: no hay filtros, trae todo
        return actividadRepository.findAll(pageable);
    }

    /**
     * 🔹 Listar todas las actividades paginadas sin filtros adicionales.
     */
    @Override
    public Page<Actividad> getActividadesWithPagination(int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        return actividadRepository.findAll(pageable);
    }

    /**
     * 🔹 Listar TODAS las actividades sin paginación (devuelve un List completo).
     */
    @Override
    public List<Actividad> listarActividades() {
        return actividadRepository.findAll();
    }

    /**
     * 🔹 Guardar una nueva actividad en la base de datos.
     * Retorna la actividad guardada (incluyendo su ID generado).
     */
    @Override
    public Actividad agregarActividad(Actividad actividad) {

        if (actividad.getCategoria() != null) {
            Categoria categoria = actividad.getCategoria();

            if (categoria.getActividades() == null) {
                categoria.setActividades(new ArrayList<>());
            }

            categoria.getActividades().add(actividad);
        }

        return actividadRepository.save(actividad);
    }

    /**
     * 🔹 Eliminar una actividad por su ID.
     */
    @Override
    @Transactional
    public void deleteActivity(long id) {

        comentarioRepository.deleteByActividadId(id);
        actividadRepository.deleteById(id);
    }

    @Override
    public Long ContadorActividades() {
        return actividadRepository.count();
    }

    /**
     * 🔹 Buscar una actividad por su ID.
     * Si no se encuentra, retorna null en lugar de Optional.
     */
    @Override
    public Actividad listarById(Long id) {
        return actividadRepository.findById(id).orElse(null);
    }

    @Override
    @Transactional
    public void actualizarActividad(
            Long id,
            ActividadUpdateDto dto,
            MultipartFile imagenFile) throws IOException {

        if (id == null) {
            throw new IllegalArgumentException("El id no puede ser null");
        }

        Actividad actividad = actividadRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Actividad no encontrada"));

        // -------- campos simples --------
        if (dto.getTitulo() != null) {
            actividad.setTitulo(dto.getTitulo());
        }

        if (dto.getUbicacion() != null) {
            actividad.setUbicacion(dto.getUbicacion());
        }

        if (dto.getDescripcion() != null) {
            actividad.setDescripcion(dto.getDescripcion());
        }

        if (dto.getPrecio() != null) {
            actividad.setPrecio(dto.getPrecio());
        }

        // -------- relación ManyToOne --------
        if (dto.getIdIdioma() != null) {
            Idioma idioma = idiomaRepository.getReferenceById(dto.getIdIdioma());
            actividad.setIdioma(idioma);
        }

        if (dto.getIdCategoria() != null) {
            Categoria categoria = categoriaRepository.getReferenceById(dto.getIdCategoria());
            actividad.setCategoria(categoria);
        }
        if(dto.getIncluye() != null){
            actividad.setIncluye(dto.getIncluye());
        }
        if(dto.getCondiciones() != null){
            actividad.setCondiciones(dto.getCondiciones());
        }
        if(dto.getNormas() != null){
            actividad.setNormas(dto.getNormas());
        }

        // -------- imagen --------
        if (imagenFile != null && !imagenFile.isEmpty()) {

            // eliminar imagen anterior
            if (actividad.getImagen() != null && !actividad.getImagen().isEmpty()) {
                uploadFileService.delete(actividad.getImagen());
            }

            String nombreImagen = uploadFileService.copy(imagenFile);
            actividad.setImagen(nombreImagen);
        }

    }

    @Transactional
    @Override
    public Actividad actualizarTitulo(Long id, String titulo) {
        Actividad actividad = obtenerPorId(id);
        actividad.setTitulo(titulo);
        return actividadRepository.save(actividad);
    }

    @Transactional
    @Override
    public Actividad actualizarDescripcion(Long id, String descripcion) {
        Actividad actividad = obtenerPorId(id);
        actividad.setDescripcion(descripcion);
        return actividadRepository.save(actividad);
    }

    @Transactional
    @Override
    public Actividad actualizarPrecio(Long id, java.math.BigDecimal precio) {
        Actividad actividad = obtenerPorId(id);
        actividad.setPrecio(precio);
        return actividadRepository.save(actividad);
    }

    @Transactional
    @Override
    public Actividad actualizarUbicacion(Long id, String ubicacion) {
        Actividad actividad = obtenerPorId(id);
        actividad.setUbicacion(ubicacion);
        return actividadRepository.save(actividad);
    }

}

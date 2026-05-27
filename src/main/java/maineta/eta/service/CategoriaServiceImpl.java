package maineta.eta.service;

import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import maineta.eta.entity.Categoria;
import maineta.eta.repository.CategoriaRepository;

/**
 * 🔹 Implementación de la interfaz CategoriaService
 * 
 * Esta clase contiene la lógica de negocio para manejar las categorías.
 * 
 * ➡️ Se conecta con el repositorio (CategoriaRepository), que a su vez
 *     interactúa directamente con la base de datos.
 * ➡️ Aplica la "capa de servicio" dentro de la arquitectura en capas.
 */
@Service // Marca esta clase como un "Servicio" de Spring (un bean manejado por el contenedor)
public class CategoriaServiceImpl implements CategoriaService {

    // Inyección de dependencias: repositorio que maneja las operaciones CRUD de Categoria
    private final CategoriaRepository categoriaRepository;

    /**
     * Constructor para inyectar el repositorio de categorías.
     * Spring Boot lo detecta y lo conecta automáticamente.
     */
    public CategoriaServiceImpl(CategoriaRepository categoriaRepository) {
        this.categoriaRepository = categoriaRepository;
    }

    /**
     * 🔹 Guardar una nueva categoría o actualizar una existente.
     * 
     * - Si la categoría no tiene ID, se crea una nueva en la BD.
     * - Si tiene un ID válido, se actualizan sus datos.
     *
     * @param categoria Objeto Categoria a guardar
     * @return La categoría guardada con su ID generado (si era nueva)
     */
    @Override
    public Categoria guardarCategoria(Categoria categoria) {
        // Si es nueva (sin id) -> validar que no exista por nombre
        if (categoria.getIdCategoria() == null) {
            if (categoriaRepository.existsByNombre(categoria.getNombre())) {
                throw new RuntimeException("Ya existe una categoría con ese nombre: " + categoria.getNombre());
            }
            return categoriaRepository.save(categoria);
        }

        // Si es actualización -> validar SOLO si el nombre cambió
        java.util.Optional<Categoria> existente = categoriaRepository.findById(categoria.getIdCategoria());
        if (existente.isPresent()) {
            String nombreAnterior = existente.get().getNombre();
            String nombreNuevo = categoria.getNombre();
            
            // ✅ Si el nombre cambió (y no es nulo/vacío), validar unicidad
            if (nombreNuevo != null && !nombreNuevo.trim().isEmpty() && !nombreNuevo.equals(nombreAnterior)) {
                if (categoriaRepository.existsByNombre(nombreNuevo)) {
                    throw new RuntimeException("Ya existe una categoría con ese nombre: " + nombreNuevo);
                }
            }
        }

        return categoriaRepository.save(categoria);
    }

    /**
     * 🔹 Obtener todas las categorías existentes en la base de datos.
     *
     * @return Lista de todas las categorías registradas
     */
    @Override
    public List<Categoria> listarCategorias() {
        return categoriaRepository.findAll();
    }

    @Override
    public Categoria listarById(Long id) {
        return categoriaRepository.findById(id).orElse(null);
    }

    /**
     * 🔹 Buscar una categoría por su ID.
     * 
     * - Si la categoría existe, la devuelve.
     * - Si no existe, retorna null (gracias a orElse(null)).
     *
     * @param id ID de la categoría
     * @return Objeto Categoria o null si no se encontró
     */
    @Override
    public Categoria getCategoriaPorId(Long id) {
        return categoriaRepository.findById(id).orElse(null);
    }

    /**
     * 🔹 Eliminar una categoría de la base de datos por su ID.
     *
     * @param id ID de la categoría a eliminar
     */
    @Override
    public void eliminarCategoria(Long id) {
        if (!categoriaRepository.existsById(id)) {
            throw new RuntimeException("Categoría no encontrada con id: " + id);
        }
        
        categoriaRepository.deleteById(id);
    }

    @Override
    public Page<Categoria> buscarTodasPorPaginacion(int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        return categoriaRepository.findAll(pageable);
    }

    @Override
    public boolean existeLaCategoria(String nombre) {
        return categoriaRepository.existsByNombre(nombre);
    }

    @Override
    public Optional<Categoria> buscarCategoriaPorNombre(String nombre) {
        return categoriaRepository.findByNombre(nombre);
    }
}


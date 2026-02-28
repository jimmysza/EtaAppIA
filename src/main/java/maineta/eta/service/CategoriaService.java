package maineta.eta.service;

import java.util.Optional;

import org.springframework.data.domain.Page;

import maineta.eta.entity.Categoria;

/**
 * 🔹 Interfaz CategoriaService
 * 
 * Esta interfaz define los métodos que el servicio de Categoría debe implementar.
 * 
 * ➡️ La idea es separar la "definición" (qué hace el servicio) de la "implementación"
 *    (cómo lo hace). De esta manera, el código queda más organizado y se puede
 *    cambiar la lógica interna sin afectar a los controladores.
 * 
 * Los méto-dos aquí declarados serán implementados en la clase CategoriaServiceImpl.
 */
public interface CategoriaService {

    /**
     * 🔹 Guardar una nueva categoría o actualizar una existente.
     * - Si la categoría no tiene ID, se crea una nueva en la base de datos.
     * - Si la categoría ya existe (tiene un ID válido), se actualizan sus datos.
     *
     * @param categoria Objeto de tipo Categoria a guardar
     * @return La categoría guardada (incluyendo el ID generado si es nueva)
     */
    Categoria guardarCategoria(Categoria categoria);

    /**
     * 🔹 Obtener todas las categorías registradas en la base de datos.
     *
     * @return Lista con todas las categorías.
     */
    java.util.List<Categoria> listarCategorias();
    
    Categoria listarById(Long id);

    /**
     * 🔹 Buscar una categoría por su ID.
     *
     * @param id ID de la categoría a buscar
     * @return La categoría encontrada o null si no existe
     */
    Categoria getCategoriaPorId(Long id);

    /**
     * 🔹 Eliminar una categoría por su ID.
     *
     * @param id ID de la categoría a eliminar
     */
    void eliminarCategoria(Long id);
    Page<Categoria> buscarTodasPorPaginacion(int page, int size);
    boolean existeLaCategoria(String nombre);
    Optional<Categoria> buscarCategoriaPorNombre(String nombre);
}


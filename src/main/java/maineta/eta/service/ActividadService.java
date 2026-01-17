package maineta.eta.service;

import java.util.List;

import org.springframework.data.domain.Page;

import maineta.eta.entity.Actividad;
import org.springframework.data.domain.Pageable;

/**
 * 🔹 Interfaz del servicio de Actividad.
 * 
 * - Define los métodos que cualquier implementación del servicio de actividades
 *   debe proveer.
 * - El servicio actúa como intermediario entre el controlador (que maneja la 
 *   lógica de peticiones HTTP) y el repositorio (que maneja el acceso a la base de datos).
 * 
 * 🚀 Ventaja de usar interfaces:
 *   - Separa la definición de la lógica de negocio de su implementación.
 *   - Permite cambiar la implementación sin afectar al resto del sistema.
 *   - Facilita pruebas (mocking en tests).
 */
public interface ActividadService {


    Page<Actividad> ObtenerActividadesPorTitulo(String titulo, Pageable pageable);
    /**
     * Obtiene una lista de actividades paginadas desde la base de datos,
     * con un filtro opcional por nombre.
     *
     * @param page número de página (0 = primera página).
     * @param size cantidad de elementos por página.
     * @param filtroNombre filtro opcional por el nombre de la actividad (puede ser null).
     * @return una página (Page<Actividad>) con las actividades filtradas.
     * 
     * 👉 Se usa en la vista principal (dashboard de cliente).
     */
    Page<Actividad> getActividadesWithPaginationMain(int page, int size, String filtroNombre);

    /**
     * Obtiene una lista de actividades paginadas pertenecientes a un colaborador específico,
     * con un filtro opcional por título.
     *
     * @param page número de página.
     * @param size cantidad de elementos por página.
     * @param idColaborador identificador del colaborador al que pertenecen las actividades.
     * @param filtroTitulo filtro opcional por título de la actividad.
     * @return una página con las actividades del colaborador filtradas.
     * 
     * 👉 Se usa cuando un colaborador ve/gestiona sus propias actividades.
     */
    Page<Actividad> getActividadesConPaginacionDeColaborador(int page, int size, Long idColaborador, String filtroTitulo);

    int ContadorActividadesPorCategoria(Long idCategoria);

    /**
     * Obtiene todas las actividades paginadas sin filtros adicionales.
     *
     * @param page número de página.
     * @param size cantidad de actividades por página.
     * @return una página con todas las actividades.
     * 
     * 👉 Se usa en listados generales de actividades.
     */
    Page<Actividad> getActividadesWithPagination(int page, int size);

    /**
     * Lista todas las actividades sin paginación.
     *
     * @return una lista completa de actividades.
     * 
     * 👉 Útil cuando necesitamos todas las actividades de golpe,
     * por ejemplo, para exportaciones o cargar menús desplegables.
     */
    List<Actividad> listarActividades();
    Actividad obtenerPorId(Long id);
    /**
     * Agrega una nueva actividad al sistema.
     *
     * @param actividad objeto Actividad a guardar.
     * @return la actividad guardada con su ID generado.
     * 
     * 👉 Se usa al registrar nuevas actividades.
     */
    Actividad agregarActividad(Actividad actividad);

    /**
     * Elimina una actividad por su ID.
     *
     * @param id identificador de la actividad a eliminar.
     * 
     * 👉 Importante para el CRUD completo de actividades.
     */
    void deleteActivity(long id);
    Long ContadorActividades();
    /**
     * Busca una actividad por su ID.
     *
     * @param id identificador de la actividad.
     * @return la actividad encontrada o null si no existe.
     * 
     * 👉 Se usa para mostrar detalles de una actividad en específico.
     */
    Actividad listarById(Long id);


	Actividad actualizar(Long id, Actividad actividad);
    Page<Actividad> buscarActividadPorIdCategoria(Long idCategoria, int page, int size);
    Page<Actividad> buscarActividadesPorNombreDeCategoria(String nombreCategoria, int page, int size);
    Actividad buscarActividadPorNombreDeCategoria(String nombreCategoria);
}


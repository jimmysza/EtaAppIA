package maineta.eta.service;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.web.multipart.MultipartFile;

import maineta.eta.dto.ActividadUpdateDto;
import maineta.eta.entity.Actividad;
import maineta.eta.entity.Cliente;
import maineta.eta.entity.ImagenActividad;

/**
 * 🔹 Interfaz del servicio de Actividad.
 * 
 * - Define los métodos que cualquier implementación del servicio de actividades
 * debe proveer.
 * - El servicio actúa como intermediario entre el controlador (que maneja la
 * lógica de peticiones HTTP) y el repositorio (que maneja el acceso a la base
 * de datos).
 * 
 * 🚀 Ventaja de usar interfaces:
 * - Separa la definición de la lógica de negocio de su implementación.
 * - Permite cambiar la implementación sin afectar al resto del sistema.
 * - Facilita pruebas (mocking en tests).
 */
public interface ActividadService {

    Page<Actividad> buscarMisActividades(Long colaboradorId, String titulo, int page, int size);

        List<Actividad> listarPorColaborador(Long idColaborador);

        Optional<Actividad> obtenerActividadDestacadaDeColaborador(Long idColaborador);

    /**
     * Obtiene una lista de actividades paginadas desde la base de datos,
     * con un filtro opcional por nombre.
     *
     * @param page         número de página (0 = primera página).
     * @param size         cantidad de elementos por página.
     * @param filtroNombre filtro opcional por el nombre de la actividad (puede ser
     *                     null).
     * @return una página (Page<Actividad>) con las actividades filtradas.
     * 
     *         👉 Se usa en la vista principal (dashboard de cliente).
     */
    Page<Actividad> getActividadesWithPaginationMain(int page, int size, String filtroNombre);

    /**
     * Obtiene una lista de actividades paginadas pertenecientes a un colaborador
     * específico,
     * con un filtro opcional por título.
     *
     * @param page          número de página.
     * @param size          cantidad de elementos por página.
     * @param idColaborador identificador del colaborador al que pertenecen las
     *                      actividades.
     * @param filtroTitulo  filtro opcional por título de la actividad.
     * @return una página con las actividades del colaborador filtradas.
     * 
     *         👉 Se usa cuando un colaborador ve/gestiona sus propias actividades.
     */
    Page<Actividad> getActividadesConPaginacionDeColaborador(int page, int size, Long idColaborador,
            String filtroTitulo);

    int ContadorActividadesPorCategoria(Long idCategoria);

    /**
     * Obtiene todas las actividades paginadas sin filtros adicionales.
     *
     * @param page número de página.
     * @param size cantidad de actividades por página.
     * @return una página con todas las actividades.
     * 
     *         👉 Se usa en listados generales de actividades.
     */
    Page<Actividad> getActividadesWithPagination(int page, int size);

    public Map<Long, Long> contarActividadesPorCategorias(List<Long> categoriaIds);

    /**
     * Lista todas las actividades sin paginación.
     *
     * @return una lista completa de actividades.
     * 
     *         👉 Útil cuando necesitamos todas las actividades de golpe,
     *         por ejemplo, para exportaciones o cargar menús desplegables.
     */
    List<Actividad> listarActividades();

    Actividad obtenerPorId(Long id);

    /**
     * Agrega una nueva actividad al sistema.
     *
     * @param actividad objeto Actividad a guardar.
     * @return la actividad guardada con su ID generado.
     * 
     *         👉 Se usa al registrar nuevas actividades.
     */
    Actividad agregarActividad(Actividad actividad);

    Actividad actualizarTitulo(Long id, String titulo);

    Actividad actualizarDescripcion(Long id, String descripcion);

    Actividad actualizarPrecio(Long id, BigDecimal precio);

    Actividad actualizarUbicacion(Long id, String ubicacion);

    /*
     * Actividad actualizarCategoria(Long id, Categoria categoria);
     * 
     * Actividad actualizarIdioma(Long id, Idioma idioma);
     */
    /**
     * Elimina una actividad por su ID.
     *
     * @param id identificador de la actividad a eliminar.
     * 
     *           👉 Importante para el CRUD completo de actividades.
     */
    void deleteActivity(long id);

    Long ContadorActividades();

    /**
     * Busca una actividad por su ID.
     *
     * @param id identificador de la actividad.
     * @return la actividad encontrada o null si no existe.
     * 
     *         👉 Se usa para mostrar detalles de una actividad en específico.
     */
    Actividad listarById(Long id);

    /* Actividad actualizarActividad(Long id, ActividadUpdateDto dto); */
    void actualizarActividad(Long id, ActividadUpdateDto dto, MultipartFile imagenFile) throws IOException;

    Page<Actividad> buscarActividadPorIdCategoria(Long idCategoria, int page, int size);

    Page<Actividad> buscarActividadesPorNombreDeCategoria(String nombreCategoria, int page, int size);

    Actividad buscarActividadPorNombreDeCategoria(String nombreCategoria);

    /**
     * Busca actividades con filtros dinámicos usando JPA Specifications.
     *
     * @param titulo      Filtro por título (búsqueda parcial, case-insensitive)
     * @param idiomaId    Filtro por ID de idioma
     * @param categoriaId Filtro por ID de categoría
     * @param precioMin   Precio mínimo
     * @param precioMax   Precio máximo
     * @param page        Número de página
     * @param size        Tamaño de página
     * @return Una página de actividades filtradas
     */
    Page<Actividad> buscarConFiltros(
            String titulo,
            Long idiomaId,
            Long categoriaId,
            BigDecimal precioMin,
            BigDecimal precioMax,
            int page,
            int size);

    void agregarImagenes(Long idActividad, List<MultipartFile> archivos) throws IOException;

    void eliminarImagen(Long idImagen);

    List<ImagenActividad> obtenerImagenesPorActividad(Long idActividad);

    /**
     * Incrementa los contadores de vistas y tendencia de una actividad.
     * 
     * @param idActividad identificador de la actividad
     */
    void incrementarContadores(Long idActividad);

    /**
     * Obtiene las actividades con más tendencia (vistas recientes)
     */
    List<Actividad> obtenerTendencias();

    /**
     * Obtiene las actividades con más vistas totales
     */
    List<Actividad> obtenerMasVistas();

    /**
     * Obtiene las actividades más reservadas
     */
    List<Actividad> obtenerMasReservadas();

    /**
     * Obtiene actividades personalizadas para el cliente
     */
    List<Actividad> obtenerParaTi(Cliente cliente);

}
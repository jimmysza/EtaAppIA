package maineta.eta.controller;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import maineta.eta.config.UsuarioHelper;
import maineta.eta.dto.ActividadDTO;
import maineta.eta.dto.CategoriaDTO;
import maineta.eta.dto.ReservaDTO;
import maineta.eta.entity.Actividad;
import maineta.eta.entity.BusquedaForm;
import maineta.eta.entity.Categoria;
import maineta.eta.entity.Comentario;
import maineta.eta.entity.Disponibilidad;
import maineta.eta.service.ActividadService;
import maineta.eta.service.CategoriaService;
import maineta.eta.service.ComentarioService;
import maineta.eta.service.DisponibilidadService;

// 🔹 Controlador principal que maneja las páginas accesibles para todos los usuarios
@Controller
public class AllAcessController {

    // Servicios para manejar la lógica de negocio de usuarios y actividades
    private final CategoriaService categoriaService;
    private final UsuarioHelper usuarioHelper;
    private final ActividadService actividadService;
    private final ComentarioService comentarioService;
    private final DisponibilidadService disponibilidadService;

    // 🔹 Constructor con inyección de dependencias
    public AllAcessController(DisponibilidadService disponibilidadService, UsuarioHelper usuarioHelper,
            ComentarioService comentarioService, ActividadService actividadService, CategoriaService categoriaService) {
        this.categoriaService = categoriaService;
        this.actividadService = actividadService;
        this.usuarioHelper = usuarioHelper;
        this.comentarioService = comentarioService;
        this.disponibilidadService = disponibilidadService;
    }

    // 🔹 Endpoint para mostrar la página de login
    @GetMapping("/login")
    public String iniciarSesion(@RequestParam(value = "role", required = false) String role, Model model) {
        // Agregamos el rol (si viene en la URL) al modelo para usarlo en la vista
        model.addAttribute("role", role);
        return "auth/login"; // Devuelve la vista login.html
    }

    @GetMapping("/403")
    public String deniedM() {
        return "error/403"; // Devuelve la vista login.html
    }

    @GetMapping("/detalle/{id}")
    public String verDetalleActividad(
            @PathVariable("id") Long id,
            Model model,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(required = false) String nombre,
            Authentication auth) { // 👈 solo necesitas pasar Authentication

        usuarioHelper.agregarInfoUsuarioModel(model, auth);

        Actividad actividad = actividadService.listarById(id);
        if (actividad == null) {
            return "redirect:/cliente/dashboard";
        }

        BigDecimal precioConsumidor = usuarioHelper.CalcularPrecioConsumidor(actividad.getPrecio());
        model.addAttribute("precioConsumidor", precioConsumidor);

        model.addAttribute("actividad", actividad);

        int pageSize = 3;
        Page<Actividad> actividadesPage = actividadService.getActividadesWithPaginationMain(page, pageSize, nombre);
        Page<Comentario> comentarioPage = comentarioService.listarComentarioPorIdYPaginacion(id, page, pageSize);
        List<Disponibilidad> disponibilidads = disponibilidadService.obtenerPorActividad(id);

        model.addAttribute("reservaDTO", new ReservaDTO());
        model.addAttribute("actividades", actividadesPage);
        model.addAttribute("comentarios", comentarioPage);
        model.addAttribute("currentPage", page);
        model.addAttribute("disponibilidades", disponibilidads);
        model.addAttribute("totalPages", comentarioPage.getTotalPages());
        model.addAttribute("filtroNombre", nombre);
        model.addAttribute("id", id);

        List<Integer> pageNumbers = IntStream.range(0, actividadesPage.getTotalPages())
                .boxed()
                .collect(Collectors.toList());
        model.addAttribute("pageNumbers", pageNumbers);
        model.addAttribute("pagina", "detalle");
        return "detalle-actividad";
    }

    @GetMapping("/")
    public String landingPage(Model model, Authentication auth,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(required = false) String nombre) {

        usuarioHelper.agregarInfoUsuarioModel(model, auth);

        int pageSize = 6;
        int pageSizeCategorias = 10;
        Page<Actividad> actividadesPage = actividadService
                .getActividadesWithPaginationMain(page, pageSize, nombre);

        Page<Categoria> categoriaPage = categoriaService.buscarTodasPorPaginacion(page, pageSizeCategorias);

        List<CategoriaDTO> categoriaDTOS = categoriaPage.stream().map(
                categoria -> {
                    CategoriaDTO dto = new CategoriaDTO();
                    dto.setNombre(categoria.getNombre());
                    dto.setCantidad(actividadService.ContadorActividadesPorCategoria(categoria.getIdCategoria()));
                    dto.setIdCategoria(categoria.getIdCategoria());
                    dto.setImagen(categoria.getImagen());
                    return dto;
                }).toList();

        // Construcción del DTO con el 18%
        List<ActividadDTO> actividadesDTO = actividadesPage.stream()
                .map(actividad -> {
                    ActividadDTO dto = new ActividadDTO();
                    dto.setIdActividad(actividad.getIdActividad());
                    dto.setTitulo(actividad.getTitulo());
                    dto.setDescripcion(actividad.getDescripcion());
                    dto.setCalificacion(actividad.getCalificacion());
                    dto.setUbicacion(actividad.getUbicacion());
                    dto.setImagen(actividad.getImagen());
                    dto.setIdIdioma(actividad.getIdioma().getIdIdioma());
                    dto.setNombreIdioma(actividad.getIdioma().getNombre());
                    dto.setCodigoIdioma(actividad.getIdioma().getCodigo());
                    dto.setCreatedAt(actividad.getCreatedAt());
                    dto.setCalificacion(actividad.getCalificacion());
                    dto.setCantidadComentario(
                            comentarioService.ContarComentariosPorActividad(actividad.getIdActividad()));
                    dto.setIdCategoria(
                            actividad.getCategoria() != null ? actividad.getCategoria().getIdCategoria() : null);
                    dto.setIdColaborador(
                            actividad.getColaborador() != null ? actividad.getColaborador().getIdColaborador() : null);
                    dto.setNombreCategoria(
                            actividad.getCategoria() != null ? actividad.getCategoria().getNombre() : "Sin categoría");

                    System.out.println("categoria => " + actividad.getCategoria());
                    System.out.println("colaborador => " + actividad.getColaborador());

                    // precios
                    dto.setPrecio(actividad.getPrecio());
                    dto.setPrecioConsumidor(usuarioHelper.CalcularPrecioConsumidor(actividad.getPrecio()));

                    return dto;
                })
                .toList();

        model.addAttribute("actividades", actividadesDTO); // ← YA NO SE SOBREESCRIBE

        model.addAttribute("busqueda", new BusquedaForm());
        model.addAttribute("actividad", new Actividad());
        model.addAttribute("currentPage", page);
        model.addAttribute("totalPages", actividadesPage.getTotalPages());
        model.addAttribute("filtroNombre", nombre);
        model.addAttribute("categorias", categoriaDTOS);
        model.addAttribute("pagina", "indice");

        List<Integer> pageNumbers = IntStream.range(0, actividadesPage.getTotalPages())
                .boxed()
                .collect(Collectors.toList());
        model.addAttribute("pageNumbers", pageNumbers);

        return "main";
    }

    // Pagina de terminos y condiciones
    @GetMapping("/terminos-condiciones")
    public String showTerminos(Model model) {
        model.addAttribute("pagina", "terminos");
        return "componentes/terminos-condiciones";
    }

    @PostMapping("/actividades/buscar")
    public String buscarActividades(
            @ModelAttribute("busqueda") BusquedaForm form,
            @RequestParam(defaultValue = "0") int page,
            Model model, Authentication auth) {

        String termino = form.getTitulo();
        int pageSize = 6;
        usuarioHelper.agregarInfoUsuarioModel(model, auth);

        Page<Actividad> actividadesPage;

        if (termino != null && !termino.trim().isEmpty()) {
            actividadesPage = actividadService.ObtenerActividadesPorTitulo(
                    termino.trim(),
                    PageRequest.of(page, pageSize) // ← ✔ correcto
            );
        } else {
            actividadesPage = actividadService.getActividadesWithPaginationMain(page, pageSize, null);
        }

        // Construcción del DTO con el 18%
        List<ActividadDTO> actividadesDTO = actividadesPage.stream()
                .map(actividad -> {
                    ActividadDTO dto = new ActividadDTO();
                    dto.setIdActividad(actividad.getIdActividad());
                    dto.setTitulo(actividad.getTitulo());
                    dto.setDescripcion(actividad.getDescripcion());
                    dto.setCalificacion(actividad.getCalificacion());
                    dto.setUbicacion(actividad.getUbicacion());
                    dto.setImagen(actividad.getImagen());
                    dto.setIdIdioma(actividad.getIdioma().getIdIdioma());
                    dto.setNombreIdioma(actividad.getIdioma().getNombre());
                    dto.setCodigoIdioma(actividad.getIdioma().getCodigo());
                    dto.setCreatedAt(actividad.getCreatedAt());
                    dto.setCalificacion(actividad.getCalificacion());
                    dto.setCantidadComentario(
                            comentarioService.ContarComentariosPorActividad(actividad.getIdActividad()));
                    dto.setIdCategoria(
                            actividad.getCategoria() != null ? actividad.getCategoria().getIdCategoria() : null);
                    dto.setIdColaborador(
                            actividad.getColaborador() != null ? actividad.getColaborador().getIdColaborador() : null);
                    dto.setNombreCategoria(
                            actividad.getCategoria() != null ? actividad.getCategoria().getNombre() : "Sin categoría");

                    System.out.println("categoria => " + actividad.getCategoria());
                    System.out.println("colaborador => " + actividad.getColaborador());

                    // precios
                    dto.setPrecio(actividad.getPrecio());
                    dto.setPrecioConsumidor(usuarioHelper.CalcularPrecioConsumidor(actividad.getPrecio()));

                    return dto;
                })
                .toList();

        model.addAttribute("busqueda", new BusquedaForm());
        model.addAttribute("actividades", actividadesDTO);
        model.addAttribute("currentPage", page);
        model.addAttribute("totalPages", actividadesPage.getTotalPages());
        model.addAttribute("filtroNombre", termino);

        List<Integer> pageNumbers = IntStream.range(0, actividadesPage.getTotalPages())
                .boxed()
                .collect(Collectors.toList());
        model.addAttribute("pageNumbers", pageNumbers);

        return "resultados-busqueda";
    }

    @GetMapping("/actividades/categoria/{nombreCategoria}")
    public String BuscarActividadesPorCategorias(
            @PathVariable("nombreCategoria") String nombreCategoria,
            Model model,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(required = false) String nombre,
            Authentication auth, RedirectAttributes redirectAttributes) {

        usuarioHelper.agregarInfoUsuarioModel(model, auth);

        Optional<Categoria> categoria = categoriaService.buscarCategoriaPorNombre(nombreCategoria);
        if (categoria == null) {
            redirectAttributes.addFlashAttribute("error", "No existe una Categoria Creada con ese id");

            return "redirect:/cliente/dashboard";
        }
        int pageSize = 6;

        Page<Actividad> actividadesCategorias = actividadService.buscarActividadesPorNombreDeCategoria(nombreCategoria,
                page, pageSize);
        model.addAttribute("busqueda", new BusquedaForm());

        model.addAttribute("actividades", actividadesCategorias);
        model.addAttribute("currentPage", page);
        model.addAttribute("totalPages", actividadesCategorias.getTotalPages());
        model.addAttribute("filtroNombre", nombreCategoria); // útil para mostrar "Resultados para: X"

        List<Integer> pageNumbers = IntStream.range(0, actividadesCategorias.getTotalPages())
                .boxed()
                .collect(Collectors.toList());
        model.addAttribute("pageNumbers", pageNumbers);
        return "resultados-busqueda";
        /* actividadService.buscarActividadPorIdCategoria(id,page); */
    }
}

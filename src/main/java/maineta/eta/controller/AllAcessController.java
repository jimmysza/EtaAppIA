package maineta.eta.controller;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import maineta.eta.config.UsuarioHelper;
import maineta.eta.dto.ActividadCercanaDTO;
import maineta.eta.dto.ActividadDTO;
import maineta.eta.dto.CategoriaDTO;
import maineta.eta.dto.ColaboradorPublicoDTO;
import maineta.eta.dto.ReservaDTO;
import maineta.eta.entity.Actividad;
import maineta.eta.entity.BusquedaForm;
import maineta.eta.entity.Categoria;
import maineta.eta.entity.Cliente;
import maineta.eta.entity.Colaborador;
import maineta.eta.entity.Comentario;
import maineta.eta.entity.Disponibilidad;
import maineta.eta.entity.Usuario;
import maineta.eta.service.ActividadService;
import maineta.eta.service.CategoriaService;
import maineta.eta.service.ClienteService;
import maineta.eta.service.ColaboradorService;
import maineta.eta.service.ComentarioService;
import maineta.eta.service.DisponibilidadService;
import maineta.eta.service.FavoritoService;
import maineta.eta.service.IdiomaService;
import maineta.eta.service.UsuarioService;

// 🔹 Controlador principal que maneja las páginas accesibles para todos los usuarios

@Controller
public class AllAcessController {

        // Servicios para manejar la lógica de negocio de usuarios y actividades
        private final CategoriaService categoriaService;
        private final UsuarioHelper usuarioHelper;
        private final ActividadService actividadService;
        private final ComentarioService comentarioService;
        private final DisponibilidadService disponibilidadService;
        private final IdiomaService idiomaService;
        private final FavoritoService favoritoService;
        private final ClienteService clienteService;
        private final UsuarioService usuarioService;
        private final ColaboradorService colaboradorService;

        // 🔹 Constructor con inyección de dependencias
        public AllAcessController(DisponibilidadService disponibilidadService, UsuarioHelper usuarioHelper,
                        ComentarioService comentarioService, ActividadService actividadService,
                        CategoriaService categoriaService, IdiomaService idiomaService,
                        FavoritoService favoritoService, ClienteService clienteService,
                        UsuarioService usuarioService, ColaboradorService colaboradorService) {
                this.categoriaService = categoriaService;
                this.actividadService = actividadService;
                this.usuarioHelper = usuarioHelper;
                this.comentarioService = comentarioService;
                this.disponibilidadService = disponibilidadService;
                this.idiomaService = idiomaService;
                this.favoritoService = favoritoService;
                this.clienteService = clienteService;
                this.usuarioService = usuarioService;
                this.colaboradorService = colaboradorService;
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

        @GetMapping("/actividad/{slug}-{id}")
        public String verDetalleActividad(
                        @PathVariable("slug") String slug,
                        @PathVariable("id") Long id,
                        Model model,
                        Authentication auth,
                        @RequestParam(name = "comentariosPage", defaultValue = "0") int comentariosPage,
                        @RequestParam(required = false) Integer anioReserva,
                        @RequestParam(required = false) Integer mesReserva,
                        @RequestParam(required = false) String fechaReserva,
                        @RequestParam(required = false) String nombre
                ) {
                // 👈 solo necesitas pasar Authentication

                usuarioHelper.agregarInfoUsuarioModel(model, auth);

                Actividad actividad = actividadService.listarById(id);
                if (actividad == null) {
                        return "redirect:/cliente/dashboard";
                }

                // Incrementar contadores de vistas y tendencia
                actividadService.incrementarContadores(id);

                BigDecimal precioConsumidor = usuarioHelper.CalcularPrecioConsumidor(actividad.getPrecio());
                model.addAttribute("precioConsumidor", precioConsumidor);

                model.addAttribute("actividad", actividad);

                int pageSize = 3;
                Page<Actividad> actividadesPage = actividadService.getActividadesWithPaginationMain(0, pageSize,
                                nombre);
                Page<Comentario> comentarioPage = comentarioService.listarComentarioPorIdYPaginacion(id, comentariosPage,
                                pageSize);
                List<Disponibilidad> disponibilidads = disponibilidadService.obtenerPorActividad(id);
                List<Disponibilidad> disponibilidadesDisponibles = disponibilidads.stream()
                                .filter(disponibilidad -> "DISPONIBLE".equalsIgnoreCase(disponibilidad.getEstado())
                                                && disponibilidad.getCuposDisponibles() > 0)
                                .toList();
                TreeSet<LocalDate> fechasDisponibles = disponibilidadesDisponibles.stream()
                                .map(Disponibilidad::getFecha)
                                .filter(java.util.Objects::nonNull)
                                .collect(Collectors.toCollection(TreeSet::new));

                LocalDate fechaReservaSeleccionada = null;
                if (fechaReserva != null && !fechaReserva.isBlank()) {
                        fechaReservaSeleccionada = LocalDate.parse(fechaReserva);
                }

                YearMonth calendarioReserva;
                if (anioReserva != null && mesReserva != null) {
                        calendarioReserva = YearMonth.of(anioReserva, mesReserva);
                } else if (fechaReservaSeleccionada != null) {
                        calendarioReserva = YearMonth.from(fechaReservaSeleccionada);
                } else if (!fechasDisponibles.isEmpty()) {
                        calendarioReserva = YearMonth.from(fechasDisponibles.first());
                } else {
                        calendarioReserva = YearMonth.now();
                }

                Locale localeEs = Locale.forLanguageTag("es-CO");
                Map<Integer, String> diasDisponiblesMap = fechasDisponibles.stream()
                                .filter(fechaDisponible -> YearMonth.from(fechaDisponible).equals(calendarioReserva))
                                .collect(Collectors.toMap(
                                                LocalDate::getDayOfMonth,
                                                LocalDate::toString,
                                                (actual, ignored) -> actual,
                                                LinkedHashMap::new));

                if (fechaReservaSeleccionada == null
                                || !YearMonth.from(fechaReservaSeleccionada).equals(calendarioReserva)
                                || !diasDisponiblesMap.containsValue(fechaReservaSeleccionada.toString())) {
                        fechaReservaSeleccionada = !diasDisponiblesMap.isEmpty()
                                        ? LocalDate.parse(diasDisponiblesMap.values().iterator().next())
                                        : null;
                }

                model.addAttribute("reservaDTO", new ReservaDTO());
                model.addAttribute("actividades", actividadesPage);
                model.addAttribute("comentarios", comentarioPage);
                model.addAttribute("currentPage", comentariosPage);
                model.addAttribute("comentariosCurrentPage", comentariosPage);
                model.addAttribute("disponibilidades", disponibilidads);
                model.addAttribute("disponibilidadesDisponibles", disponibilidadesDisponibles);
                model.addAttribute("diasDisponiblesMap", diasDisponiblesMap);
                model.addAttribute("fechaReservaSeleccionada", fechaReservaSeleccionada);
                model.addAttribute("mesReserva", calendarioReserva.getMonthValue());
                model.addAttribute("anioReserva", calendarioReserva.getYear());
                model.addAttribute("primerDiaReservaSemana", calendarioReserva.atDay(1).getDayOfWeek().getValue());
                model.addAttribute("diasEnMesReserva", calendarioReserva.lengthOfMonth());
                model.addAttribute("mesReservaNombre", calendarioReserva.getMonth()
                                .getDisplayName(java.time.format.TextStyle.FULL, localeEs));
                model.addAttribute("prevMesReserva", calendarioReserva.minusMonths(1).getMonthValue());
                model.addAttribute("prevAnioReserva", calendarioReserva.minusMonths(1).getYear());
                model.addAttribute("nextMesReserva", calendarioReserva.plusMonths(1).getMonthValue());
                model.addAttribute("nextAnioReserva", calendarioReserva.plusMonths(1).getYear());
                model.addAttribute("totalPages", comentarioPage.getTotalPages());
                model.addAttribute("comentariosTotalPages", comentarioPage.getTotalPages());
                model.addAttribute("filtroNombre", nombre);
                model.addAttribute("id", id);

                List<Integer> pageNumbers = IntStream.range(0, actividadesPage.getTotalPages())
                                .boxed()
                                .collect(Collectors.toList());
                model.addAttribute("pageNumbers", pageNumbers);
                model.addAttribute("comentariosPageNumbers", IntStream.range(0, comentarioPage.getTotalPages())
                                .boxed()
                                .collect(Collectors.toList()));
                model.addAttribute("pagina", "detalle");
                model.addAttribute("imagenes", actividadService.obtenerImagenesPorActividad(id));

                // Datos dinámicos para la sección de reseñas
                Double promedioCalificacion = comentarioService.calcularPromedioDecimal(id);
                Map<Integer, Long> distribucionEstrellas = comentarioService.obtenerDistribucionEstrellas(id);
                long totalComentarios = comentarioPage.getTotalElements();

                model.addAttribute("promedioCalificacion", promedioCalificacion);
                model.addAttribute("distribucionEstrellas", distribucionEstrellas);
                model.addAttribute("totalComentarios", totalComentarios);

                // Verificar si la actividad es favorita del cliente logueado
                boolean esFavorito = false;
                if (auth != null && auth.isAuthenticated() && !auth.getPrincipal().equals("anonymousUser")) {
                        try {
                                Usuario usuario = usuarioService.obtenerPorEmail(auth.getName());
                                Optional<Cliente> clienteOpt = clienteService.obtenerPorUsuario(usuario);
                                if (clienteOpt.isPresent()) {
                                        esFavorito = favoritoService.esFavorito(clienteOpt.get(), actividad);
                                }
                        } catch (Exception e) {
                                // No es cliente, ignorar
                        }
                }
                model.addAttribute("esFavorito", esFavorito);

                return "detalle-actividad";
        }

        @GetMapping("/")
        public String landingPage(
                        Model model,
                        Authentication auth,
                        @RequestParam(defaultValue = "0") int page,
                        @RequestParam(required = false) String nombre) {

                usuarioHelper.agregarInfoUsuarioModel(model, auth);

                int pageSize = 6;
                int pageSizeCategorias = 10;

                Page<Actividad> actividadesPage = actividadService.getActividadesWithPaginationMain(page, pageSize,
                                nombre);

                Page<Categoria> categoriaPage = categoriaService.buscarTodasPorPaginacion(page, pageSizeCategorias);
                List<Categoria> categoriasVisibles = categoriaPage.getContent();
                Set<Long> categoriasVisiblesIds = categoriasVisibles.stream()
                                .map(Categoria::getIdCategoria)
                                .collect(Collectors.toSet());
                List<Categoria> categoriasModal = categoriaService.listarCategorias().stream()
                                .filter(categoria -> !categoriasVisiblesIds.contains(categoria.getIdCategoria()))
                                .toList();
                /*
                 * =========================
                 * IDs necesarios
                 * =========================
                 */

                List<Long> actividadIds = actividadesPage.stream()
                                .map(Actividad::getIdActividad)
                                .toList();

                List<Long> categoriaIds = categoriaPage.stream()
                                .map(Categoria::getIdCategoria)
                                .toList();

                /*
                 * =========================
                 * Conteos en UNA query
                 * =========================
                 */

                Map<Long, Integer> comentariosPorActividad = comentarioService
                                .contarComentariosPorActividades(actividadIds);

                Map<Long, Long> actividadesPorCategoria = actividadService.contarActividadesPorCategorias(categoriaIds);

                /*
                 * =========================
                 * Categorías DTO
                 * =========================
                 */

                List<CategoriaDTO> categoriaDTOS = categoriaPage.stream()
                                .map(categoria -> {
                                        CategoriaDTO dto = new CategoriaDTO();
                                        dto.setIdCategoria(categoria.getIdCategoria());
                                        dto.setNombre(categoria.getNombre());
                                        dto.setImagen(categoria.getImagen());

                                        dto.setCantidad(
                                                        actividadesPorCategoria
                                                                        .getOrDefault(categoria.getIdCategoria(), 0L)
                                                                        .intValue());

                                        return dto;
                                })
                                .toList();

                /*
                 * =========================
                 * Actividades DTO
                 * =========================
                 */

                List<ActividadDTO> actividadesDTO = actividadesPage.stream()
                                .map(actividad -> {
                                        ActividadDTO dto = new ActividadDTO();

                                        dto.setIdActividad(actividad.getIdActividad());
                                        dto.setTitulo(actividad.getTitulo());
                                        dto.setDescripcion(actividad.getDescripcion());
                                        dto.setCalificacion(actividad.getCalificacion());
                                        dto.setUbicacion(actividad.getUbicacion());
                                        dto.setImagen(actividad.getImagen());
                                        dto.setCreatedAt(actividad.getCreatedAt());

                                        dto.setIdIdioma(actividad.getIdioma().getIdIdioma());
                                        dto.setNombreIdioma(actividad.getIdioma().getNombre());
                                        dto.setCodigoIdioma(actividad.getIdioma().getCodigo());

                                        dto.setCantidadComentario(
                                                        comentariosPorActividad.getOrDefault(
                                                                        actividad.getIdActividad(), 0));

                                        if (actividad.getCategoria() != null) {
                                                dto.setIdCategoria(actividad.getCategoria().getIdCategoria());
                                                dto.setNombreCategoria(actividad.getCategoria().getNombre());
                                        } else {
                                                dto.setNombreCategoria("Sin categoría");
                                        }

                                        if (actividad.getColaborador() != null) {
                                                dto.setIdColaborador(actividad.getColaborador().getIdColaborador());
                                        }

                                        dto.setPrecio(actividad.getPrecio());
                                        dto.setPrecioConsumidor(
                                                        usuarioHelper.CalcularPrecioConsumidor(actividad.getPrecio()));

                                        return dto;
                                })
                                .toList();

                /*
                 * =========================
                 * Model
                 * =========================
                 */

                model.addAttribute("actividades", actividadesDTO);
                model.addAttribute("categorias", categoriaDTOS);
                model.addAttribute("idiomas", idiomaService.listarIdiomas());
                model.addAttribute("busqueda", new BusquedaForm());
                model.addAttribute("actividad", new Actividad());
                model.addAttribute("anfitrionesDestacados", colaboradorService.obtenerDestacadosPorReservas(4));
                model.addAttribute("currentPage", page);
                model.addAttribute("totalPages", actividadesPage.getTotalPages());
                model.addAttribute("filtroNombre", nombre);
                model.addAttribute("pagina", "indice");

                List<Integer> pageNumbers = IntStream
                                .range(0, actividadesPage.getTotalPages())
                                .boxed()
                                .toList();

                model.addAttribute("pageNumbers", pageNumbers);

                // Favoritos del cliente logueado
                Set<Long> favoritosIds = Collections.emptySet();
                if (auth != null && auth.isAuthenticated() && !auth.getPrincipal().equals("anonymousUser")) {
                        try {
                                Usuario usuario = usuarioService.obtenerPorEmail(auth.getName());
                                Optional<Cliente> clienteOpt = clienteService.obtenerPorUsuario(usuario);
                                if (clienteOpt.isPresent()) {
                                        favoritosIds = favoritoService.obtenerIdsFavoritosDeCliente(clienteOpt.get());
                                }
                        } catch (Exception e) {
                                // No es cliente, ignorar
                        }
                }
                model.addAttribute("favoritosIds", favoritosIds);
                model.addAttribute("tendencias", actividadService.obtenerTendencias());
                                        model.addAttribute("masVistas", actividadService.obtenerMasVistas());
                                        model.addAttribute("masReservadas", actividadService.obtenerMasReservadas());
                // Si el usuario es cliente autenticado y completó onboarding, agregar listas personalizadas
                if (auth != null && auth.isAuthenticated() && !auth.getPrincipal().equals("anonymousUser")) {
                        try {
                                Usuario usuario = usuarioService.obtenerPorEmail(auth.getName());
                                Optional<Cliente> clienteOpt = clienteService.obtenerPorUsuario(usuario);
                                if (clienteOpt.isPresent() && clienteOpt.get().isOnboardingCompletado()) {
                                        Cliente cliente = clienteOpt.get();
                                        
                                        model.addAttribute("paraTi", actividadService.obtenerParaTi(cliente));
                                }
                        } catch (Exception e) {
                                // No es cliente o no completó onboarding, no mostrar listas personalizadas
                        }
                }

                return "main";
        }

        @GetMapping("/colaboradores/{idColaborador}")
        public String verPerfilPublicoColaborador(
                        @PathVariable Long idColaborador,
                        Model model,
                        Authentication auth) {

                usuarioHelper.agregarInfoUsuarioModel(model, auth);

                Colaborador colaborador = colaboradorService.obtenerPorId(idColaborador)
                                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));

                ColaboradorPublicoDTO perfilColaborador = colaboradorService.obtenerResumenPublico(idColaborador)
                                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));

                List<Actividad> actividadesColaborador = actividadService.listarPorColaborador(idColaborador);
                List<Long> actividadIds = actividadesColaborador.stream()
                                .map(Actividad::getIdActividad)
                                .toList();

                Map<Long, Integer> comentariosPorActividad = actividadIds.isEmpty()
                                ? Collections.emptyMap()
                                : comentarioService.contarComentariosPorActividades(actividadIds);

                List<ActividadDTO> actividadesDTO = actividadesColaborador.stream()
                                .map(actividad -> {
                                        ActividadDTO dto = new ActividadDTO();
                                        dto.setIdActividad(actividad.getIdActividad());
                                        dto.setTitulo(actividad.getTitulo());
                                        dto.setDescripcion(actividad.getDescripcion());
                                        dto.setCalificacion(actividad.getCalificacion());
                                        dto.setUbicacion(actividad.getUbicacion());
                                        dto.setImagen(actividad.getImagen());
                                        dto.setCreatedAt(actividad.getCreatedAt());
                                        dto.setPrecio(actividad.getPrecio());
                                        dto.setPrecioConsumidor(usuarioHelper.CalcularPrecioConsumidor(actividad.getPrecio()));

                                        if (actividad.getIdioma() != null) {
                                                dto.setIdIdioma(actividad.getIdioma().getIdIdioma());
                                                dto.setNombreIdioma(actividad.getIdioma().getNombre());
                                                dto.setCodigoIdioma(actividad.getIdioma().getCodigo());
                                        }

                                        if (actividad.getCategoria() != null) {
                                                dto.setIdCategoria(actividad.getCategoria().getIdCategoria());
                                                dto.setNombreCategoria(actividad.getCategoria().getNombre());
                                        } else {
                                                dto.setNombreCategoria("Sin categoría");
                                        }

                                        dto.setIdColaborador(idColaborador);
                                        dto.setCantidadComentario(
                                                        comentariosPorActividad.getOrDefault(actividad.getIdActividad(), 0));

                                        return dto;
                                })
                                .toList();

                model.addAttribute("perfilColaborador", perfilColaborador);
                model.addAttribute("colaborador", colaborador);
                model.addAttribute("actividades", actividadesDTO);
                model.addAttribute("favoritosIds", obtenerFavoritosIds(auth));
                model.addAttribute("pagina", "perfil-colaborador");

                return "perfil-colaborador";
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
                        @RequestParam(required = false) Long idiomaId,
                        @RequestParam(required = false) Long categoriaId,
                        @RequestParam(required = false) BigDecimal precioMin,
                        @RequestParam(required = false) BigDecimal precioMax,
                        Model model,
                        Authentication auth) {

                // Redirigir al GET con todos los parámetros para permitir URLs compartibles
                String redirectUrl = "redirect:/actividades/buscar?";
                
                if (form.getTitulo() != null && !form.getTitulo().trim().isEmpty()) {
                        redirectUrl += "nombre=" + form.getTitulo().trim();
                }
                if (idiomaId != null) {
                        redirectUrl += "&idiomaId=" + idiomaId;
                }
                if (categoriaId != null) {
                        redirectUrl += "&categoriaId=" + categoriaId;
                }
                if (precioMin != null) {
                        redirectUrl += "&precioMin=" + precioMin;
                }
                if (precioMax != null) {
                        redirectUrl += "&precioMax=" + precioMax;
                }
                if (page > 0) {
                        redirectUrl += "&page=" + page;
                }
                
                return redirectUrl;
        }

        @GetMapping("/actividades/buscar")
        public String buscarActividadesGet(
                        @RequestParam(required = false) String nombre,
                        @RequestParam(required = false) Long idiomaId,
                        @RequestParam(required = false) Long categoriaId,
                        @RequestParam(required = false) BigDecimal precioMin,
                        @RequestParam(required = false) BigDecimal precioMax,
                        @RequestParam(defaultValue = "0") int page,
                        Model model,
                        Authentication auth) {

                usuarioHelper.agregarInfoUsuarioModel(model, auth);

                int pageSize = 6;
                int pageSizeCategorias = 8;

                // Usar el nuevo método con filtros
                Page<Actividad> actividadesPage = actividadService.buscarConFiltros(
                                nombre,
                                idiomaId,
                                categoriaId,
                                precioMin,
                                precioMax,
                                page,
                                pageSize);

                /*
                 * ===============================
                 * 🔹 Obtener IDs de actividades
                 * ===============================
                 */
                List<Long> actividadIds = actividadesPage.stream()
                                .map(Actividad::getIdActividad)
                                .toList();

                /*
                 * ======================================
                 * 🔹 Conteo de comentarios en una query
                 * ======================================
                 */
                Map<Long, Integer> comentariosPorActividad = comentarioService
                                .contarComentariosPorActividades(actividadIds);

                /*
                 * ===============================
                 * 🔹 Mapear a DTO (SIN N+1)
                 * ===============================
                 */
                List<ActividadDTO> actividadesDTO = actividadesPage.map(actividad -> {
                        ActividadDTO dto = new ActividadDTO();

                        dto.setIdActividad(actividad.getIdActividad());
                        dto.setTitulo(actividad.getTitulo());
                        dto.setDescripcion(actividad.getDescripcion());
                        dto.setCalificacion(actividad.getCalificacion());
                        dto.setUbicacion(actividad.getUbicacion());
                        dto.setImagen(actividad.getImagen());
                        dto.setCreatedAt(actividad.getCreatedAt());

                        // Idioma
                        dto.setIdIdioma(actividad.getIdioma().getIdIdioma());
                        dto.setNombreIdioma(actividad.getIdioma().getNombre());
                        dto.setCodigoIdioma(actividad.getIdioma().getCodigo());

                        // Categoría
                        dto.setIdCategoria(
                                        actividad.getCategoria() != null
                                                        ? actividad.getCategoria().getIdCategoria()
                                                        : null);
                        dto.setNombreCategoria(
                                        actividad.getCategoria() != null
                                                        ? actividad.getCategoria().getNombre()
                                                        : "Sin categoría");

                        // Colaborador
                        dto.setIdColaborador(
                                        actividad.getColaborador() != null
                                                        ? actividad.getColaborador().getIdColaborador()
                                                        : null);

                        // Comentarios (O(1))
                        dto.setCantidadComentario(
                                        comentariosPorActividad.getOrDefault(
                                                        actividad.getIdActividad(), 0));

                        // Precios
                        dto.setPrecio(actividad.getPrecio());
                        dto.setPrecioConsumidor(
                                        usuarioHelper.CalcularPrecioConsumidor(actividad.getPrecio()));

                        return dto;
                }).getContent();

                Page<Categoria> categoriaPage = categoriaService.buscarTodasPorPaginacion(page, pageSizeCategorias);
                List<Categoria> categoriasVisiblesBusqueda = categoriaPage.getContent();
                Set<Long> categoriasVisiblesBusquedaIds = categoriasVisiblesBusqueda.stream()
                                .map(Categoria::getIdCategoria)
                                .collect(Collectors.toSet());
                List<Categoria> categoriasModal = categoriaService.listarCategorias().stream()
                                .filter(categoria -> !categoriasVisiblesBusquedaIds.contains(categoria.getIdCategoria()))
                                .toList();

                /*
                 * ===============================
                 * 🔹 Model - Datos para la vista
                 * ===============================
                 */
                model.addAttribute("busqueda", new BusquedaForm());
                model.addAttribute("actividades", actividadesDTO);
                model.addAttribute("categorias", categoriaPage);
                model.addAttribute("categoriasModal", categoriasModal);
                model.addAttribute("idiomas", idiomaService.listarIdiomas());
                model.addAttribute("currentPage", page);
                model.addAttribute("totalPages", actividadesPage.getTotalPages());

                // Filtros actuales para mantenerlos en la vista
                model.addAttribute("filtroNombre", nombre);
                model.addAttribute("filtroIdiomaId", idiomaId);
                model.addAttribute("filtroCategoriaId", categoriaId);
                model.addAttribute("filtroPrecioMin", precioMin);
                model.addAttribute("filtroPrecioMax", precioMax);

                List<Integer> pageNumbers = IntStream
                                .range(0, actividadesPage.getTotalPages())
                                .boxed()
                                .toList();

                model.addAttribute("pageNumbers", pageNumbers);

                return "resultados-busqueda";
        }

        @GetMapping("/actividades/categoria/{nombreCategoria}")
        public String BuscarActividadesPorCategorias(
                        @PathVariable("nombreCategoria") String nombreCategoria,
                        Model model,
                        @RequestParam(defaultValue = "0") int page,
                        Authentication auth,
                        RedirectAttributes redirectAttributes) {

                usuarioHelper.agregarInfoUsuarioModel(model, auth);

                Optional<Categoria> categoriaOpt = categoriaService.buscarCategoriaPorNombre(nombreCategoria);

                if (categoriaOpt.isEmpty()) {
                        redirectAttributes.addFlashAttribute(
                                        "error",
                                        "No existe una categoría con ese nombre");
                        return "redirect:/cliente/dashboard";
                }

                int pageSize = 6;

                Page<Actividad> actividadesPage = actividadService.buscarActividadesPorNombreDeCategoria(
                                nombreCategoria,
                                page,
                                pageSize);

                /*
                 * ===============================
                 * 🔹 Obtener IDs de actividades
                 * ===============================
                 */
                List<Long> actividadIds = actividadesPage.stream()
                                .map(Actividad::getIdActividad)
                                .toList();

                /*
                 * ======================================
                 * 🔹 Conteo de comentarios en una query
                 * ======================================
                 */
                Map<Long, Integer> comentariosPorActividad = comentarioService
                                .contarComentariosPorActividades(actividadIds);

                /*
                 * ===============================
                 * 🔹 Mapear a DTO
                 * ===============================
                 */
                List<ActividadDTO> actividadesDTO = actividadesPage.stream()
                                .map(actividad -> {
                                        ActividadDTO dto = new ActividadDTO();

                                        dto.setIdActividad(actividad.getIdActividad());
                                        dto.setTitulo(actividad.getTitulo());
                                        dto.setDescripcion(actividad.getDescripcion());
                                        dto.setCalificacion(actividad.getCalificacion());
                                        dto.setUbicacion(actividad.getUbicacion());
                                        dto.setImagen(actividad.getImagen());
                                        dto.setCreatedAt(actividad.getCreatedAt());

                                        // Idioma
                                        dto.setIdIdioma(actividad.getIdioma().getIdIdioma());
                                        dto.setNombreIdioma(actividad.getIdioma().getNombre());
                                        dto.setCodigoIdioma(actividad.getIdioma().getCodigo());

                                        // Categoría
                                        if (actividad.getCategoria() != null) {
                                                dto.setIdCategoria(actividad.getCategoria().getIdCategoria());
                                                dto.setNombreCategoria(actividad.getCategoria().getNombre());
                                        } else {
                                                dto.setNombreCategoria("Sin categoría");
                                        }

                                        // Colaborador
                                        if (actividad.getColaborador() != null) {
                                                dto.setIdColaborador(
                                                                actividad.getColaborador().getIdColaborador());
                                        }

                                        // Comentarios (O(1))
                                        dto.setCantidadComentario(
                                                        comentariosPorActividad.getOrDefault(
                                                                        actividad.getIdActividad(), 0));

                                        // Precios
                                        dto.setPrecio(actividad.getPrecio());
                                        dto.setPrecioConsumidor(
                                                        usuarioHelper.CalcularPrecioConsumidor(
                                                                        actividad.getPrecio()));

                                        return dto;
                                })
                                .toList();
                int pageSizeCategorias = 8;
                Page<Categoria> categoriaPage = categoriaService.buscarTodasPorPaginacion(page, pageSizeCategorias);
                /*
                 * ===============================
                 * 🔹 Model
                 * ===============================
                 */
                model.addAttribute("busqueda", new BusquedaForm());
                model.addAttribute("actividades", actividadesDTO);
                model.addAttribute("currentPage", page);
                model.addAttribute("totalPages", actividadesPage.getTotalPages());
                model.addAttribute("filtroNombre", nombreCategoria);
                model.addAttribute("categorias", categoriaPage);
                List<Integer> pageNumbers = IntStream
                                .range(0, actividadesPage.getTotalPages())
                                .boxed()
                                .toList();

                model.addAttribute("pageNumbers", pageNumbers);

                return "resultados-busqueda";
        }

        @GetMapping("/actividades/tendencias")
        public String verTodasTendencias(
                        @RequestParam(defaultValue = "0") int page,
                        Model model,
                        Authentication auth) {

                usuarioHelper.agregarInfoUsuarioModel(model, auth);

                int pageSize = 12;
                int pageSizeCategorias = 8;

                Page<Actividad> actividadesPage = actividadService.obtenerTodasTendencias(page, pageSize);

                List<Long> actividadIds = actividadesPage.stream()
                                .map(Actividad::getIdActividad)
                                .toList();

                Map<Long, Integer> comentariosPorActividad = comentarioService
                                .contarComentariosPorActividades(actividadIds);

                List<ActividadDTO> actividadesDTO = actividadesPage.stream()
                                .map(actividad -> {
                                        ActividadDTO dto = new ActividadDTO();
                                        dto.setIdActividad(actividad.getIdActividad());
                                        dto.setTitulo(actividad.getTitulo());
                                        dto.setDescripcion(actividad.getDescripcion());
                                        dto.setCalificacion(actividad.getCalificacion());
                                        dto.setUbicacion(actividad.getUbicacion());
                                        dto.setImagen(actividad.getImagen());
                                        dto.setCreatedAt(actividad.getCreatedAt());

                                        dto.setIdIdioma(actividad.getIdioma().getIdIdioma());
                                        dto.setNombreIdioma(actividad.getIdioma().getNombre());
                                        dto.setCodigoIdioma(actividad.getIdioma().getCodigo());

                                        if (actividad.getCategoria() != null) {
                                                dto.setIdCategoria(actividad.getCategoria().getIdCategoria());
                                                dto.setNombreCategoria(actividad.getCategoria().getNombre());
                                        } else {
                                                dto.setNombreCategoria("Sin categoría");
                                        }

                                        if (actividad.getColaborador() != null) {
                                                dto.setIdColaborador(actividad.getColaborador().getIdColaborador());
                                        }

                                        dto.setCantidadComentario(
                                                        comentariosPorActividad.getOrDefault(actividad.getIdActividad(), 0));

                                        dto.setPrecio(actividad.getPrecio());
                                        dto.setPrecioConsumidor(usuarioHelper.CalcularPrecioConsumidor(actividad.getPrecio()));

                                        return dto;
                                })
                                .toList();

                Page<Categoria> categoriaPage = categoriaService.buscarTodasPorPaginacion(page, pageSizeCategorias);

                model.addAttribute("busqueda", new BusquedaForm());
                model.addAttribute("actividades", actividadesDTO);
                model.addAttribute("categorias", categoriaPage);
                model.addAttribute("idiomas", idiomaService.listarIdiomas());
                model.addAttribute("currentPage", page);
                model.addAttribute("totalPages", actividadesPage.getTotalPages());
                model.addAttribute("filtroNombre", "Actividades Populares");
                model.addAttribute("favoritosIds", obtenerFavoritosIds(auth));

                List<Integer> pageNumbers = IntStream.range(0, actividadesPage.getTotalPages()).boxed().toList();
                model.addAttribute("pageNumbers", pageNumbers);

                return "resultados-busqueda";
        }

        @GetMapping("/actividades/mas-vistas")
        public String verTodasMasVistas(
                        @RequestParam(defaultValue = "0") int page,
                        Model model,
                        Authentication auth) {

                usuarioHelper.agregarInfoUsuarioModel(model, auth);

                int pageSize = 12;
                int pageSizeCategorias = 8;

                Page<Actividad> actividadesPage = actividadService.obtenerTodasMasVistas(page, pageSize);

                List<Long> actividadIds = actividadesPage.stream()
                                .map(Actividad::getIdActividad)
                                .toList();

                Map<Long, Integer> comentariosPorActividad = comentarioService
                                .contarComentariosPorActividades(actividadIds);

                List<ActividadDTO> actividadesDTO = actividadesPage.stream()
                                .map(actividad -> {
                                        ActividadDTO dto = new ActividadDTO();
                                        dto.setIdActividad(actividad.getIdActividad());
                                        dto.setTitulo(actividad.getTitulo());
                                        dto.setDescripcion(actividad.getDescripcion());
                                        dto.setCalificacion(actividad.getCalificacion());
                                        dto.setUbicacion(actividad.getUbicacion());
                                        dto.setImagen(actividad.getImagen());
                                        dto.setCreatedAt(actividad.getCreatedAt());

                                        dto.setIdIdioma(actividad.getIdioma().getIdIdioma());
                                        dto.setNombreIdioma(actividad.getIdioma().getNombre());
                                        dto.setCodigoIdioma(actividad.getIdioma().getCodigo());

                                        if (actividad.getCategoria() != null) {
                                                dto.setIdCategoria(actividad.getCategoria().getIdCategoria());
                                                dto.setNombreCategoria(actividad.getCategoria().getNombre());
                                        } else {
                                                dto.setNombreCategoria("Sin categoría");
                                        }

                                        if (actividad.getColaborador() != null) {
                                                dto.setIdColaborador(actividad.getColaborador().getIdColaborador());
                                        }

                                        dto.setCantidadComentario(
                                                        comentariosPorActividad.getOrDefault(actividad.getIdActividad(), 0));

                                        dto.setPrecio(actividad.getPrecio());
                                        dto.setPrecioConsumidor(usuarioHelper.CalcularPrecioConsumidor(actividad.getPrecio()));

                                        return dto;
                                })
                                .toList();

                Page<Categoria> categoriaPage = categoriaService.buscarTodasPorPaginacion(page, pageSizeCategorias);

                model.addAttribute("busqueda", new BusquedaForm());
                model.addAttribute("actividades", actividadesDTO);
                model.addAttribute("categorias", categoriaPage);
                model.addAttribute("idiomas", idiomaService.listarIdiomas());
                model.addAttribute("currentPage", page);
                model.addAttribute("totalPages", actividadesPage.getTotalPages());
                model.addAttribute("filtroNombre", "Actividades Más Vistas");
                model.addAttribute("favoritosIds", obtenerFavoritosIds(auth));

                List<Integer> pageNumbers = IntStream.range(0, actividadesPage.getTotalPages()).boxed().toList();
                model.addAttribute("pageNumbers", pageNumbers);

                return "resultados-busqueda";
        }

        @GetMapping("/actividades/mas-reservadas")
        public String verTodasMasReservadas(
                        @RequestParam(defaultValue = "0") int page,
                        Model model,
                        Authentication auth) {

                usuarioHelper.agregarInfoUsuarioModel(model, auth);

                int pageSize = 12;
                int pageSizeCategorias = 8;

                Page<Actividad> actividadesPage = actividadService.obtenerTodasMasReservadas(page, pageSize);

                List<Long> actividadIds = actividadesPage.stream()
                                .map(Actividad::getIdActividad)
                                .toList();

                Map<Long, Integer> comentariosPorActividad = comentarioService
                                .contarComentariosPorActividades(actividadIds);

                List<ActividadDTO> actividadesDTO = actividadesPage.stream()
                                .map(actividad -> {
                                        ActividadDTO dto = new ActividadDTO();
                                        dto.setIdActividad(actividad.getIdActividad());
                                        dto.setTitulo(actividad.getTitulo());
                                        dto.setDescripcion(actividad.getDescripcion());
                                        dto.setCalificacion(actividad.getCalificacion());
                                        dto.setUbicacion(actividad.getUbicacion());
                                        dto.setImagen(actividad.getImagen());
                                        dto.setCreatedAt(actividad.getCreatedAt());

                                        dto.setIdIdioma(actividad.getIdioma().getIdIdioma());
                                        dto.setNombreIdioma(actividad.getIdioma().getNombre());
                                        dto.setCodigoIdioma(actividad.getIdioma().getCodigo());

                                        if (actividad.getCategoria() != null) {
                                                dto.setIdCategoria(actividad.getCategoria().getIdCategoria());
                                                dto.setNombreCategoria(actividad.getCategoria().getNombre());
                                        } else {
                                                dto.setNombreCategoria("Sin categoría");
                                        }

                                        if (actividad.getColaborador() != null) {
                                                dto.setIdColaborador(actividad.getColaborador().getIdColaborador());
                                        }

                                        dto.setCantidadComentario(
                                                        comentariosPorActividad.getOrDefault(actividad.getIdActividad(), 0));

                                        dto.setPrecio(actividad.getPrecio());
                                        dto.setPrecioConsumidor(usuarioHelper.CalcularPrecioConsumidor(actividad.getPrecio()));

                                        return dto;
                                })
                                .toList();

                Page<Categoria> categoriaPage = categoriaService.buscarTodasPorPaginacion(page, pageSizeCategorias);

                model.addAttribute("busqueda", new BusquedaForm());
                model.addAttribute("actividades", actividadesDTO);
                model.addAttribute("categorias", categoriaPage);
                model.addAttribute("idiomas", idiomaService.listarIdiomas());
                model.addAttribute("currentPage", page);
                model.addAttribute("totalPages", actividadesPage.getTotalPages());
                model.addAttribute("filtroNombre", "Actividades Más Reservadas");
                model.addAttribute("favoritosIds", obtenerFavoritosIds(auth));

                List<Integer> pageNumbers = IntStream.range(0, actividadesPage.getTotalPages()).boxed().toList();
                model.addAttribute("pageNumbers", pageNumbers);

                return "resultados-busqueda";
        }

        @GetMapping("/actividades/para-ti")
        public String verTodasParaTi(
                        @RequestParam(defaultValue = "0") int page,
                        Model model,
                        Authentication auth) {

                usuarioHelper.agregarInfoUsuarioModel(model, auth);

                // Verificar autenticación
                if (auth == null || !auth.isAuthenticated() || auth.getPrincipal().equals("anonymousUser")) {
                        return "redirect:/login";
                }

                Usuario usuario = usuarioService.obtenerPorEmail(auth.getName());
                Optional<Cliente> clienteOpt = clienteService.obtenerPorUsuario(usuario);

                if (clienteOpt.isEmpty()) {
                        return "redirect:/login";
                }

                Cliente cliente = clienteOpt.get();
                // Usar el id del cliente para obtener el objeto actualizado (por si acaso)
                Cliente clienteActual = clienteService.obtenerPorId(cliente.getId());
                int pageSize = 12;
                int pageSizeCategorias = 8;

                Page<Actividad> actividadesPage = actividadService.obtenerTodasParaTi(clienteActual.getId(), page, pageSize);

                List<Long> actividadIds = actividadesPage.stream()
                                .map(Actividad::getIdActividad)
                                .toList();

                Map<Long, Integer> comentariosPorActividad = comentarioService
                                .contarComentariosPorActividades(actividadIds);

                List<ActividadDTO> actividadesDTO = actividadesPage.stream()
                                .map(actividad -> {
                                        ActividadDTO dto = new ActividadDTO();
                                        dto.setIdActividad(actividad.getIdActividad());
                                        dto.setTitulo(actividad.getTitulo());
                                        dto.setDescripcion(actividad.getDescripcion());
                                        dto.setCalificacion(actividad.getCalificacion());
                                        dto.setUbicacion(actividad.getUbicacion());
                                        dto.setImagen(actividad.getImagen());
                                        dto.setCreatedAt(actividad.getCreatedAt());

                                        dto.setIdIdioma(actividad.getIdioma().getIdIdioma());
                                        dto.setNombreIdioma(actividad.getIdioma().getNombre());
                                        dto.setCodigoIdioma(actividad.getIdioma().getCodigo());

                                        if (actividad.getCategoria() != null) {
                                                dto.setIdCategoria(actividad.getCategoria().getIdCategoria());
                                                dto.setNombreCategoria(actividad.getCategoria().getNombre());
                                        } else {
                                                dto.setNombreCategoria("Sin categoría");
                                        }

                                        if (actividad.getColaborador() != null) {
                                                dto.setIdColaborador(actividad.getColaborador().getIdColaborador());
                                        }

                                        dto.setCantidadComentario(
                                                        comentariosPorActividad.getOrDefault(actividad.getIdActividad(), 0));

                                        dto.setPrecio(actividad.getPrecio());
                                        dto.setPrecioConsumidor(usuarioHelper.CalcularPrecioConsumidor(actividad.getPrecio()));

                                        return dto;
                                })
                                .toList();

                Page<Categoria> categoriaPage = categoriaService.buscarTodasPorPaginacion(page, pageSizeCategorias);

                model.addAttribute("busqueda", new BusquedaForm());
                model.addAttribute("actividades", actividadesDTO);
                model.addAttribute("categorias", categoriaPage);
                model.addAttribute("idiomas", idiomaService.listarIdiomas());
                model.addAttribute("currentPage", page);
                model.addAttribute("totalPages", actividadesPage.getTotalPages());
                model.addAttribute("filtroNombre", "Actividades Para Ti");
                model.addAttribute("favoritosIds", obtenerFavoritosIds(auth));

                List<Integer> pageNumbers = IntStream.range(0, actividadesPage.getTotalPages()).boxed().toList();
                model.addAttribute("pageNumbers", pageNumbers);

                return "resultados-busqueda";
        }

        @GetMapping("/top-colaboradores")
        public String verTopColaboradores(
                        Model model,
                        Authentication auth) {

                usuarioHelper.agregarInfoUsuarioModel(model, auth);

                List<ColaboradorPublicoDTO> topColaboradores = colaboradorService.obtenerDestacadosPorReservas(10);

                model.addAttribute("topColaboradores", topColaboradores);
                model.addAttribute("pagina", "top-colaboradores");

                return "top-colaboradores";
        }

        /**
         * Endpoint JSON para búsqueda de actividades (usado en crear planes)
         */
        @GetMapping("/api/actividades/buscar")
        @org.springframework.web.bind.annotation.ResponseBody
        public List<ActividadDTO> buscarActividadesJson(
                        @RequestParam(required = false) String nombre,
                        @RequestParam(required = false) Long categoriaId,
                        @RequestParam(required = false) Long idiomaId,
                        @RequestParam(defaultValue = "0") int page,
                        @RequestParam(defaultValue = "10") int size) {

                Page<Actividad> actividadesPage = actividadService.buscarConFiltros(
                                nombre, categoriaId, idiomaId, null, null, page, size);

                // IDs de actividades para batch query de comentarios
                List<Long> actividadIds = actividadesPage.getContent().stream()
                                .map(Actividad::getIdActividad)
                                .collect(Collectors.toList());

                // Batch query para evitar N+1
                Map<Long, Integer> comentariosPorActividad = comentarioService
                                .contarComentariosPorActividades(actividadIds);

                return actividadesPage.getContent().stream()
                                .map(actividad -> {
                                        ActividadDTO dto = new ActividadDTO();
                                        dto.setIdActividad(actividad.getIdActividad());
                                        dto.setTitulo(actividad.getTitulo());
                                        dto.setDescripcion(actividad.getDescripcion());
                                        dto.setImagen(actividad.getImagen());
                                        dto.setUbicacion(actividad.getUbicacion());
                                        dto.setPrecio(actividad.getPrecio());
                                        dto.setPrecioConsumidor(
                                                        usuarioHelper.CalcularPrecioConsumidor(actividad.getPrecio()));
                                        dto.setNombreCategoria(actividad.getCategoria().getNombre());
                                        dto.setIdCategoria(actividad.getCategoria().getIdCategoria());
                                        dto.setCantidadComentario(
                                                        comentariosPorActividad.getOrDefault(actividad.getIdActividad(),
                                                                        0));
                                        return dto;
                                })
                                .collect(Collectors.toList());
        }

        private Set<Long> obtenerFavoritosIds(Authentication auth) {
                if (auth == null || !auth.isAuthenticated() || auth.getPrincipal().equals("anonymousUser")) {
                        return Collections.emptySet();
                }

                try {
                        Usuario usuario = usuarioService.obtenerPorEmail(auth.getName());
                        Optional<Cliente> clienteOpt = clienteService.obtenerPorUsuario(usuario);
                        if (clienteOpt.isPresent()) {
                                return favoritoService.obtenerIdsFavoritosDeCliente(clienteOpt.get());
                        }
                } catch (Exception e) {
                        return Collections.emptySet();
                }

                return Collections.emptySet();
        }

        /**
         * Vista principal del mapa de actividades cercanas.
         * El navegador obtiene la geolocalización en el cliente y luego llama al endpoint JSON via AJAX.
         */
        @GetMapping("/actividades/cercanas")
        public String vistaMapa(Model model, Authentication auth) {
                usuarioHelper.agregarInfoUsuarioModel(model, auth);
                return "actividades-cercanas";
        }

        /**
         * Endpoint AJAX que devuelve las actividades cercanas en formato JSON.
         * 
         * @param lat latitud del usuario
         * @param lon longitud del usuario
         * @param radio radio de búsqueda en kilómetros (1-5)
         * @return lista de ActividadCercanaDTO ordenadas por distancia
         */
        @GetMapping("/actividades/cercanas/json")
        @ResponseBody
        public List<ActividadCercanaDTO> cercanasJson(
                        @RequestParam double lat,
                        @RequestParam double lon,
                        @RequestParam(defaultValue = "3") int radio) {
                // Clampear el radio entre 1 y 5 km
                int radioClamped = Math.max(1, Math.min(5, radio));
                return actividadService.buscarCercanas(lat, lon, radioClamped, 10);
        }

}

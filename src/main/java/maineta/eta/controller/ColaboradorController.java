package maineta.eta.controller;

import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.data.domain.Page;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import jakarta.persistence.EntityNotFoundException;
import maineta.eta.dto.ActividadUpdateDto;
import maineta.eta.dto.CalendarioDiaDTO;
import maineta.eta.dto.DisponibilidadDetalleDTO;
import maineta.eta.dto.PatronDisponibilidadDTO;
import maineta.eta.entity.Actividad;
import maineta.eta.entity.Colaborador;
import maineta.eta.entity.Disponibilidad;
import maineta.eta.entity.PatronDisponibilidad;
import maineta.eta.entity.Reserva;
import maineta.eta.entity.Usuario;
import maineta.eta.service.ActividadService;
import maineta.eta.service.CategoriaService;
import maineta.eta.service.ColaboradorService;
import maineta.eta.service.DisponibilidadService;
import maineta.eta.service.IUploadFileService;
import maineta.eta.service.IdiomaService;
import maineta.eta.service.PatronDisponibilidadService;
import maineta.eta.service.ReservaService;
import maineta.eta.service.UsuarioService;

@Controller
@RequestMapping("/colaborador") // Todas las rutas de este controlador empiezan por "/colaborador"
public class ColaboradorController {

    // Servicios necesarios para la gestión de actividades, usuarios, colaboradores
    // y archivos
    private final IUploadFileService uploadFileService;
    private final ActividadService actividadService;
    private final ColaboradorService colaboradorService;
    private final UsuarioService usuarioService;
    private final CategoriaService categoriaService;
    private final IdiomaService idiomaService;
    private final DisponibilidadService disponibilidadService;
    private final ReservaService reservaService;
    private final PatronDisponibilidadService patronDisponibilidadService;

    public ColaboradorController(ActividadService actividadService, UsuarioService usuarioService,
            ColaboradorService colaboradorService, IUploadFileService uploadFileService,
            CategoriaService categoriaService, IdiomaService idiomaService, DisponibilidadService disponibilidadService,
            ReservaService reservaService, PatronDisponibilidadService patronDisponibilidadService) {
        this.actividadService = actividadService;
        this.usuarioService = usuarioService;
        this.colaboradorService = colaboradorService;
        this.categoriaService = categoriaService;
        this.uploadFileService = uploadFileService;
        this.idiomaService = idiomaService;
        this.disponibilidadService = disponibilidadService;
        this.reservaService = reservaService;
        this.patronDisponibilidadService = patronDisponibilidadService;
    }

    // 🔹 Vista para cambiar cliente (ejemplo de plantilla simple)
    @GetMapping("/cambiar")
    public String CambiarCliente() {
        return "colaborador/cambiar";
    }

    // 🔹 Dashboard principal del colaborador
    @GetMapping("/dashboard")
    public String dashboard() {

        return "colaborador/dashboard";
    }

    @GetMapping("/actividades/nueva")
    public String mostrarFormulario(Model model) {
        model.addAttribute("actividad", new Actividad());
        model.addAttribute("categorias", categoriaService.listarCategorias());
        model.addAttribute("idiomas", idiomaService.listarIdiomas());
        return "colaborador/crearActividad";
    }

    @PostMapping("/actividades/addAct")
    public String addActivity(
            @ModelAttribute("actividad") Actividad actividad,
            @RequestParam("imagenFile") MultipartFile imagenFile,
            Authentication authentication) {

        // Paso 1: Obtener usuario autenticado
        String email = authentication.getName();
        Usuario usuario = usuarioService.obtenerPorEmail(email);

        // Paso 2: Obtener colaborador asociado al usuario
        Colaborador colaborador = colaboradorService.obtenerPorUsuario(usuario)
                .orElseThrow(
                        () -> new RuntimeException("Colaborador no encontrado para el usuario: " + usuario.getEmail()));

        // Asociar colaborador y fecha a la actividad
        actividad.setColaborador(colaborador);
        actividad.setCreatedAt(LocalDateTime.now());

        // Paso 3: Manejar la imagen subida
        try {
            if (!imagenFile.isEmpty()) {
                String nombreImagen = uploadFileService.copy(imagenFile);
                actividad.setImagen(nombreImagen);
            }
        } catch (Exception e) {
            throw new RuntimeException("Error al subir la imagen", e);
        }

        // Paso 5: Guardar la actividad en la base de datos
        actividadService.agregarActividad(actividad);
        return "redirect:/colaborador/actividades";
    }

    @PostMapping("/actividades/{id}/actualizar")
    public String actualizarActividad(
            @PathVariable Long id,
            @ModelAttribute("actividadUpdateDto") ActividadUpdateDto dto,
            @RequestParam(value = "imagenFile", required = false) MultipartFile imagenFile,
            RedirectAttributes flash) {

        try {

            actividadService.actualizarActividad(id, dto, imagenFile);

            flash.addFlashAttribute("message", "Actividad actualizada correctamente");
            flash.addFlashAttribute("type", "success");

        } catch (EntityNotFoundException e) {

            flash.addFlashAttribute("message", e.getMessage());
            flash.addFlashAttribute("type", "danger");

        } catch (IOException e) {

            flash.addFlashAttribute("message", "Error al subir la imagen");
            flash.addFlashAttribute("type", "danger");

        } catch (Exception e) {

            flash.addFlashAttribute("message", "Error al actualizar la actividad");
            flash.addFlashAttribute("type", "danger");
        }

        return "redirect:/colaborador/detalle/" + id;
    }

    // 🔹 Listar actividades del colaborador con paginación y filtros
    @GetMapping("/actividades")
    public String listar(@RequestParam(defaultValue = "0") int page,
            @RequestParam(required = false) String filtroNombre,
            Authentication authentication,
            Model model) {

        int pageSize = 10; // Tamaño de página (número de actividades por página)

        // Paso 1: obtener el usuario autenticado
        String email = authentication.getName();
        Usuario usuario = usuarioService.obtenerPorEmail(email);

        // Paso 2: obtener el colaborador asociado
        Colaborador colaborador = colaboradorService.obtenerPorUsuario(usuario)
                .orElseThrow(
                        () -> new RuntimeException("Colaborador no encontrado para el usuario: " + usuario.getEmail()));

        Long idColaborador = colaborador.getIdColaborador();

        // Paso 3: obtener las actividades paginadas filtradas por colaborador
        Page<Actividad> actividadesPage = actividadService
                .getActividadesConPaginacionDeColaborador(page, pageSize, idColaborador, filtroNombre);

        // Paso 4: pasar datos a la vista
        model.addAttribute("actividades", actividadesPage);
        model.addAttribute("actividad", new Actividad());
        model.addAttribute("currentPage", page);
        model.addAttribute("totalPages", actividadesPage.getTotalPages());
        model.addAttribute("filtroNombre", filtroNombre);

        return "colaborador/actividad"; // Vista Thymeleaf que lista actividades
    }

    // 🔹 Eliminar actividad por ID
    @GetMapping("/eliminar/{id}")
    public String deleteById(@PathVariable("id") Long id) {

        Actividad actividad = actividadService.listarById(id);

        // Si existe y tiene imagen asociada, eliminar también la imagen del servidor
        if (actividad != null && actividad.getImagen() != null) {
            uploadFileService.delete(actividad.getImagen());
        }

        // Eliminar la actividad de la base de datos
        actividadService.deleteActivity(id);

        return "redirect:/colaborador/actividades";
    }

    // ======================= DISPONIBILIDADES - VISTA CALENDARIO =======================

    @GetMapping("/disponibilidades/{idActividad}")
    public String verDisponibilidades(
            @PathVariable Long idActividad,
            @RequestParam(required = false) Integer anio,
            @RequestParam(required = false) Integer mes,
            @RequestParam(required = false) String fecha,
            Model model) {

        Actividad actividad = actividadService.obtenerPorId(idActividad);

        // Mes actual por defecto
        YearMonth ym;
        if (anio != null && mes != null) {
            ym = YearMonth.of(anio, mes);
        } else {
            ym = YearMonth.now();
        }

        // Datos del calendario
        List<CalendarioDiaDTO> calendarioDias = disponibilidadService
                .obtenerCalendarioMensual(idActividad, ym.getYear(), ym.getMonthValue());

        // Mapa día-del-mes -> CalendarioDiaDTO para acceso rápido en template
        Map<Integer, CalendarioDiaDTO> calendarioMap = calendarioDias.stream()
                .collect(Collectors.toMap(d -> d.getFecha().getDayOfMonth(), d -> d));

        // Si se seleccionó una fecha, cargar detalle del día
        List<DisponibilidadDetalleDTO> detalleDia = null;
        LocalDate fechaSeleccionada = null;
        if (fecha != null && !fecha.isEmpty()) {
            fechaSeleccionada = LocalDate.parse(fecha);
            detalleDia = disponibilidadService.obtenerDetallePorFecha(idActividad, fechaSeleccionada);
        }

        // Patrones existentes
        List<PatronDisponibilidad> patrones = patronDisponibilidadService.obtenerPorActividad(idActividad);

        model.addAttribute("actividad", actividad);
        model.addAttribute("calendarioMap", calendarioMap);
        model.addAttribute("anio", ym.getYear());
        model.addAttribute("mes", ym.getMonthValue());
        model.addAttribute("mesNombre", ym.getMonth().toString());
        model.addAttribute("primerDiaSemana", ym.atDay(1).getDayOfWeek().getValue()); // 1=Lun ... 7=Dom
        model.addAttribute("diasEnMes", ym.lengthOfMonth());
        model.addAttribute("detalleDia", detalleDia);
        model.addAttribute("fechaSeleccionada", fechaSeleccionada);
        model.addAttribute("patrones", patrones);
        model.addAttribute("disponibilidad", new Disponibilidad());
        model.addAttribute("patronDTO", new PatronDisponibilidadDTO());

        // Para hoy
        LocalDate hoy = LocalDate.now();
        model.addAttribute("hoyDia", hoy.getDayOfMonth());
        model.addAttribute("hoyAnio", hoy.getYear());
        model.addAttribute("hoyMes", hoy.getMonthValue());

        // Día seleccionado (número)
        model.addAttribute("diaSeleccionado", fechaSeleccionada != null ? fechaSeleccionada.getDayOfMonth() : -1);

        // Para navegación prev/next
        YearMonth prev = ym.minusMonths(1);
        YearMonth next = ym.plusMonths(1);
        model.addAttribute("prevAnio", prev.getYear());
        model.addAttribute("prevMes", prev.getMonthValue());
        model.addAttribute("nextAnio", next.getYear());
        model.addAttribute("nextMes", next.getMonthValue());

        return "colaborador/disponibilidad-actividad";
    }

    // Agregar disponibilidad individual
    @PostMapping("/disponibilidades/agregar")
    public String crearDisponibilidad(
            @ModelAttribute Disponibilidad disponibilidad,
            @RequestParam(required = true) Long idActividad,
            RedirectAttributes redirectAttrs) {
        try {
            Actividad actividad = actividadService.obtenerPorId(idActividad);
            disponibilidad.setActividad(actividad);
            disponibilidad.setCuposDisponibles(disponibilidad.getCuposTotales());
            disponibilidad.setEstado("DISPONIBLE");
            disponibilidadService.guardarDisponibilidad(disponibilidad);

            redirectAttrs.addFlashAttribute("mensaje", "Disponibilidad creada correctamente");
            String fechaStr = disponibilidad.getFecha() != null ? "&fecha=" + disponibilidad.getFecha() : "";
            return "redirect:/colaborador/disponibilidades/" + idActividad
                    + "?anio=" + disponibilidad.getFecha().getYear()
                    + "&mes=" + disponibilidad.getFecha().getMonthValue()
                    + fechaStr;
        } catch (Exception e) {
            e.printStackTrace();
            redirectAttrs.addFlashAttribute("error", "Error al crear disponibilidad: " + e.getMessage());
            return "redirect:/colaborador/disponibilidades/" + idActividad;
        }
    }

    // Crear patrón recurrente y generar disponibilidades
    @PostMapping("/disponibilidades/patron/crear")
    public String crearPatron(
            @ModelAttribute PatronDisponibilidadDTO patronDTO,
            @RequestParam Long idActividad,
            RedirectAttributes redirectAttrs) {
        try {
            patronDTO.setIdActividad(idActividad);
            patronDisponibilidadService.crearPatronYGenerar(patronDTO);
            redirectAttrs.addFlashAttribute("mensaje", "Patrón creado. Se generaron las disponibilidades automáticamente.");
        } catch (Exception e) {
            e.printStackTrace();
            redirectAttrs.addFlashAttribute("error", "Error al crear patrón: " + e.getMessage());
        }
        return "redirect:/colaborador/disponibilidades/" + idActividad;
    }

    // Eliminar patrón
    @PostMapping("/disponibilidades/patron/eliminar")
    public String eliminarPatron(
            @RequestParam Long idPatron,
            @RequestParam Long idActividad,
            RedirectAttributes redirectAttrs) {
        try {
            patronDisponibilidadService.eliminar(idPatron);
            redirectAttrs.addFlashAttribute("mensaje", "Patrón eliminado correctamente.");
        } catch (Exception e) {
            redirectAttrs.addFlashAttribute("error", "Error al eliminar patrón: " + e.getMessage());
        }
        return "redirect:/colaborador/disponibilidades/" + idActividad;
    }

    // Editar cupos de una disponibilidad
    @PostMapping("/disponibilidades/editar-cupos")
    public String editarCupos(
            @RequestParam Long idDisponibilidad,
            @RequestParam int nuevosCuposTotales,
            @RequestParam Long idActividad,
            @RequestParam(required = false) String fecha,
            RedirectAttributes redirectAttrs) {
        try {
            disponibilidadService.editarCupos(idDisponibilidad, nuevosCuposTotales);
            redirectAttrs.addFlashAttribute("mensaje", "Cupos actualizados correctamente.");
        } catch (Exception e) {
            redirectAttrs.addFlashAttribute("error", e.getMessage());
        }
        String redirect = "redirect:/colaborador/disponibilidades/" + idActividad;
        if (fecha != null && !fecha.isEmpty()) {
            LocalDate f = LocalDate.parse(fecha);
            redirect += "?anio=" + f.getYear() + "&mes=" + f.getMonthValue() + "&fecha=" + fecha;
        }
        return redirect;
    }

    // Cambiar estado de una disponibilidad
    @PostMapping("/disponibilidades/cambiar-estado")
    public String cambiarEstado(
            @RequestParam Long idDisponibilidad,
            @RequestParam String nuevoEstado,
            @RequestParam Long idActividad,
            @RequestParam(required = false) String fecha,
            RedirectAttributes redirectAttrs) {
        try {
            disponibilidadService.cambiarEstado(idDisponibilidad, nuevoEstado);
            redirectAttrs.addFlashAttribute("mensaje", "Estado actualizado correctamente.");
        } catch (Exception e) {
            redirectAttrs.addFlashAttribute("error", e.getMessage());
        }
        String redirect = "redirect:/colaborador/disponibilidades/" + idActividad;
        if (fecha != null && !fecha.isEmpty()) {
            LocalDate f = LocalDate.parse(fecha);
            redirect += "?anio=" + f.getYear() + "&mes=" + f.getMonthValue() + "&fecha=" + fecha;
        }
        return redirect;
    }

    @GetMapping("/reservas/{idActividad}")
    public String verReservas(@PathVariable Long idActividad, Model model) {
        Actividad actividad = actividadService.obtenerPorId(idActividad);
        List<Disponibilidad> disponibilidades = disponibilidadService.obtenerPorActividad(idActividad);
        List<Reserva> reservas = reservaService.getReservasPorIdActividad(idActividad);

        if (reservas == null) {
            reservas = List.of();
        }

        // Agrupar reservas por disponibilidad
        Map<Long, List<Reserva>> reservasPorDisponibilidad = reservas.stream()
                .filter(r -> r.getDisponibilidad() != null)
                .collect(Collectors.groupingBy(
                        r -> r.getDisponibilidad().getIdDisponibilidad(),
                        LinkedHashMap::new,
                        Collectors.toList()));

        // Reservas sin disponibilidad asignada
        List<Reserva> reservasSinDisponibilidad = reservas.stream()
                .filter(r -> r.getDisponibilidad() == null)
                .collect(Collectors.toList());

        int totalReservas = reservas.size();
        int totalPersonasReservadas = reservas.stream().mapToInt(Reserva::getCantidad).sum();

        // Calcular ingresos por disponibilidad (cantidad * precio de la actividad)
        BigDecimal precioActividad = actividad.getPrecio() != null ? actividad.getPrecio() : BigDecimal.ZERO;
        Map<Long, BigDecimal> ingresosPorDisponibilidad = new LinkedHashMap<>();
        for (Map.Entry<Long, List<Reserva>> entry : reservasPorDisponibilidad.entrySet()) {
            int totalCantidad = entry.getValue().stream().mapToInt(Reserva::getCantidad).sum();
            ingresosPorDisponibilidad.put(entry.getKey(), precioActividad.multiply(BigDecimal.valueOf(totalCantidad)));
        }

        // Calcular ingreso total
        BigDecimal ingresoTotal = ingresosPorDisponibilidad.values().stream()
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        model.addAttribute("actividad", actividad);
        model.addAttribute("disponibilidades", disponibilidades);
        model.addAttribute("reservasPorDisponibilidad", reservasPorDisponibilidad);
        model.addAttribute("reservasSinDisponibilidad", reservasSinDisponibilidad);
        model.addAttribute("totalReservas", totalReservas);
        model.addAttribute("totalPersonasReservadas", totalPersonasReservadas);
        model.addAttribute("ingresosPorDisponibilidad", ingresosPorDisponibilidad);
        model.addAttribute("ingresoTotal", ingresoTotal);
        return "colaborador/reservaciones-actividad";
    }

    // 🔹 Actualizar estado de una reserva
    @PostMapping("/reserva/actualizar/{idReserva}")
    public String actualizarEstadoReserva(@PathVariable Long idReserva,
                                          @RequestParam String estado,
                                          @RequestParam Long idActividad,
                                          RedirectAttributes redirectAttributes) {
        Reserva reserva = reservaService.ObtenerReservaPorId(idReserva)
                .orElseThrow(() -> new EntityNotFoundException("Reserva no encontrada"));
        reserva.setEstado(estado);
        reservaService.guardarReserva(reserva);
        redirectAttributes.addFlashAttribute("mensaje", "Estado de reserva actualizado correctamente");
        return "redirect:/colaborador/reservas/" + idActividad;
    }

    // 🔹 Ver detalle de una actividad específica
    @GetMapping("/detalle/{id}")
    public String verDetalleAcividad(@PathVariable("id") Long id, Model model) {

        // Buscar actividad por ID
        Actividad actividad = actividadService.obtenerPorId(id);

        if (actividad == null) {
            return "redirect:/colaborador/dashboard";
        }

        List<Reserva> reservas = reservaService.getReservasPorIdActividad(id);
        if (reservas == null) {
            reservas = List.of();
        }

        BigDecimal plataGanada = BigDecimal.ZERO;
        BigDecimal precioBase = actividad.getPrecio();

        for (Reserva reserva : reservas) {
            if ("Hecho".equals(reserva.getEstado())) {
                BigDecimal cantidad = BigDecimal.valueOf(reserva.getCantidad());
                plataGanada = plataGanada.add(precioBase.multiply(cantidad));
            }
        }

        // Crear y poblar el DTO para el formulario de edición
        ActividadUpdateDto dto = new ActividadUpdateDto();
        dto.setIdActividad(actividad.getIdActividad());
        dto.setTitulo(actividad.getTitulo());
        dto.setDescripcion(actividad.getDescripcion());
        dto.setUbicacion(actividad.getUbicacion());
        dto.setPrecio(actividad.getPrecio());
        dto.setCondiciones(actividad.getCondiciones());
        dto.setNormas(actividad.getNormas());
        dto.setIncluye(actividad.getIncluye());
        if (actividad.getCategoria() != null)
            dto.setIdCategoria(actividad.getCategoria().getIdCategoria());
        if (actividad.getIdioma() != null)
            dto.setIdIdioma(actividad.getIdioma().getIdIdioma());

        model.addAttribute("categorias", categoriaService.listarCategorias());
        model.addAttribute("idiomas", idiomaService.listarIdiomas());
        model.addAttribute("reservas", reservas);
        model.addAttribute("actividad", actividad);
        model.addAttribute("actividadUpdateDto", dto);
        model.addAttribute("plataGanada", plataGanada);
        model.addAttribute("imagenes", actividadService.obtenerImagenesPorActividad(actividad.getIdActividad()));

        return "colaborador/detalle-actividad";
    }

    // Agregar imágenes adicionales a una actividad
    @PostMapping("/actividades/{id}/imagenes/agregar")
    public String agregarImagenes(@PathVariable("id") Long id,
            @RequestParam("imagenesExtra") List<MultipartFile> imagenesExtra,
            RedirectAttributes redirectAttributes) {
        try {
            actividadService.agregarImagenes(id, imagenesExtra);
            redirectAttributes.addFlashAttribute("message", "Imágenes agregadas correctamente");
            redirectAttributes.addFlashAttribute("type", "success");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("message", "Error al agregar imágenes: " + e.getMessage());
            redirectAttributes.addFlashAttribute("type", "danger");
        }
        return "redirect:/colaborador/detalle/" + id;
    }

    // Eliminar una imagen adicional
    @GetMapping("/imagenes/eliminar/{idImagen}/{idActividad}")
    public String eliminarImagen(@PathVariable("idImagen") Long idImagen,
            @PathVariable("idActividad") Long idActividad,
            RedirectAttributes redirectAttributes) {
        try {
            actividadService.eliminarImagen(idImagen);
            redirectAttributes.addFlashAttribute("message", "Imagen eliminada correctamente");
            redirectAttributes.addFlashAttribute("type", "success");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("message", "Error al eliminar imagen: " + e.getMessage());
            redirectAttributes.addFlashAttribute("type", "danger");
        }
        return "redirect:/colaborador/detalle/" + idActividad;
    }

}

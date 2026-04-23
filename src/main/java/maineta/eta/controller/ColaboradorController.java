package maineta.eta.controller;

import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.TreeMap;
import java.util.stream.Collectors;

import org.springframework.data.domain.Page;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
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
import maineta.eta.dto.ColaboradorPerfilForm;
import maineta.eta.dto.DisponibilidadDetalleDTO;
import maineta.eta.dto.PatronDisponibilidadDTO;
import maineta.eta.dto.PrediccionOcupacionDTO;
import maineta.eta.entity.Actividad;
import maineta.eta.entity.Colaborador;
import maineta.eta.entity.ConversacionChat;
import maineta.eta.entity.Disponibilidad;
import maineta.eta.entity.MensajeChat;
import maineta.eta.entity.PatronDisponibilidad;
import maineta.eta.entity.PreguntaFrecuenteActividad;
import maineta.eta.entity.Reserva;
import maineta.eta.entity.Usuario;
import maineta.eta.service.ActividadService;
import maineta.eta.service.CategoriaService;
import maineta.eta.service.ChatService;
import maineta.eta.service.ColaboradorService;
import maineta.eta.service.DisponibilidadService;
import maineta.eta.service.IUploadFileService;
import maineta.eta.service.IdiomaService;
import maineta.eta.service.KpiColaboradorService;
import maineta.eta.service.PatronDisponibilidadService;
import maineta.eta.service.PrediccionService;
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
    private final ChatService chatService;
    private final KpiColaboradorService kpiColaboradorService;
    private final PrediccionService prediccionService;

    public ColaboradorController(ActividadService actividadService, UsuarioService usuarioService,
            ColaboradorService colaboradorService, IUploadFileService uploadFileService,
            CategoriaService categoriaService, IdiomaService idiomaService, DisponibilidadService disponibilidadService,
            ReservaService reservaService, PatronDisponibilidadService patronDisponibilidadService,
            ChatService chatService, KpiColaboradorService kpiColaboradorService,
            PrediccionService prediccionService) {
        this.actividadService = actividadService;
        this.usuarioService = usuarioService;
        this.colaboradorService = colaboradorService;
        this.categoriaService = categoriaService;
        this.uploadFileService = uploadFileService;
        this.idiomaService = idiomaService;
        this.disponibilidadService = disponibilidadService;
        this.reservaService = reservaService;
        this.patronDisponibilidadService = patronDisponibilidadService;
        this.chatService = chatService;
        this.kpiColaboradorService = kpiColaboradorService;
        this.prediccionService = prediccionService;
    }

    @ModelAttribute
    public void agregarDatosSidebar(Authentication authentication, Model model) {
        if (authentication == null || authentication.getName() == null) {
            return;
        }

        try {
            Usuario usuario = usuarioService.obtenerPorEmail(authentication.getName());
            colaboradorService.obtenerPorUsuario(usuario).ifPresent(colaborador -> {
                model.addAttribute("nombreColaboradorSidebar", usuario.getNombre());
                model.addAttribute("fotoPerfilColaborador", colaborador.getFotoPerfil());
            });
        } catch (RuntimeException ignored) {
            // Evita romper otras respuestas si el contexto autenticado cambia.
        }
    }

    // 🔹 Vista para cambiar cliente (ejemplo de plantilla simple)
    @GetMapping("/cambiar")
    public String CambiarCliente() {
        return "colaborador/cambiar";
    }

    // 🔹 Dashboard principal del colaborador
    @GetMapping("/dashboard")
    public String dashboard(
            @RequestParam(required = false) String periodo,
            Authentication authentication, 
            Model model) {

        String email = authentication.getName();
        Usuario usuario = usuarioService.obtenerPorEmail(email);
        Colaborador colaborador = colaboradorService.obtenerPorUsuario(usuario)
                .orElseThrow(() -> new RuntimeException("Colaborador no encontrado"));

        // Parsear período o usar mes actual
        YearMonth periodoSeleccionado;
        if (periodo != null && !periodo.isEmpty()) {
            try {
                periodoSeleccionado = YearMonth.parse(periodo);
            } catch (Exception e) {
                periodoSeleccionado = YearMonth.now();
            }
        } else {
            periodoSeleccionado = YearMonth.now();
        }

        // Obtener KPIs
        var resumen = kpiColaboradorService.obtenerResumen(colaborador.getIdColaborador(), periodoSeleccionado);
        var kpisPorActividad = kpiColaboradorService.obtenerKpiPorActividad(colaborador.getIdColaborador(), periodoSeleccionado);
        var tendenciaReservas = kpiColaboradorService.obtenerTendenciaReservas(colaborador.getIdColaborador(), 4);
        var estadosReserva = kpiColaboradorService.obtenerEstadosReserva(colaborador.getIdColaborador(), periodoSeleccionado);
        var ocupacion = kpiColaboradorService.obtenerOcupacion(colaborador.getIdColaborador(), periodoSeleccionado);
        var ingresosMensuales = kpiColaboradorService.obtenerIngresosMensuales(colaborador.getIdColaborador(), 6);
        List<Reserva> ultimasReservas = reservaService.getReservasColaborador(colaborador.getIdColaborador())
                .stream()
                .limit(3)
                .toList();

        // Pasar al modelo
        model.addAttribute("resumen", resumen);
        model.addAttribute("kpisPorActividad", kpisPorActividad);
        model.addAttribute("tendenciaReservas", tendenciaReservas);
        model.addAttribute("estadosReserva", estadosReserva);
        model.addAttribute("ocupacion", ocupacion);
        model.addAttribute("ingresosMensuales", ingresosMensuales);
        model.addAttribute("periodoSeleccionado", periodoSeleccionado);
        model.addAttribute("nombreColaborador", usuario.getNombre());
        model.addAttribute("colaboradorPerfil", colaborador);
        model.addAttribute("usuarioPerfil", usuario);
        model.addAttribute("ultimasReservas", ultimasReservas);

        return "colaborador/dashboard";
    }

    @GetMapping("/notificaciones-reservas")
    public String verNotificacionesReservas(Authentication authentication, Model model) {
        Colaborador colaborador = obtenerColaboradorAutenticado(authentication);
        List<Reserva> reservasPendientes = reservaService.getReservasColaborador(colaborador.getIdColaborador())
                .stream()
                .filter(reserva -> reserva.getEstado() != null && "pendiente".equalsIgnoreCase(reserva.getEstado()))
                .toList();

        Map<String, List<Reserva>> reservasPorActividad = reservasPendientes.stream()
                .collect(Collectors.groupingBy(
                        reserva -> reserva.getActividad() != null ? reserva.getActividad().getTitulo() : "Actividad sin titulo",
                        LinkedHashMap::new,
                        Collectors.toList()));

        model.addAttribute("reservasRecientes", reservasPendientes.stream().limit(3).toList());
        model.addAttribute("reservasPorActividad", reservasPorActividad);

        return "colaborador/notificaciones-reservas";
    }

    @GetMapping({"/settings", "/informacion"})
    public String mostrarInformacionPersonal(Authentication authentication, Model model) {
        Colaborador colaborador = obtenerColaboradorAutenticado(authentication);
        model.addAttribute("perfilForm", construirPerfilForm(colaborador));
        model.addAttribute("fotoPerfilActual", colaborador.getFotoPerfil());
        return "colaborador/informacion";
    }

    @PostMapping("/settings")
    public String actualizarInformacionPersonal(
            @ModelAttribute("perfilForm") ColaboradorPerfilForm perfilForm,
            @RequestParam(value = "fotoPerfil", required = false) MultipartFile fotoPerfil,
            Authentication authentication,
            RedirectAttributes redirectAttributes) {

        Colaborador colaborador = obtenerColaboradorAutenticado(authentication);
        String fotoAnterior = colaborador.getFotoPerfil();
        String nuevaFoto = null;

        try {
            if (fotoPerfil != null && !fotoPerfil.isEmpty()) {
                nuevaFoto = uploadFileService.copy(fotoPerfil);
            }

            Colaborador actualizado = colaboradorService.actualizarPerfil(
                    colaborador.getIdColaborador(),
                    perfilForm,
                    nuevaFoto);

            if (nuevaFoto != null && fotoAnterior != null && !fotoAnterior.isBlank()) {
                uploadFileService.delete(fotoAnterior);
            }

            Authentication nuevaAutenticacion = new UsernamePasswordAuthenticationToken(
                    actualizado.getUsuario().getEmail(),
                    authentication.getCredentials(),
                    authentication.getAuthorities());
            SecurityContextHolder.getContext().setAuthentication(nuevaAutenticacion);

            redirectAttributes.addFlashAttribute("exito", "Tu informacion personal se actualizo correctamente.");
        } catch (IOException e) {
            if (nuevaFoto != null) {
                uploadFileService.delete(nuevaFoto);
            }
            redirectAttributes.addFlashAttribute("error", "No se pudo guardar la foto seleccionada.");
        } catch (RuntimeException e) {
            if (nuevaFoto != null) {
                uploadFileService.delete(nuevaFoto);
            }
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }

        return "redirect:/colaborador/settings";
    }

    @GetMapping("/chats")
    public String verChatsColaborador(Authentication authentication, Model model) {
        String email = authentication.getName();
        Usuario usuario = usuarioService.obtenerPorEmail(email);
        Colaborador colaborador = colaboradorService.obtenerPorUsuario(usuario)
                .orElseThrow(() -> new RuntimeException("Colaborador no encontrado"));

        model.addAttribute("emailUsuario", email);
        model.addAttribute("reservasColaborador", reservaService.getReservasColaborador(colaborador.getIdColaborador()));
        model.addAttribute("conversaciones", chatService.listarConversacionesColaborador(email));

        return "colaborador/chats";
    }

    @GetMapping("/chats/{idReserva}")
    public String verChatColaboradorPorReserva(@PathVariable Long idReserva, Authentication authentication, Model model) {
        String email = authentication.getName();
        Usuario usuario = usuarioService.obtenerPorEmail(email);
        Colaborador colaborador = colaboradorService.obtenerPorUsuario(usuario)
                .orElseThrow(() -> new RuntimeException("Colaborador no encontrado"));

        ConversacionChat conversacion = chatService.obtenerOCrearConversacionDesdeReservaColaborador(idReserva, email);
        List<MensajeChat> mensajes = chatService.listarMensajes(
            Objects.requireNonNull(conversacion.getIdConversacion()),
            email);

        model.addAttribute("emailUsuario", email);
        model.addAttribute("reservasColaborador", reservaService.getReservasColaborador(colaborador.getIdColaborador()));
        model.addAttribute("conversaciones", chatService.listarConversacionesColaborador(email));
        model.addAttribute("conversacionSeleccionada", conversacion);
        model.addAttribute("mensajes", mensajes);

        return "colaborador/chats";
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

        // Calcular el día de la semana del primer día del mes (1=Lun...7=Dom)
        int primerDiaSemana = ym.atDay(1).getDayOfWeek().getValue();
        System.out.println("DEBUG: Calendario " + ym + " - Primer día del mes cae en: " + 
                           ym.atDay(1).getDayOfWeek() + " (valor=" + primerDiaSemana + ")");

        // Si se seleccionó una fecha, cargar detalle del día
        List<DisponibilidadDetalleDTO> detalleDia = null;
        LocalDate fechaSeleccionada = null;
        if (fecha != null && !fecha.isEmpty()) {
            fechaSeleccionada = LocalDate.parse(fecha);
            detalleDia = disponibilidadService.obtenerDetallePorFecha(idActividad, fechaSeleccionada);
            
            // Agregar predicciones para cada disponibilidad del día
            if (detalleDia != null && !detalleDia.isEmpty()) {
                for (DisponibilidadDetalleDTO detalle : detalleDia) {
                    try {
                        PrediccionOcupacionDTO prediccion = prediccionService.predecirOcupacion(detalle.getIdDisponibilidad());
                        detalle.setPrediccion(prediccion);
                    } catch (Exception e) {
                        System.err.println("Error al predecir ocupación para disponibilidad " + 
                            detalle.getIdDisponibilidad() + ": " + e.getMessage());
                        // Continuar con las demás predicciones aunque falle una
                    }
                }
            }
        }

        // Patrones existentes
        List<PatronDisponibilidad> patrones = patronDisponibilidadService.obtenerPorActividad(idActividad);
        Locale localeEs = Locale.forLanguageTag("es-CO");
        String mesNombre = capitalizarTexto(ym.atDay(1).format(DateTimeFormatter.ofPattern("MMMM", localeEs)));
        String fechaSeleccionadaTexto = fechaSeleccionada != null
                ? capitalizarTexto(fechaSeleccionada.format(DateTimeFormatter.ofPattern("EEEE dd 'de' MMMM yyyy", localeEs)))
                : null;

        model.addAttribute("actividad", actividad);
        model.addAttribute("calendarioMap", calendarioMap);
        model.addAttribute("anio", ym.getYear());
        model.addAttribute("mes", ym.getMonthValue());
        model.addAttribute("mesNombre", mesNombre);
        model.addAttribute("primerDiaSemana", primerDiaSemana); // 1=Lun ... 7=Dom
        model.addAttribute("diasEnMes", ym.lengthOfMonth());
        model.addAttribute("detalleDia", detalleDia);
        model.addAttribute("fechaSeleccionada", fechaSeleccionada);
        model.addAttribute("fechaSeleccionadaTexto", fechaSeleccionadaTexto);
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

        // Agrupar disponibilidades por fecha (día)
        Map<LocalDate, List<Disponibilidad>> disponibilidadesPorDia = disponibilidades.stream()
                .collect(Collectors.groupingBy(
                        Disponibilidad::getFecha,
                        TreeMap::new,
                        Collectors.toList()));

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

        // Calcular totales por día
        Map<LocalDate, Integer> reservasPorDia = new LinkedHashMap<>();
        Map<LocalDate, BigDecimal> ingresosPorDia = new LinkedHashMap<>();
        DateTimeFormatter fechaReservaFormatter = DateTimeFormatter.ofPattern("EEEE, dd MMMM yyyy", new Locale("es", "ES"));
        Map<LocalDate, String> etiquetasDia = new LinkedHashMap<>();
        for (Map.Entry<LocalDate, List<Disponibilidad>> diaEntry : disponibilidadesPorDia.entrySet()) {
            int totalReservasDia = 0;
            BigDecimal totalIngresosDia = BigDecimal.ZERO;
            for (Disponibilidad disp : diaEntry.getValue()) {
                Long idDisp = disp.getIdDisponibilidad();
                if (reservasPorDisponibilidad.containsKey(idDisp)) {
                    totalReservasDia += reservasPorDisponibilidad.get(idDisp).size();
                }
                if (ingresosPorDisponibilidad.containsKey(idDisp)) {
                    totalIngresosDia = totalIngresosDia.add(ingresosPorDisponibilidad.get(idDisp));
                }
            }
            reservasPorDia.put(diaEntry.getKey(), totalReservasDia);
            ingresosPorDia.put(diaEntry.getKey(), totalIngresosDia);
            etiquetasDia.put(diaEntry.getKey(), diaEntry.getKey().format(fechaReservaFormatter));
        }

        model.addAttribute("actividad", actividad);
        model.addAttribute("disponibilidadesPorDia", disponibilidadesPorDia);
        model.addAttribute("totalDisponibilidades", disponibilidades.size());
        model.addAttribute("reservasPorDisponibilidad", reservasPorDisponibilidad);
        model.addAttribute("reservasSinDisponibilidad", reservasSinDisponibilidad);
        model.addAttribute("totalReservas", totalReservas);
        model.addAttribute("totalPersonasReservadas", totalPersonasReservadas);
        model.addAttribute("ingresosPorDisponibilidad", ingresosPorDisponibilidad);
        model.addAttribute("ingresoTotal", ingresoTotal);
        model.addAttribute("reservasPorDia", reservasPorDia);
        model.addAttribute("ingresosPorDia", ingresosPorDia);
        model.addAttribute("etiquetasDia", etiquetasDia);
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
        List<PreguntaFrecuenteActividad> preguntasFrecuentes = actividad.getPreguntasFrecuentes();
        if (preguntasFrecuentes != null && !preguntasFrecuentes.isEmpty()) {
            preguntasFrecuentes.stream()
                    .sorted(Comparator.comparingInt(PreguntaFrecuenteActividad::getOrdenVisual))
                    .forEach(faq -> {
                        dto.getPreguntasFrecuentesPreguntas().add(faq.getPregunta());
                        dto.getPreguntasFrecuentesRespuestas().add(faq.getRespuesta());
                    });
        } else {
            dto.getPreguntasFrecuentesPreguntas().add("");
            dto.getPreguntasFrecuentesRespuestas().add("");
        }
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

    private Colaborador obtenerColaboradorAutenticado(Authentication authentication) {
        String email = authentication.getName();
        Usuario usuario = usuarioService.obtenerPorEmail(email);
        return colaboradorService.obtenerPorUsuario(usuario)
                .orElseThrow(() -> new RuntimeException("Colaborador no encontrado"));
    }

    private ColaboradorPerfilForm construirPerfilForm(Colaborador colaborador) {
        Usuario usuario = colaborador.getUsuario();
        return new ColaboradorPerfilForm(
                usuario.getNombre(),
                usuario.getEmail(),
                usuario.getTelefono(),
                colaborador.getNit(),
                colaborador.getCorreoSeguridad());
    }

    private String construirEtiquetaDisponibilidad(Reserva reserva) {
        if (reserva.getDisponibilidad() == null) {
            return "Reserva sin disponibilidad asignada";
        }

        String fecha = reserva.getDisponibilidad().getFecha() != null
                ? reserva.getDisponibilidad().getFecha().toString()
                : "Fecha pendiente";
        String horaInicio = reserva.getDisponibilidad().getHoraInicio() != null
                ? reserva.getDisponibilidad().getHoraInicio().toString()
                : "--:--";
        String horaFin = reserva.getDisponibilidad().getHoraFin() != null
                ? reserva.getDisponibilidad().getHoraFin().toString()
                : "--:--";

        return fecha + " • " + horaInicio + " - " + horaFin;
    }

    private String capitalizarTexto(String texto) {
        if (texto == null || texto.isBlank()) {
            return texto;
        }
        return texto.substring(0, 1).toUpperCase() + texto.substring(1);
    }
}

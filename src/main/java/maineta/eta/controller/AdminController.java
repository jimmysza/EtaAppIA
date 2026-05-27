package maineta.eta.controller;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import maineta.eta.dto.ColaboradorEstadisticasAdminDTO;
import maineta.eta.entity.Actividad;
import maineta.eta.entity.Admin;
import maineta.eta.entity.Categoria;
import maineta.eta.entity.Cliente;
import maineta.eta.entity.Colaborador;
import maineta.eta.entity.EstadoPagoColaborador;
import maineta.eta.entity.EstadoReembolso;
import maineta.eta.entity.Idioma;
import maineta.eta.entity.Reserva;
import maineta.eta.service.ActividadService;
import maineta.eta.service.AdminService;
import maineta.eta.service.CancelacionService;
import maineta.eta.service.CategoriaService;
import maineta.eta.service.ClienteService;
import maineta.eta.service.ColaboradorService;
import maineta.eta.service.DisponibilidadService;
import maineta.eta.service.IUploadFileService;
import maineta.eta.service.IdiomaService;
import maineta.eta.service.KpiColaboradorService;
import maineta.eta.service.ReservaService;
import maineta.eta.service.UsuarioService;

@Controller
@RequestMapping("/admin")
public class AdminController {

    private final AdminService adminService;
    private final IUploadFileService uploadFileService;
    private final CategoriaService categoriaService;
    private final IdiomaService idiomaService;

    private final ActividadService actividadService;
    private final UsuarioService usuarioService;
    private final ReservaService reservaService;
    private final ClienteService clienteService;
    private final ColaboradorService colaboradorService;
    private final DisponibilidadService disponibilidadService;
    private final CancelacionService cancelacionService;
    private final KpiColaboradorService kpiColaboradorService;

    /* @Autowired */
    public AdminController(AdminService adminService, IUploadFileService uploadFileService, CategoriaService categoriaService, IdiomaService idiomaService,
            ActividadService actividadService,
            UsuarioService usuarioService, ColaboradorService colaboradorService,
            ReservaService reservaService,
            ClienteService clienteService, DisponibilidadService disponibilidadService,
            CancelacionService cancelacionService, KpiColaboradorService kpiColaboradorService) {
        this.adminService = adminService;
        this.uploadFileService = uploadFileService;
        this.categoriaService = categoriaService;
        this.idiomaService = idiomaService;
        this.disponibilidadService = disponibilidadService;
        this.actividadService = actividadService;
        this.clienteService = clienteService;
        this.usuarioService = usuarioService;
        this.reservaService = reservaService;
        this.colaboradorService = colaboradorService;
        this.cancelacionService = cancelacionService;
        this.kpiColaboradorService = kpiColaboradorService;
    }

    @GetMapping("/dashboard")
    public String adminHome(Model model) {
        Admin admin;
        try {
            admin = adminService.obtenerAdminPrincipal();
        } catch (IllegalStateException e) {
            admin = new Admin();
            admin.setPorcentajeComision(new BigDecimal("18"));
            admin.setHorasCancelacion(24);
        }
        
        BigDecimal porcentajeComision = admin.getPorcentajeComision() != null ? admin.getPorcentajeComision() : new BigDecimal("18");

        List<Actividad> actividades = actividadService.listarActividades();

        BigDecimal plataGanada = BigDecimal.ZERO;

        for (Actividad act : actividades) {
            for (Reserva reserva : act.getReservas()) {
                if (esReservaFinalizada(reserva)) {
                    plataGanada = plataGanada.add(reserva.getComisionEtaSafe());
                }
            }
        }

        model.addAttribute("porcentajeComision", porcentajeComision);
        model.addAttribute("plataGanada", plataGanada);
        model.addAttribute("CantidadCliente", clienteService.ContadorCliente());
        model.addAttribute("CantidadColaborador", colaboradorService.ContadorColaborador());
        
        // 1. Reservaciones recientes
        List<Reserva> reservacionesRecientes = reservaService.obtenerTodasReservas(PageRequest.of(0, 5)).getContent();
        model.addAttribute("reservacionesRecientes", reservacionesRecientes);
        
        // 2. Actividades recientes
        List<Actividad> actividadesRecientes = actividades.stream()
            .sorted(Comparator.comparing(Actividad::getCreatedAt, Comparator.nullsLast(Comparator.reverseOrder())))
            .limit(5)
            .collect(Collectors.toList());
        model.addAttribute("actividadesRecientes", actividadesRecientes);

        // 3. Top colaboradores por comisión ETA
        Map<maineta.eta.entity.Colaborador, BigDecimal> comisionesPorColaborador = new HashMap<>();
        for (Actividad act : actividades) {
            maineta.eta.entity.Colaborador colab = act.getColaborador();
            for (Reserva r : act.getReservas()) {
                if (esReservaFinalizada(r)) {
                    comisionesPorColaborador.merge(colab, r.getComisionEtaSafe(), BigDecimal::add);
                }
            }
        }
        List<Object[]> topColaboradores = comisionesPorColaborador.entrySet().stream()
            .sorted((e1, e2) -> e2.getValue().compareTo(e1.getValue()))
            .limit(4)
            .map(e -> new Object[]{e.getKey(), e.getValue()})
            .collect(Collectors.toList());
        model.addAttribute("topColaboradores", topColaboradores);
        model.addAttribute("CantidadUsuarios", usuarioService.ContadorUsuario());
        model.addAttribute("CantidadReservacion", reservaService.ContadorReservas());
        model.addAttribute("CantidadActividad", actividadService.ContadorActividades());
        model.addAttribute("CantidadDisponibilidades", disponibilidadService.ContadorDisponibilidades());
        Integer horasCancelacion = admin.getHorasCancelacion();
        model.addAttribute("horasCancelacion", horasCancelacion != null ? horasCancelacion : 24);

        return "admin/dashboard";
    }

    @PostMapping("/comision")
    public String actualizarComision(@RequestParam BigDecimal porcentajeComision, RedirectAttributes redirectAttrs) {
        if (porcentajeComision.compareTo(BigDecimal.ZERO) < 0 || porcentajeComision.compareTo(new BigDecimal("100")) > 0) {
            redirectAttrs.addFlashAttribute("error", "El porcentaje de comision debe estar entre 0 y 100.");
            return "redirect:/admin/dashboard";
        }

        try {
            adminService.actualizarPorcentajeComision(porcentajeComision);
            redirectAttrs.addFlashAttribute("mensaje", "Porcentaje de comision actualizado correctamente.");
        } catch (IllegalStateException e) {
            redirectAttrs.addFlashAttribute("error", "No existe un administrador configurado para actualizar.");
        }
        return "redirect:/admin/dashboard";
    }

    @GetMapping("/categorias")
    public String listarCategorias(Model model, Categoria categoria) {
        model.addAttribute("categorias", categoriaService.listarCategorias());
        model.addAttribute("categoria", new Categoria());
        return "admin/categorias"; // Asegúrate de tener esta plantilla creada
    }

    @PostMapping("/categorias/nueva")
    public String nuevaCategoria(Categoria categoria,
            @RequestParam(value = "imagenFile", required = false) MultipartFile imagenFile,
            RedirectAttributes redirectAttrs) {
        try {
            if (imagenFile != null && !imagenFile.isEmpty()) {
                categoria.setImagen("/uploads/" + uploadFileService.copy(imagenFile));
            }

            categoriaService.guardarCategoria(categoria);
            redirectAttrs.addFlashAttribute("mensaje", "Categoria creada correctamente.");
        } catch (Exception e) {
            redirectAttrs.addFlashAttribute("error", e.getMessage());
        }

        return "redirect:/admin/categorias"; // Redirige a la lista de categorías después de guardar
    }

    @GetMapping("/categorias/editar/{id}")
    public String editarCategoriaForm(@PathVariable Long id, Model model, RedirectAttributes redirectAttrs) {
        Categoria categoria = categoriaService.getCategoriaPorId(id);
        if (categoria == null) {
            redirectAttrs.addFlashAttribute("error", "Categoría no encontrada");
            return "redirect:/admin/categorias";
        }
        model.addAttribute("categoria", categoria);
        return "admin/categoria-edit";
    }

    @PostMapping("/categorias/editar")
    public String editarCategoria(Categoria categoria,
            @RequestParam(value = "imagenFile", required = false) MultipartFile imagenFile,
            RedirectAttributes redirectAttrs) {
        try {
            Categoria existente = categoriaService.getCategoriaPorId(categoria.getIdCategoria());
            if (existente == null) {
                redirectAttrs.addFlashAttribute("error", "Categoría no encontrada");
                return "redirect:/admin/categorias";
            }

            existente.setNombre(categoria.getNombre());

            if (imagenFile != null && !imagenFile.isEmpty()) {
                // borrar imagen anterior si pertenece a /uploads/
                if (existente.getImagen() != null && existente.getImagen().startsWith("/uploads/")) {
                    try {
                        uploadFileService.delete(existente.getImagen().replace("/uploads/", ""));
                    } catch (Exception ignored) {
                    }
                }
                existente.setImagen("/uploads/" + uploadFileService.copy(imagenFile));
            }

            categoriaService.guardarCategoria(existente);
            redirectAttrs.addFlashAttribute("mensaje", "Categoría actualizada correctamente.");
        } catch (Exception e) {
            redirectAttrs.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/admin/categorias";
    }

    @PostMapping("/categorias/eliminar")
    public String eliminarCategoria(Long id) {
        categoriaService.eliminarCategoria(id);
        return "redirect:/admin/categorias"; // Redirige a la lista de categorías después de eliminar
    }

    @GetMapping("/idiomas")
    public String listaridiomas(Model model, Idioma idioma) {

        List<Idioma> idiomas = idiomaService.listarIdiomas();
        model.addAttribute("idiomas", idiomas);
        model.addAttribute("idioma", new Idioma());

        return "admin/idiomas"; // Asegúrate de tener esta plantilla creada
    }

    @PostMapping("/idiomas/nueva")
    public String nuevaIdioma(Idioma idioma) {
        idiomaService.guardarIdioma(idioma);
        return "redirect:/admin/idiomas"; // Redirige a la lista de categorías después de guardar
    }

    @PostMapping("/idiomas/eliminar")
    public String eliminarIdioma(Long id) {
        idiomaService.eliminarIdioma(id);
        return "redirect:/admin/idiomas"; // Redirige a la lista de categorías después de eliminar
    }

    @GetMapping("/clientes")
    public String listarClientes(@RequestParam(defaultValue = "0") int page, Model model) {
        Page<Cliente> clientesPage = clienteService.findAll(PageRequest.of(page, 10));

        model.addAttribute("clientes", clientesPage.getContent());
        model.addAttribute("currentPage", page);
        model.addAttribute("totalPages", clientesPage.getTotalPages());

        return "admin/clientes";
    }

    @GetMapping("/eliminar/categoria/{id}")
    public String eliminar(@PathVariable Long id, RedirectAttributes redirectAttrs) {
        try {
            Categoria categoria = categoriaService.getCategoriaPorId(id);
            if (categoria != null && categoria.getImagen() != null && categoria.getImagen().startsWith("/uploads/")) {
                uploadFileService.delete(categoria.getImagen().replace("/uploads/", ""));
            }
            categoriaService.eliminarCategoria(id);
            redirectAttrs.addFlashAttribute("mensaje", "Categoría eliminada correctamente");
        } catch (RuntimeException e) {
            redirectAttrs.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/admin/categorias";
    }

    // ===== ENDPOINTS DE GESTIÓN DE PAGOS Y REEMBOLSOS =====

    /**
     * RN-13: Panel de administración de pagos pendientes a colaboradores
     */
    @GetMapping("/pagos")
    public String listarPagosPendientes(@RequestParam(defaultValue = "0") int page, Model model) {
        // Obtener solo reservas en estado Pendiente con pago colaborador pendiente
        Page<Reserva> reservasPage = reservaService.obtenerReservasPendientesDePago(PageRequest.of(page, 20));

        // Calcular totales
        BigDecimal totalPendiente = reservasPage.getContent().stream()
            .map(r -> r.getPrecioColaborador() != null ? r.getPrecioColaborador().multiply(new BigDecimal(r.getCantidad())) : BigDecimal.ZERO)
            .reduce(BigDecimal.ZERO, BigDecimal::add);

        model.addAttribute("pagosPage", reservasPage);
        //model.addAttribute("reservas", reservasPage.getContent());
        model.addAttribute("currentPage", page);
        model.addAttribute("totalPages", reservasPage.getTotalPages());
        model.addAttribute("totalPendiente", totalPendiente);
        model.addAttribute("pagina", "pagos");

        return "admin/pagos";
    }

    /**
     * Marcar un pago al colaborador como pagado
     */
    @PostMapping("/pagos/marcar-pagado/{idReserva}")
    public String marcarPagoPagado(@PathVariable Long idReserva, RedirectAttributes redirectAttrs) {
        try {
            Reserva reserva = reservaService.obtenerPorId(idReserva)
                .orElseThrow(() -> new IllegalArgumentException("Reserva no encontrada"));

            reserva.setEstadoPagoColaborador(EstadoPagoColaborador.PAGADO);
            reserva.setFechaPagoColaborador(LocalDateTime.now());
            reservaService.guardarReserva(reserva);

            redirectAttrs.addFlashAttribute("mensaje", "Pago marcado como realizado");
        } catch (Exception e) {
            redirectAttrs.addFlashAttribute("error", "Error: " + e.getMessage());
        }

        return "redirect:/admin/pagos";
    }

    /**
     * RN-13: Panel de administración de reembolsos pendientes
     */
    @GetMapping("/reembolsos")
    public String listarReembolsosPendientes(@RequestParam(defaultValue = "0") int page, Model model) {
        Page<Reserva> reservasPage = reservaService.obtenerReservasConReembolsoPendiente(PageRequest.of(page, 20));

        BigDecimal totalReembolsos = reservasPage.getContent().stream()
            .map(r -> r.getMontoReembolso() != null ? r.getMontoReembolso() : BigDecimal.ZERO)
            .reduce(BigDecimal.ZERO, BigDecimal::add);

        model.addAttribute("reembolsosPage", reservasPage);
        model.addAttribute("currentPage", page);
        model.addAttribute("totalPages", reservasPage.getTotalPages());
        model.addAttribute("totalReembolsos", totalReembolsos);
        model.addAttribute("pagina", "reembolsos");

        return "admin/reembolsos";
    }

    /**
     * Marcar un reembolso como enviado
     */
    @PostMapping("/reembolsos/marcar-enviado/{idReserva}")
    public String marcarReembolsoEnviado(@PathVariable Long idReserva, RedirectAttributes redirectAttrs) {
        try {
            Reserva reserva = reservaService.obtenerPorId(idReserva)
                .orElseThrow(() -> new IllegalArgumentException("Reserva no encontrada"));

            reserva.setEstadoReembolso(EstadoReembolso.REEMBOLSADO);
            reserva.setFechaReembolso(LocalDateTime.now());
            reservaService.guardarReserva(reserva);

            redirectAttrs.addFlashAttribute("mensaje", "Reembolso marcado como enviado");
        } catch (Exception e) {
            redirectAttrs.addFlashAttribute("error", "Error: " + e.getMessage());
        }

        return "redirect:/admin/reembolsos";
    }

    /**
     * RN-13: Cancelación manual de reserva por admin
     */
    @PostMapping("/reserva/cancelar/{idReserva}")
    public String cancelarReservaAdmin(@PathVariable Long idReserva,
                                      @RequestParam String tipoReembolso,
                                      @RequestParam(required = false) BigDecimal montoReembolso,
                                      @RequestParam String motivo,
                                      RedirectAttributes redirectAttrs) {
        try {
            cancelacionService.cancelarPorAdmin(idReserva, tipoReembolso, montoReembolso, motivo);
            redirectAttrs.addFlashAttribute("mensaje", "Reserva cancelada por administrador");
        } catch (Exception e) {
            redirectAttrs.addFlashAttribute("error", "Error: " + e.getMessage());
        }

        return "redirect:/admin/dashboard";
    }

    /**
     * Configuración de ventana de cancelación
     */
    @PostMapping("/configuracion/cancelacion")
    public String actualizarConfiguracionCancelacion(@RequestParam Integer horasCancelacion,
                                                     RedirectAttributes redirectAttrs) {
        try {
            adminService.actualizarHorasCancelacion(horasCancelacion);
            redirectAttrs.addFlashAttribute("mensaje", "Configuración de cancelación actualizada: " + horasCancelacion + " horas");
        } catch (IllegalStateException e) {
            redirectAttrs.addFlashAttribute("error", "No existe un administrador configurado para actualizar.");
        } catch (Exception e) {
            redirectAttrs.addFlashAttribute("error", "Error: " + e.getMessage());
        }

        return "redirect:/admin/dashboard";
    }

    /**
     * Panel de ingresos y comisiones ganadas por ETA
     */
    @GetMapping("/ingresos")
    public String listarIngresos(@RequestParam(defaultValue = "0") int page,
                                @RequestParam(required = false) String estado,
                                Model model) {
        Page<Reserva> reservasPage = reservaService.obtenerTodasReservas(PageRequest.of(page, 50));

        // Filtrar por estado si se especifica
        if (estado != null && !estado.isEmpty()) {
            if ("Hecho".equalsIgnoreCase(estado)) {
                reservasPage = reservaService.obtenerPorEstado("Hecho", PageRequest.of(page, 50));
            } else if (esEstadoFinalizado(estado)) {
                reservasPage = reservaService.obtenerPorEstados(ESTADOS_FINALIZADOS, PageRequest.of(page, 50));
            } else {
                reservasPage = reservaService.obtenerPorEstado(estado, PageRequest.of(page, 50));
            }
        }

        // Calcular estadísticas
        BigDecimal totalComisionesGanadas = reservaService.calcularTotalComisionesGanadas();

        long totalReservas = reservasPage.getTotalElements();
        long reservasCompletadas = reservasPage.getContent().stream()
            .filter(this::esReservaFinalizada).count();
        long reservasCanceladas = reservasPage.getContent().stream()
            .filter(r -> r.getEstado().startsWith("CANCELADA")).count();

        model.addAttribute("reservas", reservasPage.getContent());
        model.addAttribute("currentPage", page);
        model.addAttribute("totalPages", reservasPage.getTotalPages());
        model.addAttribute("totalComisionesGanadas", totalComisionesGanadas != null ? totalComisionesGanadas : BigDecimal.ZERO);
        model.addAttribute("totalReservas", totalReservas);
        model.addAttribute("reservasCompletadas", reservasCompletadas);
        model.addAttribute("reservasCanceladas", reservasCanceladas);
        model.addAttribute("estadoFiltro", estado);
        model.addAttribute("pagina", "ingresos");

        return "admin/ingresos";
    }

    private static final List<String> ESTADOS_FINALIZADOS = List.of("Hecho", "COMPLETADA", "COMPLETADO", "CONFIRMADA");

    private boolean esReservaFinalizada(Reserva reserva) {
        return reserva != null && esEstadoFinalizado(reserva.getEstado());
    }

    private boolean esEstadoFinalizado(String estado) {
        if (estado == null) {
            return false;
        }
        return ESTADOS_FINALIZADOS.stream().anyMatch(e -> e.equalsIgnoreCase(estado));
    }

    // ===== GESTIÓN Y ESTADÍSTICAS DE COLABORADORES =====

    @GetMapping("/colaboradores")
    public String listarColaboradores(Model model) {
        List<Colaborador> colaboradores = colaboradorService.findAll();
        List<ColaboradorEstadisticasAdminDTO> estadisticas = colaboradores.stream()
            .map(c -> kpiColaboradorService.obtenerEstadisticasAdmin(c.getIdColaborador()))
            .collect(Collectors.toList());
        model.addAttribute("estadisticas", estadisticas);
        model.addAttribute("pagina", "colaboradores");
        return "admin/colaboradores-lista";
    }

    @GetMapping("/colaboradores/{id}")
    public String verDetalleColaborador(@PathVariable Long id, Model model) {
        ColaboradorEstadisticasAdminDTO estadisticas = kpiColaboradorService.obtenerEstadisticasAdmin(id);
        model.addAttribute("estadisticas", estadisticas);
        model.addAttribute("pagina", "colaboradores");
        return "admin/colaborador-detalle";
    }

}

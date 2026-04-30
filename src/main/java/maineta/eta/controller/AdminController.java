package maineta.eta.controller;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

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

import maineta.eta.entity.Actividad;
import maineta.eta.entity.Admin;
import maineta.eta.entity.Categoria;
import maineta.eta.entity.Cliente;
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

    /* @Autowired */
    public AdminController(AdminService adminService, IUploadFileService uploadFileService, CategoriaService categoriaService, IdiomaService idiomaService,
            ActividadService actividadService,
            UsuarioService usuarioService, ColaboradorService colaboradorService,
            ReservaService reservaService,
            ClienteService clienteService, DisponibilidadService disponibilidadService,
            CancelacionService cancelacionService) {
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
    }

    @GetMapping("/dashboard")
    public String adminHome(Model model) {
        Admin admin = adminService.obtenerAdminPrincipal();
        BigDecimal porcentajeDecimal = admin.getPorcentajeComision().divide(new BigDecimal("100"));

        List<Actividad> actividades = actividadService.listarActividades();

        BigDecimal plataGanada = BigDecimal.ZERO;

        for (Actividad act : actividades) {
            BigDecimal precioBase = act.getPrecio();

            for (Reserva reserva : act.getReservas()) {
                if ("Hecho".equals(reserva.getEstado())) {

                    BigDecimal cantidad = BigDecimal.valueOf(reserva.getCantidad());

                    // precioBase * cantidad → total de esa reserva
                    BigDecimal totalReserva = precioBase.multiply(cantidad);

                    BigDecimal comision = totalReserva.multiply(porcentajeDecimal);

                    // acumular la comisión
                    plataGanada = plataGanada.add(comision);
                }
            }
        }

        model.addAttribute("porcentajeComision", admin.getPorcentajeComision());
        model.addAttribute("plataGanada", plataGanada);
        model.addAttribute("CantidadCliente", clienteService.ContadorCliente());
        model.addAttribute("CantidadColaborador", colaboradorService.ContadorColaborador());
        model.addAttribute("CantidadUsuarios", usuarioService.ContadorUsuario());
        model.addAttribute("CantidadReservacion", reservaService.ContadorReservas());
        model.addAttribute("CantidadActividad", actividadService.ContadorActividades());
        model.addAttribute("CantidadDisponibilidades", disponibilidadService.ContadorDisponibilidades());
        model.addAttribute("horasCancelacion", admin.getHorasCancelacion());

        return "admin/dashboard";
    }

    @PostMapping("/comision")
    public String actualizarComision(@RequestParam BigDecimal porcentajeComision, RedirectAttributes redirectAttrs) {
        if (porcentajeComision.compareTo(BigDecimal.ZERO) < 0 || porcentajeComision.compareTo(new BigDecimal("100")) > 0) {
            redirectAttrs.addFlashAttribute("error", "El porcentaje de comision debe estar entre 0 y 100.");
            return "redirect:/admin/dashboard";
        }

        adminService.actualizarPorcentajeComision(porcentajeComision);
        redirectAttrs.addFlashAttribute("mensaje", "Porcentaje de comision actualizado correctamente.");
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
        // Obtener reservas con estado CONFIRMADA o COMPLETADA y estadoPagoColaborador = PENDIENTE_PAGO
        Page<Reserva> reservasPage = reservaService.obtenerReservasConPagoPendiente(PageRequest.of(page, 20));

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
            reservasPage = reservaService.obtenerPorEstado(estado, PageRequest.of(page, 50));
        }

        // Calcular estadísticas
        BigDecimal totalComisionesGanadas = reservasPage.getContent().stream()
            .filter(r -> "CONFIRMADA".equals(r.getEstado()) || "COMPLETADA".equals(r.getEstado()))
            .map(r -> r.getComisionEta() != null ? r.getComisionEta() : BigDecimal.ZERO)
            .reduce(BigDecimal.ZERO, BigDecimal::add);

        long totalReservas = reservasPage.getTotalElements();
        long reservasCompletadas = reservasPage.getContent().stream()
            .filter(r -> "COMPLETADA".equals(r.getEstado())).count();
        long reservasCanceladas = reservasPage.getContent().stream()
            .filter(r -> r.getEstado().startsWith("CANCELADA")).count();

        model.addAttribute("reservas", reservasPage.getContent());
        model.addAttribute("currentPage", page);
        model.addAttribute("totalPages", reservasPage.getTotalPages());
        model.addAttribute("totalComisionesGanadas", totalComisionesGanadas);
        model.addAttribute("totalReservas", totalReservas);
        model.addAttribute("reservasCompletadas", reservasCompletadas);
        model.addAttribute("reservasCanceladas", reservasCanceladas);
        model.addAttribute("estadoFiltro", estado);
        model.addAttribute("pagina", "ingresos");

        return "admin/ingresos";
    }

}

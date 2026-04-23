package maineta.eta.controller;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

import org.springframework.http.HttpStatus;
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
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import maineta.eta.config.EpaycoConfig;
import maineta.eta.config.UsuarioHelper;
import maineta.eta.dto.ActividadDTO;
import maineta.eta.dto.ReservaDTO;
import maineta.eta.entity.Actividad;
import maineta.eta.entity.Cliente;
import maineta.eta.entity.ConversacionChat;
import maineta.eta.entity.Disponibilidad;
import maineta.eta.entity.Favorito;
import maineta.eta.entity.MensajeChat;
import maineta.eta.entity.Reserva;
import maineta.eta.entity.Usuario;
import maineta.eta.service.ActividadService;
import maineta.eta.service.ChatService;
import maineta.eta.service.ClienteService;
import maineta.eta.service.ComentarioService;
import maineta.eta.service.DisponibilidadService;
import maineta.eta.service.EmailReservaService;
import maineta.eta.service.FavoritoService;
import maineta.eta.service.ReservaService;
import maineta.eta.service.UsuarioService;

@Controller
@RequestMapping("/cliente")
public class ClienteController {

    private final ActividadService actividadService;
    private final ReservaService reservaService;
    private final DisponibilidadService disponibilidadService;
    private final UsuarioHelper usuarioHelper;
    private final ClienteService clienteService;
    private final UsuarioService usuarioService;
    private final FavoritoService favoritoService;
    private final ComentarioService comentarioService;
    private final ChatService chatService;
    private final EmailReservaService emailReservaService;
    private final EpaycoConfig epaycoConfig;

    public ClienteController(
            ActividadService actividadService,
            ReservaService reservaService,
            DisponibilidadService disponibilidadService,
            UsuarioHelper usuarioHelper,
            ClienteService clienteService,
            UsuarioService usuarioService,
            FavoritoService favoritoService,
            ComentarioService comentarioService,
            ChatService chatService,
            EmailReservaService emailReservaService,
            EpaycoConfig epaycoConfig) {

        this.actividadService = actividadService;
        this.reservaService = reservaService;
        this.disponibilidadService = disponibilidadService;
        this.usuarioHelper = usuarioHelper;
        this.clienteService = clienteService;
        this.usuarioService = usuarioService;
        this.favoritoService = favoritoService;
        this.comentarioService = comentarioService;
        this.chatService = chatService;
        this.emailReservaService = emailReservaService;
        this.epaycoConfig = epaycoConfig;
    }

    @GetMapping("/dashboard")
    public String dashboard(Authentication authentication, Model model, Authentication auth) {
        usuarioHelper.agregarInfoUsuarioModel(model, auth);
        // Obtener el cliente autenticado
        String email = authentication.getName();
        System.out.println(email + "authentication name");
        Usuario usuario = usuarioService.obtenerPorEmail(email);
        Cliente cliente = clienteService.obtenerPorUsuario(usuario)
                .orElseThrow(() -> new RuntimeException("Cliente no encontrado"));

        // Obtener todas las reservas del cliente
        List<Reserva> reservas = reservaService.getReservasCliente(cliente);

        // Pasarlas al modelo
        model.addAttribute("reservas", reservas);

        return "cliente/dashboard";
    }

    @PostMapping("/checkout")
    public String checkout(@ModelAttribute ReservaDTO reservaDTO,
            RedirectAttributes redirectAttributes,
            @RequestParam("idActividad") Long idActividad,
            @RequestParam("fechaSeleccionada") String fechaSeleccionada,
            Model model,
            Authentication auth) {
        usuarioHelper.agregarInfoUsuarioModel(model, auth);
        Actividad actividad = actividadService.listarById(idActividad);
        if (actividad == null) {
            redirectAttributes.addFlashAttribute("error", "Debes seleccionar una disponibilidad valida.");
            return "redirect:/";
        }

        if (fechaSeleccionada == null || fechaSeleccionada.isBlank()) {
            redirectAttributes.addFlashAttribute("error", "Selecciona un dia disponible antes de continuar.");
            return "redirect:/actividad/" + usuarioHelper.generarTituloUrl(actividad.getTitulo()) + "-" + idActividad;
        }

        return "redirect:/cliente/checkout/actividad/" + idActividad + "?fecha=" + fechaSeleccionada;
    }

    @GetMapping("/checkout/{idDispo}")
    public String mostrarCheckout(@PathVariable Long idDispo,
            Authentication auth,
            Model model) {
        usuarioHelper.agregarInfoUsuarioModel(model, auth);

        // Obtener datos del cliente autenticado
        String email = auth.getName();
        Usuario usuario = usuarioService.obtenerPorEmail(email);
        Cliente cliente = clienteService.obtenerPorUsuario(usuario)
                .orElseThrow(() -> new RuntimeException("Cliente no encontrado"));

        Disponibilidad disponibilidad = disponibilidadService.obtenerPorId(idDispo)
                .orElseThrow(() -> new RuntimeException("Disponibilidad no encontrada"));

        ReservaDTO reservaDTO = new ReservaDTO();
        reservaDTO.setIdDisponibilidad(disponibilidad.getIdDisponibilidad());
        cargarModeloCheckout(model, disponibilidad.getActividad(), disponibilidad.getFecha(), disponibilidad, reservaDTO, null, cliente);

        return "cliente/checkout";
    }

    @GetMapping("/checkout/actividad/{idActividad}")
    public String mostrarCheckoutPorDia(
            @PathVariable Long idActividad,
            @RequestParam("fecha") String fecha,
            Authentication auth,
            Model model) {
        usuarioHelper.agregarInfoUsuarioModel(model, auth);

        // Obtener datos del cliente autenticado para ePayco
        String email = auth.getName();
        Usuario usuario = usuarioService.obtenerPorEmail(email);
        Cliente cliente = clienteService.obtenerPorUsuario(usuario)
                .orElseThrow(() -> new RuntimeException("Cliente no encontrado"));

        LocalDate fechaSeleccionada = LocalDate.parse(fecha);
        Actividad actividad = actividadService.listarById(idActividad);
        if (actividad == null) {
            throw new RuntimeException("Actividad no encontrada");
        }

        cargarModeloCheckout(model, actividad, fechaSeleccionada, null, new ReservaDTO(), null, cliente);
        return "cliente/checkout";
    }

    @GetMapping("/informacion")
    public String mostrarInformacionCliente(Authentication auth, Model model) {
        usuarioHelper.agregarInfoUsuarioModel(model, auth);
        String email = auth.getName();
        Usuario usuario = usuarioService.obtenerPorEmail(email);
        Optional<Cliente> clienteOpt = clienteService.obtenerPorUsuario(usuario);

        // Desempaquetar el Optional
        if (clienteOpt.isPresent()) {
            model.addAttribute("cliente", clienteOpt.get());
        } else {
            model.addAttribute("cliente", new Cliente());
        }

        return "cliente/informacion";
    }

    @PostMapping("/actualizar/{id}")
    public String actualizarActividad(@PathVariable Long id,
            @ModelAttribute("cliente") Cliente cliente,
            Authentication authentication,
            RedirectAttributes redirectAttributes) {
        try {
            // 1️⃣ Obtener el usuario autenticado actual
            // 2️⃣ Actualizar el cliente (y el usuario dentro de él)
            Cliente clienteActualizado = clienteService.actualizarCliente(id, cliente);

            // 3️⃣ Actualizar el Authentication con los nuevos datos
            Authentication newAuth = new UsernamePasswordAuthenticationToken(
                    clienteActualizado.getUsuario().getEmail(),
                    authentication.getCredentials(),
                    authentication.getAuthorities());
            SecurityContextHolder.getContext().setAuthentication(newAuth);

            redirectAttributes.addFlashAttribute("exito", "Se ha actualizado de manera exitosa");
        } catch (RuntimeException e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }

        return "redirect:/cliente/informacion";
    }

    @PostMapping("/reservar")
    public String procesarReserva(
            @RequestParam("idDisponibilidad") Long disponibilidadId,
            @RequestParam("cantidad") Integer cantidad,
            @RequestParam("idActividad") Long idActividad,
            Authentication authentication,
            Model model) {

        // ✅ Obtener el cliente autenticado (fuera del try para disponibilidad en catch)
        String email = authentication.getName();
        Usuario usuario = usuarioService.obtenerPorEmail(email);
        Cliente cliente = clienteService.obtenerPorUsuario(usuario)
                .orElseThrow(() -> new RuntimeException("Cliente no encontrado"));

        try {
            // ✅ Obtener la actividad
            Actividad actividad = actividadService.listarById(idActividad);
            if (actividad == null) {
                throw new RuntimeException("Actividad no encontrada");
            }

            // ✅ Obtener la disponibilidad
            Disponibilidad disponibilidad = disponibilidadService.obtenerPorId(disponibilidadId)
                    .orElseThrow(() -> new RuntimeException("Disponibilidad no encontrada"));

            // ✅ Crear la reserva y guardarla
            Reserva reservaGuardada = reservaService.hacerReserva(cliente, actividad, disponibilidad, cantidad);

            // ✅ Enviar email de confirmación
            try {
                emailReservaService.enviarEmailConfirmacionReserva(reservaGuardada);
            } catch (Exception e) {
                // Log error pero no interrumpir flujo
                System.err.println("Error al enviar email de confirmación: " + e.getMessage());
            }

            return "redirect:/cliente/reservas/" + reservaGuardada.getIdReserva() + "/recibo?nuevaReserva=true";

        } catch (Exception e) {
            // ⚠️ Error: recargar checkout con mensaje visible
            Actividad actividad = actividadService.listarById(idActividad);
            Disponibilidad disponibilidad = disponibilidadService.obtenerPorId(disponibilidadId)
                    .orElse(null);

            model.addAttribute("actividad", actividad);
            model.addAttribute("disponibilidad", disponibilidad);
            ReservaDTO reservaDTO = new ReservaDTO();
            reservaDTO.setIdDisponibilidad(disponibilidadId);
            reservaDTO.setCantidad(cantidad);
            model.addAttribute("reservaDTO", reservaDTO);
            model.addAttribute("pagina", "checkout");
            if (disponibilidad != null) {
                cargarModeloCheckout(model, actividad, disponibilidad.getFecha(), disponibilidad, reservaDTO, null, cliente);
            }
            model.addAttribute("error", "❌ Error al realizar la reserva: " + e.getMessage());

            return "cliente/checkout";
        }

    }

    @GetMapping("/reservas/{idReserva}/recibo")
    public String verReciboReserva(
            @PathVariable Long idReserva,
            @RequestParam(defaultValue = "false") boolean nuevaReserva,
            Authentication authentication,
            Authentication auth,
            Model model) {
        usuarioHelper.agregarInfoUsuarioModel(model, auth);

        Reserva reserva = obtenerReservaDelCliente(idReserva, authentication);
        cargarModeloRecibo(model, reserva, nuevaReserva);

        return "cliente/recibo";
    }

    @GetMapping("/cancelar/reserva/{id}")
    public String cancelarReservacion(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        Optional<Reserva> reservaOpt = reservaService.ObtenerReservaPorId(id);

        if (reservaOpt.isEmpty()) {
            redirectAttributes.addFlashAttribute("error", "No se encontró la reserva a cancelar");
            return "redirect:/cliente/dashboard";
        }

        Reserva reserva = reservaOpt.get();
        Disponibilidad disponibilidad = reserva.getDisponibilidad();

        // 🔄 Devolver cupos
        disponibilidad.setCuposDisponibles(disponibilidad.getCuposDisponibles() + reserva.getCantidad());

        disponibilidadService.guardarDisponibilidad(disponibilidad); // 🔹 asegurado update BD
        reservaService.EliminarReservacion(reserva); // 🔹 eliminar reserva

        redirectAttributes.addFlashAttribute("exito", "Reserva cancelada correctamente");
        return "redirect:/cliente/dashboard";
    }

    // ========================
    // FAVORITOS
    // ========================
    /**
     * Toggle favorito vía AJAX (POST). Retorna JSON con { "favorito":
     * true/false }
     */
    @PostMapping("/favorito/toggle/{idActividad}")
    @ResponseBody
    public Map<String, Object> toggleFavorito(@PathVariable Long idActividad, Authentication authentication) {
        String email = authentication.getName();
        Usuario usuario = usuarioService.obtenerPorEmail(email);
        Cliente cliente = clienteService.obtenerPorUsuario(usuario)
                .orElseThrow(() -> new RuntimeException("Cliente no encontrado"));

        Actividad actividad = actividadService.listarById(idActividad);
        boolean agregado = favoritoService.toggleFavorito(cliente, actividad);

        return Map.of("favorito", agregado);
    }

    /**
     * Página de favoritos del cliente.
     */
    @GetMapping("/favoritos")
    public String verFavoritos(Authentication authentication, Model model, Authentication auth) {
        usuarioHelper.agregarInfoUsuarioModel(model, auth);

        String email = authentication.getName();
        Usuario usuario = usuarioService.obtenerPorEmail(email);
        Cliente cliente = clienteService.obtenerPorUsuario(usuario)
                .orElseThrow(() -> new RuntimeException("Cliente no encontrado"));

        List<Favorito> favoritos = favoritoService.obtenerFavoritosDeCliente(cliente);

        // Obtener IDs de actividades para contar comentarios
        List<Long> actividadIds = favoritos.stream()
                .map(f -> f.getActividad().getIdActividad())
                .toList();

        Map<Long, Integer> comentariosPorActividad = actividadIds.isEmpty()
                ? Map.of()
                : comentarioService.contarComentariosPorActividades(actividadIds);

        // Mapear a DTOs
        List<ActividadDTO> actividadesDTO = favoritos.stream()
                .map(fav -> {
                    Actividad actividad = fav.getActividad();
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
                            comentariosPorActividad.getOrDefault(actividad.getIdActividad(), 0));

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
                    dto.setPrecioConsumidor(usuarioHelper.CalcularPrecioConsumidor(actividad.getPrecio()));

                    return dto;
                })
                .collect(Collectors.toList());

        model.addAttribute("actividades", actividadesDTO);
        return "cliente/favoritos";
    }

    @GetMapping("/chats")
    public String verChatsCliente(Authentication authentication, Model model, Authentication auth) {
        usuarioHelper.agregarInfoUsuarioModel(model, auth);

        String email = authentication.getName();
        Usuario usuario = usuarioService.obtenerPorEmail(email);
        Cliente cliente = clienteService.obtenerPorUsuario(usuario)
                .orElseThrow(() -> new RuntimeException("Cliente no encontrado"));

        model.addAttribute("emailUsuario", email);
        model.addAttribute("reservas", reservaService.getReservasCliente(cliente));
        model.addAttribute("conversaciones", chatService.listarConversacionesCliente(email));

        return "cliente/chats";
    }

    @GetMapping("/chats/{idReserva}")
    public String verChatClientePorReserva(@PathVariable Long idReserva, Authentication authentication, Model model,
            Authentication auth) {
        usuarioHelper.agregarInfoUsuarioModel(model, auth);

        String email = authentication.getName();
        Usuario usuario = usuarioService.obtenerPorEmail(email);
        Cliente cliente = clienteService.obtenerPorUsuario(usuario)
                .orElseThrow(() -> new RuntimeException("Cliente no encontrado"));

        ConversacionChat conversacion = chatService.obtenerOCrearConversacionDesdeReservaCliente(idReserva, email);
        List<MensajeChat> mensajes = chatService.listarMensajes(
                Objects.requireNonNull(conversacion.getIdConversacion()),
                email);

        model.addAttribute("emailUsuario", email);
        model.addAttribute("reservas", reservaService.getReservasCliente(cliente));
        model.addAttribute("conversaciones", chatService.listarConversacionesCliente(email));
        model.addAttribute("conversacionSeleccionada", conversacion);
        model.addAttribute("mensajes", mensajes);

        return "cliente/chats";
    }

    private Cliente obtenerClienteAutenticado(Authentication authentication) {
        String email = authentication.getName();
        Usuario usuario = usuarioService.obtenerPorEmail(email);
        return clienteService.obtenerPorUsuario(usuario)
                .orElseThrow(() -> new RuntimeException("Cliente no encontrado"));
    }

    private Reserva obtenerReservaDelCliente(Long idReserva, Authentication authentication) {
        Cliente cliente = obtenerClienteAutenticado(authentication);
        Reserva reserva = reservaService.ObtenerReservaPorId(idReserva)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Reserva no encontrada"));

        if (reserva.getCliente() == null || reserva.getCliente().getId() == null
                || !reserva.getCliente().getId().equals(cliente.getId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "No tienes permisos para ver esta reserva");
        }

        return reserva;
    }

    private void cargarModeloRecibo(Model model, Reserva reserva, boolean nuevaReserva) {
        BigDecimal precioUnitario = reserva.getActividad() != null && reserva.getActividad().getPrecio() != null
                ? reserva.getActividad().getPrecio()
                : BigDecimal.ZERO;
        BigDecimal totalReserva = precioUnitario.multiply(BigDecimal.valueOf(reserva.getCantidad()));

        model.addAttribute("reserva", reserva);
        model.addAttribute("precioUnitario", precioUnitario);
        model.addAttribute("totalReserva", totalReserva);
        model.addAttribute("nuevaReserva", nuevaReserva);
        model.addAttribute("tituloRecibo", nuevaReserva ? "Thank you!" : "Detalle de tu reserva");
        model.addAttribute("subtituloRecibo",
                nuevaReserva
                        ? "Tu reserva fue emitida correctamente y ya tienes tu recibo digital."
                        : "Aqui tienes el recibo de la reserva que seleccionaste desde tu dashboard.");
    }

    private void cargarModeloCheckout(Model model, Actividad actividad, LocalDate fechaSeleccionada,
            Disponibilidad disponibilidadSeleccionada, ReservaDTO reservaDTO, String error, Cliente cliente) {
        List<Disponibilidad> disponibilidadesDelDia = disponibilidadService.obtenerPorActividad(actividad.getIdActividad())
                .stream()
                .filter(disponibilidad -> "DISPONIBLE".equalsIgnoreCase(disponibilidad.getEstado()))
                .filter(disponibilidad -> disponibilidad.getCuposDisponibles() > 0)
                .filter(disponibilidad -> fechaSeleccionada != null && fechaSeleccionada.equals(disponibilidad.getFecha()))
                .toList();

        Disponibilidad disponibilidadActiva = disponibilidadSeleccionada;
        if (disponibilidadActiva == null && !disponibilidadesDelDia.isEmpty()) {
            disponibilidadActiva = disponibilidadesDelDia.get(0);
            reservaDTO.setIdDisponibilidad(disponibilidadActiva.getIdDisponibilidad());
        }

        // Calcular precio consumidor (con comisión)
        BigDecimal precioUnitario = usuarioHelper.CalcularPrecioConsumidor(actividad.getPrecio());

        model.addAttribute("actividad", actividad);
        model.addAttribute("fechaSeleccionada", fechaSeleccionada);
        model.addAttribute("disponibilidadesDelDia", disponibilidadesDelDia);
        model.addAttribute("disponibilidad", disponibilidadActiva);
        model.addAttribute("reservaDTO", reservaDTO);
        model.addAttribute("precioUnitario", precioUnitario);
        model.addAttribute("pagina", "checkout");
        
        // ✅ Datos de ePayco
        model.addAttribute("epaycoPublicKey", epaycoConfig.getPublicKey());
        model.addAttribute("epaycoTest", epaycoConfig.isTest());
        
        // ✅ Datos del cliente para pre-llenar ePayco
        if (cliente != null) {
            model.addAttribute("clienteNombre", cliente.getUsuario().getNombre());
            model.addAttribute("clienteEmail", cliente.getUsuario().getEmail());
            model.addAttribute("clienteTelefono", cliente.getUsuario().getTelefono());
            model.addAttribute("clienteId", cliente.getId());
        }
        
        if (error != null) {
            model.addAttribute("error", error);
        }
    }

}

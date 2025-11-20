package maineta.eta.controller;

import java.util.List;
import java.util.Optional;

import maineta.eta.dto.ReservaDTO;
import maineta.eta.entity.*;
import maineta.eta.service.*;
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
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import maineta.eta.config.UsuarioHelper;

@Controller
@RequestMapping("/cliente")
public class ClienteController {

    private final ActividadService actividadService;
    private final ReservaService reservaService;
    private final DisponibilidadService disponibilidadService;
    private final UsuarioHelper usuarioHelper;
    private final ClienteService clienteService;
    private final UsuarioService usuarioService;

    public ClienteController(
            ActividadService actividadService,
            ReservaService reservaService,
            DisponibilidadService disponibilidadService,
            UsuarioHelper usuarioHelper,
            ClienteService clienteService,
            UsuarioService usuarioService) {

        this.actividadService = actividadService;
        this.reservaService = reservaService;
        this.disponibilidadService = disponibilidadService;
        this.usuarioHelper = usuarioHelper;
        this.clienteService = clienteService;
        this.usuarioService = usuarioService;
    }

    @GetMapping("/dashboard")
    public String dashboard(Authentication authentication, Model model,Authentication auth, @RequestParam(required = false) String exito) {
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

        if (exito != null) {
            model.addAttribute("exito", true);
        }

        return "cliente/dashboard";
    }

    @PostMapping("/checkout")
    public String checkout(@ModelAttribute ReservaDTO reservaDTO,
                           RedirectAttributes redirectAttributes) {

        Long idDispo = reservaDTO.getIdDisponibilidad();
        return "redirect:/cliente/checkout/" + idDispo;
    }


    @GetMapping("/checkout/{idDispo}")
    public String mostrarCheckout(@PathVariable Long idDispo,
                                  Authentication auth,
                                  Model model) {


        Disponibilidad disponibilidad = disponibilidadService.obtenerPorId(idDispo)
                .orElseThrow(() -> new RuntimeException("Disponibilidad no encontrada"));

        model.addAttribute("disponibilidad", disponibilidad);
        model.addAttribute("reservaDTO", new ReservaDTO());
        model.addAttribute("actividad", disponibilidad.getActividad());

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
            String emailActual = authentication.getName();
            Usuario usuario = usuarioService.obtenerPorEmail(emailActual);

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

        try {
            // ✅ Obtener el cliente autenticado
            String email = authentication.getName();
            Usuario usuario = usuarioService.obtenerPorEmail(email);
            Cliente cliente = clienteService.obtenerPorUsuario(usuario)
                    .orElseThrow(() -> new RuntimeException("Cliente no encontrado"));

            // ✅ Obtener la actividad
            Actividad actividad = actividadService.listarById(idActividad);
            if (actividad == null) {
                throw new RuntimeException("Actividad no encontrada");
            }

            // ✅ Obtener la disponibilidad
            Disponibilidad disponibilidad = disponibilidadService.obtenerPorId(disponibilidadId)
                    .orElseThrow(() -> new RuntimeException("Disponibilidad no encontrada"));

            // ✅ Crear la reserva y guardarla
            reservaService.hacerReserva(cliente, actividad, disponibilidad, cantidad);

            // ✅ Mensaje de éxito
            model.addAttribute("mensaje", "🎉 ¡Reserva realizada con éxito!");
            model.addAttribute("actividad", actividad);
            model.addAttribute("pagina", "checkout");

            // 🔁 También mostrar disponibilidades en caso de querer reservar otro cupo
            List<Disponibilidad> disponibilidades = disponibilidadService.obtenerPorActividad(idActividad);
            model.addAttribute("disponibilidades", disponibilidades);

            return "redirect:/cliente/dashboard?exito";

        } catch (Exception e) {
            // ⚠️ Error: recargar checkout con mensaje visible
            Actividad actividad = actividadService.listarById(idActividad);
            List<Disponibilidad> disponibilidades = disponibilidadService.obtenerPorActividad(idActividad);

            model.addAttribute("actividad", actividad);
            model.addAttribute("disponibilidades", disponibilidades);
            model.addAttribute("reserva", new Reserva());
            model.addAttribute("pagina", "checkout");
            model.addAttribute("error", "❌ Error al realizar la reserva: " + e.getMessage());

            return "cliente/checkout";
        }

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


}

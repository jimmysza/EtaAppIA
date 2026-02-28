package maineta.eta.controller;

import java.security.Principal;
import java.time.LocalDateTime;

import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import maineta.eta.config.UsuarioHelper;
import maineta.eta.entity.Actividad;
import maineta.eta.entity.Cliente;
import maineta.eta.entity.Comentario;
import maineta.eta.entity.Usuario;
import maineta.eta.service.ActividadService;
import maineta.eta.service.ClienteService;
import maineta.eta.service.ComentarioService;
import maineta.eta.service.ReservaService;
import maineta.eta.service.UsuarioService;

@Controller
@RequestMapping("/comentarios")
public class ComentarioController {

    private final ComentarioService comentarioService;
    private final ActividadService actividadService;
    private final UsuarioService usuarioService;
    private final ReservaService reservaService;
    private final ClienteService clienteService;
    private final UsuarioHelper usuarioHelper;

    public ComentarioController(ComentarioService comentarioService,
            ActividadService actividadService,
            UsuarioService usuarioService,
            ReservaService reservaService, ClienteService clienteService, UsuarioHelper usuarioHelper) {
        this.comentarioService = comentarioService;
        this.actividadService = actividadService;
        this.usuarioService = usuarioService;
        this.reservaService = reservaService;
        this.clienteService = clienteService;
        this.usuarioHelper = usuarioHelper;
    }

    public void ActualizarCalificacionActividad() {

    }

    @Transactional
    @PostMapping("/agregar/{idActividad}")
    public String agregarComentario(
            @PathVariable Long idActividad,
            @RequestParam("texto") String texto,
            @RequestParam("calificacion") Integer calificacion,
            Principal principal,
            RedirectAttributes redirectAttributes) {

        Usuario usuario = usuarioService.obtenerPorEmail(principal.getName());
        Actividad actividad = actividadService.obtenerPorId(idActividad);

        if (actividad == null) {
            redirectAttributes.addFlashAttribute("error", "Actividad no encontrada");
            return "redirect:/";
        }

        Cliente cliente = clienteService.obtenerPorUsuario(usuario)
                .orElseThrow(() -> new RuntimeException("No se encontró cliente para el usuario"));

        // 🔹 Validar reserva realizada (usar cliente.getId(), no usuario.getId())
        boolean realizoActividad = reservaService.existeReservaRealizada(
                cliente.getId(),
                actividad.getIdActividad());

        if (!realizoActividad) {
            redirectAttributes.addFlashAttribute(
                    "error",
                    "Debes realizar la actividad para poder comentar");
            return "redirect:/actividad/" + usuarioHelper.generarTituloUrl(actividad.getTitulo()) + "-" + idActividad;
        }

        // 🔹 Guardar comentario
        Comentario comentario = new Comentario();
        comentario.setActividad(actividad);
        comentario.setCliente(cliente);
        comentario.setTexto(texto);
        comentario.setCalificacion(calificacion);
        comentario.setFechaComentario(LocalDateTime.now());

        comentarioService.guardar(comentario);

        // 🔹 Recalcular promedio (INCLUYE el nuevo comentario)
        int promedio = comentarioService.calcularPromedioActividad(idActividad);
        actividad.setCalificacion(promedio);

        redirectAttributes.addFlashAttribute("exito", "Comentario creado correctamente");

        return "redirect:/actividad/" + usuarioHelper.generarTituloUrl(actividad.getTitulo())
            + "-" + idActividad;
    }

}

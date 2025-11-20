package maineta.eta.controller;

import java.security.Principal;
import java.time.LocalDateTime;
import java.util.List;

import maineta.eta.entity.Actividad;
import maineta.eta.entity.Cliente;
import maineta.eta.entity.Comentario;
import maineta.eta.entity.Usuario;
import maineta.eta.service.*;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/comentarios")
public class ComentarioController {

    private final ComentarioService comentarioService;
    private final ActividadService actividadService;
    private final UsuarioService usuarioService;
    private final ReservaService reservaService;
    private final ClienteService clienteService;

    public ComentarioController(ComentarioService comentarioService,
                                ActividadService actividadService,
                                UsuarioService usuarioService,
                                ReservaService reservaService,ClienteService clienteService) {
        this.comentarioService = comentarioService;
        this.actividadService = actividadService;
        this.usuarioService = usuarioService;
        this.reservaService = reservaService;
        this.clienteService = clienteService;
    }

    public void ActualizarCalificacionActividad(){

    }

    @Transactional
    @PostMapping("/agregar/{idActividad}")
    public String agregarComentario(@PathVariable Long idActividad,
                                    @RequestParam("texto") String texto,
                                    @RequestParam("calificacion") Integer calificacion,
                                    Principal principal,RedirectAttributes redirectAttributes) {

        Usuario usuario = usuarioService.obtenerPorEmail(principal.getName());
        Actividad actividad = actividadService.obtenerPorId(idActividad);


        int suma = 0;
        int contador = 0;

        List<Comentario> comentarios = comentarioService.listarPorActividad(idActividad);

        for (Comentario comentario : comentarios) {
            suma += comentario.getCalificacion();
            contador++;
        }

        int promedio = 0;

        if (contador > 0) {
            promedio = (int) Math.round((double) suma / contador);
        }

        actividad.setCalificacion(promedio);

        boolean realizoActividad = reservaService.existeReservaRealizada(usuario.getId(), actividad.getIdActividad());

        if (realizoActividad) {
            redirectAttributes.addFlashAttribute("error", "No Has Realizado la Actividad");
            return "redirect:/detalle/" + idActividad;
        }

        Cliente cliente = clienteService.obtenerPorUsuario(usuario)
                .orElseThrow(() -> new RuntimeException("No se encontró cliente para el usuario"));




        Comentario comentario = new Comentario();
        comentario.setActividad(actividad);
        comentario.setFechaComentario(LocalDateTime.now());
        comentario.setTexto(texto);
        comentario.setCliente(cliente);
        comentario.setCalificacion(calificacion);

        comentario.setFechaComentario(LocalDateTime.now());




        comentarioService.guardar(comentario);

        redirectAttributes.addFlashAttribute("exito", "Comentario Creado");
        return "redirect:/detalle/" + idActividad;
    }


}

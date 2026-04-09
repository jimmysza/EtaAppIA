package maineta.eta.controller;

import java.util.List;

import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;

import maineta.eta.dto.PlanDTO;
import maineta.eta.entity.Cliente;
import maineta.eta.entity.Usuario;
import maineta.eta.service.ClienteService;
import maineta.eta.service.PlanService;
import maineta.eta.service.UsuarioService;

/**
 * Controlador para rutas públicas de Planes del Día.
 * 
 * Rutas:
 * - GET /planes → Vista principal con top 5 planes y mapa
 * - GET /planes/{id} → Detalle de un plan específico
 */
@Controller
@RequestMapping("/planes")
public class PlanController {

    private final PlanService planService;
    private final UsuarioService usuarioService;
    private final ClienteService clienteService;

    public PlanController(PlanService planService, UsuarioService usuarioService, ClienteService clienteService) {
        this.planService = planService;
        this.usuarioService = usuarioService;
        this.clienteService = clienteService;
    }

    /**
     * Vista principal de planes con mapa y lista.
     */
    @GetMapping("")
    public String verPlanes(Model model, Authentication authentication) {
        // Obtener top 5 planes recientes
        List<PlanDTO> topPlanes = planService.obtenerTop5Recientes();
        model.addAttribute("topPlanes", topPlanes);

        // Verificar si el usuario está autenticado para mostrar botón crear
        if (authentication != null && authentication.isAuthenticated()) {
            Usuario usuario = usuarioService.obtenerPorEmail(authentication.getName());
            model.addAttribute("usuarioAutenticado", usuario);
            
            // Verificar si es cliente para saber qué ruta usar en el botón crear
            try {
                Cliente cliente = clienteService.obtenerPorUsuario(usuario).orElse(null);
                model.addAttribute("esCliente", cliente != null);
            } catch (Exception e) {
                model.addAttribute("esCliente", false);
            }
        }

        return "planes/planes"; // templates/planes/planes.html
    }

    /**
     * Detalle de un plan específico.
     */
    @GetMapping("/{id}")
    public String verDetallePlan(@PathVariable Long id, Model model, Authentication authentication) {
        // Obtener plan y sus actividades
        PlanDTO plan = planService.obtenerPorId(id);
        model.addAttribute("plan", plan);

        // Verificar si el usuario está autenticado
        if (authentication != null && authentication.isAuthenticated()) {
            Usuario usuario = usuarioService.obtenerPorEmail(authentication.getName());
            model.addAttribute("usuarioAutenticado", usuario);
        }

        return "planes/detalle-plan"; // templates/planes/detalle-plan.html
    }
}

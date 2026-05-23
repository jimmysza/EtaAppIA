package maineta.eta.controller;

import java.util.List;

import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import jakarta.validation.Valid;
import maineta.eta.config.UsuarioHelper;
import maineta.eta.dto.CrearPlanFormDTO;
import maineta.eta.dto.PlanDTO;
import maineta.eta.entity.Cliente;
import maineta.eta.entity.Plan;
import maineta.eta.entity.Usuario;
import maineta.eta.service.ClienteService;
import maineta.eta.service.PlanService;
import maineta.eta.service.UsuarioService;

/**
 * Controlador para gestión de Planes del Día por parte de Clientes.
 * 
 * Rutas (todas bajo /cliente/planes/*):
 * - GET /crear → Formulario para crear plan
 * - POST /crear → Procesa creación de plan
 * - GET /mis-planes → Lista de planes creados por el cliente
 */
@Controller
@RequestMapping("/cliente/planes")
public class ClientePlanController {

    private final PlanService planService;
    private final ClienteService clienteService;
    private final UsuarioService usuarioService;
    private final UsuarioHelper usuarioHelper;

    public ClientePlanController(PlanService planService, ClienteService clienteService, UsuarioService usuarioService, UsuarioHelper usuarioHelper) {
        this.planService = planService;
        this.clienteService = clienteService;
        this.usuarioService = usuarioService;
        this.usuarioHelper = usuarioHelper;
    }

    /**
     * Muestra el formulario para crear un nuevo plan.
     */
    @GetMapping("/crear")
    public String mostrarFormularioCrear(Model model, Authentication authentication) {
        model.addAttribute("form", new CrearPlanFormDTO());
        model.addAttribute("rolActual", "CLIENTE");
        model.addAttribute("formAction", "/cliente/planes/crear");
        usuarioHelper.agregarInfoUsuarioModel(model, authentication);
        
        // Obtener cliente para validaciones futuras
        Usuario usuario = usuarioService.obtenerPorEmail(authentication.getName());
        Cliente cliente = clienteService.obtenerPorUsuario(usuario)
            .orElseThrow(() -> new RuntimeException("Cliente no encontrado"));
        model.addAttribute("cliente", cliente);

        return "planes/crear-plan"; // templates/planes/crear-plan.html
    }

    /**
     * Procesa la creación de un nuevo plan.
     */
    @PostMapping("/crear")
    public String crearPlan(
            @Valid @ModelAttribute("form") CrearPlanFormDTO form,
            BindingResult result,
            Authentication authentication,
            RedirectAttributes redirectAttributes) {

        if (result.hasErrors()) {
            redirectAttributes.addFlashAttribute("error", "Por favor completa todos los campos obligatorios");
            return "redirect:/cliente/planes/crear";
        }

        try {
            // Obtener ID del cliente autenticado
            Usuario usuario = usuarioService.obtenerPorEmail(authentication.getName());
            Cliente cliente = clienteService.obtenerPorUsuario(usuario)
                .orElseThrow(() -> new RuntimeException("Cliente no encontrado"));
            
            // Crear plan
            Plan plan = planService.crearPlan(form, cliente.getId(), "CLIENTE");
            
            redirectAttributes.addFlashAttribute("success", "Plan creado exitosamente");
            return "redirect:/planes/" + plan.getId();
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Error al crear el plan: " + e.getMessage());
            return "redirect:/cliente/planes/crear";
        }
    }

    /**
     * Lista todos los planes creados por el cliente.
     */
    @GetMapping("/mis-planes")
    public String listarMisPlanes(Model model, Authentication authentication) {
        // Obtener cliente autenticado
        Usuario usuario = usuarioService.obtenerPorEmail(authentication.getName());
        Cliente cliente = clienteService.obtenerPorUsuario(usuario)
            .orElseThrow(() -> new RuntimeException("Cliente no encontrado"));
        
        // Obtener planes del cliente
        List<PlanDTO> planes = planService.obtenerPlanesPorCliente(cliente.getId());
        model.addAttribute("planes", planes);
        model.addAttribute("rolCreador", "CLIENTE");

        return "planes/mis-planes"; // templates/planes/mis-planes.html
    }
}

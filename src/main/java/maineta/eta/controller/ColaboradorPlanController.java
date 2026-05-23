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
import maineta.eta.entity.Colaborador;
import maineta.eta.entity.Plan;
import maineta.eta.entity.Usuario;
import maineta.eta.service.ColaboradorService;
import maineta.eta.service.PlanService;
import maineta.eta.service.UsuarioService;

/**
 * Controlador para gestión de Planes del Día por parte de Colaboradores.
 * 
 * Rutas (todas bajo /colaborador/planes/*):
 * - GET /crear → Formulario para crear plan
 * - POST /crear → Procesa creación de plan
 * - GET /mis-planes → Lista de planes creados por el colaborador
 */
@Controller
@RequestMapping("/colaborador/planes")
public class ColaboradorPlanController {

    private final PlanService planService;
    private final ColaboradorService colaboradorService;
    private final UsuarioService usuarioService;
    private final UsuarioHelper usuarioHelper;

    public ColaboradorPlanController(PlanService planService, ColaboradorService colaboradorService, UsuarioService usuarioService, UsuarioHelper usuarioHelper) {
        this.planService = planService;
        this.colaboradorService = colaboradorService;
        this.usuarioService = usuarioService;
        this.usuarioHelper = usuarioHelper;
    }

    /**
     * Muestra el formulario para crear un nuevo plan.
     */
    @GetMapping("/crear")
    public String mostrarFormularioCrear(Model model, Authentication authentication) {
        model.addAttribute("form", new CrearPlanFormDTO());
        model.addAttribute("rolActual", "COLABORADOR");
        model.addAttribute("formAction", "/colaborador/planes/crear");
        usuarioHelper.agregarInfoUsuarioModel(model, authentication);
        
        // Obtener colaborador para validaciones futuras
        Usuario usuario = usuarioService.obtenerPorEmail(authentication.getName());
        Colaborador colaborador = colaboradorService.obtenerPorUsuario(usuario)
            .orElseThrow(() -> new RuntimeException("Colaborador no encontrado"));
        model.addAttribute("colaborador", colaborador);

        return "planes/crear-plan"; // templates/planes/crear-plan.html (mismo template que cliente)
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
            return "redirect:/colaborador/planes/crear";
        }

        try {
            // Obtener ID del colaborador autenticado
            Usuario usuario = usuarioService.obtenerPorEmail(authentication.getName());
            Colaborador colaborador = colaboradorService.obtenerPorUsuario(usuario)
                .orElseThrow(() -> new RuntimeException("Colaborador no encontrado"));
            
            // Crear  plan (convertir idColaborador de Integer a Long)
            Plan plan = planService.crearPlan(form, colaborador.getIdColaborador(), "COLABORADOR");
            
            redirectAttributes.addFlashAttribute("success", "Plan creado exitosamente");
            return "redirect:/planes/" + plan.getId().toString();
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Error al crear el plan: " + e.getMessage());
            return "redirect:/colaborador/planes/crear";
        }
    }

    /**
     * Lista todos los planes creados por el colaborador.
     */
    @GetMapping("/mis-planes")
    public String listarMisPlanes(Model model, Authentication authentication) {
        // Obtener colaborador autenticado
        Usuario usuario = usuarioService.obtenerPorEmail(authentication.getName());
        Colaborador colaborador = colaboradorService.obtenerPorUsuario(usuario)
            .orElseThrow(() -> new RuntimeException("Colaborador no encontrado"));
        
        // Obtener planes del colaborador (convertir Integer a Long)
        List<PlanDTO> planes = planService.obtenerPlanesPorColaborador(colaborador.getIdColaborador());
        model.addAttribute("planes", planes);
        model.addAttribute("rolCreador", "COLABORADOR");

        return "planes/mis-planes"; // templates/planes/mis-planes.html (mismo template)
    }
}

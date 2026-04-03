package maineta.eta.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import jakarta.validation.Valid;
import maineta.eta.dto.OnboardingForm;
import maineta.eta.entity.Cliente;
import maineta.eta.entity.Usuario;
import maineta.eta.service.CategoriaService;
import maineta.eta.service.ClienteService;
import maineta.eta.service.UsuarioService;

/**
 * Controlador para el flujo de onboarding del cliente
 */
@Controller
@RequestMapping("/cliente/onboarding")
public class OnboardingController {

    private final ClienteService clienteService;
    private final CategoriaService categoriaService;
    private final UsuarioService usuarioService;

    @Autowired
    public OnboardingController(
            ClienteService clienteService,
            CategoriaService categoriaService,
            UsuarioService usuarioService
    ) {
        this.clienteService = clienteService;
        this.categoriaService = categoriaService;
        this.usuarioService = usuarioService;
    }

    @GetMapping
    public String mostrarOnboarding(
            Model model,
            @AuthenticationPrincipal UserDetails userDetails
    ) {
        // Cargar todas las categorías para la pregunta 1
        model.addAttribute("categorias", categoriaService.listarCategorias());
        model.addAttribute("form", new OnboardingForm());
        
        return "cliente/onboarding";
    }

    @PostMapping
    public String guardarOnboarding(
            @Valid @ModelAttribute("form") OnboardingForm form,
            BindingResult result,
            Model model,
            @AuthenticationPrincipal UserDetails userDetails
    ) {
        if (result.hasErrors()) {
            model.addAttribute("categorias", categoriaService.listarCategorias());
            return "cliente/onboarding";
        }

        try {
            // Obtener el usuario autenticado
            Usuario usuario = usuarioService.obtenerPorEmail(userDetails.getUsername());
            
            // Obtener el cliente asociado
            Cliente cliente = clienteService.obtenerPorUsuario(usuario)
                .orElseThrow(() -> new RuntimeException("Cliente no encontrado"));
            
            // Guardar las preferencias
            clienteService.guardarPreferencias(cliente, form);
            
            return "redirect:/cliente/dashboard";
        } catch (Exception e) {
            model.addAttribute("error", "Error al guardar las preferencias: " + e.getMessage());
            model.addAttribute("categorias", categoriaService.listarCategorias());
            return "cliente/onboarding";
        }
    }
}

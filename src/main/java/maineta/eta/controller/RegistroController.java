package maineta.eta.controller;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import maineta.eta.dto.OnboardingForm;
import maineta.eta.entity.Admin;
import maineta.eta.entity.Cliente;
import maineta.eta.entity.Colaborador;
import maineta.eta.entity.DisponibilidadSemana;
import maineta.eta.entity.GrupoViaje;
import maineta.eta.entity.RangoPrecio;
import maineta.eta.entity.Usuario;
import maineta.eta.service.AdminService;
import maineta.eta.service.CategoriaService;
import maineta.eta.service.ClienteService;
import maineta.eta.service.ColaboradorService;
import maineta.eta.service.UsuarioService;
import maineta.eta.service.VerificacionCorreoService;
import maineta.eta.service.VerificacionCorreoService.EstadoVerificacion;

/**
 * 🔹 Controlador que se encarga del registro de nuevos usuarios en el sistema.
 */
@Controller
@RequestMapping("/registro")
public class RegistroController {

    private final UsuarioService usuarioService;
    private final ClienteService clienteService;
    private final ColaboradorService colaboradorService;
    private final AdminService adminService;
    private final VerificacionCorreoService verificacionCorreoService;
    private final CategoriaService categoriaService;

    @Autowired
    public RegistroController(
            UsuarioService usuarioService,
            ClienteService clienteService,
            AdminService adminService,
            ColaboradorService colaboradorService,
            VerificacionCorreoService verificacionCorreoService,
            CategoriaService categoriaService) {
        this.usuarioService = usuarioService;
        this.colaboradorService = colaboradorService;
        this.clienteService = clienteService;
        this.adminService = adminService;
        this.verificacionCorreoService = verificacionCorreoService;
        this.categoriaService = categoriaService;
    }

    // ==================================================
    //  REGISTRO DE CLIENTE
    // ==================================================
    @GetMapping("/cliente")
    public String mostrarRegistroCliente(Model model) {
        Cliente cliente = new Cliente();
        cliente.setUsuario(new Usuario());
        model.addAttribute("cliente", cliente);
        model.addAttribute("role", "Cliente");
        // Cargar categorías para el onboarding integrado
        model.addAttribute("categorias", categoriaService.listarCategorias());
        return "auth/registroCliente";
    }

    @PostMapping("/cliente")
    public String registrarCliente(
            @ModelAttribute("cliente") Cliente cliente,
            @RequestParam(value = "categoriasIds", required = false) List<Long> categoriasIds,
            @RequestParam(value = "grupoViaje", required = false) String grupoViaje,
            @RequestParam(value = "rangoPrecio", required = false) String rangoPrecio,
            @RequestParam(value = "disponibilidadSemana", required = false) String disponibilidadSemana,
            RedirectAttributes redirectAttributes,
            Model model) {
        try {
            // Crear el OnboardingForm con los datos recibidos
            OnboardingForm form = new OnboardingForm();
            form.setCategoriasIds(categoriasIds);
            
            // Convertir String a ENUM
            if (grupoViaje != null && !grupoViaje.isEmpty()) {
                form.setGrupoViaje(GrupoViaje.valueOf(grupoViaje));
            }
            if (rangoPrecio != null && !rangoPrecio.isEmpty()) {
                form.setRangoPrecio(RangoPrecio.valueOf(rangoPrecio));
            }
            if (disponibilidadSemana != null && !disponibilidadSemana.isEmpty()) {
                form.setDisponibilidadSemana(DisponibilidadSemana.valueOf(disponibilidadSemana));
            }
            
            // Registrar cliente con preferencias y enviar email
            clienteService.registrarClienteConPreferencias(cliente, form);
            
            // Redirigir al login con mensaje de verificación pendiente
            return "redirect:/login?role=cliente&pendingVerification";
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
            model.addAttribute("categorias", categoriaService.listarCategorias());
            return "auth/registroCliente";
        }
    }

    // ==================================================
    //  REGISTRO DE COLABORADOR
    // ==================================================
    @GetMapping("/colaborador")
    public String mostrarRegistroColaborador(Model model) {
        Colaborador colaborador = new Colaborador();
        colaborador.setUsuario(new Usuario());
        model.addAttribute("colaborador", colaborador);
        model.addAttribute("role", "Colaborador");
        return "auth/registroColaborador";
    }

    @PostMapping("/colaborador")
    public String registrarColaborador(
            @ModelAttribute("colaborador") Colaborador colaborador,
            RedirectAttributes redirectAttributes) {
        try {
            colaboradorService.registrarColaborador(colaborador); 
            return "redirect:/login?role=colaborador&pendingVerification";
        } catch (RuntimeException e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
            return "redirect:/registro/colaborador";
        }
    }

    // ==================================================
    //  REGISTRO DE ADMIN
    // ==================================================
    @GetMapping("/admin")
    public String mostrarRegistroAdmin(Model model) {
        Admin admin = new Admin();
        admin.setUsuario(new Usuario());
        model.addAttribute("admin", admin);
        model.addAttribute("role", "Admin");
        return "auth/registroAdmin";
    }

    @PostMapping("/admin")
    public String registrarAdmin(
            @ModelAttribute("admin") Admin admin,
            RedirectAttributes redirectAttributes) {
        try {
            adminService.registrarAdmin(admin);
            redirectAttributes.addFlashAttribute("exito", "Administrador registrado con éxito.");
            return "redirect:/login";
        } catch (RuntimeException e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
            return "redirect:/registro/admin";
        }
    }

    // ==================================================
    //  VERIFICACIÓN DE CORREO
    // ==================================================
    @GetMapping("/verificar")
    public String verificarCuenta(@RequestParam("token") String token) {
        EstadoVerificacion estado = verificacionCorreoService.verificarCuenta(token);

        if (estado == EstadoVerificacion.VERIFICADA || estado == EstadoVerificacion.YA_VERIFICADA) {
            return "redirect:/login?verified";
        }

        if (estado == EstadoVerificacion.TOKEN_EXPIRADO) {
            return "redirect:/login?expiredToken";
        }

        return "redirect:/login?invalidToken";
    }
}

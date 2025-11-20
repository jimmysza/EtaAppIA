package maineta.eta.controller;

import maineta.eta.entity.Admin;
import maineta.eta.entity.Cliente;
import maineta.eta.entity.Colaborador;
import maineta.eta.entity.Usuario;
import maineta.eta.service.AdminService;
import maineta.eta.service.ClienteService;
import maineta.eta.service.ColaboradorService;
import maineta.eta.service.UsuarioService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/**
 * 🔹 Controlador que se encarga del registro de nuevos usuarios en el sistema.
 *
 * Existen dos tipos de registros: - Cliente - Colaborador
 *
 * Este controlador muestra los formularios de registro y procesa los datos
 * enviados.
 *
 * Spring MVC utiliza est   e patrón: 1. El usuario hace una petición (GET o POST).
 * 2. El controlador recibe la petición y prepara los datos. 3. Los datos se
 * pasan a la vista (HTML con Thymeleaf). 4. En el caso de POST, los datos se
 * envían al servicio para guardarlos en la base de datos.
 */
@Controller
@RequestMapping("/registro") // Todas las rutas definidas aquí empezarán con "/registro"
public class RegistroController {

    // Servicios que contienen la lógica de negocio
    // UsuarioService: Maneja la lógica de usuarios en general
    // ClienteService: Lógica específica para clientes
    // ColaboradorService: Lógica específica para colaboradores
    @Autowired
    private final UsuarioService usuarioService;
    private final ClienteService clienteService;
    private final ColaboradorService colaboradorService;
    private final AdminService adminService;

    // 🔹 Constructor con inyección de dependencias
    // Spring automáticamente inyecta los servicios necesarios al crear este controlador
    RegistroController(UsuarioService usuarioService,
            ClienteService clienteService, AdminService adminService,
            ColaboradorService colaboradorService) {
        this.usuarioService = usuarioService;
        this.colaboradorService = colaboradorService;
        this.clienteService = clienteService;
        this.adminService = adminService;
    }

    // ==================================================
    //  REGISTRO DE CLIENTE
    // ==================================================
    /**
     * Muestra el formulario de registro para un cliente.
     *
     * - Crea un objeto "Cliente" vacío. - También crea un objeto "Usuario"
     * dentro del cliente, porque cada cliente necesita estar vinculado a un
     * usuario (correo, contraseña, etc.). - Se añade al modelo el objeto
     * cliente y el rol "Cliente".
     *
     * El objeto agregado al modelo será utilizado en el formulario Thymeleaf
     * para enlazar los campos del formulario con las propiedades del objeto.
     */
    @GetMapping("/cliente")
    public String mostrarRegistroCliente(Model model) {
        Cliente cliente = new Cliente();
        cliente.setUsuario(new Usuario()); // Asignamos un usuario vacío al cliente
        model.addAttribute("cliente", cliente); // Se pasa al formulario
        model.addAttribute("role", "Cliente");  // El rol se pasa para personalizar la vista
        return "auth/registroCliente"; // templates/auth/registroCliente.html
    }

    /**
     * Procesa el formulario de registro de cliente.
     *
     * - Recibe el objeto Cliente con los datos del formulario gracias a
     *
     * @ModelAttribute. - Llama al servicio para guardar el cliente en la base
     * de datos. - Agrega un mensaje de éxito con RedirectAttributes (mensaje
     * temporal que vive solo hasta la siguiente petición). - Redirige al login,
     * pasando por parámetro el rol "cliente" para que el formulario de login se
     * adapte a ese rol.
     */
    @PostMapping("/cliente")
    public String registrarCliente(@ModelAttribute("cliente") Cliente cliente,
            RedirectAttributes redirectAttributes) {
        try {
            clienteService.registrarCliente(cliente);
            redirectAttributes.addFlashAttribute("exito", "Cliente registrado con éxito.");
            return "redirect:/login?role=cliente";
        } catch (RuntimeException e) {
            // ✅ Usa el mensaje EXACTO que vino del servicio
            redirectAttributes.addFlashAttribute("error", e.getMessage());
            return "redirect:/registro/cliente";
        }
    }

    // ==================================================
    //  REGISTRO DE COLABORADOR
    // ==================================================
    /**
     * Muestra el formulario de registro para un colaborador.
     *
     * Funciona igual que el registro de cliente: - Se crea un objeto
     * "Colaborador". - Se asocia un objeto "Usuario" vacío. - Se añade al
     * modelo para que el formulario de Thymeleaf lo use.
     */
    @GetMapping("/colaborador")
    public String mostrarRegistroColaborador(Model model) {
        Colaborador colaborador = new Colaborador();
        colaborador.setUsuario(new Usuario()); // Asociamos un usuario vacío al colaborador
        model.addAttribute("colaborador", colaborador);
        model.addAttribute("role", "Colaborador"); // Se pasa el rol a la vista
        return "auth/registroColaborador"; // templates/auth/registroColaborador.html
    }

  
    @PostMapping("/colaborador")
    public String registrarColaborador(@ModelAttribute("colaborador") Colaborador colaborador,
            RedirectAttributes redirectAttributes) {

        try {
            colaboradorService.registrarColaborador(colaborador); 
            redirectAttributes.addFlashAttribute("exito", "Colaborador registrado con éxito.");
            return "redirect:/login?role=colaborador&exito";

        } catch (RuntimeException e) {
            
            redirectAttributes.addFlashAttribute("error", e.getMessage());
            return "redirect:/registro/colaborador";
        }
    }

    @GetMapping("/admin")
    public String mostrarRegistroAdmin(Model model) {
        Admin admin = new Admin();
        admin.setUsuario(new Usuario()); // Asignamos un usuario vacío al cliente
        model.addAttribute("admin", admin); // Se pasa al formulario
        model.addAttribute("role", "Admin");  // El rol se pasa para personalizar la vista
        return "auth/registroAdmin"; // templates/auth/registroCliente.html
    }

    @PostMapping("/admin")
    public String registrarColaborador(@ModelAttribute("admin") Admin admin, RedirectAttributes redirectAttributes) {
        try{
            adminService.registrarAdmin(admin); // Guardar colaborador en BD
            redirectAttributes.addFlashAttribute("exito", "Colaborador registrado con éxito.");
            return "redirect:/login"; // Redirige al login con rol "colaborador"
        } catch (RuntimeException e) {
            // ✅ Usa el mensaje EXACTO que vino del servicio
            redirectAttributes.addFlashAttribute("error", e.getMessage());
            return "redirect:/registro/admin";
        }
    }
}

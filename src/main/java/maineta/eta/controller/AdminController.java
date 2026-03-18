package maineta.eta.controller;

import java.math.BigDecimal;
import java.util.List;

import maineta.eta.entity.Actividad;
import maineta.eta.entity.Categoria;
import maineta.eta.entity.Idioma;
import maineta.eta.entity.Reserva;
import maineta.eta.service.*;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/admin")
public class AdminController {

    private final CategoriaService categoriaService;
    private final IdiomaService idiomaService;

    private final ComentarioService comentarioService;
    private final ActividadService actividadService;
    private final UsuarioService usuarioService;
    private final ReservaService reservaService;
    private final ClienteService clienteService;
    private final ColaboradorService colaboradorService;
    private final DisponibilidadService disponibilidadService;

    /* @Autowired */
    public AdminController(CategoriaService categoriaService, IdiomaService idiomaService, ComentarioService comentarioService,
            ActividadService actividadService,
            UsuarioService usuarioService, ColaboradorService colaboradorService,
            ReservaService reservaService,
            ClienteService clienteService, DisponibilidadService disponibilidadService) {
        this.categoriaService = categoriaService;
        this.idiomaService = idiomaService;
        this.disponibilidadService = disponibilidadService;
        this.comentarioService = comentarioService;
        this.actividadService = actividadService;
        this.clienteService = clienteService;
        this.usuarioService = usuarioService;
        this.reservaService = reservaService;
        this.colaboradorService = colaboradorService;
    }

    @GetMapping("/dashboard")
    public String adminHome(Model model) {

        List<Actividad> actividades = actividadService.listarActividades();

        BigDecimal plataGanada = BigDecimal.ZERO;

        for (Actividad act : actividades) {
            BigDecimal precioBase = act.getPrecio();

            for (Reserva reserva : act.getReservas()) {
                if ("Hecho".equals(reserva.getEstado())) {

                    BigDecimal cantidad = BigDecimal.valueOf(reserva.getCantidad());

                    // precioBase * cantidad → total de esa reserva
                    BigDecimal totalReserva = precioBase.multiply(cantidad);

                    // 18% de comisión para el admin
                    BigDecimal comision = totalReserva.multiply(new BigDecimal("0.18"));

                    // acumular la comisión
                    plataGanada = plataGanada.add(comision);
                }
            }
        }

        model.addAttribute("plataGanada", plataGanada);
        model.addAttribute("CantidadCliente", clienteService.ContadorCliente());
        model.addAttribute("CantidadColaborador", colaboradorService.ContadorColaborador());
        model.addAttribute("CantidadUsuarios", usuarioService.ContadorUsuario());
        model.addAttribute("CantidadReservacion", reservaService.ContadorReservas());
        model.addAttribute("CantidadActividad", actividadService.ContadorActividades());
        model.addAttribute("CantidadDisponibilidades", disponibilidadService.ContadorDisponibilidades());

        return "admin/dashboard";
    }

    @GetMapping("/categorias")
    public String listarCategorias(Model model, Categoria categoria) {
        model.addAttribute("categorias", categoriaService.listarCategorias());
        model.addAttribute("categoria", new Categoria());
        return "admin/categorias"; // Asegúrate de tener esta plantilla creada
    }

    @PostMapping("/categorias/nueva")
    public String nuevaCategoria(Categoria categoria) {
        categoriaService.guardarCategoria(categoria);
        return "redirect:/admin/categorias"; // Redirige a la lista de categorías después de guardar
    }

    @PostMapping("/categorias/eliminar")
    public String eliminarCategoria(Long id) {
        categoriaService.eliminarCategoria(id);
        return "redirect:/admin/categorias"; // Redirige a la lista de categorías después de eliminar
    }

    @GetMapping("/idiomas")
    public String listaridiomas(Model model, Idioma idioma) {

        List<Idioma> idiomas = idiomaService.listarIdiomas();
        model.addAttribute("idiomas", idiomas);
        model.addAttribute("idioma", new Idioma());

        return "admin/idiomas"; // Asegúrate de tener esta plantilla creada
    }

    @PostMapping("/idiomas/nueva")
    public String nuevaIdioma(Idioma idioma) {
        idiomaService.guardarIdioma(idioma);
        return "redirect:/admin/idiomas"; // Redirige a la lista de categorías después de guardar
    }

    @PostMapping("/idiomas/eliminar")
    public String eliminarIdioma(Long id) {
        idiomaService.eliminarIdioma(id);
        return "redirect:/admin/idiomas"; // Redirige a la lista de categorías después de eliminar
    }

    @GetMapping("/eliminar/categoria/{id}")
    public String eliminar(@PathVariable Long id, RedirectAttributes redirectAttrs) {
        try {
            categoriaService.eliminarCategoria(id);
            redirectAttrs.addFlashAttribute("mensaje", "Categoría eliminada correctamente");
        } catch (RuntimeException e) {
            redirectAttrs.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/admin/categorias";
    }

}

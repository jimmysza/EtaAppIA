package maineta.eta.controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;
import jakarta.servlet.http.HttpSession;

import maineta.eta.entity.Usuario;
import maineta.eta.service.DocumentoService;

@Controller
@RequestMapping("/documentos")
public class DocumentoController {

    private final DocumentoService service;

    public DocumentoController(DocumentoService service) {
        this.service = service;
    }

    private Usuario obtenerUsuarioSesion(HttpSession session) {
        return (Usuario) session.getAttribute("usuario");
    }

    @GetMapping("/subir")
    public String mostrarFormulario() {
        return "colaborardor/subir-documentos";
    }

    @PostMapping("/subir")
    public String subirDocumentos(
            @RequestParam("rut") MultipartFile rut,
            @RequestParam("cedula") MultipartFile cedula,
            Model model,
            HttpSession session) {

        try {
            Usuario usuario = obtenerUsuarioSesion(session); // tu lógica

            service.guardarDocumento(rut, usuario);
            service.guardarDocumento(cedula, usuario);

            model.addAttribute("mensaje", "Documentos subidos correctamente");

        } catch (Exception e) {
            model.addAttribute("error", e.getMessage());
        }

        return "subir-documentos";
    }
}

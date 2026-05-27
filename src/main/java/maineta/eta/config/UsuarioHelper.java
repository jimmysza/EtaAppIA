package maineta.eta.config;

import java.math.BigDecimal;
import java.util.Collection;

import maineta.eta.entity.Admin;
import maineta.eta.service.AdminService;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Component;
import org.springframework.ui.Model;

import maineta.eta.entity.Usuario;
import maineta.eta.service.UsuarioService;
import maineta.eta.service.ClienteService;
import maineta.eta.service.ColaboradorService;

@Component
public class UsuarioHelper {

    private final UsuarioService usuarioService;
    private final AdminService adminService;
    private final ClienteService clienteService;
    private final ColaboradorService colaboradorService;

    public UsuarioHelper(UsuarioService usuarioService, AdminService adminService,
            ClienteService clienteService, ColaboradorService colaboradorService) {
        this.usuarioService = usuarioService;
        this.adminService = adminService;
        this.clienteService = clienteService;
        this.colaboradorService = colaboradorService;
    }

    public BigDecimal CalcularPrecioConsumidor(BigDecimal precio) {

        Admin admin = adminService.buscarAdminPorId(1);

        BigDecimal porcentaje = admin.getPorcentajeComision(); // ejemplo: 10

        // Convertir porcentaje 10 → 0.10
        BigDecimal porcentajeDecimal = porcentaje.divide(BigDecimal.valueOf(100));

        BigDecimal precioFinalBD = precio.multiply(BigDecimal.ONE.add(porcentajeDecimal));

        return precioFinalBD;
    }

    public
    String generarTituloUrl(String titulo) {
    return titulo.toLowerCase()
            .replaceAll("[^a-z0-9\\s-]", "")
            .replaceAll("\\s+", "-")
            .replaceAll("-+", "-");
}



    public void agregarInfoUsuarioModel(Model model, Authentication auth) {
        if (auth != null && auth.isAuthenticated() && !auth.getPrincipal().equals("anonymousUser")) {

            // Extraer email correctamente dependiendo del tipo de autenticación
            String email = null;
            
            // Si es OAuth2 (Google login), extraer email de los atributos
            if (auth.getPrincipal() instanceof OAuth2User) {
                OAuth2User oauth2User = (OAuth2User) auth.getPrincipal();
                email = (String) oauth2User.getAttributes().get("email");
            } else {
                // Si es login tradicional (username/password), usar auth.getName()
                email = auth.getName();
            }
            
            try {
                if (email != null) {
                    Usuario usuario = usuarioService.obtenerPorEmail(email);
                    // Si existe en la BD, mostramos su nombre
                    model.addAttribute("nombreUsuario", usuario.getNombre());

                    // Intentar obtener foto de perfil (cliente o colaborador)
                    try {
                        var clienteOpt = clienteService.obtenerPorUsuario(usuario);
                        if (clienteOpt.isPresent() && clienteOpt.get().getFotoPerfil() != null) {
                            model.addAttribute("fotoPerfilUsuario", clienteOpt.get().getFotoPerfil());
                        } else {
                            var colOpt = colaboradorService.obtenerPorUsuario(usuario);
                            colOpt.ifPresent(c -> model.addAttribute("fotoPerfilUsuario", c.getFotoPerfil()));
                        }
                    } catch (Exception ignored) {
                        // No romper si alguno de los servicios falla
                    }
                } else {
                    model.addAttribute("nombreUsuario", "Usuario");
                }
            } catch (RuntimeException e) {
                // Si no existe, mostramos el email/identificador
                model.addAttribute("nombreUsuario", email != null ? email : "Usuario");
            }

            // Obtener el rol del usuario autenticado
            Collection<? extends GrantedAuthority> authorities = auth.getAuthorities();
            String role = authorities.stream()
                    .map(GrantedAuthority::getAuthority)
                    .findFirst()
                    .orElse("ROLE_ANONYMOUS");

            // Pasamos el rol al modelo
            model.addAttribute("userRole", role);

        } else {
            // Si no está logueado → valores por defecto
            model.addAttribute("nombreUsuario", null);
            model.addAttribute("userRole", "ROLE_ANONYMOUS");
        }
    }
}

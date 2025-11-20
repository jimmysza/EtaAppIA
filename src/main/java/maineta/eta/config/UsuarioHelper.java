package maineta.eta.config;

import java.math.BigDecimal;
import java.util.Collection;

import maineta.eta.entity.Admin;
import maineta.eta.service.AdminService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.stereotype.Component;
import org.springframework.ui.Model;

import maineta.eta.entity.Usuario;
import maineta.eta.service.UsuarioService;

@Component
public class UsuarioHelper {

    private final UsuarioService usuarioService;
    private final AdminService adminService;

    public UsuarioHelper(UsuarioService usuarioService, AdminService adminService) {
        this.usuarioService = usuarioService;
        this.adminService = adminService;
    }

    public BigDecimal CalcularPrecioConsumidor(BigDecimal precio) {

        Admin admin = adminService.buscarAdminPorId(1);

        BigDecimal porcentaje = admin.getPorcentajeComision(); // ejemplo: 10

        // Convertir porcentaje 10 → 0.10
        BigDecimal porcentajeDecimal = porcentaje.divide(BigDecimal.valueOf(100));

        BigDecimal precioFinalBD = precio.multiply(BigDecimal.ONE.add(porcentajeDecimal));

        return precioFinalBD;
    }


    public void agregarInfoUsuarioModel(Model model, Authentication auth) {
        if (auth != null && auth.isAuthenticated() && !auth.getPrincipal().equals("anonymousUser")) {

            // Obtenemos el email del usuario logueado
            String email = auth.getName();
            Usuario usuario = usuarioService.obtenerPorEmail(email);

            // Si existe en la BD, mostramos su nombre; si no, el email
            if (usuario != null) {
                model.addAttribute("nombreUsuario", usuario.getNombre());
            } else {
                model.addAttribute("nombreUsuario", email);
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

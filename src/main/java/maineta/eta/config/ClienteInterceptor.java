package maineta.eta.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import maineta.eta.entity.Cliente;
import maineta.eta.entity.Usuario;
import maineta.eta.service.ClienteService;
import maineta.eta.service.UsuarioService;

/**
 * Interceptor que verifica si el cliente ha completado el onboarding
 * antes de acceder a rutas protegidas de cliente
 */
@Component
public class ClienteInterceptor implements HandlerInterceptor {

    @Autowired
    private ClienteService clienteService;

    @Autowired
    private UsuarioService usuarioService;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) 
            throws Exception {
        
        // Si la ruta es el propio onboarding, dejar pasar
        String uri = request.getRequestURI();
        if (uri.contains("/onboarding") || uri.contains("/logout") || 
            uri.contains("/static") || uri.contains("/css") || 
            uri.contains("/js") || uri.contains("/images") ||
            uri.contains("/registro")) { // No interceptar rutas de registro
            return true;
        }

        // Obtener el usuario autenticado
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.isAuthenticated() && 
            auth.getPrincipal() instanceof UserDetails) {
            
            UserDetails userDetails = (UserDetails) auth.getPrincipal();
            
            try {
                // Buscar el usuario en la base de datos
                Usuario usuario = usuarioService.obtenerPorEmail(userDetails.getUsername());
                
                if (usuario != null) {
                    // Buscar el cliente asociado
                    Cliente cliente = clienteService.obtenerPorUsuario(usuario)
                        .orElse(null);
                    
                    // Si es un cliente y no ha completado el onboarding, redirigir
                    if (cliente != null && !cliente.isOnboardingCompletado()) {
                        response.sendRedirect("/cliente/onboarding");
                        return false;
                    }
                }
            } catch (Exception e) {
                // Log el error pero permitir continuar para evitar bloqueos
                e.printStackTrace();
            }
        }
        
        return true;
    }
}

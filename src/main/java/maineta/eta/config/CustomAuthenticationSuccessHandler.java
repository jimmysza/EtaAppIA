package maineta.eta.config;

import java.io.IOException;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * Clase personalizada que maneja la redirección después de un inicio de sesión exitoso.
 * Se utiliza para dirigir al usuario a una página distinta dependiendo de su rol.
 */
@Component
public class CustomAuthenticationSuccessHandler implements AuthenticationSuccessHandler {

    /**
     * Método que se ejecuta automáticamente cuando un usuario inicia sesión con éxito.
     *
     * @param request  objeto que representa la petición HTTP
     * @param response objeto que representa la respuesta HTTP
     * @param authentication contiene la información del usuario autenticado (incluyendo roles)
     */
    @Override
public void onAuthenticationSuccess(HttpServletRequest request,
                                    HttpServletResponse response,
                                    Authentication authentication) throws IOException, ServletException {

    // Loguear cookies presentes en la request después del login
    Cookie[] cookies = request.getCookies();
    if (cookies != null) {
        for (Cookie cookie : cookies) {
            System.out.println("✅ Login Exitoso - Cookie: " 
                    + cookie.getName() + " = " + cookie.getValue());
        }
    } else {
        System.out.println("⚠️ Login exitoso pero sin cookies en la request");
    }

    // URL por defecto
    String redirectURL = "/";

    for (GrantedAuthority auth : authentication.getAuthorities()) {
        String role = auth.getAuthority();

        if (role.equals("ROLE_COLABORADOR")) {
            redirectURL = "/colaborador/dashboard";
            break;
        } else if (role.equals("ROLE_CLIENTE")) {
            redirectURL = "/";
            break;
        }else if (role.equals("ROLE_ADMIN")) {
            redirectURL = "/admin/dashboard";
            break;
        }

    }

    // Redirigir al usuario según rol
    response.sendRedirect(redirectURL);
}

}


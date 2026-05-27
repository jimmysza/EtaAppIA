package maineta.eta.config;

import org.springframework.security.core.Authentication;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

@ControllerAdvice
public class GlobalModelAdvice {

    private final UsuarioHelper usuarioHelper;

    public GlobalModelAdvice(UsuarioHelper usuarioHelper) {
        this.usuarioHelper = usuarioHelper;
    }

    @ModelAttribute
    public void addUserAttributes(Model model, Authentication auth) {
        // Delegar a UsuarioHelper para poblar nombreUsuario y fotoPerfilUsuario
        usuarioHelper.agregarInfoUsuarioModel(model, auth);
    }
}

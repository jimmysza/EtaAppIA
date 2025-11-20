package maineta.eta.config;

import java.util.Set;

import maineta.eta.entity.Rol;
import maineta.eta.entity.Usuario;
import maineta.eta.repository.RolRepository;
import maineta.eta.repository.UsuarioRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;

@Service
public class CustomOAuth2UserService extends DefaultOAuth2UserService {

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private RolRepository rolRepository;

    @Override
    public OAuth2User loadUser(OAuth2UserRequest userRequest) throws OAuth2AuthenticationException {
        // Llama al método original de Spring para obtener el usuario de Google
        OAuth2User oauth2User = super.loadUser(userRequest);

        String email = (String) oauth2User.getAttributes().get("email");
        String nombre = (String) oauth2User.getAttributes().get("name");

        if (email == null) {
            throw new OAuth2AuthenticationException("No se encontró el email en los datos de Google");
        }

        // Si el usuario no existe, lo creamos
        usuarioRepository.findByEmail(email).orElseGet(() -> {
            Usuario nuevo = new Usuario();
            nuevo.setEmail(email);
            nuevo.setNombre(nombre);
            nuevo.setPassword("OAUTH_USER"); // marcador simbólico, no se usa
            Rol rolUser = rolRepository.findByNombre("ROLE_CLIENTE")
                    .orElseThrow(() -> new RuntimeException("No existe el rol ROLE_CLIENTE en la base de datos"));
            nuevo.setRoles(Set.of(rolUser));
            return usuarioRepository.save(nuevo);
        });

        return oauth2User;
    }
}

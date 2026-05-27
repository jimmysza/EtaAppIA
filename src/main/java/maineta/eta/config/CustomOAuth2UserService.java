package maineta.eta.config;


import java.util.Collection;
import java.util.HashSet;
import java.util.Locale;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.user.DefaultOAuth2User;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;

import maineta.eta.entity.Cliente;
import maineta.eta.entity.Rol;
import maineta.eta.entity.Usuario;
import maineta.eta.repository.ClienteRepository;
import maineta.eta.repository.RolRepository;
import maineta.eta.repository.UsuarioRepository;

@Service
public class CustomOAuth2UserService extends DefaultOAuth2UserService {

    private static final Logger LOGGER = LoggerFactory.getLogger(CustomOAuth2UserService.class);

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private RolRepository rolRepository;

    @Autowired
    private ClienteRepository clienteRepository;

    @Override
    public OAuth2User loadUser(OAuth2UserRequest userRequest) throws OAuth2AuthenticationException {
        // Llama al método original de Spring para obtener el usuario de Google
        OAuth2User oauth2User = super.loadUser(userRequest);

        String email = (String) oauth2User.getAttributes().get("email");
        String nombre = (String) oauth2User.getAttributes().get("name");
        String locale = (String) oauth2User.getAttributes().get("locale");

        if (email == null) {
            throw new OAuth2AuthenticationException("No se encontró el email en los datos de Google");
        }

        // Extraer el país desde el locale de Google
        String paisOrigen = null;
        if (locale != null && !locale.isEmpty()) {
            try {
                String[] partes = locale.split("-");
                if (partes.length > 1) {
                    // Crear Locale desde el idioma y país
                    Locale loc = Locale.forLanguageTag(partes[0] + "-" + partes[1]);
                    paisOrigen = loc.getDisplayCountry(Locale.forLanguageTag("es"));
                }
            } catch (Exception e) {
                // Si hay algún error, simplemente no se guarda el país
                LOGGER.warn("No se pudo resolver el país desde el locale de Google", e);
            }
        }

        final String paisFinal = paisOrigen;

        // Si el usuario no existe, lo creamos
        Usuario usuario = usuarioRepository.findByEmail(email).orElseGet(() -> {
            Usuario nuevo = new Usuario();
            nuevo.setEmail(email);
            nuevo.setNombre(nombre);
            nuevo.setPassword("OAUTH_USER"); // marcador simbólico, no se usa
            Rol rolUser = rolRepository.findByNombre("ROLE_CLIENTE")
                    .orElseThrow(() -> new RuntimeException("No existe el rol ROLE_CLIENTE en la base de datos"));
            nuevo.setRol(rolUser);
            nuevo.setEmailVerificado(Boolean.TRUE);
            return usuarioRepository.save(nuevo);
        });

        // Verificar si el cliente ya existe, si no, crearlo
        clienteRepository.findByUsuario(usuario).orElseGet(() -> {
            Cliente nuevoCliente = new Cliente();
            nuevoCliente.setUsuario(usuario);
            nuevoCliente.setCedula(0L); // Valor temporal, deberá completarse después
            nuevoCliente.setPaisOrigen(paisFinal);
            nuevoCliente.setOnboardingCompletado(false);
            return clienteRepository.save(nuevoCliente);
        });

        Collection<GrantedAuthority> authorities = new HashSet<>(oauth2User.getAuthorities());
        authorities.add(new SimpleGrantedAuthority("ROLE_CLIENTE"));

        Map<String, Object> attributes = oauth2User.getAttributes();
        String userNameAttribute = userRequest.getClientRegistration().getProviderDetails()
                .getUserInfoEndpoint().getUserNameAttributeName();
        if (userNameAttribute == null || userNameAttribute.isBlank()) {
            userNameAttribute = "email";
        }

        return new DefaultOAuth2User(authorities, attributes, userNameAttribute);
    }
}

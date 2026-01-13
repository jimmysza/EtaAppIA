package maineta.eta.service;

import java.util.HashSet;
import java.util.Set;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import maineta.eta.entity.Rol;
import maineta.eta.entity.Usuario;
import maineta.eta.repository.UsuarioRepository;

/**
 * 🔹 Implementación de UsuarioService
 *
 * Esta clase conecta la lógica de negocio con Spring Security.
 * Se encarga de cargar los usuarios desde la base de datos
 * y transformar sus roles en "authorities" que entiende Spring Security.
 */
@Service
public class UsuarioServiceImpl implements UsuarioService {

    // Repositorio para acceder a los datos de Usuario
    private final UsuarioRepository usuarioRepository;

    @Autowired
    public UsuarioServiceImpl(UsuarioRepository usuarioRepository) {
        this.usuarioRepository = usuarioRepository;
    }

    /**
     * 🔹 Método que usa Spring Security para autenticar al usuario.
     *
     * Aquí es donde Spring Security busca un usuario por su "username" (en este caso, el email).
     * Si existe, se construye un objeto `UserDetails` con su email, contraseña y roles.
     * Si no existe, se lanza una excepción para indicar que el login falló.
     *
     * @param username El email ingresado en el login.
     * @return UserDetails objeto que Spring Security utiliza en el proceso de autenticación.
     * @throws UsernameNotFoundException si el usuario no existe en la BD.
     */
    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        // Buscar usuario por email en la BD
        Usuario usuario = usuarioRepository.findByEmail(username)
                .orElseThrow(() -> new UsernameNotFoundException("Usuario no encontrado"));

        // Convertir rol de la BD en authority que entiende Spring Security
        Set<GrantedAuthority> authorities = new HashSet<>();
        Rol rol = usuario.getRol();
        if (rol != null) {
            authorities.add(new SimpleGrantedAuthority(rol.getNombre())); 
            // Ejemplo: "ROLE_CLIENTE", "ROLE_ADMIN"
        }

        // Crear y devolver un objeto User de Spring Security
        return User.builder()
                .username(usuario.getEmail())       // el "username" será el email
                .password(usuario.getPassword())   // contraseña encriptada
                .authorities(authorities)          // roles convertidos en authorities
                .build();
    }

    @Override
    public Long ContadorUsuario() {
        return usuarioRepository.count();
    }
    /**
     * 🔹 Buscar usuario por email (método personalizado).
     *
     * Útil para otros servicios o controladores que necesiten
     * obtener datos de un usuario fuera del login.
     *
     * @param email Correo electrónico del usuario.
     * @return Optional con el usuario encontrado o vacío si no existe.
     */
     @Override
    public Usuario obtenerPorEmail(String email) {
        return usuarioRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("No se encontró el usuario con correo: " + email));
    }
}


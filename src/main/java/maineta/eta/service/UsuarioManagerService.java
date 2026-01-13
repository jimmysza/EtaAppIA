package maineta.eta.service;

import jakarta.transaction.Transactional;
import maineta.eta.entity.Rol;
import maineta.eta.entity.Usuario;
import maineta.eta.repository.ColaboradorRepository;
import maineta.eta.repository.RolRepository;
import maineta.eta.repository.UsuarioRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.Set;

@Service
public class UsuarioManagerService {


    private final UsuarioRepository usuarioRepository;
    private final ColaboradorRepository colaboradorRepository;
    private final RolRepository rolRepository;
    private final PasswordEncoder passwordEncoder;


    @Autowired
    public UsuarioManagerService(UsuarioRepository usuarioRepository, RolRepository rolRepository,ColaboradorRepository colaboradorRepository,PasswordEncoder passwordEncoder) {
        this.usuarioRepository = usuarioRepository;
        this.rolRepository = rolRepository;
        this.colaboradorRepository = colaboradorRepository;
        this.passwordEncoder = passwordEncoder;
    }

    /**
     * Valida y registra un usuario con un rol específico.
     */
    @Transactional
    public Usuario prepararYGuardarUsuario(Usuario usuario, String rolNombre) {

        // Validar email
        if (usuarioRepository.findByEmail(usuario.getEmail()).isPresent()
                || colaboradorRepository.findByCorreoSeguridad(usuario.getEmail()).isPresent()) {
            throw new RuntimeException("El correo ya está registrado (como email o correo de seguridad): "
                    + usuario.getEmail());
        }


        // Validar teléfono
        if (usuarioRepository.findByTelefono(usuario.getTelefono()).isPresent()) {
            throw new RuntimeException("El teléfono ya está registrado: " + usuario.getTelefono());
        }

        // Validar contraseña
        if (usuario.getPassword() == null || usuario.getPassword().isEmpty()) {
            throw new IllegalArgumentException("La contraseña no puede estar vacía");
        }

        // Encriptar
        usuario.setPassword(passwordEncoder.encode(usuario.getPassword()));

        // Buscar rol
        Rol rol = rolRepository.findByNombre(rolNombre)
                .orElseThrow(() -> new RuntimeException("Rol no encontrado: " + rolNombre));

        // Asignar rol
        usuario.setRol(rol);

        // Guardar usuario
        return usuarioRepository.save(usuario);
    }
}


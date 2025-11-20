package maineta.eta.service;

import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import maineta.eta.entity.Colaborador;
import maineta.eta.entity.Usuario;
import maineta.eta.repository.ColaboradorRepository;
import maineta.eta.repository.RolRepository;
import maineta.eta.repository.UsuarioRepository;
import maineta.eta.entity.Usuario;

/**
 * 🔹 Implementación de la interfaz ColaboradorService.
 *
 * Aquí se desarrolla la lógica de negocio para manejar la entidad Colaborador,
 * incluyendo validaciones, codificación de contraseñas y asignación de roles.
 */
@Service
public class ColaboradorServiceImpl implements ColaboradorService {

    private final UsuarioRepository usuarioRepository;
    private final ColaboradorRepository colaboradorRepository;
    private final UsuarioManagerService usuarioManagerService;

    /**
     * 🔹 Constructor con inyección de dependencias
     *
     * Spring se encarga de inyectar automáticamente los repositorios y el
     * passwordEncoder necesarios para trabajar con la base de datos y la seguridad.
     */
    @Autowired
    public ColaboradorServiceImpl(UsuarioRepository usuarioRepository,
                                ColaboradorRepository colaboradorRepository,UsuarioManagerService usuarioManagerService) {
        this.usuarioRepository = usuarioRepository;


        this.colaboradorRepository = colaboradorRepository;
        this.usuarioManagerService = usuarioManagerService;
    }

    @Override
    public Long ContadorColaborador() {
        return colaboradorRepository.count();
    }

    @Override
    public Colaborador registrarColaborador(Colaborador colaborador) {

        if (colaboradorRepository.findByNit(colaborador.getNit()).isPresent()) {
            throw new RuntimeException("El NIT ya está registrado: " + colaborador.getNit());
        }


        //obtener correo del recien registrado
        String correoSeguridad = colaborador.getCorreoSeguridad();
        // true si encuenrta el correo
        boolean correoEnColaboradores = colaboradorRepository.findByCorreoSeguridad(correoSeguridad).isPresent();
        boolean correoEnUsuarios = usuarioRepository.findByEmail(correoSeguridad).isPresent();

        if (correoEnColaboradores || correoEnUsuarios) {
            throw new RuntimeException("El correo de seguridad ya está registrado: " + correoSeguridad);
        }


        // Asignación del rol COLABORADOR (ejemplo: ID = 2 en BD)
        Usuario usuarioGuardado = usuarioManagerService.prepararYGuardarUsuario(
                colaborador.getUsuario(),
                "ROLE_COLABORADOR"
        );



        colaborador.setUsuario(usuarioGuardado);
        return colaboradorRepository.save(colaborador);
    }

    /**
     * 🔹 Listar todos los colaboradores registrados.
     *
     * @return Lista de colaboradores en la BD.
     */
    @Override
    public List<Colaborador> findAll() {
        return colaboradorRepository.findAll();
    }

    /**
     * 🔹 Buscar un colaborador a partir de su usuario.
     *
     * Permite obtener el colaborador asociado a un objeto Usuario específico.
     *
     * @param usuario Usuario relacionado al colaborador.
     * @return Optional con el Colaborador encontrado, o vacío si no existe.
     */
    @Override
    public Optional<Colaborador> obtenerPorUsuario(Usuario usuario) {
        return colaboradorRepository.findByUsuario(usuario);
    }
}


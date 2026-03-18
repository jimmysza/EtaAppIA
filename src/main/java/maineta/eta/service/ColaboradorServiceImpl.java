package maineta.eta.service;

import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import maineta.eta.dto.ColaboradorPublicoDTO;
import maineta.eta.entity.Actividad;
import maineta.eta.entity.Colaborador;
import maineta.eta.entity.Usuario;
import maineta.eta.repository.ActividadRepository;
import maineta.eta.repository.ColaboradorRepository;
import maineta.eta.repository.UsuarioRepository;

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
    private final ActividadRepository actividadRepository;
    private final UsuarioManagerService usuarioManagerService;
    private final VerificacionCorreoService verificacionCorreoService;

    /**
     * 🔹 Constructor con inyección de dependencias
     *
     * Spring se encarga de inyectar automáticamente los repositorios y el
     * passwordEncoder necesarios para trabajar con la base de datos y la seguridad.
     */
    public ColaboradorServiceImpl(UsuarioRepository usuarioRepository,
                                ColaboradorRepository colaboradorRepository,
                                ActividadRepository actividadRepository,
                                UsuarioManagerService usuarioManagerService,
                                VerificacionCorreoService verificacionCorreoService) {
        this.usuarioRepository = usuarioRepository;


        this.colaboradorRepository = colaboradorRepository;
        this.actividadRepository = actividadRepository;
        this.usuarioManagerService = usuarioManagerService;
        this.verificacionCorreoService = verificacionCorreoService;
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
        Colaborador colaboradorGuardado = colaboradorRepository.save(colaborador);
        verificacionCorreoService.enviarCorreoVerificacion(usuarioGuardado);
        return colaboradorGuardado;
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

    @Override
    public Optional<Colaborador> obtenerPorId(Long idColaborador) {
        return colaboradorRepository.findByIdColaborador(idColaborador);
    }

    @Override
    public List<ColaboradorPublicoDTO> obtenerDestacadosPorReservas(int limite) {
        List<ColaboradorPublicoDTO> destacados = colaboradorRepository.findColaboradoresDestacados(
                PageRequest.of(0, limite));
        destacados.forEach(this::completarDatosPublicos);
        return destacados;
    }

    @Override
    public Optional<ColaboradorPublicoDTO> obtenerResumenPublico(Long idColaborador) {
        Optional<ColaboradorPublicoDTO> resumen = colaboradorRepository.findResumenPublicoById(idColaborador);
        resumen.ifPresent(this::completarDatosPublicos);
        return resumen;
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

    private void completarDatosPublicos(ColaboradorPublicoDTO dto) {
        dto.setIniciales(obtenerIniciales(dto.getNombre()));

        Optional<Actividad> actividadDestacada = actividadRepository
                .findFirstByColaborador_IdColaboradorOrderByCreatedAtDesc(dto.getIdColaborador());

        actividadDestacada.ifPresent(actividad -> {
            dto.setImagenPrincipal(actividad.getImagen());
            dto.setActividadDestacada(actividad.getTitulo());
        });
    }

    private String obtenerIniciales(String nombre) {
        if (nombre == null || nombre.isBlank()) {
            return "ET";
        }

        String[] partes = nombre.trim().split("\\s+");
        StringBuilder iniciales = new StringBuilder();

        for (String parte : partes) {
            if (!parte.isBlank()) {
                iniciales.append(Character.toUpperCase(parte.charAt(0)));
            }
            if (iniciales.length() == 2) {
                break;
            }
        }

        return iniciales.isEmpty() ? "ET" : iniciales.toString();
    }
}


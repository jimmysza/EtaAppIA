package maineta.eta.service;

import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import maineta.eta.dto.ColaboradorPerfilForm;
import maineta.eta.dto.ColaboradorPublicoDTO;
import maineta.eta.entity.Actividad;
import maineta.eta.entity.Colaborador;
import maineta.eta.entity.Usuario;
import maineta.eta.repository.ActividadRepository;
import maineta.eta.repository.ColaboradorRepository;
import maineta.eta.repository.UsuarioRepository;

/**
 * ðŸ”¹ ImplementaciÃ³n de la interfaz ColaboradorService.
 *
 * AquÃ­ se desarrolla la lÃ³gica de negocio para manejar la entidad Colaborador,
 * incluyendo validaciones, codificaciÃ³n de contraseÃ±as y asignaciÃ³n de roles.
 */
@Service
public class ColaboradorServiceImpl implements ColaboradorService {

    private final UsuarioRepository usuarioRepository;
    private final ColaboradorRepository colaboradorRepository;
    private final ActividadRepository actividadRepository;
    private final UsuarioManagerService usuarioManagerService;
    private final VerificacionCorreoService verificacionCorreoService;

    /**
     * ðŸ”¹ Constructor con inyecciÃ³n de dependencias
     *
     * Spring se encarga de inyectar automÃ¡ticamente los repositorios y el
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
            throw new RuntimeException("El NIT ya estÃ¡ registrado: " + colaborador.getNit());
        }

        String correoSeguridad = colaborador.getCorreoSeguridad();
        boolean correoEnColaboradores = colaboradorRepository.findByCorreoSeguridad(correoSeguridad).isPresent();
        boolean correoEnUsuarios = usuarioRepository.findByEmail(correoSeguridad).isPresent();

        if (correoEnColaboradores || correoEnUsuarios) {
            throw new RuntimeException("El correo de seguridad ya estÃ¡ registrado: " + correoSeguridad);
        }

        Usuario usuarioGuardado = usuarioManagerService.prepararYGuardarUsuario(
                colaborador.getUsuario(),
                "ROLE_COLABORADOR");

        colaborador.setUsuario(usuarioGuardado);
        Colaborador colaboradorGuardado = colaboradorRepository.save(colaborador);
        verificacionCorreoService.enviarCorreoVerificacion(usuarioGuardado);
        return colaboradorGuardado;
    }

    /**
     * ðŸ”¹ Listar todos los colaboradores registrados.
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

    @Override
    @Transactional
    public Colaborador actualizarPerfil(Long idColaborador, ColaboradorPerfilForm form, String fotoPerfil) {
        return colaboradorRepository.findByIdColaborador(idColaborador)
                .map(colaborador -> {
                    Usuario usuarioActual = colaborador.getUsuario();

                    String nuevoNombre = normalizarCampo(form.getNombre(), "nombre");
                    String nuevoEmail = normalizarCampo(form.getEmail(), "correo principal");
                    String nuevoTelefono = normalizarCampo(form.getTelefono(), "telefono");
                    String nuevoNit = normalizarCampo(form.getNit(), "NIT");
                    String nuevoCorreoSeguridad = normalizarCampo(form.getCorreoSeguridad(), "correo de seguridad");

                    Optional<Usuario> otroUsuarioConEmail = usuarioRepository.findByEmail(nuevoEmail);
                    if (otroUsuarioConEmail.isPresent()
                            && !otroUsuarioConEmail.get().getId().equals(usuarioActual.getId())) {
                        throw new RuntimeException("El correo ya esta registrado: " + nuevoEmail);
                    }

                    Optional<Colaborador> otroColaboradorConCorreoPrincipal = colaboradorRepository
                            .findByCorreoSeguridad(nuevoEmail);
                    if (otroColaboradorConCorreoPrincipal.isPresent()
                            && !otroColaboradorConCorreoPrincipal.get().getIdColaborador()
                                    .equals(colaborador.getIdColaborador())) {
                        throw new RuntimeException(
                                "El correo principal ya pertenece al correo de seguridad de otro colaborador.");
                    }

                    Optional<Usuario> otroUsuarioConTelefono = usuarioRepository.findByTelefono(nuevoTelefono);
                    if (otroUsuarioConTelefono.isPresent()
                            && !otroUsuarioConTelefono.get().getId().equals(usuarioActual.getId())) {
                        throw new RuntimeException("El telefono ya esta registrado: " + nuevoTelefono);
                    }

                    Optional<Colaborador> otroColaboradorConNit = colaboradorRepository.findByNit(nuevoNit);
                    if (otroColaboradorConNit.isPresent()
                            && !otroColaboradorConNit.get().getIdColaborador().equals(colaborador.getIdColaborador())) {
                        throw new RuntimeException("El NIT ya esta registrado: " + nuevoNit);
                    }

                    if (nuevoCorreoSeguridad.equalsIgnoreCase(nuevoEmail)) {
                        throw new RuntimeException("El correo de seguridad debe ser diferente al correo principal.");
                    }

                    Optional<Usuario> otroUsuarioConCorreoSeguridad = usuarioRepository.findByEmail(nuevoCorreoSeguridad);
                    if (otroUsuarioConCorreoSeguridad.isPresent()
                            && !otroUsuarioConCorreoSeguridad.get().getId().equals(usuarioActual.getId())) {
                        throw new RuntimeException(
                                "El correo de seguridad ya esta registrado como usuario: " + nuevoCorreoSeguridad);
                    }

                    Optional<Colaborador> otroColaboradorConCorreoSeguridad = colaboradorRepository
                            .findByCorreoSeguridad(nuevoCorreoSeguridad);
                    if (otroColaboradorConCorreoSeguridad.isPresent()
                            && !otroColaboradorConCorreoSeguridad.get().getIdColaborador()
                                    .equals(colaborador.getIdColaborador())) {
                        throw new RuntimeException(
                                "El correo de seguridad ya esta registrado: " + nuevoCorreoSeguridad);
                    }

                    usuarioActual.setNombre(nuevoNombre);
                    usuarioActual.setEmail(nuevoEmail);
                    usuarioActual.setTelefono(nuevoTelefono);
                    usuarioRepository.save(usuarioActual);

                    colaborador.setNit(nuevoNit);
                    colaborador.setCorreoSeguridad(nuevoCorreoSeguridad);

                    if (fotoPerfil != null && !fotoPerfil.isBlank()) {
                        colaborador.setFotoPerfil(fotoPerfil);
                    }

                    return colaboradorRepository.save(colaborador);
                })
                .orElseThrow(() -> new RuntimeException("Colaborador no encontrado"));
    }

    /**
     * ðŸ”¹ Buscar un colaborador a partir de su usuario.
     *
     * Permite obtener el colaborador asociado a un objeto Usuario especÃ­fico.
     *
     * @param usuario Usuario relacionado al colaborador.
     * @return Optional con el Colaborador encontrado, o vacÃ­o si no existe.
     */
    @Override
    public Optional<Colaborador> obtenerPorUsuario(Usuario usuario) {
        return colaboradorRepository.findByUsuario(usuario);
    }

    private void completarDatosPublicos(ColaboradorPublicoDTO dto) {
        dto.setIniciales(obtenerIniciales(dto.getNombre()));
        colaboradorRepository.findByIdColaborador(dto.getIdColaborador())
                .ifPresent(colaborador -> dto.setFotoPerfil(colaborador.getFotoPerfil()));

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

    private String normalizarCampo(String valor, String nombreCampo) {
        if (valor == null || valor.isBlank()) {
            throw new RuntimeException("El campo " + nombreCampo + " es obligatorio.");
        }
        return valor.trim();
    }
}

package maineta.eta.service;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.lang.NonNull;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import maineta.eta.dto.OnboardingForm;
import maineta.eta.entity.Categoria;
import maineta.eta.entity.Cliente;
import maineta.eta.entity.Usuario;
import maineta.eta.repository.CategoriaRepository;
import maineta.eta.repository.ClienteRepository;
import maineta.eta.repository.ColaboradorRepository;
import maineta.eta.repository.RolRepository;
import maineta.eta.repository.UsuarioRepository;
/**
 * 🔹 Implementación de la interfaz ClienteService
 *
 * Esta clase contiene la lógica de negocio para manejar clientes.
 * Combina el manejo de:
 *  - Usuario (credenciales y roles de acceso)
 *  - Cliente (datos del cliente)
 *
 * Uso de anotaciones:
 *  @Service -> Marca esta clase como un servicio de Spring.
 *  @Transactional -> Garantiza que las operaciones se realicen en una transacción.
 */

@Service
@Transactional
public class ClienteServiceImpl implements ClienteService {

    private final UsuarioRepository usuarioRepository;
    private final RolRepository rolRepository;
    private final PasswordEncoder passwordEncoder;
    private final ClienteRepository clienteRepository;
    private final ColaboradorRepository colaboradorRepository;
    private final UsuarioManagerService usuarioManagerService;
    private final VerificacionCorreoService verificacionCorreoService;
    private final CategoriaRepository categoriaRepository;

    
    public ClienteServiceImpl(
            ColaboradorRepository colaboradorRepository,
            UsuarioRepository usuarioRepository,
            RolRepository rolRepository,
            PasswordEncoder passwordEncoder,
            ClienteRepository clienteRepository,
            UsuarioManagerService usuarioManagerService,
            VerificacionCorreoService verificacionCorreoService,
            CategoriaRepository categoriaRepository
    ) {
        this.usuarioRepository = usuarioRepository;
        this.rolRepository = rolRepository;
        this.colaboradorRepository = colaboradorRepository;
        this.passwordEncoder = passwordEncoder;
        this.clienteRepository = clienteRepository;
        this.usuarioManagerService = usuarioManagerService;
        this.verificacionCorreoService = verificacionCorreoService;
        this.categoriaRepository = categoriaRepository;
    }

    @Override
    public Cliente obtenerPorId(Long id) {
        return clienteRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("No se encontró el cliente con ID: " + id));
    }

    @Override
    @Transactional
    public Cliente registrarCliente(Cliente cliente) {
        if (clienteRepository.findByCedula(cliente.getCedula()).isPresent()) {
            throw new RuntimeException("La cédula ya está registrada");
        }

        // Reutilizar la lógica genérica para crear el usuario
        var usuarioGuardado = usuarioManagerService.prepararYGuardarUsuario(
                cliente.getUsuario(),
                "ROLE_CLIENTE"
        );

        cliente.setUsuario(usuarioGuardado);
        Cliente clienteGuardado = clienteRepository.save(cliente);
        verificacionCorreoService.enviarCorreoVerificacion(usuarioGuardado);
        return clienteGuardado;
    }

    @Override
    public Long ContadorCliente() {
        return clienteRepository.count();
    }

    @Override
    public List<Cliente> findAll() {
        return clienteRepository.findAll();
    }

    @Override
    public Page<Cliente> findAll(@NonNull Pageable pageable) {
        return clienteRepository.findAll(pageable);
    }

    @Override
    public Cliente actualizarCliente(Long id, Cliente cliente) {
        return clienteRepository.findById(id).map(clienteCambio -> {

            Usuario usuarioActual = clienteCambio.getUsuario();
            String nuevoEmail = cliente.getUsuario().getEmail();
            String nuevoTelefono = cliente.getUsuario().getTelefono();
            Long nuevaCedula = cliente.getCedula();

            // 1️⃣ Validar que otro usuario no tenga el mismo email
            Optional<Usuario> otroUsuarioConEmail = usuarioRepository.findByEmail(nuevoEmail);
            if (otroUsuarioConEmail.isPresent() && !otroUsuarioConEmail.get().getId().equals(usuarioActual.getId())) {
                throw new RuntimeException("El correo ya está registrado: " + nuevoEmail);
            }

            // 2️⃣ Validar que no exista en colaboradores
            boolean existeEnColaborador = colaboradorRepository.findByCorreoSeguridad(nuevoEmail).isPresent();
            if (existeEnColaborador) {
                throw new RuntimeException("El correo ya pertenece a un colaborador: " + nuevoEmail);
            }

            // 3️⃣ Validar teléfono
            Optional<Usuario> otroUsuarioConTelefono = usuarioRepository.findByTelefono(nuevoTelefono);
            if (otroUsuarioConTelefono.isPresent() && !otroUsuarioConTelefono.get().getId().equals(usuarioActual.getId())) {
                throw new RuntimeException("El teléfono ya está registrado: " + nuevoTelefono);
            }

            // 4️⃣ Validar cédula
            Optional<Cliente> otroClienteConCedula = clienteRepository.findByCedula(nuevaCedula);
            if (otroClienteConCedula.isPresent() && !otroClienteConCedula.get().getId().equals(clienteCambio.getId())) {
                throw new RuntimeException("La cédula ya está registrada por otro cliente");
            }

            // 5️⃣ Actualizar usuario
            usuarioActual.setNombre(cliente.getUsuario().getNombre());
            usuarioActual.setTelefono(nuevoTelefono);
            usuarioActual.setEmail(nuevoEmail);

            // 6️⃣ Actualizar cliente
            clienteCambio.setCedula(nuevaCedula);
            clienteCambio.setPreferencias(cliente.getPreferencias());
            clienteCambio.setDireccion(cliente.getDireccion());

            return clienteRepository.save(clienteCambio);

        }).orElseThrow(() -> new RuntimeException("Cliente no encontrado"));
    }




    @Override
    public Optional<Cliente> obtenerPorUsuario(Usuario usuario) {
        return clienteRepository.findByUsuario(usuario);
    }

    @Override
    @Transactional
    public Cliente actualizarFotoPerfil(Long id, String filename) {
        return clienteRepository.findById(id).map(cliente -> {
            cliente.setFotoPerfil(filename);
            return clienteRepository.save(cliente);
        }).orElseThrow(() -> new RuntimeException("Cliente no encontrado"));
    }

    @Override
    @Transactional
    public Cliente guardarPreferencias(Cliente cliente, OnboardingForm form) {
        // Guardar las preferencias ENUMs
        cliente.setGrupoViaje(form.getGrupoViaje());
        cliente.setRangoPrecio(form.getRangoPrecio());
        cliente.setDisponibilidadSemana(form.getDisponibilidadSemana());
        
        // Obtener y guardar las categorías preferidas
        Set<Categoria> categorias = form.getCategoriasIds().stream()
            .map(id -> categoriaRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Categoría no encontrada: " + id)))
            .collect(Collectors.toSet());
        
        cliente.setCategoriasPreferidas(categorias);
        cliente.setOnboardingCompletado(true);
        
        return clienteRepository.save(cliente);
    }

    @Override
    @Transactional
    public Cliente registrarClienteConPreferencias(Cliente cliente, OnboardingForm form) {
        // Validar cédula
        if (clienteRepository.findByCedula(cliente.getCedula()).isPresent()) {
            throw new RuntimeException("La cédula ya está registrada");
        }

        // Guardar las preferencias en el cliente ANTES de registrar
        cliente.setGrupoViaje(form.getGrupoViaje());
        cliente.setRangoPrecio(form.getRangoPrecio());
        cliente.setDisponibilidadSemana(form.getDisponibilidadSemana());
        
        // Obtener y guardar las categorías preferidas
        Set<Categoria> categorias = form.getCategoriasIds().stream()
            .map(id -> categoriaRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Categoría no encontrada: " + id)))
            .collect(Collectors.toSet());
        
        cliente.setCategoriasPreferidas(categorias);
        cliente.setOnboardingCompletado(true);

        // Crear el usuario y guardar el cliente
        var usuarioGuardado = usuarioManagerService.prepararYGuardarUsuario(
                cliente.getUsuario(),
                "ROLE_CLIENTE"
        );

        cliente.setUsuario(usuarioGuardado);
        Cliente clienteGuardado = clienteRepository.save(cliente);
        
        // Enviar email de verificación (si falla, no cancela el registro)
        try {
            verificacionCorreoService.enviarCorreoVerificacion(usuarioGuardado);
        } catch (Exception e) {
            // Log del error pero no falla el registro
            System.err.println("⚠️ Error al enviar email de verificación a " + usuarioGuardado.getEmail() + ": " + e.getMessage());
            e.printStackTrace();
        }
        
        return clienteGuardado;
    }
}

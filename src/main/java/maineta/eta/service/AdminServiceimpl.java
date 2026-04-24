package maineta.eta.service;

import java.math.BigDecimal;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import maineta.eta.entity.Admin;
import maineta.eta.entity.Usuario;
import maineta.eta.repository.AdminRepository;
import maineta.eta.repository.RolRepository;
import maineta.eta.repository.UsuarioRepository;

@Service
public class AdminServiceimpl implements AdminService {

    private final UsuarioRepository usuarioRepository;
    private final RolRepository rolRepository;
    private final PasswordEncoder passwordEncoder;
    private final AdminRepository adminRepository;

    public  AdminServiceimpl(UsuarioRepository usuarioRepository,AdminRepository adminRepository,PasswordEncoder passwordEncoder,RolRepository rolRepository) {
        this.usuarioRepository = usuarioRepository;
        this.adminRepository = adminRepository;
        this.passwordEncoder = passwordEncoder;
        this.rolRepository = rolRepository;

    }

    @Override
    public Admin registrarAdmin(Admin admin){
        Usuario usuario = admin.getUsuario();

        // Validación de contraseña
        if (usuario.getPassword() == null || usuario.getPassword().isEmpty()) {
            throw new IllegalArgumentException("La contraseña no puede estar vacía");
        }

        // Encriptación de la contraseña
        usuario.setPassword(passwordEncoder.encode(usuario.getPassword()));

        // Asignación del rol COLABORADOR (ejemplo: ID = 2 en BD)
        rolRepository.findById(3L).ifPresent(rol -> usuario.setRol(rol));
            

        // Guardar primero el usuario
        usuarioRepository.save(usuario);

        // Luego guardar el colaborador vinculado al usuario
        return adminRepository.save(admin);
    }

    @Override
    public Admin buscarAdminPorId(long idAdmin) {
        return adminRepository.findByIdAdmin(idAdmin);
    }

    @Override
    public Admin obtenerAdminPrincipal() {
        return adminRepository.findTopByOrderByIdAdminAsc()
                .orElseThrow(() -> new IllegalStateException("No existe un administrador configurado"));
    }

    @Override
    public Admin actualizarPorcentajeComision(BigDecimal porcentajeComision) {
        Admin admin = obtenerAdminPrincipal();
        admin.setPorcentajeComision(porcentajeComision);
        return adminRepository.save(admin);
    }

    @Override
    public Admin actualizarHorasCancelacion(Integer horasCancelacion) {
        Admin admin = obtenerAdminPrincipal();
        admin.setHorasCancelacion(horasCancelacion);
        return adminRepository.save(admin);
    }
}

package maineta.eta.service;

import org.springframework.security.core.userdetails.UserDetailsService;

import maineta.eta.entity.Usuario;

/**
 * 🔹 Interfaz UsuarioService
 *
 * Esta interfaz extiende de UserDetailsService (propia de Spring Security),
 * lo que significa que además de nuestros métodos personalizados,
 * también se debe implementar el método `loadUserByUsername()` 
 * que Spring Security usa para autenticar usuarios.
 *
 * Aquí se definen las operaciones de servicio relacionadas con la entidad Usuario.
 */
public interface UsuarioService extends UserDetailsService {

    /**
     * 🔹 Buscar un usuario en la base de datos por su correo electrónico.
     *
     * @param email El email del usuario a buscar.
     * @return Optional que contiene el Usuario si existe, o vacío si no se encuentra.
     */

    Long ContadorUsuario();

    public Usuario obtenerPorEmail(String email);
}


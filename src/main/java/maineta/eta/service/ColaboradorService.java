package maineta.eta.service;

import java.util.List;
import java.util.Optional;

import maineta.eta.entity.Colaborador;
import maineta.eta.entity.Usuario;

/**
 * 🔹 Interfaz ColaboradorService
 *
 * Define el contrato (métodos) que cualquier implementación debe cumplir
 * para manejar la lógica de negocio relacionada con los colaboradores.
 *
 * Un "Colaborador" está asociado a un "Usuario", 
 * por lo que aquí también se contempla la búsqueda por Usuario.
 */
public interface ColaboradorService {

    /**
     * 🔹 Registrar un nuevo colaborador en el sistema.
     *
     * @param colaborador Objeto Colaborador que se quiere registrar.
     * @return Colaborador registrado en la base de datos.
     */
    Colaborador registrarColaborador(Colaborador colaborador);
    Long ContadorColaborador();
    /**
     * 🔹 Obtener todos los colaboradores registrados.
     *
     * @return Lista de todos los colaboradores almacenados en la BD.
     */
    List<Colaborador> findAll();

    /**
     * 🔹 Buscar un colaborador a partir de su Usuario.
     *
     * Esto es útil porque cada colaborador tiene un usuario asociado
     * (con email, contraseña y roles), y a veces la búsqueda se hace por Usuario.
     *
     * @param usuario Objeto Usuario asociado al colaborador.
     * @return Optional con el Colaborador encontrado, o vacío si no existe.
     */
    Optional<Colaborador> obtenerPorUsuario(Usuario usuario);
}

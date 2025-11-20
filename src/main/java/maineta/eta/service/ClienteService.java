package maineta.eta.service;

import java.util.List;
import java.util.Optional;

import maineta.eta.entity.Actividad;
import maineta.eta.entity.Cliente;
import maineta.eta.entity.Usuario;

/**
 * 🔹 Interfaz ClienteService
 *
 * Define las operaciones que se pueden realizar sobre los clientes.
 * 
 * ➡️ La interfaz establece el "contrato" (qué métodos debe tener el servicio).
 * ➡️ La implementación (ClienteServiceImpl) será la que indique "cómo" se hacen realmente.
 */
public interface ClienteService {
    Cliente obtenerPorId(Long id);
    /**
     * 🔹 Registrar un nuevo cliente en el sistema.
     * 
     * - Normalmente, aquí también se podría asociar el cliente con un usuario (Usuario).
     * - Devuelve el cliente guardado en la base de datos, ya con su ID asignado.
     *
     * @param cliente Objeto Cliente a registrar
     * @return Cliente registrado en la base de datos
     */
    Cliente registrarCliente(Cliente cliente);
    Long ContadorCliente();
    /**
     * 🔹 Listar todos los clientes existentes.
     *
     * - Permite traer la lista completa de clientes registrados en la BD.
     * - Se devuelve un List<Cliente>.
     *
     * @return Lista con todos los clientes
     */
    List<Cliente> findAll();
    Cliente actualizarCliente(Long id, Cliente cliente);
    Optional<Cliente> obtenerPorUsuario(Usuario usuario);
}


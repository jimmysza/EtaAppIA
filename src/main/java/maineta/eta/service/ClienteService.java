package maineta.eta.service;

import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.lang.NonNull;

import maineta.eta.dto.OnboardingForm;
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
    Page<Cliente> findAll(@NonNull Pageable pageable);
    Cliente actualizarCliente(Long id, Cliente cliente);
    Optional<Cliente> obtenerPorUsuario(Usuario usuario);
    
    /**
     * 🔹 Guardar las preferencias del onboarding del cliente.
     * 
     * @param cliente Cliente al que se le guardarán las preferencias
     * @param form Formulario con las respuestas del onboarding
     * @return Cliente actualizado con las preferencias guardadas
     */
    Cliente guardarPreferencias(Cliente cliente, OnboardingForm form);
    
    /**
     * 🔹 Registrar un nuevo cliente con sus preferencias de onboarding.
     * Este método combina el registro del cliente con las preferencias del onboarding.
     * 
     * @param cliente Cliente a registrar
     * @param form Formulario con las respuestas del onboarding
     * @return Cliente registrado con preferencias guardadas
     */
    Cliente registrarClienteConPreferencias(Cliente cliente, OnboardingForm form);
}


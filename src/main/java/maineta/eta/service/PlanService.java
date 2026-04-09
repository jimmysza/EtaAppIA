package maineta.eta.service;

import java.util.List;

import maineta.eta.dto.CrearPlanFormDTO;
import maineta.eta.dto.PlanDTO;
import maineta.eta.entity.Plan;

/**
 * Interfaz del servicio de Planes del Día.
 * 
 * Define los métodos para gestionar planes turísticos que agrupan
 * múltiples actividades en una ruta temática.
 */
public interface PlanService {

    /**
     * Obtiene los top 5 planes públicos más recientes.
     * 
     * @return Lista de DTOs con información de los planes
     */
    List<PlanDTO> obtenerTop5Recientes();

    /**
     * Obtiene el detalle completo de un plan por su ID.
     * Incrementa el contador de vistas.
     * 
     * @param id ID del plan
     * @return DTO con la información completa del plan incluyendo actividades
     */
    PlanDTO obtenerPorId(Long id);

    /**
     * Crea un nuevo plan desde un formulario.
     * 
     * @param form Formulario con datos del plan
     * @param idCreador ID del creador (Cliente o Colaborador)
     * @param rolCreador "CLIENTE" o "COLABORADOR"
     * @return Plan creado
     */
    Plan crearPlan(CrearPlanFormDTO form, Long idCreador, String rolCreador);

    /**
     * Incrementa el contador de vistas sin hacer SELECT previo.
     * 
     * @param idPlan ID del plan
     */
    void incrementarVistas(Long idPlan);

    /**
     * Obtiene todos los planes públicos creados por un cliente.
     * 
     * @param idCliente ID del cliente
     * @return Lista de DTOs
     */
    List<PlanDTO> obtenerPlanesPorCliente(Long idCliente);

    /**
     * Obtiene todos los planes públicos creados por un colaborador.
     * 
     * @param idColaborador ID del colaborador
     * @return Lista de DTOs
     */
    List<PlanDTO> obtenerPlanesPorColaborador(Long idColaborador);
}

package maineta.eta.service;

import java.util.Map;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.transaction.annotation.Transactional;

import maineta.eta.config.EpaycoConfig;
import maineta.eta.entity.Actividad;
import maineta.eta.entity.Cliente;
import maineta.eta.entity.Disponibilidad;
import maineta.eta.entity.Reserva;
import maineta.eta.repository.ReservaRepository;

/**
 * Servicio encargado de procesar las confirmaciones de pago desde ePayco.
 * Verifica firmas, valida pagos y crea reservas cuando el pago es exitoso.
 * 
 * ⚠️ DESHABILITADO: Migrado a Wompi. Bean desactivado para evitar errores de inyección.
 * Mantener código por si se requiere rollback.
 */
// @Service  // ⚠️ Deshabilitado tras migración a Wompi
public class EpaycoService {

    private static final Logger logger = LoggerFactory.getLogger(EpaycoService.class);

    private final EpaycoConfig epaycoConfig;
    private final ReservaService reservaService;
    private final ReservaRepository reservaRepository;
    private final DisponibilidadService disponibilidadService;
    private final ClienteService clienteService;
    private final ActividadService actividadService;

    public EpaycoService(
            EpaycoConfig epaycoConfig,
            ReservaService reservaService,
            ReservaRepository reservaRepository,
            DisponibilidadService disponibilidadService,
            ClienteService clienteService,
            ActividadService actividadService) {
        this.epaycoConfig = epaycoConfig;
        this.reservaService = reservaService;
        this.reservaRepository = reservaRepository;
        this.disponibilidadService = disponibilidadService;
        this.clienteService = clienteService;
        this.actividadService = actividadService;
    }

    /**
     * Procesa la confirmación de pago recibida desde ePayco.
     * 
     * @param params Parámetros enviados por ePayco en el webhook de confirmación
     * @throws Exception si hay errores de validación o procesamiento
     */
    @Transactional
    public void procesarConfirmacion(Map<String, String> params) throws Exception {
        
        // 1. Verificar firma
        if (!epaycoConfig.verificarFirma(params)) {
            logger.error("Firma inválida en confirmación de ePayco. Parámetros: {}", params);
            throw new SecurityException("Firma de ePayco inválida");
        }

        logger.info("Firma de ePayco verificada correctamente");

        // 2. Extraer parámetros
        String xRefPayco = params.get("x_ref_payco");
        String xTransactionState = params.get("x_transaction_state");
        String xAmount = params.get("x_amount");
        String xExtra1 = params.get("x_extra1"); // idDisponibilidad
        String xExtra2 = params.get("x_extra2"); // cantidad
        String xExtra3 = params.get("x_extra3"); // idActividad|idCliente

        logger.info("Procesando confirmación - Ref: {}, Estado: {}, Monto: {}", 
                    xRefPayco, xTransactionState, xAmount);

        // 3. Validar estado de la transacción
        if (!"Aceptada".equalsIgnoreCase(xTransactionState)) {
            if ("Rechazada".equalsIgnoreCase(xTransactionState) || "Fallida".equalsIgnoreCase(xTransactionState)) {
                logger.warn("Pago rechazado o fallido. Ref: {}, Estado: {}", xRefPayco, xTransactionState);
            } else if ("Pendiente".equalsIgnoreCase(xTransactionState)) {
                logger.info("Pago pendiente. Ref: {}, Estado: {}", xRefPayco, xTransactionState);
            }
            return; // No crear reserva si el pago no fue aceptado
        }

        // 4. Verificar idempotencia (que no exista una reserva con este refPayco)
        Optional<Reserva> reservaExistente = reservaRepository.findByRefPayco(xRefPayco);
        if (reservaExistente.isPresent()) {
            logger.info("Reserva ya existe para refPayco: {}. Ignorando confirmación duplicada.", xRefPayco);
            return;
        }

        // 5. Parsear extra3 para obtener idActividad e idCliente
        String[] extra3Parts = xExtra3.split("\\|");
        if (extra3Parts.length != 2) {
            throw new IllegalArgumentException("Formato inválido en x_extra3. Esperado: idActividad|idCliente");
        }

        Long idActividad = Long.parseLong(extra3Parts[0]);
        Long idCliente = Long.parseLong(extra3Parts[1]);
        Long idDisponibilidad = Long.parseLong(xExtra1);
        int cantidad = Integer.parseInt(xExtra2);

        // 6. Obtener entidades necesarias
        Disponibilidad disponibilidad = disponibilidadService.obtenerDisponibilidadPorId(idDisponibilidad)
                .orElseThrow(() -> new Exception("Disponibilidad no encontrada: " + idDisponibilidad));

        Cliente cliente = clienteService.obtenerPorId(idCliente);
        if (cliente == null) {
            throw new Exception("Cliente no encontrado: " + idCliente);
        }

        Actividad actividad = actividadService.listarById(idActividad);
        if (actividad == null) {
            throw new Exception("Actividad no encontrada: " + idActividad);
        }

        // 7. Crear la reserva con refPayco
        logger.info("Creando reserva desde ePayco. Cliente: {}, Actividad: {}, Cantidad: {}", 
                    idCliente, idActividad, cantidad);

        reservaService.crearReservaDesdeEpayco(
            idDisponibilidad, 
            idCliente, 
            idActividad, 
            cantidad, 
            xRefPayco
        );

        logger.info("Reserva creada exitosamente desde ePayco. Ref: {}", xRefPayco);
    }
}

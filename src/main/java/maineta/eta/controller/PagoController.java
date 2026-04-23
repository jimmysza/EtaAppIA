package maineta.eta.controller;

import java.util.Map;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import maineta.eta.entity.Reserva;
import maineta.eta.repository.ReservaRepository;
import maineta.eta.service.EpaycoService;

/**
 * Controller que maneja los webhooks y respuestas de ePayco.
 * 
 * - POST /cliente/pago/confirmacion → Endpoint público llamado por ePayco (server-to-server)
 * - GET /cliente/pago/respuesta → Endpoint protegido donde llega el cliente tras el pago
 */
@Controller
@RequestMapping("/cliente/pago")
public class PagoController {

    private static final Logger logger = LoggerFactory.getLogger(PagoController.class);

    private final EpaycoService epaycoService;
    private final ReservaRepository reservaRepository;

    public PagoController(EpaycoService epaycoService, ReservaRepository reservaRepository) {
        this.epaycoService = epaycoService;
        this.reservaRepository = reservaRepository;
    }

    /**
     * Endpoint de confirmación llamado por ePayco (webhook server-to-server).
     * DEBE ser público (sin autenticación).
     * 
     * @param allParams Todos los parámetros enviados por ePayco en formato form-urlencoded
     * @return HTTP 200 OK siempre (ePayco espera 200 para confirmar recepción)
     */
    @PostMapping("/confirmacion")
    public ResponseEntity<String> confirmacion(@RequestParam Map<String, String> allParams) {
        
        logger.info("Confirmación recibida de ePayco: {}", allParams);

        try {
            // Procesar la confirmación (verifica firma, crea reserva si pago exitoso)
            epaycoService.procesarConfirmacion(allParams);
            return ResponseEntity.ok("OK");

        } catch (SecurityException e) {
            // Firma inválida → devolver 400 Bad Request
            logger.error("Firma inválida en confirmación de ePayco", e);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Firma inválida");

        } catch (Exception e) {
            // Otros errores → loguear pero devolver 200 OK para que ePayco no reintente
            logger.error("Error al procesar confirmación de ePayco", e);
            return ResponseEntity.ok("ERROR_PROCESADO");
        }
    }

    /**
     * Endpoint de respuesta al que llega el cliente tras completar el pago en ePayco.
     * Protegido con ROLE_CLIENTE.
     * 
     * @param refPayco Referencia del pago en ePayco
     * @param xTransactionState Estado de la transacción
     * @param xExtra1 idDisponibilidad
     * @param xExtra3 idActividad|idCliente
     * @param redirectAttributes Para pasar mensajes de alerta
     * @return Redirección al dashboard o al checkout según el estado
     */
    @GetMapping("/respuesta")
    public String respuesta(
            @RequestParam(name = "ref_payco", required = false) String refPayco,
            @RequestParam(name = "x_transaction_state", required = false) String xTransactionState,
            @RequestParam(name = "x_extra1", required = false) String xExtra1,
            @RequestParam(name = "x_extra3", required = false) String xExtra3,
            RedirectAttributes redirectAttributes) {

        logger.info("Respuesta de ePayco - Ref: {}, Estado: {}", refPayco, xTransactionState);

        if (xTransactionState == null || refPayco == null) {
            logger.warn("Parámetros incompletos en respuesta de ePayco");
            return "redirect:/cliente/dashboard?pago=error";
        }

        // Parsear extra3 para obtener idActividad
        Long idActividad = null;
        if (xExtra3 != null && xExtra3.contains("|")) {
            String[] parts = xExtra3.split("\\|");
            if (parts.length == 2) {
                try {
                    idActividad = Long.parseLong(parts[0]);
                } catch (NumberFormatException e) {
                    logger.error("Error al parsear idActividad de x_extra3: {}", xExtra3);
                }
            }
        }

        // Manejar según estado
        switch (xTransactionState.toLowerCase()) {
            case "aceptada":
                // Verificar si la reserva ya fue creada
                Optional<Reserva> reserva = reservaRepository.findByRefPayco(refPayco);
                if (reserva.isPresent()) {
                    logger.info("Pago exitoso y reserva creada. Ref: {}", refPayco);
                    return "redirect:/cliente/dashboard?pago=exitoso";
                } else {
                    // El webhook de confirmación aún no llegó
                    logger.info("Pago aceptado pero reserva aún no creada. Ref: {}", refPayco);
                    return "redirect:/cliente/dashboard?pago=procesando";
                }

            case "rechazada":
            case "fallida":
                logger.warn("Pago rechazado/fallido. Ref: {}, Estado: {}", refPayco, xTransactionState);
                if (idActividad != null && xExtra1 != null) {
                    return String.format("redirect:/cliente/checkout/actividad/%d?pago=fallido&idDispo=%s", 
                                       idActividad, xExtra1);
                }
                return "redirect:/cliente/dashboard?pago=fallido";

            case "pendiente":
                logger.info("Pago pendiente. Ref: {}", refPayco);
                return "redirect:/cliente/dashboard?pago=pendiente";

            default:
                logger.warn("Estado de transacción desconocido: {}", xTransactionState);
                return "redirect:/cliente/dashboard?pago=desconocido";
        }
    }
}

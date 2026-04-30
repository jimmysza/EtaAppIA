package maineta.eta.controller;

import java.security.Principal;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import maineta.eta.entity.Actividad;
import maineta.eta.entity.Cliente;
import maineta.eta.entity.Disponibilidad;
import maineta.eta.entity.PagoIntento;
import maineta.eta.entity.Usuario;
import maineta.eta.service.ActividadService;
import maineta.eta.service.ClienteService;
import maineta.eta.service.DisponibilidadService;
import maineta.eta.service.ReservaService;
import maineta.eta.service.UsuarioService;
import maineta.eta.service.WompiService;

/**
 * Controller que maneja los pagos a través de Wompi.
 * 
 * - GET /cliente/pago/iniciar → Redirige directamente a checkout.wompi.co con URL completa
 * - POST /cliente/pago/webhook → Endpoint público llamado por Wompi (server-to-server)
 * - GET /cliente/pago/respuesta → Endpoint donde llega el cliente tras completar el pago en Wompi
 */
@Controller
@RequestMapping("/cliente/pago")
public class PagoController {

    private static final Logger logger = LoggerFactory.getLogger(PagoController.class);

    private final WompiService wompiService;
    private final UsuarioService usuarioService;
    private final ClienteService clienteService;
    private final ReservaService reservaService;
    private final ActividadService actividadService;
    private final DisponibilidadService disponibilidadService;
    
    @Value("${pago.enabled:false}")
    private boolean pagoEnabled;

    public PagoController(WompiService wompiService, UsuarioService usuarioService, 
                         ClienteService clienteService, ReservaService reservaService,
                         ActividadService actividadService, DisponibilidadService disponibilidadService) {
        this.wompiService = wompiService;
        this.usuarioService = usuarioService;
        this.clienteService = clienteService;
        this.reservaService = reservaService;
        this.actividadService = actividadService;
        this.disponibilidadService = disponibilidadService;
    }

    /**
     * Endpoint que redirige directamente a la página de pago de Wompi.
     * Protegido con ROLE_CLIENTE.
     * 
     * @param idDisponibilidad ID de la disponibilidad a reservar
     * @param idActividad ID de la actividad
     * @param cantidad Cantidad de cupos a reservar
     * @param principal Usuario autenticado
     * @param redirectAttributes Para pasar mensajes de error
     * @return Redirect a la URL de pago de Wompi
     */
    @GetMapping("/iniciar")
    @PreAuthorize("hasRole('CLIENTE')")
    public String iniciarPago(
            @RequestParam Long idDisponibilidad,
            @RequestParam Long idActividad,
            @RequestParam Integer cantidad,
            Principal principal,
            RedirectAttributes redirectAttributes) {

        logger.info("Iniciando pago Wompi - Usuario: {}, Actividad: {}, Cantidad: {}", 
                    principal.getName(), idActividad, cantidad);

        try {
            // Obtener usuario autenticado
            Usuario usuario = usuarioService.obtenerPorEmail(principal.getName());
            if (usuario == null) {
                logger.error("Usuario no encontrado: {}", principal.getName());
                redirectAttributes.addFlashAttribute("error", "Usuario no encontrado");
                return "redirect:/cliente/dashboard";
            }

            // Obtener cliente asociado al usuario
            Cliente cliente = clienteService.obtenerPorUsuario(usuario)
                .orElseThrow(() -> new RuntimeException("Cliente no encontrado para usuario: " + usuario.getEmail()));

            // ⚠️ MODO SIN PAGO: Crear reserva directamente (pago.enabled=false)
            if (!pagoEnabled) {
                logger.info("Modo sin pago activado - Creando reserva directamente");
                try {
                    // Obtener entidades necesarias
                    Actividad actividad = actividadService.obtenerPorId(idActividad);
                    if (actividad == null) {
                        throw new RuntimeException("Actividad no encontrada");
                    }
                    
                    Optional<Disponibilidad> dispOpt = disponibilidadService.obtenerPorId(idDisponibilidad);
                    if (!dispOpt.isPresent()) {
                        throw new RuntimeException("Disponibilidad no encontrada");
                    }
                    Disponibilidad disponibilidad = dispOpt.get();
                    
                    // Crear reserva sin pago
                    reservaService.hacerReserva(cliente, actividad, disponibilidad, cantidad);
                    redirectAttributes.addFlashAttribute("success", "¡Reserva creada exitosamente! (Modo sin pago)");
                    return "redirect:/cliente/dashboard";
                } catch (Exception e) {
                    logger.error("Error al crear reserva sin pago", e);
                    redirectAttributes.addFlashAttribute("error", "Error al crear reserva: " + e.getMessage());
                    return String.format("redirect:/cliente/checkout/actividad/%d?idDispo=%d", 
                                       idActividad, idDisponibilidad);
                }
            }
            
            // ✅ MODO CON PAGO: Redirigir a Wompi (pago.enabled=true)
            logger.info("Modo con pago activado - Generando URL de Wompi");
            String urlWompi = wompiService.generarUrlPago(
                idDisponibilidad,
                cliente.getId(),
                idActividad,
                cantidad,
                usuario.getEmail(),
                usuario.getNombre(),
                usuario.getTelefono()
            );

            logger.info("Redirigiendo a Wompi - URL generada (longitud: {})", urlWompi.length());

            // Redirigir directamente a Wompi (sin renderizar template)
            return "redirect:" + urlWompi;

        } catch (Exception e) {
            logger.error("Error al generar URL de pago de Wompi", e);
            redirectAttributes.addFlashAttribute("error", "Error al procesar el pago: " + e.getMessage());
            return String.format("redirect:/cliente/checkout/actividad/%d?idDispo=%d", 
                               idActividad, idDisponibilidad);
        }
    }

    /**
     * Endpoint de webhook llamado por Wompi (server-to-server).
     * DEBE ser público (sin autenticación).
     * 
     * @param body JSON enviado por Wompi
     * @param checksumHeader Hash de verificación enviado en el header "wompi-signature-checksum"
     * @return HTTP 200 OK siempre (Wompi espera 200 para confirmar recepción)
     */
    @PostMapping("/webhook")
    public ResponseEntity<String> webhook(
            @RequestBody String body,
            @RequestHeader(name = "wompi-signature-checksum", required = false) String checksumHeader) {
        
        logger.info("Webhook recibido de Wompi");
        logger.debug("Body: {}", body);
        logger.debug("Checksum: {}", checksumHeader);

        try {
            // Procesar el webhook (verifica firma, crea reserva si pago exitoso)
            wompiService.procesarWebhook(body, checksumHeader);
            return ResponseEntity.ok("OK");

        } catch (SecurityException e) {
            // Firma inválida → devolver 401 Unauthorized
            logger.error("Firma inválida en webhook de Wompi", e);
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Firma inválida");

        } catch (Exception e) {
            // Otros errores → loguear pero devolver 200 OK para que Wompi no reintente
            logger.error("Error al procesar webhook de Wompi", e);
            return ResponseEntity.ok("ERROR_PROCESADO");
        }
    }

    /**
     * Endpoint de respuesta al que llega el cliente tras completar el pago en Wompi.
     * Protegido con ROLE_CLIENTE.
     * 
     * @param reference Referencia del pago (query param: ?ref=ETA-123-456-789)
     * @param redirectAttributes Para pasar mensajes de alerta
     * @return Redirección al dashboard o al checkout según el estado
     */
    @GetMapping("/respuesta")
    @PreAuthorize("hasRole('CLIENTE')")
    public String respuesta(
            @RequestParam(name = "ref", required = false) String reference,
            RedirectAttributes redirectAttributes) {

        logger.info("Respuesta de Wompi - Reference: {}", reference);

        if (reference == null || reference.isEmpty()) {
            logger.warn("Parámetros incompletos en respuesta de Wompi");
            return "redirect:/cliente/dashboard?pago=error";
        }

        // Buscar PagoIntento por referencia
        Optional<PagoIntento> pagoIntentoOpt = wompiService.obtenerPorReferencia(reference);
        
        if (!pagoIntentoOpt.isPresent()) {
            logger.warn("PagoIntento no encontrado para referencia: {}", reference);
            return "redirect:/cliente/dashboard?pago=error";
        }

        PagoIntento pagoIntento = pagoIntentoOpt.get();

        // Redirigir según estado del PagoIntento
        switch (pagoIntento.getEstado()) {
            case "PROCESADO":
                logger.info("Pago procesado exitosamente. Ref: {}", reference);
                return "redirect:/cliente/dashboard?pago=exitoso";

            case "PENDIENTE":
                logger.info("Pago pendiente de confirmación. Ref: {}", reference);
                return "redirect:/cliente/dashboard?pago=procesando";

            case "FALLIDO":
                logger.warn("Pago fallido. Ref: {}", reference);
                return String.format("redirect:/cliente/checkout/actividad/%d?pago=fallido&idDispo=%d", 
                                   pagoIntento.getIdActividad(), pagoIntento.getIdDisponibilidad());

            default:
                logger.warn("Estado de PagoIntento desconocido: {}. Ref: {}", pagoIntento.getEstado(), reference);
                return "redirect:/cliente/dashboard?pago=desconocido";
        }
    }
}

package maineta.eta.service;

import java.io.UnsupportedEncodingException;
import java.math.BigDecimal;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import maineta.eta.config.UsuarioHelper;
import maineta.eta.config.WompiConfig;
import maineta.eta.entity.Actividad;
import maineta.eta.entity.Disponibilidad;
import maineta.eta.entity.PagoIntento;
import maineta.eta.repository.PagoIntentoRepository;

/**
 * Servicio encargado de gestionar pagos a través de Wompi.
 * 
 * Responsabilidades:
 * - Generar URLs de pago firmadas para redirigir al cliente
 * - Procesar webhooks de confirmación de Wompi
 * - Verificar firmas de seguridad
 * - Crear reservas cuando el pago es exitoso
 */
@Service
public class WompiService {

    private static final Logger logger = LoggerFactory.getLogger(WompiService.class);

    private final WompiConfig wompiConfig;
    private final PagoIntentoRepository pagoIntentoRepository;
    private final ActividadService actividadService;
    private final DisponibilidadService disponibilidadService;
    private final ReservaService reservaService;
    private final UsuarioHelper usuarioHelper;
    private final ObjectMapper objectMapper;

    @Value("${app.base-url:http://localhost:8080}")
    private String appBaseUrl;

    public WompiService(
            WompiConfig wompiConfig,
            PagoIntentoRepository pagoIntentoRepository,
            ActividadService actividadService,
            DisponibilidadService disponibilidadService,
            ReservaService reservaService,
            UsuarioHelper usuarioHelper,
            ObjectMapper objectMapper) {
        this.wompiConfig = wompiConfig;
        this.pagoIntentoRepository = pagoIntentoRepository;
        this.actividadService = actividadService;
        this.disponibilidadService = disponibilidadService;
        this.reservaService = reservaService;
        this.usuarioHelper = usuarioHelper;
        this.objectMapper = objectMapper;
    }

    /**
     * Crea un PagoIntento con todos los datos necesarios para el widget de Wompi.
     * 
     * @param idDisponibilidad ID de la disponibilidad a reservar
     * @param idCliente ID del cliente que realiza la reserva
     * @param idActividad ID de la actividad
     * @param cantidad Cantidad de cupos a reservar
     * @param clienteEmail Email del cliente (para logging)
     * @param clienteNombre Nombre del cliente (para logging)
     * @param clienteTelefono Teléfono del cliente (para logging)
     * @return PagoIntento creado y guardado en BD
     * @throws Exception si hay errores de validación o procesamiento
     */
    @Transactional
    public PagoIntento crearPagoIntento(Long idDisponibilidad, Long idCliente, Long idActividad, 
                                 Integer cantidad, String clienteEmail, String clienteNombre, 
                                 String clienteTelefono) throws Exception {
        
        logger.info("Creando PagoIntento - Actividad: {}, Cliente: {}, Cantidad: {}", 
                    idActividad, idCliente, cantidad);

        // 1. Validar y obtener actividad
        Actividad actividad = actividadService.listarById(idActividad);
        if (actividad == null) {
            throw new Exception("Actividad no encontrada con ID: " + idActividad);
        }

        // 2. Validar y obtener disponibilidad
        Disponibilidad disponibilidad = disponibilidadService.obtenerPorId(idDisponibilidad)
                .orElseThrow(() -> new Exception("Disponibilidad no encontrada con ID: " + idDisponibilidad));

        // 3. Verificar cupos disponibles
        if (disponibilidad.getCuposDisponibles() < cantidad) {
            throw new Exception("No hay suficientes cupos. Disponibles: " + disponibilidad.getCuposDisponibles());
        }

        // 4. Calcular precio consumidor (con comisión de ETA)
        BigDecimal precioColaborador = actividad.getPrecio();
        BigDecimal precioConsumidor = usuarioHelper.CalcularPrecioConsumidor(precioColaborador);
        BigDecimal totalBigDecimal = precioConsumidor.multiply(BigDecimal.valueOf(cantidad));
        
        // 5. Convertir a centavos (Wompi requiere el monto en centavos)
        Long amountInCents = totalBigDecimal.multiply(BigDecimal.valueOf(100)).longValue();

        // 6. Generar referencia única: "ETA-{idDispo}-{idCliente}-{timestamp}"
        String timestamp = String.valueOf(System.currentTimeMillis());
        String reference = String.format("ETA-%d-%d-%s", idDisponibilidad, idCliente, timestamp);

        logger.info("Referencia generada: {} - Monto en centavos: {}", reference, amountInCents);

        // 7. Guardar PagoIntento en BD para recuperar contexto en el widget y webhook
        PagoIntento pagoIntento = new PagoIntento();
        pagoIntento.setReference(reference);
        pagoIntento.setIdDisponibilidad(idDisponibilidad);
        pagoIntento.setIdCliente(idCliente);
        pagoIntento.setIdActividad(idActividad);
        pagoIntento.setCantidad(cantidad);
        pagoIntento.setAmountInCents(amountInCents);
        pagoIntento.setEstado("PENDIENTE");
        pagoIntento.setCreatedAt(LocalDateTime.now());
        pagoIntentoRepository.save(pagoIntento);

        logger.info("PagoIntento guardado con ID: {}", pagoIntento.getId());

        return pagoIntento;
    }

    /**
     * Genera una URL de pago de Wompi con todos los parámetros necesarios y firma de integridad.
     * Esta URL redirige al cliente directamente a checkout.wompi.co.
     * 
     * @param idDisponibilidad ID de la disponibilidad a reservar
     * @param idCliente ID del cliente que realiza la reserva
     * @param idActividad ID de la actividad
     * @param cantidad Cantidad de cupos a reservar
     * @param clienteEmail Email del cliente
     * @param clienteNombre Nombre completo del cliente
     * @param clienteTelefono Teléfono del cliente
     * @return URL completa de pago de Wompi
     * @throws Exception si hay errores de validación o procesamiento
     */
    @Transactional
    public String generarUrlPago(Long idDisponibilidad, Long idCliente, Long idActividad,
                                 Integer cantidad, String clienteEmail, String clienteNombre, 
                                 String clienteTelefono) throws Exception {
        
        logger.info("Generando URL de pago de Wompi - Actividad: {}, Cliente: {}, Cantidad: {}", 
                    idActividad, idCliente, cantidad);

        // 1. Validar y obtener actividad
        Actividad actividad = actividadService.listarById(idActividad);
        if (actividad == null) {
            throw new Exception("Actividad no encontrada con ID: " + idActividad);
        }

        // 2. Validar y obtener disponibilidad
        Disponibilidad disponibilidad = disponibilidadService.obtenerPorId(idDisponibilidad)
                .orElseThrow(() -> new Exception("Disponibilidad no encontrada con ID: " + idDisponibilidad));

        // 3. Verificar cupos disponibles
        if (disponibilidad.getCuposDisponibles() < cantidad) {
            throw new Exception("No hay suficientes cupos. Disponibles: " + disponibilidad.getCuposDisponibles());
        }

        // 4. Calcular precio consumidor (con comisión de ETA)
        BigDecimal precioColaborador = actividad.getPrecio();
        BigDecimal precioConsumidor = usuarioHelper.CalcularPrecioConsumidor(precioColaborador);
        BigDecimal totalBigDecimal = precioConsumidor.multiply(BigDecimal.valueOf(cantidad));
        
        // 5. Convertir a centavos (Wompi requiere el monto en centavos)
        Long amountInCents = totalBigDecimal.multiply(BigDecimal.valueOf(100)).longValue();

        // 6. Generar referencia única: "ETA-{idDispo}-{idCliente}-{timestamp}"
        String timestamp = String.valueOf(System.currentTimeMillis());
        String reference = String.format("ETA-%d-%d-%s", idDisponibilidad, idCliente, timestamp);

        logger.info("Referencia generada: {} - Monto en centavos: {}", reference, amountInCents);

        // 7. Verificar que no exista PagoIntento con la misma referencia (por seguridad)
        Optional<PagoIntento> existente = pagoIntentoRepository.findByReference(reference);
        if (existente.isPresent()) {
            logger.warn("Ya existe un PagoIntento con referencia: {}", reference);
            throw new Exception("Ya existe un intento de pago con esta referencia");
        }

        // 8. Guardar PagoIntento en BD ANTES de redirigir
        PagoIntento pagoIntento = new PagoIntento();
        pagoIntento.setReference(reference);
        pagoIntento.setIdDisponibilidad(idDisponibilidad);
        pagoIntento.setIdCliente(idCliente);
        pagoIntento.setIdActividad(idActividad);
        pagoIntento.setCantidad(cantidad);
        pagoIntento.setAmountInCents(amountInCents);
        pagoIntento.setEstado("PENDIENTE");
        pagoIntento.setCreatedAt(LocalDateTime.now());
        pagoIntentoRepository.save(pagoIntento);

        logger.info("PagoIntento guardado con ID: {}", pagoIntento.getId());

        // 9. Calcular firma de integridad
        String currency = "COP";
        String integrity = wompiConfig.calcularIntegridad(reference, amountInCents.toString(), currency);

        logger.info("Hash de integridad calculado: {}", integrity);

        // 10. Construir URL de redirect de vuelta a ETA
        String redirectUrl = appBaseUrl + "/cliente/pago/respuesta?ref=" + encode(reference);

        // 11. Construir URL completa de Wompi
        StringBuilder urlBuilder = new StringBuilder(wompiConfig.getBaseUrl());
        urlBuilder.append("?public-key=").append(encode(wompiConfig.getPublicKey()));
        urlBuilder.append("&currency=").append(currency);
        urlBuilder.append("&amount-in-cents=").append(amountInCents);
        urlBuilder.append("&reference=").append(encode(reference));
        urlBuilder.append("&signature:integrity=").append(integrity);
        urlBuilder.append("&redirect-url=").append(encode(redirectUrl));
        
        // Agregar datos del cliente
        urlBuilder.append("&customer-data:email=").append(encode(clienteEmail));
        urlBuilder.append("&customer-data:full-name=").append(encode(clienteNombre));
        
        if (clienteTelefono != null && !clienteTelefono.isEmpty()) {
            urlBuilder.append("&customer-data:phone-number=").append(encode(clienteTelefono));
            urlBuilder.append("&customer-data:phone-number-prefix=%2B57"); // +57 para Colombia
        }

        String urlPago = urlBuilder.toString();
        logger.info("URL de pago generada (longitud: {})", urlPago.length());
        logger.debug("URL completa: {}", urlPago);

        return urlPago;
    }

    /**
     * Procesa el webhook de confirmación de Wompi.
     * Verifica la firma, valida el estado del pago y crea la reserva si es exitoso.
     * 
     * @param bodyJson JSON del webhook enviado por Wompi
     * @param checksumHeader Hash de verificación enviado en el header "wompi-signature-checksum"
     * @throws Exception si hay errores de procesamiento
     */
    @Transactional
    public void procesarWebhook(String bodyJson, String checksumHeader) throws Exception {
        
        logger.info("Procesando webhook de Wompi");
        logger.debug("Body: {}", bodyJson);

        // 1. Parsear JSON del webhook
        JsonNode root = objectMapper.readTree(bodyJson);
        
        String event = root.path("event").asText();
        JsonNode transaction = root.path("data").path("transaction");
        
        String transactionId = transaction.path("id").asText();
        String reference = transaction.path("reference").asText();
        String status = transaction.path("status").asText();
        Long amountInCents = transaction.path("amount_in_cents").asLong();
        Long timestamp = root.path("timestamp").asLong();

        logger.info("Webhook - Evento: {}, Ref: {}, Estado: {}, Monto: {}", 
                    event, reference, status, amountInCents);

        // 2. Verificar firma del webhook
        if (checksumHeader == null || checksumHeader.isEmpty()) {
            logger.error("Webhook sin checksum en header");
            throw new SecurityException("Checksum no presente en header del webhook");
        }

        boolean firmaValida = wompiConfig.verificarFirmaWebhook(
            transactionId, status, amountInCents, timestamp, checksumHeader);

        if (!firmaValida) {
            logger.error("Firma inválida en webhook de Wompi. Checksum recibido: {}", checksumHeader);
            throw new SecurityException("Firma de webhook Wompi inválida");
        }

        logger.info("Firma del webhook verificada correctamente");

        // 3. Buscar PagoIntento por referencia
        Optional<PagoIntento> pagoIntentoOpt = pagoIntentoRepository.findByReference(reference);
        
        if (!pagoIntentoOpt.isPresent()) {
            logger.warn("PagoIntento no encontrado para referencia: {}. Webhook ignorado.", reference);
            return;
        }

        PagoIntento pagoIntento = pagoIntentoOpt.get();

        // 4. Verificar idempotencia (si ya fue procesado, no hacer nada)
        if ("PROCESADO".equals(pagoIntento.getEstado())) {
            logger.info("PagoIntento {} ya fue procesado anteriormente. Ignorando webhook duplicado.", reference);
            return;
        }

        // 5. Actualizar wompiTransactionId
        pagoIntento.setWompiTransactionId(transactionId);

        // 6. Manejar según estado de la transacción
        switch (status) {
            case "APPROVED":
                logger.info("Pago aprobado. Creando reserva...");
                
                try {
                    // Crear reserva usando el servicio existente
                    reservaService.crearReservaDesdeWompi(
                        pagoIntento.getIdDisponibilidad(),
                        pagoIntento.getIdCliente(),
                        pagoIntento.getIdActividad(),
                        pagoIntento.getCantidad(),
                        reference,
                        transactionId
                    );

                    // Actualizar estado del PagoIntento
                    pagoIntento.setEstado("PROCESADO");
                    pagoIntentoRepository.save(pagoIntento);

                    logger.info("Reserva creada exitosamente para referencia: {}", reference);

                } catch (Exception e) {
                    logger.error("Error al crear reserva desde webhook Wompi", e);
                    pagoIntento.setEstado("FALLIDO");
                    pagoIntentoRepository.save(pagoIntento);
                    throw e;
                }
                break;

            case "DECLINED":
            case "ERROR":
            case "VOIDED":
                logger.warn("Pago rechazado/fallido. Estado: {}. Ref: {}", status, reference);
                pagoIntento.setEstado("FALLIDO");
                pagoIntentoRepository.save(pagoIntento);
                break;

            default:
                logger.info("Estado de transacción no manejado: {}. Ref: {}", status, reference);
                // No cambiar estado, mantener PENDIENTE
        }
    }

    /**
     * Obtiene un PagoIntento por su referencia.
     * Útil para el endpoint de respuesta del cliente tras el pago.
     * 
     * @param reference Referencia del pago
     * @return Optional con el PagoIntento si existe
     */
    public Optional<PagoIntento> obtenerPorReferencia(String reference) {
        return pagoIntentoRepository.findByReference(reference);
    }

    /**
     * Codifica un parámetro para URL.
     * 
     * @param value Valor a codificar
     * @return Valor codificado para URL
     */
    private String encode(String value) {
        try {
            return URLEncoder.encode(value, StandardCharsets.UTF_8.toString());
        } catch (UnsupportedEncodingException e) {
            throw new RuntimeException("Error al codificar parámetro URL", e);
        }
    }
}

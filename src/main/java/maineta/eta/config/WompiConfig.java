package maineta.eta.config;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

import lombok.Getter;

/**
 * Configuración de credenciales de Wompi.
 * Lee las claves desde application.properties y proporciona utilidades para:
 * - Calcular hash de integridad para URLs de pago
 * - Verificar firmas de webhooks
 */
@Configuration
@Getter
public class WompiConfig {

    @Value("${wompi.public.key}")
    private String publicKey;

    @Value("${wompi.private.key}")
    private String privateKey;

    @Value("${wompi.events.key}")
    private String eventsKey;

    @Value("${wompi.integrity.key}")
    private String integrityKey;

    @Value("${wompi.test:true}")
    private boolean test;

    @Value("${wompi.base.url:https://checkout.wompi.co/p/}")
    private String baseUrl;

    /**
     * Calcula el hash de integridad requerido por Wompi para validar transacciones.
     * 
     * Hash = SHA256(reference + amountInCents + currency + integrityKey)
     * 
     * @param reference Referencia única de la transacción (ej: "ETA-123-456-789")
     * @param amountInCents Monto en centavos como String (ej: "11800000")
     * @param currency Código de moneda (ej: "COP")
     * @return Hash SHA-256 en formato hexadecimal
     */
    public String calcularIntegridad(String reference, String amountInCents, String currency) {
        try {
            String data = reference + amountInCents + currency + integrityKey;
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(data.getBytes(StandardCharsets.UTF_8));

            // Convertir a hexadecimal
            StringBuilder hexString = new StringBuilder();
            for (byte b : hash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) {
                    hexString.append('0');
                }
                hexString.append(hex);
            }

            return hexString.toString();

        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("Error al calcular SHA-256 para integridad de Wompi", e);
        }
    }

    /**
     * Verifica la firma de un webhook de Wompi.
     * 
     * Firma esperada: SHA256(transaction.id + transaction.status + transaction.amount_in_cents + timestamp + eventsKey)
     * 
     * @param transactionId ID de la transacción en Wompi
     * @param status Estado de la transacción
     * @param amountInCents Monto en centavos
     * @param timestamp Timestamp del evento
     * @param checksumRecibido Checksum recibido en el header del webhook
     * @return true si la firma es válida, false en caso contrario
     */
    public boolean verificarFirmaWebhook(String transactionId, String status, 
                                         Long amountInCents, Long timestamp, String checksumRecibido) {
        try {
            String data = transactionId + status + amountInCents.toString() + 
                         timestamp.toString() + eventsKey;
            
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(data.getBytes(StandardCharsets.UTF_8));

            // Convertir a hexadecimal
            StringBuilder hexString = new StringBuilder();
            for (byte b : hash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) {
                    hexString.append('0');
                }
                hexString.append(hex);
            }

            String calculatedChecksum = hexString.toString();

            return calculatedChecksum.equalsIgnoreCase(checksumRecibido);

        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("Error al verificar firma de webhook Wompi", e);
        }
    }
}

package maineta.eta.config;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Map;

import org.springframework.beans.factory.annotation.Value;

import lombok.Getter;

/**
 * Configuración de credenciales de ePayco.
 * Lee las claves desde application.properties y proporciona utilidades para verificar firmas.
 * 
 * ⚠️ DESHABILITADO: Migrado a Wompi. Bean desactivado para evitar errores de inyección.
 * Mantener código por si se requiere rollback.
 */
// @Configuration  // ⚠️ Deshabilitado tras migración a Wompi
@Getter
public class EpaycoConfig {

    @Value("${epayco.public.key}")
    private String publicKey;

    @Value("${epayco.private.key}")
    private String privateKey;

    @Value("${epayco.client.id}")
    private String clientId;

    @Value("${epayco.test:true}")
    private boolean test;

    /**
     * Verifica la firma de ePayco para validar que la respuesta es legítima.
     * 
     * Firma esperada: SHA-256 de clientId^publicKey^x_ref_payco^x_transaction_id^x_amount^x_currency_code
     * 
     * @param params Mapa de parámetros recibidos de ePayco
     * @return true si la firma es válida, false en caso contrario
     */
    public boolean verificarFirma(Map<String, String> params) {
        try {
            String xRefPayco = params.get("x_ref_payco");
            String xTransactionId = params.get("x_transaction_id");
            String xAmount = params.get("x_amount");
            String xCurrencyCode = params.get("x_currency_code");
            String xSignature = params.get("x_signature");

            if (xRefPayco == null || xTransactionId == null || xAmount == null || 
                xCurrencyCode == null || xSignature == null) {
                return false;
            }

            // Construir cadena para hash
            String data = clientId + "^" + publicKey + "^" + xRefPayco + "^" + 
                         xTransactionId + "^" + xAmount + "^" + xCurrencyCode;

            // Calcular SHA-256
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(data.getBytes(StandardCharsets.UTF_8));

            // Convertir a hexadecimal
            StringBuilder hexString = new StringBuilder();
            for (byte b : hash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) hexString.append('0');
                hexString.append(hex);
            }

            String calculatedSignature = hexString.toString();

            return calculatedSignature.equalsIgnoreCase(xSignature);

        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("Error al calcular SHA-256 para verificación de firma ePayco", e);
        }
    }
}
